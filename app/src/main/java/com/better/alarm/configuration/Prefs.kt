package com.better.alarm.configuration

import com.better.alarm.lollipop
import io.reactivex.Observable
import io.reactivex.Single

/**
 * Created by Yuriy on 10.06.2017.
 */

enum class Layout {
    CLASSIC, COMPACT, BOLD;
}

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
    fun layout(): Layout {
        return listRowLayout
                .take(1)
                .map {
                    when {
                        !lollipop() -> Layout.CLASSIC
                        it == LIST_ROW_LAYOUT_CLASSIC -> Layout.CLASSIC
                        it == LIST_ROW_LAYOUT_COMPACT -> Layout.COMPACT
                        else -> Layout.BOLD
                    }
                }.blockingFirst()
    }

    companion object {
        const val KEY_ALARM_IN_SILENT_MODE = "alarm_in_silent_mode"
        const val KEY_ALARM_SNOOZE = "snooze_duration"
        const val KEY_AUTO_SILENCE = "auto_silence"
        const val KEY_PREALARM_DURATION = "prealarm_duration"
        const val KEY_FADE_IN_TIME_SEC = "fade_in_time_sec"
        const val LONGCLICK_DISMISS_DEFAULT = false
        const val LONGCLICK_DISMISS_KEY = "longclick_dismiss_key"
        const val MAX_PREALARM_VOLUME = 10
        const val DEFAULT_PREALARM_VOLUME = 5
        const val KEY_PREALARM_VOLUME = "key_prealarm_volume"
        const val LIST_ROW_LAYOUT = "list_row_layout"
        const val LIST_ROW_LAYOUT_COMPACT = "compact"
        const val LIST_ROW_LAYOUT_CLASSIC = "classic"
    }
}
