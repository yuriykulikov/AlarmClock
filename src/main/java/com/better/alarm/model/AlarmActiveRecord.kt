package com.better.alarm.model

import android.media.RingtoneManager
import android.net.Uri
import java.util.*

class AlarmActiveRecord(
        val nextTime: Calendar,
        val state: String,
        val persistence: Persistence,
        val alarmValue: AlarmData
) : AlarmValue by alarmValue {
    // This string is used to indicate a silent alarm in the db.
    private val ALARM_ALERT_SILENT = "silent"

    val isSilent: Boolean
        get() = ALARM_ALERT_SILENT == alarmValue.alertString

    // @Value.Lazy
    // If the database alert is null or it failed to parse, use the
    // default alert.
    @Deprecated("TODO move to where it is used")
    val alert: Uri by lazy {
        val alertString = alertString
        if (alertString.isNotEmpty()) {
            try {
                Uri.parse(alertString)
            } catch (e: Exception) {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            }
        } else {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        }
    }

    interface Persistence {
        fun persist(activeRecord: AlarmActiveRecord)
        fun delete(activeRecord: AlarmActiveRecord)

    }

    fun delete() {
        persistence.delete(this)
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
            alertString = data.alertString,
            isVibrate = data.isVibrate,
            label = data.label,
            daysOfWeek = data.daysOfWeek
    ))

    fun withLabel(label: String) = copyAndPersist(alarmValue = alarmValue.copy(label = label))

    fun copyAndPersist(alarmValue: AlarmData = this.alarmValue,
                       persistence: Persistence = this.persistence,
                       nextTime: Calendar = this.nextTime,
                       state: String = this.state
    ): AlarmActiveRecord {
        return AlarmActiveRecord(nextTime, state, persistence, alarmValue)
                .apply { persistence.persist(this) }
    }
}

data class AlarmData(
        override val id: Int,
        override val isEnabled: Boolean,
        override val hour: Int,
        override val minutes: Int,
        override val isPrealarm: Boolean,
        override val alertString: String,
        override val isVibrate: Boolean,
        override val label: String,
        override val daysOfWeek: DaysOfWeek
) : AlarmValue
