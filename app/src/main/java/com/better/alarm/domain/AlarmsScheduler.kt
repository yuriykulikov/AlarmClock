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
package com.better.alarm.domain

import com.better.alarm.BuildConfig
import com.better.alarm.data.AlarmValue
import com.better.alarm.data.CalendarType
import com.better.alarm.data.Prefs
import com.better.alarm.logger.Logger
import com.better.alarm.util.Optional
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.PriorityQueue

class AlarmsScheduler(
    private val setter: AlarmSetter,
    private val log: Logger,
    private val store: Store,
    private val prefs: Prefs,
    private val calendars: Calendars
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
      return "$id $type on ${DATE_FORMAT.format(calendar.time)}"
    }
  }

  private val queue: PriorityQueue<ScheduledAlarm> = PriorityQueue()

  private var isStarted = false

  /** Actually start scheduling alarms */
  fun start() {
    isStarted = true
    fireAlarmsInThePast()
    if (queue.isNotEmpty()) {
      // do not use iterator, order is not defined
      queue.peek()?.let { currentHead ->
        setter.setUpRTCAlarm(currentHead.id, currentHead.type.name, currentHead.calendar)
      }
    }
    notifyListeners()
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
    val prevHead: ScheduledAlarm? = queue.peek()

    // remove if we have already an alarm
    queue.removeAll { it.id == id }
    if (newAlarm != null) {
      queue.add(newAlarm)
    }

    if (isStarted) {
      fireAlarmsInThePast()
    }

    val currentHead: ScheduledAlarm? = queue.peek()
    when {
      !isStarted -> {
        log.trace { "skip setting $currentHead (not started yet)" }
      }
      // no alarms, remove
      currentHead == null -> {
        setter.removeRTCAlarm()
        notifyListeners()
      }
      // update current RTC, id will be used as the request code
      currentHead != prevHead -> {
        setter.setUpRTCAlarm(currentHead.id, currentHead.type.name, currentHead.calendar)
        notifyListeners()
      }
      // if head remains the same, do nothing
      else -> log.trace { "skip setting $currentHead (already set)" }
    }
  }

  /**
   * If two alarms were set for the same time, then the second alarm will be processed in the past.
   * In this case we remove it from the queue and fire it.
   */
  private fun fireAlarmsInThePast() {
    val now = calendars.now()
    while (!queue.isEmpty() && queue.peek()?.calendar?.before(now) == true) {
      // remove happens in fire
      queue.poll()?.let { firedInThePastAlarm ->
        log.warning { "In the past - $firedInThePastAlarm" }
        setter.fireNow(firedInThePastAlarm.id, firedInThePastAlarm.type.name)
      }
    }
  }

  /**
   * TODO the whole mechanism has to be revised. Currently we can only know when next alarm is
   * scheduled and we do not know what is the reason for that. Maybe create a separate component for
   * that or notify from the alarm SM. Actually notify this component from SM :-) and he can notify
   * the rest.
   */
  private fun notifyListeners() {
    findNextNormalAlarm()
        ?.let { scheduledAlarm: ScheduledAlarm ->
          fun findNormalTime(scheduledAlarm: ScheduledAlarm): Long {
            // we can only assume that the real one will be a little later,
            // namely:
            val prealarmOffsetInMillis = prefs.preAlarmDuration.value * 60 * 1000
            return scheduledAlarm.calendar.timeInMillis + prealarmOffsetInMillis
          }

          val isPrealarm = scheduledAlarm.type == CalendarType.PREALARM
          Store.Next(
              isPrealarm = isPrealarm,
              alarm = scheduledAlarm.alarmValue,
              nextNonPrealarmTime =
                  if (isPrealarm) findNormalTime(scheduledAlarm)
                  else scheduledAlarm.calendar.timeInMillis)
        }
        .let { next: Store.Next? -> Optional.fromNullable(next) }
        .run { store.next().onNext(this) }
  }

  private fun findNextNormalAlarm(): ScheduledAlarm? {
    return queue.sorted().firstOrNull { it.type != CalendarType.AUTOSILENCE }
  }

  companion object {
    val DATE_FORMAT: SimpleDateFormat = SimpleDateFormat("dd-MM-yy HH:mm:ss", Locale.GERMANY)
    const val ACTION_FIRED = BuildConfig.APPLICATION_ID + ".ACTION_FIRED"
    const val ACTION_INEXACT_FIRED = BuildConfig.APPLICATION_ID + ".ACTION_INEXACT_FIRED"
    const val EXTRA_ID = "intent.extra.alarm"
    const val EXTRA_TYPE = "intent.extra.type"
  }
}
