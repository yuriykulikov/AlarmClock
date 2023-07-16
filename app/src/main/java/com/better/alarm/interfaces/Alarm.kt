package com.better.alarm.interfaces

import com.better.alarm.model.AlarmValue
import com.better.alarm.model.Alarmtone

/**
 * Alarm 인터스페이스가 어떻게 구성되어있는지
 */
interface Alarm {


    /* enable alarm */
    fun enable(enable: Boolean)
    fun snooze()
    fun snooze(hourOfDay: Int, minute: Int)
    fun dismiss() // 알람 끄기
    fun requestSkip()
    fun isSkipping(): Boolean
    fun delete()

    /** Change something and commit */
    fun edit(func: AlarmValue.() -> AlarmValue)
    val id: Int
    val labelOrDefault: String
    val alarmtone: Alarmtone
    val data: AlarmValue
}

//val id: Int,
//val description: String,
//val choices: List<String>,
//val correctAnswer: Int
