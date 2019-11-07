package com.better.alarm.model

import android.net.Uri
import android.provider.Settings

/**
 * Created by Yuriy on 11.06.2017.
 */
interface AlarmValue {
    val id: Int

    val isEnabled: Boolean

    val hour: Int

    val minutes: Int

    val daysOfWeek: DaysOfWeek

    val label: String

    val isPrealarm: Boolean

    val isVibrate: Boolean

    val alarmtone: Alarmtone

    /** Whether alarm is skipping one occurrence at the moment*/
    val skipping: Boolean
}

private val defaultAlarmAlertUri = Settings.System.DEFAULT_ALARM_ALERT_URI.toString()

sealed class Alarmtone(open val persistedString: String?) {
    fun ringtoneManagerString(): Uri? {
        return when (this) {
            is Silent -> null
            is Default -> Uri.parse(defaultAlarmAlertUri)
            is Sound -> Uri.parse(this.uriString)
        }
    }

    data class Silent(override val persistedString: String? = null) : Alarmtone(persistedString)
    data class Default(override val persistedString: String? = "") : Alarmtone(persistedString)
    data class Sound(val uriString: String) : Alarmtone(uriString)

    companion object {
        fun fromString(string: String?): Alarmtone {
            return when (string) {
                null -> Silent()
                "" -> Default()
                defaultAlarmAlertUri -> Default()
                else -> Sound(string)
            }
        }
    }
}