package com.better.alarm.model

import android.media.RingtoneManager
import android.net.Uri
import java.util.Calendar

data class AlarmValue(
    val nextTime: Calendar,
    val state: String,
    val id: Int,
    val isEnabled: Boolean,
    val hour: Int,
    val minutes: Int,
    val isPrealarm: Boolean,
    val alarmtone: Alarmtone,
    val isVibrate: Boolean,
    val label: String,
    val daysOfWeek: DaysOfWeek
) {
  val skipping = state.contentEquals("SkippingSetState")

  val isSilent: Boolean
    get() = alarmtone is Alarmtone.Silent

  // If the database alert is null or it failed to parse, use the
  // default alert.
  @Deprecated("TODO move to where it is used")
  val alert: Uri by lazy {
    when (alarmtone) {
      is Alarmtone.Silent -> throw RuntimeException("Alarm is silent")
      is Alarmtone.Default -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
      is Alarmtone.Sound ->
          try {
            Uri.parse(alarmtone.uriString)
          } catch (e: Exception) {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
          }
    }
  }

  override fun toString(): String {
    val box = if (isEnabled) "[x]" else "[ ]"
    return "$id $box $hour:$minutes $daysOfWeek $label"
  }

  fun withId(id: Int): AlarmValue = copy(id = id)
  fun withState(name: String): AlarmValue = copy(state = name)
  fun withIsEnabled(enabled: Boolean): AlarmValue = copy(isEnabled = enabled)
  fun withNextTime(calendar: Calendar): AlarmValue = copy(nextTime = calendar)
  fun withChangeData(data: AlarmValue) =
      copy(
          id = data.id,
          isEnabled = data.isEnabled,
          hour = data.hour,
          minutes = data.minutes,
          isPrealarm = data.isPrealarm,
          alarmtone = data.alarmtone,
          isVibrate = data.isVibrate,
          label = data.label,
          daysOfWeek = data.daysOfWeek)

  fun withLabel(label: String) = copy(label = label)
  fun withHour(hour: Int) = copy(hour = hour)
  fun withDaysOfWeek(daysOfWeek: DaysOfWeek) = copy(daysOfWeek = daysOfWeek)
  fun withIsPrealarm(isPrealarm: Boolean) = copy(isPrealarm = isPrealarm)
}
