package com.better.alarm.background

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.PowerManager
import android.telephony.TelephonyManager
import com.better.alarm.R
import com.better.alarm.interfaces.Alarm
import com.better.alarm.interfaces.IAlarmsManager
import com.better.alarm.interfaces.Intents
import com.better.alarm.logger.Logger
import com.better.alarm.wakelock.WakeLockManager
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.disposables.Disposables
import io.reactivex.functions.BiFunction
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates

/**
 * Created by Yuriy on 20.08.2017.
 */
class KlaxonServiceDelegate(
        private val log: Logger,
        pm: PowerManager,
        private val wakeLocks: WakeLockManager,
        private val alarms: IAlarmsManager,
        private val context: Context,
        private val resources: Resources,
        private val callState: Observable<Int>,
        private val prealarmVolume: Observable<Int>,
        private val fadeInTimeInSeconds: Observable<Int>,
        private val callback: KlaxonServiceCallback,
        private val scheduler: Scheduler
) : KlaxonService.KlaxonDelegate {
    private val disposables = CompositeDisposable()
    private var volume: Volume by Delegates.notNull()
    private var wakeLock: PowerManager.WakeLock by Delegates.notNull()
    private var player: MediaPlayer? = null

    var callStateSub: Disposable = Disposables.disposed()

    init {
        volume = Volume()
        player = null
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "KlaxonService")
        wakeLock.acquire()
    }

    override fun onDestroy() {
        player?.stopAndCleanup()
        disposables.dispose()
        wakeLock.release()
        log.d("Service destroyed")
    }

    override fun onStartCommand(intent: Intent): Boolean {
        wakeLocks.releasePartialWakeLock(intent)

        val action: String = intent.action

        log.d(intent.action)

        when (action) {
            Intents.ALARM_ALERT_ACTION, Intents.ALARM_PREALARM_ACTION -> {
                val alarm = alarms.getAlarm(intent.getIntExtra(Intents.EXTRA_ID, -1))
                val type = if (action == Intents.ALARM_PREALARM_ACTION) Type.PREALARM else Type.NORMAL

                // Listen for incoming calls to kill the alarm.
                callStateSub.dispose()
                callStateSub = callState
                        .map { it != TelephonyManager.CALL_STATE_IDLE }
                        .distinctUntilChanged()
                        .skip(1)//ignore the first one
                        .subscribe {
                            callActive ->
                            if (callActive) {
                                log.d("Call has started. Mute.")
                                volume.mute()
                            } else {
                                log.d("Call has ended. fadeInFast.")
                                if (!alarm.isSilent) {
                                    initializePlayer(alarm.getAlertOrDefault())
                                    volume.fadeInFast()
                                }
                            }
                        }

                onAlarm(alarm, type)
            }
            Intents.ACTION_START_PREALARM_SAMPLE -> onStartAlarmSample()
            Intents.ACTION_MUTE -> volume.mute()
            Intents.ACTION_DEMUTE -> volume.fadeInFast()
            else -> {
                volume.mute()
                callback.stopSelf()
            }
        }

        return when (action) {
            Intents.ALARM_ALERT_ACTION,
            Intents.ALARM_PREALARM_ACTION,
            Intents.ACTION_START_PREALARM_SAMPLE,
            Intents.ACTION_MUTE,
            Intents.ACTION_DEMUTE -> true
            else -> false
        }
    }

    private fun onAlarm(alarm: Alarm, type: Type) {
        volume.mute()
        volume.type = type
        if (!alarm.isSilent) {
            initializePlayer(alarm.getAlertOrDefault())
            volume.fadeInAsSetInSettings()
        }
    }

    private fun onStartAlarmSample() {
        volume.mute()
        volume.type = Type.PREALARM
        // if already playing do nothing. In this case signal continues.

        if (player == null || !player!!.isPlaying) {
            initializePlayer(callback.getDefaultUri(RingtoneManager.TYPE_ALARM))
        }

        volume.apply()
    }

    /**
     * Inits player and sets volume to 0

     * @param alert
     */
    private fun initializePlayer(alert: Uri) {
        // stop() checks to see if we are already playing.
        player?.stopAndCleanup()

        player = callback.createMediaPlayer().apply {
            setOnErrorListener { mp, what, extra ->
                log.e("Error occurred while playing audio.")
                volume.mute()
                mp.stop()
                mp.release()
                player = null
                true
            }
        }

        volume.mute()

        callState.map { it != TelephonyManager.CALL_STATE_IDLE }.firstOrError().subscribe { inCall ->
            // Check if we are in a call. If we are, use the in-call alarm
            // resource at a low targetVolume to not disrupt the call.
            if (inCall) {
                log.d("Using the in-call alarm")
                player?.setDataSourceFromResource(resources, R.raw.in_call_alarm)
                player?.startAlarm()
            } else {
                player?.run {
                    try {
                        setDataSource(context, alert)
                        startAlarm()
                    } catch (ex: Exception) {
                        log.w("Using the fallback ringtone")
                        // The alert may be on the sd card which could be busy right
                        // now. Use the fallback ringtone.
                        // Must reset the media player to clear the error state.
                        reset()
                        setDataSourceFromResource(resources, R.raw.fallbackring)
                        startAlarm()
                    }
                }
            }
        }
    }

    private fun Alarm.getAlertOrDefault(): Uri {
        // Fall back on the default alarm if the database does not have an
        // alarm stored.
        return if (alert == null) {
            val default: Uri? = callback.getDefaultUri(RingtoneManager.TYPE_ALARM)
            log.d("Using default alarm: " + default.toString())
            //TODO("Check this")
            default!!
        } else {
            alert
        }
    }

    // Do the common stuff when starting the alarm.
    @Throws(java.io.IOException::class, IllegalArgumentException::class, IllegalStateException::class)
    private fun MediaPlayer.startAlarm() {
        // do not play alarms if stream targetVolume is 0
        // (typically because ringer mode is silent).
        //if (audioManager.getStreamVolume(AudioManager.STREAM_ALARM) != 0) {
        setAudioStreamType(AudioManager.STREAM_ALARM)
        isLooping = true
        prepare()
        start()
        //}
    }

    @Throws(java.io.IOException::class)
    private fun MediaPlayer.setDataSourceFromResource(resources: Resources, res: Int) {
        resources.openRawResourceFd(res)?.run {
            setDataSource(fileDescriptor, startOffset, length)
            close()
        }
    }

    private fun MediaPlayer.setPerceptedVolume(percepted: Float) {
        val volume = percepted.squared()
        //log.d("volume=$volume")
        setVolume(volume, volume)
    }

    /**
     * Stops alarm audio
     */
    private fun MediaPlayer.stopAndCleanup() {
        log.d("stopping media player")
        try {
            if (isPlaying) stop()
            release()
        } finally {
            player = null
        }
    }

    enum class Type {
        NORMAL, PREALARM
    }

    private inner class Volume internal constructor() {
        private val FAST_FADE_IN_TIME = 5000

        private val FADE_IN_STEPS = 100

        // Volume suggested by media team for in-call alarms.
        private val IN_CALL_VOLUME = 0.125f

        private val SILENT = 0f

        internal var type = Type.NORMAL

        private var timer: Disposable = Disposables.disposed()

        /**
         * Instantly apply the targetVolume. To fade in use [.fadeInAsSetInSettings]
         */
        fun apply() {
            fadeIn(250)
        }

        /**
         * Fade in to set targetVolume
         */
        fun fadeInAsSetInSettings() {
            fadeInTimeInSeconds.firstOrError().subscribe(this::fadeIn)
        }

        fun fadeInFast() {
            fadeIn(FAST_FADE_IN_TIME)
        }

        fun mute() {
            player?.setPerceptedVolume(SILENT)
            timer.dispose()
        }

        private fun fadeIn(time: Int) {
            val fadeInTime: Long = time.toLong()
            mute()
            player?.setPerceptedVolume(SILENT)

            val fadeInStep: Long = fadeInTime / FADE_IN_STEPS

            val fadeIn = Observable.interval(fadeInStep, TimeUnit.MILLISECONDS, scheduler)
                    .map { it * fadeInStep }
                    .takeWhile { it <= fadeInTime }
                    .map { elapsed -> elapsed.toFloat() / fadeInTime }
                    .map { fraction -> fraction.squared() }
                    .doOnComplete { log.d("Completed fade-in in $time milliseconds") }

            timer = Observable.combineLatest(
                    observeVolume(type),
                    fadeIn,
                    BiFunction<Float, Float, Float> { targetVolume, frqs -> frqs * targetVolume })
                    .subscribe { perceptedVolume ->
                        player?.setPerceptedVolume(perceptedVolume)
                    }
        }

        /**
         * Gets 1f doe NORMAL and a fraction of 0.5f for PREALARM
         */
        private fun observeVolume(type: Type): Observable<Float> {
            val MAX_VOLUME = 11
            return if (type == Type.NORMAL) Observable.just(1f)
            else prealarmVolume.map {
                it
                        .plus(1)//0 is 1
                        .coerceAtMost(MAX_VOLUME)
                        .toFloat()
                        .div(MAX_VOLUME)
                        .div(2)
                        .apply { log.d("targetVolume=$this") }
            }
        }
    }

    fun Float.squared() = this * this
}