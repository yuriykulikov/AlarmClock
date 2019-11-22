package com.better.alarm

import com.better.alarm.background.*
import com.better.alarm.interfaces.IAlarmsManager
import com.better.alarm.logger.Logger
import com.better.alarm.model.Alarmtone
import com.better.alarm.wakelock.Wakelocks
import io.mockk.every
import io.mockk.mockk
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.disposables.Disposables
import io.reactivex.observers.TestObserver
import org.junit.Test
import org.mockito.Mockito.mock
import kotlin.properties.Delegates

class AlertServiceTest {
    private val alarmsManager: IAlarmsManager = mockk {
        every { getAlarm(any()) } returns mockk {
            every { id } returns 1
            every { labelOrDefault } returns ""
            every { alarmtone } returns Alarmtone.Default()
        }
    }
    private var target: TestObserver<TargetVolume> by Delegates.notNull()
    private val wakelocks = mock(Wakelocks::class.java)
    private var plugin = object : AlertPlugin {
        override fun go(alarm: PluginAlarmData, prealarm: Boolean, targetVolume: Observable<TargetVolume>): Disposable {
            target = targetVolume.test()
            return Disposables.empty()
        }
    }

    private val alertService: AlertService = AlertService(
            log = Logger.create(),
            inCall = Observable.just(false),
            wakelocks = wakelocks,
            alarms = alarmsManager,
            handleUnwantedEvent = {},
            stopSelf = {},
            plugins = arrayOf(plugin)
    )

    @Test
    fun `smoke test`() {
        alertService.onStartCommand(Event.AlarmEvent(1))

        target.assertValues(TargetVolume.FADED_IN)
    }

    @Test
    fun `alert mute demute`() {
        alertService.run {
            onStartCommand(Event.AlarmEvent(1))
            onStartCommand(Event.MuteEvent())
            onStartCommand(Event.DemuteEvent())
        }

        target.assertValues(TargetVolume.FADED_IN, TargetVolume.MUTED, TargetVolume.FADED_IN_FAST)
    }
}