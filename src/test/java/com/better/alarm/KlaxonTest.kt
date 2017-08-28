package com.better.alarm

import android.content.Context
import android.content.Intent
import android.content.res.AssetFileDescriptor
import android.content.res.Resources
import android.media.MediaPlayer
import android.net.Uri
import android.os.PowerManager
import android.telephony.TelephonyManager
import com.better.alarm.background.KlaxonServiceCallback
import com.better.alarm.background.KlaxonServiceDelegate
import com.better.alarm.interfaces.Alarm
import com.better.alarm.interfaces.IAlarmsManager
import com.better.alarm.interfaces.Intents
import com.better.alarm.logger.Logger
import com.better.alarm.logger.SysoutLogWriter
import com.better.alarm.wakelock.WakeLockManager
import io.reactivex.Observable
import io.reactivex.schedulers.TestScheduler
import io.reactivex.subjects.BehaviorSubject
import org.assertj.core.api.KotlinAssertions
import org.assertj.core.data.Offset
import org.junit.Ignore
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.*
import org.mockito.stubbing.OngoingStubbing
import java.io.FileDescriptor
import java.util.concurrent.TimeUnit

/**
 * Created by Yuriy on 20.08.2017.
 */
class KlaxonTest {
    private val FULL = 1f

    private val mediaPlayer: MediaPlayer
    private val klaxonServiceCallback: KlaxonServiceCallback
    private val alarm: Alarm
    private val wakeLock: PowerManager.WakeLock
    private val scheduler: TestScheduler
    private val resources: Resources
    private val assetFileDescriptor: AssetFileDescriptor


    private fun <T> whenCalled(methodCall: T?): OngoingStubbing<T> = `when`(methodCall)

    init {
        alarm = mock(Alarm::class.java)

        mediaPlayer = mock(MediaPlayer::class.java)

        klaxonServiceCallback = mock(KlaxonServiceCallback::class.java).apply {
            whenCalled(this.getDefaultUri(anyInt())).thenReturn(mock(Uri::class.java))
            whenCalled(this.createMediaPlayer()).thenReturn(mediaPlayer)
        }

        scheduler = TestScheduler()

        wakeLock = mock(PowerManager.WakeLock::class.java)

        assetFileDescriptor = mock(AssetFileDescriptor::class.java).apply {
            whenCalled(fileDescriptor).thenReturn(FileDescriptor())
        }

        resources = mock(android.content.res.Resources::class.java).apply {
            whenCalled(openRawResourceFd(anyInt())).thenReturn(assetFileDescriptor)
        }
    }

    private fun verifyThatVolumeIs(volume: Float) {
        ArgumentCaptor.forClass(Float::class.java)
                .apply { verify(mediaPlayer, atLeast(1)).setVolume(capture(), capture()) }
                .let { KotlinAssertions.assertThat(it.value) }
                .isCloseTo(volume, Offset.offset(.01f))
    }

    private fun createDelegate(
            callState: Observable<Int> = Observable.just(TelephonyManager.CALL_STATE_IDLE),
            prealarmVolume: Observable<Int> = Observable.just(5)
    ): KlaxonServiceDelegate {
        val powerManager = mock(PowerManager::class.java).apply {
            whenCalled(this.newWakeLock(anyInt(), anyString())).thenReturn(wakeLock)
        }

        val alarmsManager = mock(IAlarmsManager::class.java).apply {
            whenCalled(this.getAlarm(anyInt())).thenReturn(alarm)
        }

        return KlaxonServiceDelegate(
                Logger().apply { addLogWriter(SysoutLogWriter()) },
                powerManager,
                mock(WakeLockManager::class.java),
                alarmsManager,
                mock(Context::class.java),
                resources,
                callState,
                prealarmVolume = prealarmVolume,
                fadeInTimeInSeconds = Observable.just(30000),
                callback = klaxonServiceCallback,
                scheduler = scheduler
        )
    }

