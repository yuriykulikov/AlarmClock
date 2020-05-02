package com.better.alarm.interfaces

import com.better.alarm.model.AlarmValue
import com.better.alarm.model.Alarmtone
import java.util.Calendar

interface Alarm {
    fun enable(enable: Boolean)
    fun snooze()
    fun snooze(hourOfDay: Int, minute: Int)
    fun dismiss()
    fun requestSkip()
    fun isSkipping(): Boolean
    fun delete()

    /** Change something and commit */
    fun edit(func: AlarmValue.() -> AlarmValue)
    val id: Int
    val labelOrDefault: String
    val alarmtone: Alarmtone

    /**
     * @return
     */
    @Deprecated("")
    val snoozedTime: Calendar
    val data: AlarmValue
}