package com.better.alarm.model

import android.media.RingtoneManager
import android.net.Uri
import java.util.Calendar

data class AlarmActiveRecord(
        val nextTime: Calendar,
        val state: String,
        val alarmValue: AlarmData
) : AlarmValue by alarmValue {
    override val skipping = state.contentEquals("SkippingSetState")

    val isSilent: Boolean
        get() = alarmtone is Alarmtone.Silent

    // If the database alert is null or it failed to parse, use the
    // default alert.
    @Deprecated("TODO move to where it is used")
    val alert: Uri by lazy {
        when (alarmtone) {
            is Alarmtone.Silent -> throw RuntimeException("Alarm is silent")
            is Alarmtone.Default -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            is Alarmtone.Sound -> try {
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

    fun withId(id: Int): AlarmActiveRecord = copyAndPersist(alarmValue = alarmValue.copy(id = id))
    fun withState(name: String): AlarmActiveRecord = copyAndPersist(state = name)
    fun withIsEnabled(enabled: Boolean): AlarmActiveRecord = copyAndPersist(alarmValue = alarmValue.copy(isEnabled = enabled))
    fun withNextTime(calendar: Calendar): AlarmActiveRecord = copyAndPersist(nextTime = calendar)
    fun withChangeData(data: AlarmValue) = copyAndPersist(alarmValue = AlarmData(
            id = data.id,
            isEnabled = data.isEnabled,
            hour = data.hour,
            minutes = data.minutes,
            isPrealarm = data.isPrealarm,
            alarmtone = data.alarmtone,
            isVibrate = data.isVibrate,
            label = data.label,
            daysOfWeek = data.daysOfWeek,
            skipping = data.skipping
    ))

    fun withLabel(label: String) = copyAndPersist(alarmValue = alarmValue.copy(label = label))

    @Deprecated("use copy", replaceWith = ReplaceWith( "this.copy"))
    fun copyAndPersist(alarmValue: AlarmData = this.alarmValue,
                       nextTime: Calendar = this.nextTime,
                       state: String = this.state
    ): AlarmActiveRecord {
        return copy(nextTime = nextTime, state = state, alarmValue = alarmValue)
    }
}

data class AlarmData(
        override val id: Int,
        override val isEnabled: Boolean,
        override val hour: Int,
        override val minutes: Int,
        override val isPrealarm: Boolean,
        override val alarmtone: Alarmtone,
        override val isVibrate: Boolean,
        override val label: String,
        override val daysOfWeek: DaysOfWeek,
        override val skipping: Boolean = false
) : AlarmValue {
    companion object {
        fun from(value: AlarmValue): AlarmData {
            return value.run {
                AlarmData(id, isEnabled, hour, minutes, isPrealarm, alarmtone, isVibrate, label, daysOfWeek, skipping)
            }
        }
    }
}