    @Test
    fun onAlarm() {
        val klaxonServiceDelegate = createDelegate()

        klaxonServiceDelegate.onStartCommand(mock(Intent::class.java).apply {
            whenCalled(action).thenReturn(Intents.ALARM_ALERT_ACTION)
        })
        scheduler.advanceTimeBy(35, TimeUnit.SECONDS)

        verifyThatVolumeIs(FULL)
        verify(mediaPlayer).start()
    }

    @Test
    fun onPrealarm() {
        val klaxonServiceDelegate = createDelegate()

        klaxonServiceDelegate.onStartCommand(mock(Intent::class.java).apply {
            whenCalled(action).thenReturn(Intents.ALARM_PREALARM_ACTION)
        })

        scheduler.advanceTimeBy(35, TimeUnit.SECONDS)

        verifyThatVolumeIs(5.preAlarm())
        verify(mediaPlayer).start()
    }


    @Test
    fun muteWhenStartsInCall() {
        val callState = BehaviorSubject.createDefault(TelephonyManager.CALL_STATE_OFFHOOK)
        val klaxonServiceDelegate = createDelegate(callState = callState)

        klaxonServiceDelegate.onStartCommand(mock(Intent::class.java).apply {
            whenCalled(action).thenReturn(Intents.ALARM_ALERT_ACTION)
        })

        verify(resources).openRawResourceFd(eq(R.raw.in_call_alarm))
        verify(assetFileDescriptor).close()
        verify(mediaPlayer).setDataSource(eq(assetFileDescriptor.fileDescriptor), anyLong(), anyLong())
    }

    @Test
    fun muteWhenInCall() {
        val callState = BehaviorSubject.createDefault(TelephonyManager.CALL_STATE_IDLE)
        val klaxonServiceDelegate = createDelegate(callState = callState)

        klaxonServiceDelegate.onStartCommand(mock(Intent::class.java).apply {
            whenCalled(action).thenReturn(Intents.ALARM_ALERT_ACTION)
        })

        scheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        callState.onNext(TelephonyManager.CALL_STATE_OFFHOOK)

        verifyThatVolumeIs(0f)

        callState.onNext(TelephonyManager.CALL_STATE_IDLE)
        scheduler.advanceTimeBy(10, TimeUnit.SECONDS)

        verifyThatVolumeIs(FULL)
    }

    @Test
    @Ignore
    fun whenUriIsNull() {
        val klaxonServiceDelegate = createDelegate()

        whenCalled(klaxonServiceCallback.getDefaultUri(anyInt())).thenReturn(null)

        klaxonServiceDelegate.onStartCommand(mock(Intent::class.java).apply {
            whenCalled(action).thenReturn(Intents.ALARM_ALERT_ACTION)
        })

        scheduler.advanceTimeBy(35, TimeUnit.SECONDS)

        verify(resources).openRawResourceFd(eq(R.raw.fallbackring))
    }

    @Test
    fun whenSdBusy() {
        val klaxonServiceDelegate = createDelegate()

        whenCalled(mediaPlayer.start())
                .thenThrow(RuntimeException())
                .thenAnswer({ })

        klaxonServiceDelegate.onStartCommand(mock(Intent::class.java).apply {
            whenCalled(action).thenReturn(Intents.ALARM_ALERT_ACTION)
        })

        scheduler.advanceTimeBy(35, TimeUnit.SECONDS)

        verify(resources).openRawResourceFd(eq(R.raw.fallbackring))
        verify(assetFileDescriptor).close()
        verify(mediaPlayer).setDataSource(eq(assetFileDescriptor.fileDescriptor), anyLong(), anyLong())
    }

    @Test
    fun whenStop() {
        val klaxonServiceDelegate = createDelegate()

        whenCalled(klaxonServiceCallback.stopSelf()).thenAnswer({ klaxonServiceDelegate.onDestroy() })

        klaxonServiceDelegate.onStartCommand(mock(Intent::class.java).apply {
            whenCalled(action).thenReturn(Intents.ALARM_ALERT_ACTION)
        })

        klaxonServiceDelegate.onStartCommand(mock(Intent::class.java).apply {
            whenCalled(action).thenReturn(Intents.ALARM_DISMISS_ACTION)
        })

        verify(klaxonServiceCallback).stopSelf()
        verify(mediaPlayer).release()
    }

