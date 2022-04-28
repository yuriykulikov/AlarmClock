package com.better.alarm.model

import android.net.Uri
import android.provider.Settings
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private val defaultAlarmAlertUri = Settings.System.DEFAULT_ALARM_ALERT_URI.toString()

fun Alarmtone.ringtoneManagerString(): Uri? {
  return when (this) {
    is Alarmtone.Silent -> null
    is Alarmtone.Default -> Uri.parse(defaultAlarmAlertUri)
    is Alarmtone.Sound -> Uri.parse(this.uriString)
  }
}

@Serializable
sealed class Alarmtone {
  @SerialName("Silent") @Serializable object Silent : Alarmtone()
  @SerialName("Default") @Serializable object Default : Alarmtone()
  @SerialName("Sound") @Serializable data class Sound(val uriString: String) : Alarmtone()

  companion object {
    /** For migration from ContentProvider table */
    fun fromString(string: String?): Alarmtone {
      return when (string) {
        null -> Silent
        "" -> Default
        defaultAlarmAlertUri -> Default
        else -> Sound(string)
      }
    }
  }
}
