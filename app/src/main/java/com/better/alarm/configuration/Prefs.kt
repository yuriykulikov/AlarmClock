package com.better.alarm.configuration

import com.better.alarm.lollipop
import io.reactivex.Observable
import io.reactivex.Single

/**
 * Created by Yuriy on 10.06.2017.
 */

data class Prefs(
        val _is24HoutFormat: Single<Boolean>,
        val preAlarmDuration: Observable<Int>,
        val snoozeDuration: Observable<Int>,
        val listRowLayout: Observable<String>,
        val autoSilence: Observable<Int>) {

    fun is24HoutFormat(): Single<Boolean> = _is24HoutFormat
    fun preAlarmDuration(): Observable<Int> = preAlarmDuration
    fun snoozeDuration(): Observable<Int> = snoozeDuration
    fun listRowLayout(): Observable<String> = listRowLayout
    fun autoSilence(): Observable<Int> = autoSilence
    fun isCompact(): Boolean {
        return listRowLayout
                .take(1)
                .map {
                    when {
                        !lollipop() -> false
                        it == LIST_ROW_LAYOUT_COMPACT -> true
                        else -> false
                    }
                }.blockingFirst()
    }

    companion object {
        @JvmField
        val KEY_ALARM_IN_SILENT_MODE = "alarm_in_silent_mode"
        @JvmField
        val KEY_ALARM_SNOOZE = "snooze_duration"
        @JvmField
        val KEY_DEFAULT_RINGTONE = "default_ringtone"
        @JvmField
        val KEY_AUTO_SILENCE = "auto_silence"
        @JvmField
        val KEY_PREALARM_DURATION = "prealarm_duration"
        @JvmField
        val KEY_FADE_IN_TIME_SEC = "fade_in_time_sec"
        @JvmField
        val LONGCLICK_DISMISS_DEFAULT = false
        @JvmField
        val LONGCLICK_DISMISS_KEY = "longclick_dismiss_key"
        @JvmField
        val MAX_PREALARM_VOLUME = 10
        @JvmField
        val DEFAULT_PREALARM_VOLUME = 5
        @JvmField
        val KEY_PREALARM_VOLUME = "key_prealarm_volume"
        @JvmField
        val LIST_ROW_LAYOUT = "list_row_layout"
        @JvmField
        val LIST_ROW_LAYOUT_COMPACT = "compact"
    }
}
