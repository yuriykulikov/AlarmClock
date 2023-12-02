package com.better.alarm.data

import java.text.SimpleDateFormat
import java.util.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
data class AlarmValues(
    val alarms: Map<Int, AlarmValue> = emptyMap(),
)

val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

@Serializable
data class AlarmValue(
    @Serializable(with = CalendarSerializer::class)
    val nextTime: Calendar = Calendar.getInstance().apply { timeInMillis = 0 },
    val state: String = "DisabledState",
    val id: Int = -1,
    val isEnabled: Boolean = false,
    val hour: Int = 0,
    val minutes: Int = 0,
    val isPrealarm: Boolean = false,
    val alarmtone: Alarmtone = Alarmtone.Default,
    val isVibrate: Boolean = true,
    val label: String = "",
    val daysOfWeek: DaysOfWeek = DaysOfWeek(0),
    val isDeleteAfterDismiss: Boolean = false,
    @Serializable(with = CalendarSerializer::class) val date: Calendar? = null,
) {
  val skipping
    get() = state.contentEquals("SkippingSetState")

  val isRepeatSet
    get() = date == null && daysOfWeek.coded != 0

  override fun toString(): String {
    val box = if (isEnabled) "[x]" else "[ ]"
    val dateOrRepeat =
        if (date != null) dateFormat.format(date.timeInMillis) else daysOfWeek.toString()
    return "$id $box $hour:$minutes $dateOrRepeat $label"
  }

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
          daysOfWeek = data.daysOfWeek,
          isDeleteAfterDismiss = data.isDeleteAfterDismiss,
          date = data.date,
      )

  fun withHour(hour: Int) = copy(hour = hour)

  fun withDaysOfWeek(daysOfWeek: DaysOfWeek) = copy(daysOfWeek = daysOfWeek)

  fun withIsPrealarm(isPrealarm: Boolean) = copy(isPrealarm = isPrealarm)
}

object CalendarSerializer : KSerializer<Calendar> {
  override val descriptor: SerialDescriptor
    get() = Long.serializer().descriptor

  override fun deserialize(decoder: Decoder): Calendar {
    return Calendar.getInstance().apply { timeInMillis = decoder.decodeLong() }
  }

  override fun serialize(encoder: Encoder, value: Calendar) {
    encoder.encodeLong(value.timeInMillis)
  }
}
