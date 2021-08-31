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
          preAlarmDuration = factory.intStringDataStore("prealarm_duration", 30),
          preAlarmVolume = factory.intDataStore(KEY_PREALARM_VOLUME, DEFAULT_PREALARM_VOLUME),
          snoozeDuration = factory.intStringDataStore("snooze_duration", 10),
          listRowLayout = factory.stringDataStore(LIST_ROW_LAYOUT, LIST_ROW_LAYOUT_COMPACT),
          autoSilence = factory.intStringDataStore("auto_silence", 10),
          fadeInTimeInSeconds = factory.intStringDataStore(Prefs.KEY_FADE_IN_TIME_SEC, 30),
          vibrate = factory.booleanDataStore("vibrate", true),
          skipDuration = factory.intStringDataStore(SKIP_DURATION_KEY, -1),
          longClickDismiss =
              factory.booleanDataStore(LONGCLICK_DISMISS_KEY, LONGCLICK_DISMISS_DEFAULT),
          theme = factory.stringDataStore("theme", "dark"),
      )
    }

    const val SKIP_DURATION_KEY = "skip_duration"
    const val KEY_ALARM_IN_SILENT_MODE = "alarm_in_silent_mode"
    const val KEY_ALARM_SNOOZE = "snooze_duration"
    const val KEY_AUTO_SILENCE = "auto_silence"
    const val KEY_PREALARM_DURATION = "prealarm_duration"
    const val KEY_FADE_IN_TIME_SEC = "fade_in_time_sec"
    const val LONGCLICK_DISMISS_DEFAULT = true
    const val LONGCLICK_DISMISS_KEY = "longclick_dismiss_key"
    const val MAX_PREALARM_VOLUME = 10
    const val DEFAULT_PREALARM_VOLUME = 5
    const val KEY_PREALARM_VOLUME = "key_prealarm_volume"
    const val LIST_ROW_LAYOUT = "list_row_layout"
    const val LIST_ROW_LAYOUT_COMPACT = "compact"
    const val LIST_ROW_LAYOUT_CLASSIC = "classic"
  }
}
