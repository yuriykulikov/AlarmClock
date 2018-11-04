package com.better.alarm.configuration

import io.reactivex.Observable
import io.reactivex.Single

/**
 * Created by Yuriy on 10.06.2017.
 */

data class Prefs(
        val _is24HoutFormat: Single<Boolean>,
        val preAlarmDuration: Observable<Int>,
        val snoozeDuration: Observable<Int>,
        val autoSilence: Observable<Int>) {

    fun is24HoutFormat(): Single<Boolean> = _is24HoutFormat
    fun preAlarmDuration(): Observable<Int> = preAlarmDuration
    fun snoozeDuration(): Observable<Int> = snoozeDuration
    fun autoSilence(): Observable<Int> = autoSilence

    companion object {
       @JvmField val KEY_ALARM_IN_SILENT_MODE = "alarm_in_silent_mode"
       @JvmField val KEY_ALARM_SNOOZE = "snooze_duration"
       @JvmField val KEY_DEFAULT_RINGTONE = "default_ringtone"
       @JvmField val KEY_AUTO_SILENCE = "auto_silence"
       @JvmField val KEY_PREALARM_DURATION = "prealarm_duration"
       @JvmField val KEY_FADE_IN_TIME_SEC = "fade_in_time_sec"
       @JvmField val LONGCLICK_DISMISS_DEFAULT = false
       @JvmField val LONGCLICK_DISMISS_KEY = "longclick_dismiss_key"
       @JvmField val MAX_PREALARM_VOLUME = 10
       @JvmField val DEFAULT_PREALARM_VOLUME = 5
       @JvmField val KEY_PREALARM_VOLUME = "key_prealarm_volume"
    }
}
