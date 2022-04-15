package com.better.alarm

import com.better.alarm.AlarmSchedulerTest.SetterMock
import com.better.alarm.DatabaseQueryMock.Companion.createStub
import com.better.alarm.DatabaseQueryMock.Companion.createWithFactory
import com.better.alarm.configuration.Prefs
import com.better.alarm.configuration.Prefs.Companion.create
import com.better.alarm.configuration.Store
import com.better.alarm.interfaces.IAlarmsManager
import com.better.alarm.interfaces.Intents
import com.better.alarm.logger.Logger
import com.better.alarm.model.AlarmCore
import com.better.alarm.model.AlarmCore.IStateNotifier
import com.better.alarm.model.AlarmCoreFactory
import com.better.alarm.model.Alarms
import com.better.alarm.model.AlarmsScheduler
import com.better.alarm.model.CalendarType
import com.better.alarm.model.Calendars
import com.better.alarm.model.DaysOfWeek
import com.better.alarm.persistance.DatabaseQuery
import com.better.alarm.stores.InMemoryRxDataStoreFactory
import com.better.alarm.util.Optional.Companion.absent
import io.mockk.mockk
import io.mockk.verify
import io.reactivex.Single
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import java.util.*
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class AlarmsTest {
  private val stateNotifierMock: IStateNotifier = mockk(relaxed = true)
  private val alarmSetterMock = SetterMock()
  private lateinit var store: Store
  private lateinit var prefs: Prefs
  private val logger: Logger = Logger.create()
  private var currentHour = 0
  private val currentMinute = 0
  private val calendars = Calendars {
    val instance = Calendar.getInstance()
    instance[Calendar.HOUR_OF_DAY] = currentHour
    instance[Calendar.MINUTE] = currentMinute
    instance
  }
  private val containerFactory = TestContainerFactory(calendars)

  @Rule
  @JvmField
  var watcher: TestRule =
      object : TestWatcher() {
        override fun starting(description: Description) {
          println("---- " + description.methodName + " ----")
        }
      }

  @Before
  fun setUp() {
    setMainUnconfined()
    prefs = create(Single.just(true), InMemoryRxDataStoreFactory.create())
    store =
        Store(
            /* alarmsSubject */
            BehaviorSubject.createDefault(ArrayList()), /* next */
            BehaviorSubject.createDefault(absent()), /* sets */
            PublishSubject.create(), /* events */
            PublishSubject.create())
  }

  private fun createAlarms(query: DatabaseQuery = mockQuery()): Alarms {
    val alarmsScheduler = AlarmsScheduler(alarmSetterMock, logger, store, prefs, calendars)
    val alarms =
        Alarms(
            alarmsScheduler,
            query,
            AlarmCoreFactory(logger, alarmsScheduler, stateNotifierMock, prefs, store, calendars),
            containerFactory,
            logger)
    alarmsScheduler.start()
    return alarms
  }

  private fun mockQuery(): DatabaseQuery {
    return createStub(ArrayList())
  }

  @Test
  fun create() {
    // when
    val instance: IAlarmsManager = createAlarms()
    val newAlarm = instance.createNewAlarm()
    newAlarm.enable(true)
    // verify
    store.alarms().test().assertValue { alarmValues ->
      alarmValues.size == 1 && alarmValues[0].isEnabled
    }
  }

  @Test
  fun deleteDisabledAlarm() {
    // when
    val instance: IAlarmsManager = createAlarms()
    val newAlarm = instance.createNewAlarm()
    newAlarm.delete()
    // verify
    store.alarms().test().assertValue { alarmValues -> alarmValues.isEmpty() }
  }

  @Test
  fun deleteEnabledAlarm() {
    // when
    val instance: IAlarmsManager = createAlarms()
    val newAlarm = instance.createNewAlarm()
    newAlarm.enable(true)
    instance.getAlarm(0)!!.delete()
    // verify
    store.alarms().test().assertValue { alarmValues -> alarmValues.isEmpty() }
  }

  @Test
  fun createThreeAlarms() {
    // when
    val instance: IAlarmsManager = createAlarms()
    instance.createNewAlarm()
    instance.createNewAlarm().enable(true)
    instance.createNewAlarm()
    // verify
    store.alarms().test().assertValueAt(0) { alarmValues ->
      println(alarmValues)
      (alarmValues.size == 3 &&
          !alarmValues[0].isEnabled &&
          alarmValues[1].isEnabled &&
          !alarmValues[2].isEnabled)
    }
  }

  @Test
  fun alarmsFromMemoryMustBePresentInTheList() {
    // when
    val instance = createAlarms(createWithFactory(TestContainerFactory { Calendar.getInstance() }))
    instance.start()

    // verify
    store.alarms().test().assertValue { alarmValues ->
      println(alarmValues)
      (alarmValues.size == 1 && alarmValues[0].isEnabled && alarmValues[0].label == "hello")
    }
  }

  @Test
  fun editAlarm() {
    // when
    val instance: IAlarmsManager = createAlarms()
    val newAlarm = instance.createNewAlarm()
    newAlarm.edit { withIsEnabled(true).withHour(7) }
    // verify
    store.alarms().test().assertValue { alarmValues ->
      (alarmValues.size == 1 && alarmValues[0].isEnabled && alarmValues[0].hour == 7)
    }
  }

  @Test
  fun firedAlarmShouldBeDisabledIfNoRepeatingIsSet() {
    // when
    val instance = createAlarms()
    val newAlarm = instance.createNewAlarm()
    newAlarm.enable(true)
    instance.onAlarmFired((newAlarm as AlarmCore), CalendarType.NORMAL)
    verify { stateNotifierMock.broadcastAlarmState(newAlarm.id, Intents.ALARM_ALERT_ACTION) }

    newAlarm.dismiss()
    verify { stateNotifierMock.broadcastAlarmState(newAlarm.id, Intents.ALARM_DISMISS_ACTION) }

    // verify
    store.alarms().test().assertValue { alarmValues ->
      alarmValues.size == 1 && !alarmValues[0].isEnabled
    }
  }

  @Test
  fun firedAlarmShouldBeRescheduledIfRepeatingIsSet() {
    // when
    val instance = createAlarms()
    val newAlarm = instance.createNewAlarm()
    newAlarm.edit { withIsEnabled(true).withDaysOfWeek(DaysOfWeek(1)) }
    instance.onAlarmFired((newAlarm as AlarmCore), CalendarType.NORMAL)
    verify { stateNotifierMock.broadcastAlarmState(newAlarm.id, Intents.ALARM_ALERT_ACTION) }
    newAlarm.dismiss()
    verify { stateNotifierMock.broadcastAlarmState(newAlarm.id, Intents.ALARM_DISMISS_ACTION) }

    // verify
    store.alarms().test().assertValue { alarmValues ->
      alarmValues.size == 1 && alarmValues[0].isEnabled
    }
  }

  @Test
  fun changingAlarmWhileItIsFiredShouldReschedule() {
    // when
    val instance = createAlarms()
    val newAlarm = instance.createNewAlarm()
    newAlarm.enable(true)
    Assertions.assertThat(alarmSetterMock.typeName).isEqualTo("NORMAL")
    instance.onAlarmFired((newAlarm as AlarmCore), CalendarType.NORMAL)
    verify { stateNotifierMock.broadcastAlarmState(newAlarm.id, Intents.ALARM_ALERT_ACTION) }
    newAlarm.edit { withDaysOfWeek(DaysOfWeek(1)).withIsPrealarm(true) }
    verify { stateNotifierMock.broadcastAlarmState(newAlarm.id, Intents.ALARM_DISMISS_ACTION) }

    // verify
    store.alarms().test().assertValue { alarmValues ->
      alarmValues.size == 1 && alarmValues[0].isEnabled
    }
    Assertions.assertThat(alarmSetterMock.id).isEqualTo(newAlarm.id)
  }

  @Test
  fun firedAlarmShouldBeStillEnabledAfterSnoozed() {
    // given
    val instance = createAlarms()
    val newAlarm = instance.createNewAlarm()
    // TODO circle the time, otherwise the tests may fail around 0 hours
    newAlarm.edit {
      withIsEnabled(true).withHour(0).withDaysOfWeek(DaysOfWeek(1)).withIsPrealarm(true)
    }
    // TODO verify

    // when pre-alarm fired
    instance.onAlarmFired((newAlarm as AlarmCore), CalendarType.PREALARM)
    verify { stateNotifierMock.broadcastAlarmState(newAlarm.id, Intents.ALARM_PREALARM_ACTION) }

    // when pre-alarm-snoozed
    newAlarm.snooze()
    verify {
      stateNotifierMock.broadcastAlarmState(newAlarm.id, Intents.ALARM_SNOOZE_ACTION, any())
    }

    // when alarm fired
    instance.onAlarmFired(newAlarm, CalendarType.NORMAL)
    verify { stateNotifierMock.broadcastAlarmState(newAlarm.id, Intents.ACTION_CANCEL_SNOOZE) }
    verify { stateNotifierMock.broadcastAlarmState(newAlarm.id, Intents.ALARM_ALERT_ACTION) }

    // when alarm is snoozed
    newAlarm.snooze()
    verify(exactly = 2) {
      stateNotifierMock.broadcastAlarmState(newAlarm.id, Intents.ALARM_DISMISS_ACTION)
    }
    newAlarm.delete()
    verify(exactly = 2) {
      stateNotifierMock.broadcastAlarmState(newAlarm.id, Intents.ACTION_CANCEL_SNOOZE)
    }
  }

  @Test
  fun snoozeToTime() {
    // given
    val instance = createAlarms()
    val newAlarm = instance.createNewAlarm()
    // TODO circle the time, otherwise the tests may fail around 0 hours
    newAlarm.edit { withIsEnabled(true).withHour(0).withDaysOfWeek(DaysOfWeek(1)) }
    // TODO verify

    // when alarm fired
    instance.onAlarmFired((newAlarm as AlarmCore), CalendarType.NORMAL)
    verify { stateNotifierMock.broadcastAlarmState(newAlarm.id, Intents.ALARM_ALERT_ACTION) }

    // when pre-alarm-snoozed
    newAlarm.snooze(23, 59)
    verify {
      stateNotifierMock.broadcastAlarmState(newAlarm.id, Intents.ALARM_SNOOZE_ACTION, any())
    }
  }

  @Test
  fun snoozePreAlarmToTime() {
    // given
    val instance = createAlarms()
    val newAlarm = instance.createNewAlarm()
    // TODO circle the time, otherwise the tests may fail around 0 hours
    newAlarm.edit {
      withIsEnabled(true).withHour(0).withDaysOfWeek(DaysOfWeek(1)).withIsPrealarm(true)
    }
    // TODO verify

    // when alarm fired
    instance.onAlarmFired((newAlarm as AlarmCore), CalendarType.PREALARM)
    verify { stateNotifierMock.broadcastAlarmState(newAlarm.id, Intents.ALARM_PREALARM_ACTION) }

    // when pre-alarm-snoozed
    newAlarm.snooze(23, 59)
    verify {
      stateNotifierMock.broadcastAlarmState(newAlarm.id, Intents.ALARM_SNOOZE_ACTION, any())
    }
    instance.onAlarmFired(newAlarm, CalendarType.SNOOZE)
    verify { stateNotifierMock.broadcastAlarmState(newAlarm.id, Intents.ALARM_ALERT_ACTION) }
  }

  @Test
  fun prealarmTimedOutAndThenDisabled() {
    // given
    val instance = createAlarms()
    val newAlarm = instance.createNewAlarm()
    // TODO circle the time, otherwise the tests may fail around 0 hours
    newAlarm.edit {
      withIsEnabled(true).withHour(0).withDaysOfWeek(DaysOfWeek(1)).withIsPrealarm(true)
    }
    // TODO verify

    // when alarm fired
    instance.onAlarmFired((newAlarm as AlarmCore), CalendarType.PREALARM)
    verify { stateNotifierMock.broadcastAlarmState(newAlarm.id, Intents.ALARM_PREALARM_ACTION) }
    instance.onAlarmFired(newAlarm, CalendarType.NORMAL)
    verify { stateNotifierMock.broadcastAlarmState(newAlarm.id, Intents.ALARM_ALERT_ACTION) }

    // when pre-alarm-snoozed
    newAlarm.enable(false)
    verify(atLeast = 1) {
      stateNotifierMock.broadcastAlarmState(newAlarm.id, Intents.ALARM_DISMISS_ACTION)
    }
  }

  @Test
  fun snoozedAlarmsMustGoOutOfHibernation() {
    // given
    val instance = createAlarms()
    val newAlarm = instance.createNewAlarm()
    newAlarm.edit {
      withIsEnabled(true).withHour(0).withDaysOfWeek(DaysOfWeek(0)).withIsPrealarm(false)
    }

    // when alarm fired
    instance.onAlarmFired((newAlarm as AlarmCore), CalendarType.NORMAL)
    verify { stateNotifierMock.broadcastAlarmState(newAlarm.id, Intents.ALARM_ALERT_ACTION) }
    newAlarm.snooze()
    val record = containerFactory.createdRecords[0]
    println("------------")
    // now we simulate it started all over again
    alarmSetterMock.removeRTCAlarm()
    createStub(containerFactory.createdRecords)
    val query = createStub(containerFactory.createdRecords)
    val newAlarms = createAlarms(query)
    newAlarms.start()
    Assertions.assertThat(alarmSetterMock.id).isEqualTo(record.value.id)
  }

  @Test
  fun snoozedAlarmsMustCanBeRescheduled() {
    // given
    val instance = createAlarms()
    val newAlarm = instance.createNewAlarm()
    newAlarm.edit {
      withIsEnabled(true).withHour(7).withDaysOfWeek(DaysOfWeek(0)).withIsPrealarm(false)
    }

    // when alarm fired
    currentHour = 7
    instance.onAlarmFired((newAlarm as AlarmCore), CalendarType.NORMAL)
    verify { stateNotifierMock.broadcastAlarmState(newAlarm.id, Intents.ALARM_ALERT_ACTION) }
    newAlarm.snooze()
    println("----- now snooze -------")
    newAlarm.snooze(7, 42)
    Assertions.assertThat(alarmSetterMock.id).isEqualTo(newAlarm.id)
    Assertions.assertThat(alarmSetterMock.calendar!![Calendar.MINUTE]).isEqualTo(42)
  }

  @Test
  fun snoozedAlarmsMustGoOutOfHibernationIfItWasRescheduled() {
    snoozedAlarmsMustCanBeRescheduled()
    val record = containerFactory.createdRecords[0]
    println("------------")
    // now we simulate it started all over again
    alarmSetterMock.removeRTCAlarm()
    val query = createStub(containerFactory.createdRecords)
    val newAlarms = createAlarms(query)
    newAlarms.start()
    Assertions.assertThat(alarmSetterMock.id).isEqualTo(record.value.id)
    // TODO
    //  assertThat(alarmSetterMock.getCalendar().get(Calendar.MINUTE)).isEqualTo(42);
  }

  @Test
  fun prealarmFiredAlarmTransitioningToFiredShouldNotDismissTheService() {
    // given
    val instance = createAlarms()
    val newAlarm = instance.createNewAlarm()
    newAlarm.edit {
      withIsEnabled(true).withHour(0).withDaysOfWeek(DaysOfWeek(0)).withIsPrealarm(true)
    }

    // when alarm fired
    instance.onAlarmFired((newAlarm as AlarmCore), CalendarType.PREALARM)
    verify { stateNotifierMock.broadcastAlarmState(newAlarm.id, Intents.ALARM_PREALARM_ACTION) }
    instance.onAlarmFired(newAlarm, CalendarType.NORMAL)
    verify { stateNotifierMock.broadcastAlarmState(newAlarm.id, Intents.ALARM_ALERT_ACTION) }
    verify(inverse = true) {
      stateNotifierMock.broadcastAlarmState(newAlarm.id, Intents.ALARM_DISMISS_ACTION)
    }

    newAlarm.snooze()
    verify(exactly = 1) {
      stateNotifierMock.broadcastAlarmState(newAlarm.id, Intents.ALARM_DISMISS_ACTION)
    }
  }
}
