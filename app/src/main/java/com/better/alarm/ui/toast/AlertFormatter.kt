package com.better.alarm.ui.toast

import android.content.Context
import com.better.alarm.R

/**
 * format "Alarm set for 2 days 7 hours and 53 minutes from now"
 *
 * If prealarm is on it will be
 *
 * "Alarm set for 2 days 7 hours and 53 minutes from now. Prealarm will start 30 minutes before the
 * main alarm".
 */
fun formatToast(context: Context, timeInMillis: Long): String {
  val delta = timeInMillis - System.currentTimeMillis()
  val _hours = delta / (1000 * 60 * 60)
  val minutes = delta / (1000 * 60) % 60
  val days = _hours / 24
  val hours = _hours % 24

  val daySeq =
      when (days) {
        0L -> ""
        1L -> context.getString(R.string.day)
        else -> context.getString(R.string.days, days.toString())
      }

  val minSeq =
      when (minutes) {
        0L -> ""
        1L -> context.getString(R.string.minute)
        else -> context.getString(R.string.minutes, minutes.toString())
      }

  val hourSeq =
      when (hours) {
        0L -> ""
        1L -> context.getString(R.string.hour)
        else -> context.getString(R.string.hours, hours.toString())
      }

  val dispDays = days > 0
  val dispHour = hours > 0
  val dispMinute = minutes > 0

  val index =
      when {
        dispDays && !dispHour && !dispMinute -> 1 // days
        !dispDays && dispHour && !dispMinute -> 2 // hours
        dispDays && dispHour && !dispMinute -> 3 // days, hours
        !dispDays && !dispHour && dispMinute -> 4 // minutes
        dispDays && !dispHour && dispMinute -> 5 // days, minutes
        !dispDays && dispHour && dispMinute -> 6 // hours, minutes
        dispDays && dispHour && dispMinute -> 7 // days, hours, minutes
        else -> 0 // Alarm set for less than 1 minute from now.
      }

  val formats = context.resources.getStringArray(R.array.alarm_set)
  return String.format(formats[index], daySeq, hourSeq, minSeq)
}
