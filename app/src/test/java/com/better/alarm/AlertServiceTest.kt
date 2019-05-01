package com.better.alarm

import com.better.alarm.background.*
import com.better.alarm.interfaces.Alarm
import com.better.alarm.interfaces.IAlarmsManager
import com.better.alarm.logger.Logger
import com.better.alarm.model.Alarmtone
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.disposables.Disposables
import io.reactivex.observers.TestObserver
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.*
import kotlin.properties.Delegates

class AlertServiceTest {
    private val alarmsManager = mock(IAlarmsManager::class.java)
    var target: TestObserver<TargetVolume> by Delegates.notNull()
    val wakelocks = mock(Wakelocks::class.java)
    var plugin = object : AlertPlugin {
        override fun go(alarm: PluginAlarmData, prealarm: Boolean, targetVolume: Observable<TargetVolume>): Disposable {
            target = targetVolume.test()
            return Disposables.empty()
        }
    }
    val alertService: AlertService = AlertService(
            log = Logger.create(),
            inCall = Observable.just(false),
            wakelocks = wakelocks,
            alarms = alarmsManager,
            handleUnwantedEvent = {},
            stopSelf = {},
            plugins = arrayOf(plugin)
    )

    @Test
    fun `smoke`() {
        val alarmMock = mock(Alarm::class.java)
        `when`(alarmMock.id).thenReturn(1)
        `when`(alarmMock.labelOrDefault).thenReturn("")
        `when`(alarmMock.alarmtone).thenReturn(Alarmtone.Default())

        `when`(alarmsManager.getAlarm(anyInt())).thenReturn(alarmMock)
        alertService.onStartCommand(Event.AlarmEvent(1))

        target.assertValues(TargetVolume.FADED_IN)
    }

    @Test
    fun `alert mute demute`() {
        val alarmMock = mock(Alarm::class.java)
        `when`(alarmMock.id).thenReturn(1)
        `when`(alarmMock.labelOrDefault).thenReturn("")
        `when`(alarmMock.alarmtone).thenReturn(Alarmtone.Default())

        `when`(alarmsManager.getAlarm(anyInt())).thenReturn(alarmMock)

        alertService.run {
            onStartCommand(Event.AlarmEvent(1))
            onStartCommand(Event.MuteEvent())
            onStartCommand(Event.DemuteEvent())
        }

        target.assertValues(TargetVolume.FADED_IN, TargetVolume.MUTED, TargetVolume.FADED_IN_FAST)
    }

    inline fun <reified T> any(): T {
        any<T>(T::class.java)
        return when {
            T::class.java == PluginAlarmData::class.java -> PluginAlarmData(1, Alarmtone.Default(), "") as T
            T::class.java == TargetVolume::class.java -> TargetVolume.MUTED as T
            else -> mock(T::class.java)
        }
    }

    inline fun <reified T> eq(eq: T): T {
        Mockito.eq<T>(eq)
        return eq
    }
}