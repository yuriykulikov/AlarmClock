package com.better.alarm.interfaces

import com.better.alarm.model.AlarmActiveRecord
import com.better.alarm.model.AlarmData
import com.better.alarm.model.AlarmValue
import com.better.alarm.model.Alarmtone
import com.better.alarm.model.DaysOfWeek
import io.reactivex.functions.Consumer

data class AlarmEditor(val callback: Consumer<AlarmValue>,
                       val alarmValue: AlarmValue
) : AlarmValue by alarmValue {
    fun commit() {
        callback.accept(this)
    }

    // withers for Java
    fun withLabel(label: String) = copy(alarmValue = alarmValue.let { it as AlarmData }.copy(label = label))

    fun withHour(hour: Int) = with(hour = hour)
    fun withMinutes(minutes: Int) = with(minutes = minutes)
    fun withIsEnabled(isEnabled: Boolean) = with(enabled = isEnabled)
    fun withDaysOfWeek(daysOfWeek: DaysOfWeek) = with(daysOfWeek = daysOfWeek)
    fun withIsPrealarm(isPrealarm: Boolean) = with(isPrealarm = isPrealarm)

    fun with(hour: Int = alarmValue.hour,
             minutes: Int = alarmValue.minutes,
             enabled: Boolean = alarmValue.isEnabled,
             daysOfWeek: DaysOfWeek = alarmValue.daysOfWeek,
             alarmtone: Alarmtone = alarmValue.alarmtone,
             isPrealarm: Boolean = alarmValue.isPrealarm): AlarmEditor {
        return copy(callback = callback, alarmValue = alarmValue.let {
            if (it is AlarmActiveRecord) it.alarmValue else it as AlarmData
        }.copy(
                hour = hour,
                minutes = minutes,
                isEnabled = enabled,
                daysOfWeek = daysOfWeek,
                alarmtone = alarmtone,
                isPrealarm = isPrealarm
        ))
    }
}
