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
package com.better.alarm.model;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.better.alarm.model.interfaces.Intents;
import com.github.androidutils.logger.Logger;

public class AlarmsScheduler implements IAlarmsScheduler {
    private static final boolean DBG = true;
    static final String ACTION_FIRED = "com.better.alarm.ACTION_FIRED";
    static final String EXTRA_ID = "intent.extra.alarm";
    static final String EXTRA_TYPE = "intent.extra.type";

    private class ScheduledAlarm implements Comparable<ScheduledAlarm> {
        public final int id;
        public final Calendar calendar;
        public final CalendarType type;

        public ScheduledAlarm(int id, Calendar calendar, CalendarType type) {
            this.id = id;
            this.calendar = calendar;
            this.type = type;
        }

        @Override
        public boolean equals(Object o) {
            if (((ScheduledAlarm) o).id == id && ((ScheduledAlarm) o).type == type) return true;
            else return false;
        }

        @Override
        public int compareTo(ScheduledAlarm another) {
            return this.calendar.compareTo(another.calendar);
        }

    }

    private final Context mContext;

    private final PriorityQueue<ScheduledAlarm> queue;

    private final Logger log;

    public AlarmsScheduler(Context context, Logger logger) {
        mContext = context;
        queue = new PriorityQueue<ScheduledAlarm>();
        this.log = logger;
    }

    @Override
    public void removeAlarm(int id) {
        ScheduledAlarm previousHead = queue.peek();
        for (Iterator<ScheduledAlarm> iterator = queue.iterator(); iterator.hasNext();) {
            ScheduledAlarm scheduledAlarm = iterator.next();

            if (scheduledAlarm.id == id) {
                log.d("removing a ScheduledAlarm " + id + " type = " + scheduledAlarm.type.toString());
                iterator.remove();
            }
        }
        ScheduledAlarm currentHead = queue.peek();
        if (previousHead != currentHead) {
            setNextRTCAlert();
        }
        notifyListeners();
    }

    private void setNextRTCAlert() {
        if (!queue.isEmpty()) {
            // TODO problems happen because we remove, we have to keep. Remove
            // only when it is in the past
            // or removed by someone
            ScheduledAlarm scheduledAlarm = queue.peek();
            setUpRTCAlarm(scheduledAlarm.id, scheduledAlarm.calendar, scheduledAlarm.type);
        } else {
            removeRTCAlarm();
        }
    }

    /**
     * TODO the whole mechanism has to be revised. Currently we can only know
     * when next alarm is scheduled and we do not know what is the reason for
     * that. Maybe create a separate component for that or notify from the alarm
     * SM. Actually notify this component from SM :-) and he can notify the
     * rest.
     */
    private void notifyListeners() {
        Intent intent = new Intent();
        if (queue.isEmpty()) {
            intent.setAction(Intents.ACTION_ALARMS_UNSCHEDULED);
        } else if (queue.peek().type != CalendarType.AUTOSILENCE) {
            ScheduledAlarm scheduledAlarm = queue.peek();
            intent.setAction(Intents.ACTION_ALARM_SCHEDULED);
            intent.putExtra(Intents.EXTRA_ID, scheduledAlarm.id);
            // TODO add type to the intent
            mContext.sendBroadcast(intent);
        } else {
            // now this means that alarm in the closest future is AUTOSILENCE
            ScheduledAlarm scheduledAlarm = findNextNormalAlarm();
            if (scheduledAlarm != null) {
                intent.setAction(Intents.ACTION_ALARM_SCHEDULED);
                intent.putExtra(Intents.EXTRA_ID, scheduledAlarm.id);
            } else {
                intent.setAction(Intents.ACTION_ALARMS_UNSCHEDULED);
            }
        }
        mContext.sendBroadcast(intent);
    }

    private ScheduledAlarm findNextNormalAlarm() {
        // this means we have to find the next normal, snooze or prealarm
        // since iterator does not have a specific order, and we cannot
        // peek(i), remove elements one by one
        ScheduledAlarm nextNormalAlarm = null;
        ArrayList<ScheduledAlarm> temporaryCollection = new ArrayList<AlarmsScheduler.ScheduledAlarm>(queue.size());
        while (!queue.isEmpty()) {
            ScheduledAlarm scheduledAlarm = queue.poll();
            temporaryCollection.add(scheduledAlarm);
            if (scheduledAlarm.type != CalendarType.AUTOSILENCE) {
                // that is our client
                nextNormalAlarm = scheduledAlarm;
                break;
            }
        }
        // Put back everything what we have removed
        queue.addAll(temporaryCollection);
        return nextNormalAlarm;
    }

    private void removeRTCAlarm() {
        AlarmManager am = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        PendingIntent sender = PendingIntent.getBroadcast(mContext, 0, new Intent(ACTION_FIRED),
                PendingIntent.FLAG_CANCEL_CURRENT);
        am.cancel(sender);
    }

    private void setUpRTCAlarm(int id, Calendar calendar, CalendarType type) {
        AlarmManager am = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);

        DateFormat df = DateFormat.getDateTimeInstance();

        log.d("Set alarm " + id + " on " + df.format(calendar.getTime()));

        if (DBG && calendar.before(Calendar.getInstance()))
            throw new RuntimeException("Attempt to schedule alarm in the past: " + df.format(calendar.getTime()));

        Intent intent = new Intent(ACTION_FIRED);
        intent.putExtra(EXTRA_ID, id);
        intent.putExtra(EXTRA_TYPE, type.name());
        PendingIntent sender = PendingIntent.getBroadcast(mContext, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        am.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), sender);
    }

    @Override
    public void setAlarm(int id, Map<CalendarType, Calendar> activeCalendars) {
        ScheduledAlarm previousHead = queue.peek();
        for (Iterator<ScheduledAlarm> iterator = queue.iterator(); iterator.hasNext();) {
            if (iterator.next().id == id) {
                iterator.remove();
            }
        }

        for (Entry<CalendarType, Calendar> entry : activeCalendars.entrySet()) {
            ScheduledAlarm scheduledAlarm = new ScheduledAlarm(id, entry.getValue(), entry.getKey());

            if (queue.contains(scheduledAlarm)) {
                queue.remove(scheduledAlarm);
            }
            queue.add(scheduledAlarm);
        }
        ScheduledAlarm currentHead = queue.peek();
        if (previousHead != currentHead) {
            setNextRTCAlert();
        }
        notifyListeners();
    }
}
