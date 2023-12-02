package com.better.alarm

import com.better.alarm.data.AlarmsRepository
import com.better.alarm.data.DataStoreAlarmsRepository
import com.better.alarm.data.Prefs
import com.better.alarm.data.stores.InMemoryRxDataStoreFactory
import com.better.alarm.domain.AlarmCore
import com.better.alarm.domain.AlarmsScheduler
import com.better.alarm.domain.Calendars
import com.better.alarm.domain.Store
import com.better.alarm.logger.Logger
import com.better.alarm.util.Optional
import io.mockk.mockk
import io.reactivex.Single
import io.reactivex.exceptions.CompositeException
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.TestScheduler
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import java.util.*
import kotlin.io.path.createTempDirectory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class AlarmCoreWithStoreTest {
  private var stateNotifierMock: AlarmCore.IStateNotifier = mockk(relaxed = true)
  private val alarmSetterMock = AlarmSchedulerTest.SetterMock()
  private var testScheduler: TestScheduler = TestScheduler()
  private var rxjavaExceptions: CompositeException? = null
  private var store: Store =
      Store(
          alarmsSubject = BehaviorSubject.createDefault(ArrayList()),
          next = BehaviorSubject.createDefault<Optional<Store.Next>>(Optional.absent()),
          sets = PublishSubject.create(),
          events = PublishSubject.create())
  private var prefs: Prefs =
      Prefs.create(Single.just(true), InMemoryRxDataStoreFactory.create()).apply {
        autoSilence.value = 7
        skipDuration.value = 120
      }
  private var logger: Logger = Logger.create()
  private var currentHour = 0
  private var currentMinute = 5
  private var currentDay = 1
  private val calendars = Calendars {
    val instance = Calendar.getInstance()
    instance.set(Calendar.YEAR, 2019)
    instance.set(Calendar.DAY_OF_YEAR, currentDay)
    instance.set(Calendar.HOUR_OF_DAY, currentHour)
    instance.set(Calendar.MINUTE, currentMinute)
    instance.set(Calendar.SECOND, 0)
    instance
  }
  private val repository: AlarmsRepository =
      DataStoreAlarmsRepository.createBlocking(
          createTempDirectory().toFile(), logger, CoroutineScope(Dispatchers.IO))

  fun advanceTime(timeString: String) {
    require(timeString.contains(":"))
    currentHour = timeString.substringBefore(":").toInt()
    currentMinute = timeString.substringAfter(":").toInt()
  }

  @JvmField
  @Rule
  val watcher: TestRule =
      object : TestWatcher() {
        override fun starting(description: Description) {
          println("---- " + description.methodName + " ----")
        }
      }

  private var rxJavaExceptionHandler = RxJavaPlugins.getErrorHandler()

  @Before
  fun setErrorHandler() {
    RxJavaPlugins.setErrorHandler {
      rxjavaExceptions = rxjavaExceptions?.apply { addSuppressed(it) } ?: CompositeException(it)
    }
  }

  @After
  fun checkRxJavaErrors() {
    RxJavaPlugins.setErrorHandler(rxJavaExceptionHandler)
    rxjavaExceptions?.let { throw it }
  }

  fun act(what: String, func: () -> Unit) {
    println("When $what")
    func()
    testScheduler.triggerActions()
  }

  private suspend fun createAlarm(): AlarmCore {
    val alarmsScheduler = AlarmsScheduler(alarmSetterMock, logger, store, prefs, calendars)
    alarmsScheduler.start()
    return AlarmCore(
            repository.create(),
            logger,
            alarmsScheduler,
            stateNotifierMock,
            prefs,
            store,
            calendars,
            onDelete = {})
        .apply {
          start()
          testScheduler.triggerActions()
        }
  }

  @Test
  fun `Enabling alarm saved properly`() =
      runBlocking<Unit> {
        logger.debug { "CREATE" }
        val alarm = createAlarm()
        delay(1000)

        act("Enable") { alarm.enable(true) }
        delay(1000)

        assertThat(alarmSetterMock.calendar).isNotNull()

        assertThat(store.alarms().blockingFirst().first().isEnabled).isTrue()
        assertThat(alarmSetterMock.calendar).isNotNull()

        repository.query().onEach { logger.debug { "restored ${it.value} in ${it.value.state}" } }
      }
}
