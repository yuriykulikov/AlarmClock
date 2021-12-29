package com.better.alarm.configuration

import com.better.alarm.lollipop
import com.better.alarm.stores.PrimitiveDataStoreFactory
import com.better.alarm.stores.RxDataStore
import com.better.alarm.stores.intStringDataStore
import io.reactivex.Single

/** Created by Yuriy on 10.06.2017. */
enum class Layout {
  CLASSIC,
  COMPACT,
  BOLD
}

class Prefs
private constructor(
    val is24HourFormat: Single<Boolean>,
    val preAlarmDuration: RxDataStore<Int>,
    val preAlarmVolume: RxDataStore<Int>,
    val snoozeDuration: RxDataStore<Int>,
    val listRowLayout: RxDataStore<String>,
    val autoSilence: RxDataStore<Int>,
    val fadeInTimeInSeconds: RxDataStore<Int>,
    val vibrate: RxDataStore<Boolean>,
    val skipDuration: RxDataStore<Int>,
    val longClickDismiss: RxDataStore<Boolean>,
    val theme: RxDataStore<String>,
) {
  fun layout(): Layout {
    return listRowLayout
        .observe()
        .take(1)
        .map {
          when {
            !lollipop() -> Layout.CLASSIC
            it == LIST_ROW_LAYOUT_CLASSIC -> Layout.CLASSIC
            it == LIST_ROW_LAYOUT_COMPACT -> Layout.COMPACT
            else -> Layout.BOLD
          }
        }
        .blockingFirst()
  }

  companion object {

    @JvmStatic
    fun create(is24HourFormat: Single<Boolean>, factory: PrimitiveDataStoreFactory): Prefs {
      return Prefs(
          is24HourFormat = is24HourFormat,
          preAlarmDuration = factory.intStringDataStore(KEY_PREALARM_DURATION, 30),
          preAlarmVolume = factory.intDataStore(KEY_PREALARM_VOLUME, 5),
          snoozeDuration = factory.intStringDataStore(KEY_ALARM_SNOOZE, 10),
          listRowLayout = factory.stringDataStore(LIST_ROW_LAYOUT, LIST_ROW_LAYOUT_BOLD),
          autoSilence = factory.intStringDataStore(KEY_AUTO_SILENCE, 10),
          fadeInTimeInSeconds = factory.intStringDataStore(KEY_FADE_IN_TIME_SEC, 30),
          vibrate = factory.booleanDataStore(KEY_VIBRATE, true),
          skipDuration = factory.intStringDataStore(KEY_SKIP_DURATION, 30),
          longClickDismiss = factory.booleanDataStore(KEY_LONGCLICK_DISMISS, true),
          theme = factory.stringDataStore(KEY_THEME, "deusex"),
      )
    }

    const val KEY_THEME = "ui_theme"
    const val KEY_VIBRATE = "vibrate"
    const val KEY_SKIP_DURATION = "skip_notification_time"
    const val KEY_ALARM_IN_SILENT_MODE = "alarm_in_silent_mode"
    const val KEY_ALARM_SNOOZE = "snooze_duration"
    const val KEY_AUTO_SILENCE = "auto_silence"
    const val KEY_PREALARM_DURATION = "prealarm_duration"
    const val KEY_FADE_IN_TIME_SEC = "fade_in_time_sec"
    const val KEY_LONGCLICK_DISMISS = "longclick_dismiss_key"
    const val MAX_PREALARM_VOLUME = 10
    const val KEY_PREALARM_VOLUME = "key_prealarm_volume"
    const val KEY_VOLUME_PREFERENCE = "volume_preference"
    const val LIST_ROW_LAYOUT = "ui_list_row_layout"
    const val LIST_ROW_LAYOUT_COMPACT = "compact"
    const val LIST_ROW_LAYOUT_CLASSIC = "classic"
    const val LIST_ROW_LAYOUT_BOLD = "bold"
  }
}
