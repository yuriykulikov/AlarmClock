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
import com.better.alarm.configuration.Prefs
import com.better.alarm.configuration.Store
import com.better.alarm.logger.Logger
import com.better.alarm.util.Optional
import java.text.SimpleDateFormat
import java.util.*

class AlarmsScheduler(private val setter: AlarmSetter, private val log: Logger, private val store: Store, private val prefs: Prefs, private val calendars: Calendars) : IAlarmsScheduler {
    private val queue: PriorityQueue<ScheduledAlarm> = PriorityQueue()

    data class ScheduledAlarm(
            val id: Int,
            val calendar: Calendar? = null,
            val type: CalendarType? = null,
            val alarmValue: AlarmValue? = null)
        : Comparable<ScheduledAlarm> {

        override fun compareTo(another: ScheduledAlarm): Int {
            return this.calendar!!.compareTo(another.calendar)
        }

        override fun toString(): String {
            val formattedTime: String? = calendar?.let { DATE_FORMAT.format(it.time) }
            return "$id $type on $formattedTime"
        }
    }

    override fun setAlarm(id: Int, type: CalendarType, calendar: Calendar, alarmValue: AlarmValue) {
        val scheduledAlarm = ScheduledAlarm(id, calendar, type, alarmValue)
        replaceAlarm(scheduledAlarm, true)
    }

    override fun removeAlarm(id: Int) {
        replaceAlarm(ScheduledAlarm(id), false)
    }

    override fun onAlarmFired(id: Int) {
        replaceAlarm(ScheduledAlarm(id), false)
    }

    private fun replaceAlarm(newAlarm: ScheduledAlarm, add: Boolean) {
        val previousHead: ScheduledAlarm? = queue.peek()

        // remove if we have already an alarm
        queue.removeAll { it.id == newAlarm.id }

        if (add) {
            queue.add(newAlarm)
        }

        fireAlarmsInThePast()

        val currentHead = queue.peek()
        // compare by reference!
        // TODO should we compare content? Previously this was because of equals implementation
        if (previousHead !== currentHead) {
            if (!queue.isEmpty()) {
                previousHead?.run { setter.removeRTCAlarm(this) }
                setter.setUpRTCAlarm(currentHead)
            } else {
                log.d("no more alarms to schedule, remove pending intent")
                previousHead?.run { setter.removeRTCAlarm(this) }
            }
            notifyListeners()
        }
    }

    /**
     * If two alarms were set for the same time, then the second alarm will be
     * processed in the past. In this case we remove it from the queue and fire
     * it.
     */

    private fun fireAlarmsInThePast() {
        val now = calendars.now()
        while (!queue.isEmpty() && queue.peek().calendar!!.before(now)) {
            // remove happens in fire
            val firedInThePastAlarm = queue.poll()
            log.d("In the past - " + firedInThePastAlarm.toString())
            setter.fireNow(firedInThePastAlarm)
        }
    }

    /**
     * TODO the whole mechanism has to be revised. Currently we can only know
     * when next alarm is scheduled and we do not know what is the reason for
     * that. Maybe create a separate component for that or notify from the alarm
     * SM. Actually notify this component from SM :-) and he can notify the
     * rest.
     */
    private fun notifyListeners() {
        findNextNormalAlarm()
                ?.let { scheduledAlarm: ScheduledAlarm ->
                    fun findNormalTime(scheduledAlarm: ScheduledAlarm): Long {
                        // we can only assume that the real one will be a little later,
                        // namely:
                        val prealarmOffsetInMillis = prefs.preAlarmDuration().blockingFirst() * 60 * 1000
                        return scheduledAlarm.calendar!!.timeInMillis + prealarmOffsetInMillis
                    }

                    val isPrealarm = scheduledAlarm.type == CalendarType.PREALARM
                    Store.Next(
                            /* isPrealarm */ isPrealarm,
                            /* alarm */ scheduledAlarm.alarmValue!!,
                            /* nextNonPrealarmTime */ if (isPrealarm) findNormalTime(scheduledAlarm) else scheduledAlarm.calendar!!.timeInMillis)


                }
                .let { next: Store.Next? -> Optional.fromNullable(next) }
                .run { store.next().onNext(this) }
    }

    private fun findNextNormalAlarm(): ScheduledAlarm? {
        return queue
                .sorted()
                .firstOrNull { it.type != CalendarType.AUTOSILENCE }
    }

    companion object {
        val DATE_FORMAT: SimpleDateFormat = SimpleDateFormat("dd-MM-yy HH:mm:ss", Locale.GERMANY)
        const val ACTION_FIRED = BuildConfig.APPLICATION_ID + ".ACTION_FIRED"
        const val EXTRA_ID = "intent.extra.alarm"
        const val EXTRA_TYPE = "intent.extra.type"
    }
}
