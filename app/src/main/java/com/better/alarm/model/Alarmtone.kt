package com.better.alarm.model

import android.net.Uri
import android.provider.Settings

private val defaultAlarmAlertUri = Settings.System.DEFAULT_ALARM_ALERT_URI.toString()

fun Alarmtone.ringtoneManagerString(): Uri? {
  return when (this) {
    is Alarmtone.Silent -> null
    is Alarmtone.Default -> Uri.parse(defaultAlarmAlertUri)
    is Alarmtone.Sound -> Uri.parse(this.uriString)
  }
}

sealed class Alarmtone {
  object Silent : Alarmtone()
  object Default : Alarmtone()
  data class Sound(val uriString: String) : Alarmtone()

  val persistedString: String?
    get() =
        when (this) {
          is Silent -> null
          is Default -> ""
          is Sound -> uriString
        }

  companion object {
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
