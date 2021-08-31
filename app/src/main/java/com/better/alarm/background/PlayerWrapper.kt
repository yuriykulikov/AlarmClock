package com.better.alarm.background

import android.content.Context
import android.content.res.Resources
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import com.better.alarm.logger.Logger
import com.better.alarm.model.Alarmtone

class PlayerWrapper(val resources: Resources, val context: Context, val log: Logger) : Player {
  override fun setDataSource(alarmtone: Alarmtone) {
    // Fall back on the default alarm if the database does not have an
    // alarm stored.
    val uri =
        when (alarmtone) {
          is Alarmtone.Silent -> throw RuntimeException("alarm is silent")
          is Alarmtone.Default -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
          is Alarmtone.Sound -> Uri.parse(alarmtone.uriString)
        }

    player?.setDataSource(context, uri)
  }

  private var player: MediaPlayer? =
      MediaPlayer().apply {
        setOnErrorListener { mp, _, _ ->
          log.e("Error occurred while playing audio.")
          mp.stop()
          mp.release()
          player = null
          true
        }
      }

  override fun startAlarm() {
    player?.runCatching {
      setAudioStreamType(AudioManager.STREAM_ALARM)
      isLooping = true
      prepare()
      start()
    }
  }

  override fun setDataSourceFromResource(res: Int) {
    resources.openRawResourceFd(res)?.run {
      player?.setDataSource(fileDescriptor, startOffset, length)
      close()
    }
  }

  override fun setPerceivedVolume(perceived: Float) {
    val volume = perceived.squared()
    player?.setVolume(volume, volume)
  }

  /** Stops alarm audio */
  override fun stop() {
    try {
      player?.run {
        if (isPlaying) stop()
        release()
      }
    } finally {
      player = null
    }
  }

  override fun reset() {
    player?.reset()
  }

  private fun Float.squared() = this * this
}
