package com.better.alarm

import com.better.alarm.AlarmSchedulerTest.SetterMock
import com.better.alarm.data.AlarmValue
import com.better.alarm.data.DaysOfWeek
import com.better.alarm.data.Prefs
import com.better.alarm.data.Prefs.Companion.create
import com.better.alarm.data.contentprovider.DatabaseQuery
import com.better.alarm.data.modify
import com.better.alarm.data.stores.InMemoryRxDataStoreFactory
import com.better.alarm.domain.AlarmCore
import com.better.alarm.domain.AlarmCore.IStateNotifier
import com.better.alarm.domain.Alarms
import com.better.alarm.domain.AlarmsScheduler
import com.better.alarm.domain.Calendars
import com.better.alarm.domain.IAlarmsManager
import com.better.alarm.domain.Store
import com.better.alarm.logger.Logger
import com.better.alarm.receivers.Intents
import com.better.alarm.util.Optional.Companion.absent
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.reactivex.Single
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import java.util.*
import org.assertj.core.api.Assertions.assertThat
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
  private val alarmsRepository = TestAlarmsRepository()
  private val databaseQuery =
      mockk<DatabaseQuery>(relaxed = true) { every { query() } returns emptyList() }

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
    // Dispatchers.setMain(Dispatchers.Default.limitedParallelism(1))
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

  private fun createAlarms(): Alarms {
    val alarmsScheduler = AlarmsScheduler(alarmSetterMock, logger, store, prefs, calendars)
    val alarms =
        Alarms(
            prefs,
            store,
            calendars,
            alarmsScheduler,
            stateNotifierMock,
            alarmsRepository,
            logger,
            databaseQuery)
    alarms.start()
    alarmsScheduler.start()
    return alarms
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
    // given
    alarmsRepository.create().modify { copy(isEnabled = true, label = "hello") }
    createAlarms()
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
    instance.onAlarmFired((newAlarm as AlarmCore))
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
    instance.onAlarmFired((newAlarm as AlarmCore))
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
    assertThat(alarmSetterMock.typeName).isEqualTo("NORMAL")
    instance.onAlarmFired((newAlarm as AlarmCore))
    verify { stateNotifierMock.broadcastAlarmState(newAlarm.id, Intents.ALARM_ALERT_ACTION) }
    newAlarm.edit { withDaysOfWeek(DaysOfWeek(1)).withIsPrealarm(true) }
    verify { stateNotifierMock.broadcastAlarmState(newAlarm.id, Intents.ALARM_DISMISS_ACTION) }

    // verify
    store.alarms().test().assertValue { alarmValues ->
      alarmValues.size == 1 && alarmValues[0].isEnabled
    }
    assertThat(alarmSetterMock.id).isEqualTo(newAlarm.id)
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
    instance.onAlarmFired((newAlarm as AlarmCore))
    verify { stateNotifierMock.broadcastAlarmState(newAlarm.id, Intents.ALARM_PREALARM_ACTION) }

    // when pre-alarm-snoozed
    newAlarm.snooze()
    verify {
      stateNotifierMock.broadcastAlarmState(newAlarm.id, Intents.ALARM_SNOOZE_ACTION, any())
    }

    // when alarm fired
    instance.onAlarmFired(newAlarm)
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
    instance.onAlarmFired((newAlarm as AlarmCore))
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
    instance.onAlarmFired((newAlarm as AlarmCore))
    verify { stateNotifierMock.broadcastAlarmState(newAlarm.id, Intents.ALARM_PREALARM_ACTION) }

    // when pre-alarm-snoozed
    newAlarm.snooze(23, 59)
    verify {
      stateNotifierMock.broadcastAlarmState(newAlarm.id, Intents.ALARM_SNOOZE_ACTION, any())
    }
    instance.onAlarmFired(newAlarm)
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
    instance.onAlarmFired((newAlarm as AlarmCore))
    verify { stateNotifierMock.broadcastAlarmState(newAlarm.id, Intents.ALARM_PREALARM_ACTION) }
    instance.onAlarmFired(newAlarm)
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
    instance.onAlarmFired((newAlarm as AlarmCore))
    verify { stateNotifierMock.broadcastAlarmState(newAlarm.id, Intents.ALARM_ALERT_ACTION) }
    newAlarm.snooze()
    val record = alarmsRepository.createdRecords[0]
    println("------------")
    // now we simulate it started all over again
    alarmSetterMock.removeRTCAlarm()
    createAlarms()
    assertThat(alarmSetterMock.id).isEqualTo(record.value.id)
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
    instance.onAlarmFired((newAlarm as AlarmCore))
    verify { stateNotifierMock.broadcastAlarmState(newAlarm.id, Intents.ALARM_ALERT_ACTION) }
    newAlarm.snooze()
    println("----- now snooze -------")
    newAlarm.snooze(7, 42)
    assertThat(alarmSetterMock.id).isEqualTo(newAlarm.id)
    assertThat(alarmSetterMock.calendar!![Calendar.MINUTE]).isEqualTo(42)
  }

  @Test
  fun snoozedAlarmsMustGoOutOfHibernationIfItWasRescheduled() {
    snoozedAlarmsMustCanBeRescheduled()
    val record = alarmsRepository.createdRecords[0]
    println("------------")
    // now we simulate it started all over again
    alarmSetterMock.removeRTCAlarm()
    createAlarms()
    assertThat(alarmSetterMock.id).isEqualTo(record.value.id)
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
    instance.onAlarmFired((newAlarm as AlarmCore))
    verify { stateNotifierMock.broadcastAlarmState(newAlarm.id, Intents.ALARM_PREALARM_ACTION) }
    instance.onAlarmFired(newAlarm)
    verify { stateNotifierMock.broadcastAlarmState(newAlarm.id, Intents.ALARM_ALERT_ACTION) }
    verify(inverse = true) {
      stateNotifierMock.broadcastAlarmState(newAlarm.id, Intents.ALARM_DISMISS_ACTION)
    }

    newAlarm.snooze()
    verify(exactly = 1) {
      stateNotifierMock.broadcastAlarmState(newAlarm.id, Intents.ALARM_DISMISS_ACTION)
    }
  }

  @Test
  fun `when repository is not initialized and database is empty then default alarms are created`() {
    alarmsRepository.initialized = false

    // when
    createAlarms()

    // verify
    assertThat(store.alarms().test().values().first())
        .containsAll(
            listOf(
                AlarmValue(id = 0, hour = 8, minutes = 30, daysOfWeek = DaysOfWeek(31)),
                AlarmValue(id = 1, hour = 9, minutes = 0, daysOfWeek = DaysOfWeek(96)),
            ))
  }

  @Test
  fun `when repository is not initialized and database contains alarms then alarms are migrated`() {
    alarmsRepository.initialized = false
    val alarmsInDatabase =
        listOf(
            AlarmValue(id = 0, hour = 8, minutes = 30),
            AlarmValue(id = 1, hour = 9, minutes = 0, daysOfWeek = DaysOfWeek(31)),
            AlarmValue(id = 2, hour = 10, minutes = 30, isEnabled = true),
        )
    every { databaseQuery.query() } returns alarmsInDatabase

    // when
    createAlarms()

    // verify
    assertThat(store.alarms().test().values().first()).containsAll(alarmsInDatabase)
    verify { databaseQuery.delete(0) }
    verify { databaseQuery.delete(1) }
    verify { databaseQuery.delete(2) }
  }
}