    @Test
    fun whenErrorShouldRelease() {
        val klaxonServiceDelegate = createDelegate()

        klaxonServiceDelegate.onStartCommand(mock(Intent::class.java).apply {
            whenCalled(action).thenReturn(Intents.ALARM_ALERT_ACTION)
        })

        ArgumentCaptor.forClass(MediaPlayer.OnErrorListener::class.java)
                .apply { verify(mediaPlayer).setOnErrorListener(capture()) }
                .run { value.onError(mediaPlayer, 1, 2) }

        verify(mediaPlayer).stop()
        verify(mediaPlayer).release()
    }

    @Test
    fun fadeInShouldBeFastAfterDemute() {
        val klaxonServiceDelegate = createDelegate()

        klaxonServiceDelegate.onStartCommand(mock(Intent::class.java).apply {
            whenCalled(action).thenReturn(Intents.ALARM_ALERT_ACTION)
        })

        klaxonServiceDelegate.onStartCommand(mock(Intent::class.java).apply {
            whenCalled(action).thenReturn(Intents.ACTION_MUTE)
        })
        klaxonServiceDelegate.onStartCommand(mock(Intent::class.java).apply {
            whenCalled(action).thenReturn(Intents.ACTION_DEMUTE)
        })

        scheduler.advanceTimeBy(6, TimeUnit.SECONDS)

        verifyThatVolumeIs(FULL)
    }

    fun Float.squared() = Math.pow(this.toDouble(), 2.0).toFloat()
    fun Int.preAlarm() = (this.plus(1).toFloat() / 11f / 2f).squared()

    @Test
    fun preAlarmSample() {
        val prealarmVolume = BehaviorSubject.createDefault(1)
        val klaxonServiceDelegate = createDelegate(prealarmVolume = prealarmVolume)

        klaxonServiceDelegate.onStartCommand(mock(Intent::class.java).apply {
            whenCalled(action).thenReturn(Intents.ACTION_START_PREALARM_SAMPLE)
        })
        scheduler.advanceTimeBy(5, TimeUnit.SECONDS)

        verifyThatVolumeIs(1.preAlarm())
        prealarmVolume.onNext(5);
        scheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        verifyThatVolumeIs(5.preAlarm())

        prealarmVolume.onNext(10);
        scheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        verifyThatVolumeIs(10.preAlarm())

        klaxonServiceDelegate.onStartCommand(mock(Intent::class.java).apply {
            whenCalled(action).thenReturn(Intents.ACTION_STOP_PREALARM_SAMPLE)
        })

        verifyThatVolumeIs(0f)

        scheduler.advanceTimeBy(5, TimeUnit.SECONDS)
    }

    @Test
    fun volumeAfterFadeInShouldBeCorrect() {
        val prealarmVolume = BehaviorSubject.createDefault(1)
        val klaxonServiceDelegate = createDelegate(prealarmVolume = prealarmVolume)

        klaxonServiceDelegate.onStartCommand(mock(Intent::class.java).apply {
            whenCalled(action).thenReturn(Intents.ALARM_PREALARM_ACTION)
        })

        scheduler.advanceTimeBy(35, TimeUnit.SECONDS)

        verifyThatVolumeIs(1.preAlarm())
    }

    @Test
    fun volumeChangedWhilePlaying() {
        val prealarmVolume = BehaviorSubject.createDefault(2)
        val klaxonServiceDelegate = createDelegate(prealarmVolume = prealarmVolume)

        klaxonServiceDelegate.onStartCommand(mock(Intent::class.java).apply {
            whenCalled(action).thenReturn(Intents.ALARM_PREALARM_ACTION)
        })

        scheduler.advanceTimeBy(35, TimeUnit.SECONDS)

        verifyThatVolumeIs(2.preAlarm())
        verify(mediaPlayer).start()

        prealarmVolume.onNext(10);
        verifyThatVolumeIs(10.preAlarm())
    }
}
