package com.better.alarm.data

import android.content.Context
import com.better.alarm.R
import java.text.DateFormatSymbols
import java.util.Calendar
import kotlinx.serialization.Serializable

/**
 * Days of week code as a single int.
 * * 0x00: no day
 * * 0x01: Monday
 * * 0x02: Tuesday
 * * 0x04: Wednesday
 * * 0x08: Thursday
 * * 0x10: Friday
 * * 0x20: Saturday
 * * 0x40: Sunday
 */
@Serializable
data class DaysOfWeek(val coded: Int) {
  fun isDaySet(dayIndex: Int): Boolean {
    return dayIndex.isSet()
  }

  fun toString(context: Context, showNever: Boolean): String {
    return when {
      coded == 0 && showNever -> context.getText(R.string.never).toString()
      coded == 0 -> ""
      // every day
      coded == 0x7f -> return context.getText(R.string.every_day).toString()
      // count selected days
      else -> {
        val dayCount = (0..6).count { it.isSet() }
        // short or long form?
        val dayStrings =
            when {
              dayCount > 1 -> DateFormatSymbols().shortWeekdays
              else -> DateFormatSymbols().weekdays
            }

        (0..6)
            .filter { it.isSet() }
            .map { dayIndex -> DAY_MAP[dayIndex] }
            .map { calDay -> dayStrings[calDay] }
            .joinToString(context.getText(R.string.day_concat))
      }
    }
  }

  private fun Int.isSet(): Boolean {
    return coded and (1 shl this) > 0
  }

  /** returns number of days from today until next alarm */
  fun getNextAlarm(today: Calendar): Int {
    val todayIndex = (today.get(Calendar.DAY_OF_WEEK) + 5) % 7

    return (0..6).firstOrNull { dayCount ->
      val day = (todayIndex + dayCount) % 7
      day.isSet()
    }
        ?: -1
  }

  override fun toString(): String {
    return (if (0.isSet()) "m" else "_") +
        (if (1.isSet()) 't' else '_') +
        (if (2.isSet()) 'w' else '_') +
        (if (3.isSet()) 't' else '_') +
        (if (4.isSet()) 'f' else '_') +
        (if (5.isSet()) 's' else '_') +
        if (6.isSet()) 's' else '_'
  }

  fun withSet(position: Int): DaysOfWeek {
    require(position in 0..7) { "Invalid position: $position" }
    return DaysOfWeek(coded or (1 shl position))
  }

  fun withCleared(position: Int): DaysOfWeek {
    require(position in 0..7) { "Invalid position: $position" }
    return DaysOfWeek(coded and (1 shl position).inv())
  }

  companion object {
    private val DAY_MAP =
        intArrayOf(
            Calendar.MONDAY,
            Calendar.TUESDAY,
            Calendar.WEDNESDAY,
            Calendar.THURSDAY,
            Calendar.FRIDAY,
            Calendar.SATURDAY,
            Calendar.SUNDAY)
  }
}
