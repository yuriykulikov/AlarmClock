package com.better.alarm

import com.better.alarm.background.AlertPlugin
import com.better.alarm.background.AlertService
import com.better.alarm.background.EnclosingService
import com.better.alarm.background.Event
import com.better.alarm.background.NotificationsPlugin
import com.better.alarm.background.PluginAlarmData
import com.better.alarm.background.TargetVolume
import com.better.alarm.interfaces.Alarm
import com.better.alarm.interfaces.IAlarmsManager
import com.better.alarm.logger.Logger
import com.better.alarm.model.Alarmtone
import com.better.alarm.wakelock.Wakelocks
import io.mockk.Ordering
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.disposables.Disposables
import io.reactivex.observers.TestObserver
import io.reactivex.plugins.RxJavaPlugins
import kotlin.properties.Delegates
import org.assertj.core.api.KotlinAssertions.assertThat
import org.junit.Test
import org.slf4j.helpers.NOPLogger

class AlertServiceTest {
  private val alarm1 =
      mockk<Alarm> {
        every { id } returns 1
        every { labelOrDefault } returns "1"
        every { alarmtone } returns Alarmtone.Default()
        every { dismiss() } answers {}
      }
  private val alarmsManager: IAlarmsManager = mockk {
    every { getAlarm(1) } returns alarm1
    val alarm2 =
        mockk<Alarm> {
          every { id } returns 2
          every { labelOrDefault } returns "2"
          every { alarmtone } returns Alarmtone.Default()
        }
    val alarm3 =
        mockk<Alarm> {
          every { id } returns 3
          every { labelOrDefault } returns "3"
          every { alarmtone } returns Alarmtone.Default()
        }
    every { getAlarm(2) } returns alarm2
    every { getAlarm(3) } returns alarm3
  }

  private var plugin =
      object : AlertPlugin {
        var targetVolumeTest: TestObserver<TargetVolume> by Delegates.notNull()
        val disposables: MutableMap<Int, Disposable> = mutableMapOf()
        override fun go(
            alarm: PluginAlarmData,
            prealarm: Boolean,
            targetVolume: Observable<TargetVolume>
        ): Disposable {
          targetVolumeTest = targetVolume.test()
          return Disposables.empty().also { disposables[alarm.id] = it }
        }
      }

  private val wakelocks: Wakelocks = mockk(relaxed = true)
  private val enclosingService: EnclosingService = mockk(relaxed = true)
  private val notificationsPlugin: NotificationsPlugin = mockk(relaxed = true)

  private val alertService: AlertService =
      AlertService(
          log = Logger(NOPLogger.NOP_LOGGER),
          inCall = Observable.just(false),
          wakelocks = wakelocks,
          alarms = alarmsManager,
          enclosing = enclosingService,
          notifications = notificationsPlugin,
          plugins = listOf(plugin))

  init {
    RxJavaPlugins.setErrorHandler { it.printStackTrace() }
  }

  @Test
  fun `smoke test`() {
    alertService.onStartCommand(Event.AlarmEvent(1))

    plugin.targetVolumeTest.assertValues(TargetVolume.FADED_IN)
  }

  @Test
  fun `alert mute demute changes target volume`() {
    alertService.run {
      onStartCommand(Event.AlarmEvent(1))
      onStartCommand(Event.MuteEvent())
      onStartCommand(Event.DemuteEvent())
    }

    plugin.targetVolumeTest.assertValues(
        TargetVolume.FADED_IN, TargetVolume.MUTED, TargetVolume.FADED_IN_FAST)
  }

  @Test
  fun `last alarm plays the sound and previous does not`() {
    alertService.run {
      onStartCommand(Event.AlarmEvent(1))
      onStartCommand(Event.AlarmEvent(2))
    }

    assertThat(plugin.disposables.getValue(1).isDisposed).isTrue()
    assertThat(plugin.disposables.getValue(2).isDisposed).isFalse()
  }

  @Test
  fun `notifications show both playing alarms`() {
    alertService.run {
      onStartCommand(Event.AlarmEvent(1))
      onStartCommand(Event.AlarmEvent(2))
    }

    // order
    verify(ordering = Ordering.ORDERED) {
      notificationsPlugin.show(match { it.id == 1 }, 0, true)
      notificationsPlugin.show(match { it.id == 1 }, 0, false)
      notificationsPlugin.show(match { it.id == 2 }, 1, false)
    }
  }

  @Test
  fun `notifications are removed as alarms are dismissed`() {
    alertService.run {
      onStartCommand(Event.AlarmEvent(1))
      onStartCommand(Event.AlarmEvent(2))
      onStartCommand(Event.DismissEvent(1))
    }

    // order
    verify(ordering = Ordering.ORDERED) {
      notificationsPlugin.show(match { it.id == 1 }, 0, true)
      notificationsPlugin.show(match { it.id == 1 }, 0, false)
      notificationsPlugin.show(match { it.id == 2 }, 1, false)

      notificationsPlugin.show(match { it.id == 2 }, 0, false)
      notificationsPlugin.cancel(1)
    }
  }

  @Test
  fun `notifications are removed and everything is stopped as all alarms are dismissed`() {
    alertService.run {
      onStartCommand(Event.AlarmEvent(1))
      onStartCommand(Event.AlarmEvent(2))
      onStartCommand(Event.DismissEvent(1))
      onStartCommand(Event.DismissEvent(2))
    }

    // order
    verify(ordering = Ordering.ORDERED) {
      notificationsPlugin.show(match { it.id == 1 }, 0, true)
      notificationsPlugin.show(match { it.id == 1 }, 0, false)
      notificationsPlugin.show(match { it.id == 2 }, 1, false)

      notificationsPlugin.show(match { it.id == 2 }, 0, false)
      notificationsPlugin.cancel(1)

      plugin.disposables.values.forEach { assertThat(it.isDisposed).isTrue() }

      enclosingService.stopSelf()
    }
  }

  @Test
  fun `notifications are reused and counted`() {
    alertService.run {
      // two alarms
      onStartCommand(Event.AlarmEvent(1))
      onStartCommand(Event.AlarmEvent(2))

      // second dismissed
      onStartCommand(Event.DismissEvent(2))

      // two more
      onStartCommand(Event.AlarmEvent(2))
      onStartCommand(Event.AlarmEvent(3))

      // last dismissed
      onStartCommand(Event.DismissEvent(3))
    }

    // order
    verify(ordering = Ordering.ORDERED) {
      // two alarms
      notificationsPlugin.show(match { it.id == 1 }, 0, true)
      notificationsPlugin.show(match { it.id == 1 }, 0, false)
      notificationsPlugin.show(match { it.id == 2 }, 1, false)

      // second dismissed
      notificationsPlugin.show(match { it.id == 1 }, 0, false)
      notificationsPlugin.cancel(1)

      // two more
      notificationsPlugin.show(match { it.id == 1 }, 0, false)
      notificationsPlugin.show(match { it.id == 2 }, 1, false)
      notificationsPlugin.show(match { it.id == 3 }, 2, false)

      // last dismissed
      notificationsPlugin.cancel(2)
    }
  }
}
