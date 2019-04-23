package com.better.alarm.background

import android.content.Context
import android.content.res.Resources
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import com.better.alarm.R
import com.better.alarm.logger.Logger
import com.better.alarm.model.AlarmValue
import com.better.alarm.model.Alarmtone
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.disposables.Disposables

class KlaxonPlugin(
        private val log: Logger,
        private val context: Context,
        private val resources: Resources
        // TODO rename to PlayerFactory
        // private val callback: KlaxonServiceCallback
) : AlertPlugin {
    private var player: MediaPlayer? = null

    override fun go(alarm: AlarmValue, inCall: Observable<Boolean>, volume: Observable<Float>): Disposable {

        player = MediaPlayer().apply {
            setOnErrorListener { mp, what, extra ->
                log.e("Error occurred while playing audio.")
                mp.stop()
                mp.release()
                player = null
                true
            }
        }

        val callSub = inCall.subscribe { inCall ->
            // Check if we are in a call. If we are, use the in-call alarm
            // resource at a low targetVolume to not disrupt the call.
            when {
                inCall -> playInCallAlarm()
                else -> playAlarm(alarm)
            }
        }

        val volumeSub = volume
                .doOnNext { log.d("[KlaxonPlugin] volume $it") }
                .subscribe { currentVolume ->
                    player?.setPerceivedVolume(currentVolume)
                }

        return CompositeDisposable(callSub, volumeSub, Disposables.fromAction {
            player?.stopAndCleanup()
        })
    }

    private fun playAlarm(alarm: AlarmValue) {
        if (alarm.alarmtone !is Alarmtone.Silent) {
            player?.run {
                try {
                    setVolume(0f, 0f)
                    setDataSource(context, alarm.getAlertOrDefault())
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

    private fun playInCallAlarm() {
        log.d("Using the in-call alarm")
        player?.setDataSourceFromResource(resources, R.raw.in_call_alarm)
        player?.startAlarm()
    }

    private fun AlarmValue.getAlertOrDefault(): Uri {
        // Fall back on the default alarm if the database does not have an
        // alarm stored.
        val toPlay = alarmtone
        return when (toPlay) {
            is Alarmtone.Silent -> throw RuntimeException("alarm is silent")
            is Alarmtone.Default -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            is Alarmtone.Sound ->  Uri.parse(toPlay.uriString)
        }
    }

    private fun MediaPlayer.startAlarm() {
        setAudioStreamType(AudioManager.STREAM_ALARM)
        isLooping = true
        prepare()
        start()
    }

    private fun MediaPlayer.setDataSourceFromResource(resources: Resources, res: Int) {
        resources.openRawResourceFd(res)?.run {
            setDataSource(fileDescriptor, startOffset, length)
            close()
        }
    }

    private fun MediaPlayer.setPerceivedVolume(perceived: Float) {
        val volume = perceived.squared()
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

    private fun Float.squared() = this * this
}
