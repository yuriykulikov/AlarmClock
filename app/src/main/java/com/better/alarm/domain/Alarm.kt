package com.better.alarm.domain

import com.better.alarm.data.AlarmValue
import com.better.alarm.data.Alarmtone

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
  val data: AlarmValue
}
