/*
 * Copyright (C) 2012 Yuriy Kulikov yuriy.kulikov.87@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.better.alarm.model

import com.better.alarm.BuildConfig
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AlarmsScheduler(
    private val setter: AlarmSetter,
) : IAlarmsScheduler {

    data class ScheduledAlarm(
        val id: Int,
        val calendar: Calendar,
        val type: CalendarType,
        val alarmValue: AlarmValue
    ) : Comparable<ScheduledAlarm> {

        override fun compareTo(other: ScheduledAlarm): Int {
            return this.calendar.compareTo(other.calendar)
        }

        override fun toString(): String {
            return "$id " +
                "on ${DATE_FORMAT.format(calendar.time)}"
        }
    }

    private var isStarted = false

    /** Actually start scheduling alarms */
    fun start() {
        isStarted = true
    }

    override fun setAlarm(id: Int, type: CalendarType, calendar: Calendar, alarmValue: AlarmValue) {
        val scheduledAlarm = ScheduledAlarm(id, calendar, type, alarmValue)
        replaceAlarm(id, scheduledAlarm)
    }

    override fun setInexactAlarm(id: Int, cal: Calendar) {
        setter.setInexactAlarm(id, cal)
    }

    override fun removeInexactAlarm(id: Int) {
        setter.removeInexactAlarm(id)
    }

    override fun removeAlarm(id: Int) {
        replaceAlarm(id, null)
    }

    private fun replaceAlarm(id: Int, newAlarm: ScheduledAlarm?) {
        when
        {
            !isStarted -> {
            }
            // no alarms, remove
            newAlarm == null -> {
                setter.removeRTCAlarm(id)
            }
            // update current RTC, id will be used as the request code
            // if head remains the same, do nothing
            else -> {
                setter.setUpRTCAlarm(newAlarm.id, newAlarm.calendar)

            }
        }
    }

  companion object {
      val DATE_FORMAT: SimpleDateFormat = SimpleDateFormat("dd-MM-yy HH:mm:ss", Locale.GERMANY)
    const val ACTION_FIRED = BuildConfig.APPLICATION_ID + ".ACTION_FIRED"
    const val ACTION_INEXACT_FIRED = BuildConfig.APPLICATION_ID + ".ACTION_INEXACT_FIRED"
    const val EXTRA_ID = "intent.extra.alarm"
  }
}
