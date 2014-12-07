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

import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Locale;
import java.util.PriorityQueue;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.better.alarm.model.interfaces.Intents;
import com.github.androidutils.logger.Logger;

public class AlarmsScheduler implements IAlarmsScheduler {
    static final String ACTION_FIRED = "com.better.alarm.ACTION_FIRED";
    static final String EXTRA_ID = "intent.extra.alarm";
    static final String EXTRA_TYPE = "intent.extra.type";

    private interface ISetAlarmStrategy {

        void setRTCAlarm(ScheduledAlarm alarm, PendingIntent sender);

    }

    private class ScheduledAlarm implements Comparable<ScheduledAlarm> {
        public final int id;
        public final Calendar calendar;
        public final CalendarType type;
        private final DateFormat df;

        public ScheduledAlarm(int id, Calendar calendar, CalendarType type) {
            this.id = id;
            this.calendar = calendar;
            this.type = type;
            this.df = new SimpleDateFormat("dd-MM-yy HH:mm:ss", Locale.GERMANY);
        }

        public ScheduledAlarm(int id) {
            this.id = id;
            this.calendar = null;
            this.type = null;
            this.df = new SimpleDateFormat("dd-MM-yy HH:mm:ss", Locale.GERMANY);
        }

        @Override
        public boolean equals(Object o) {
            if (((ScheduledAlarm) o).id == id) return true;
            else return false;
        }

        @Override
        public int compareTo(ScheduledAlarm another) {
            return this.calendar.compareTo(another.calendar);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(id).append(" ");
            sb.append(type != null ? type.toString() : "null").append(" ");
            sb.append("on ").append(calendar != null ? df.format(calendar.getTime()) : "null");
            return sb.toString();
        }
    }

    private final Context mContext;
    private final SharedPreferences sp;

    private final PriorityQueue<ScheduledAlarm> queue;

    private final Logger log;
    private final ISetAlarmStrategy setAlarmStrategy;
    private final AlarmManager am;

    public AlarmsScheduler(Context context, Logger logger) {
        mContext = context;
        am = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        queue = new PriorityQueue<ScheduledAlarm>();
        this.log = logger;
        context.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                notifyListeners();
            }
        }, new IntentFilter(Intents.REQUEST_LAST_SCHEDULED_ALARM));
        setAlarmStrategy = initSetStrategyForVersion();
        sp = PreferenceManager.getDefaultSharedPreferences(mContext);
        log.d("Using " + setAlarmStrategy);
    }

    private ISetAlarmStrategy initSetStrategyForVersion() {
        log.d("SDK is " + android.os.Build.VERSION.SDK_INT);
        boolean kitkat = android.os.Build.VERSION.SDK_INT >= 19;
        if (kitkat) {
            try {
                final Method setExactMethod = AlarmManager.class.getMethod("setExact", new Class[] { Integer.TYPE,
                        Long.TYPE, PendingIntent.class });
                return new KitKatSetter(setExactMethod);
            } catch (NoSuchMethodException e) {
                log.e("Could not find setExact even though was supposed to!");
                return new IceCreamSetter();
            }
        } else return new IceCreamSetter();
    }

    @Override
    public void setAlarm(int id, CalendarType type, Calendar calendar) {
        ScheduledAlarm scheduledAlarm = new ScheduledAlarm(id, calendar, type);
        replaceAlarm(scheduledAlarm, true);
    }

    @Override
    public void removeAlarm(int id) {
        replaceAlarm(new ScheduledAlarm(id), false);
    }

    @Override
    public void onAlarmFired(int id) {
        replaceAlarm(new ScheduledAlarm(id), false);
    }

    private void replaceAlarm(ScheduledAlarm newAlarm, boolean add) {
        ScheduledAlarm previousHead = queue.peek();

        // remove if we have already an alarm
        for (Iterator<ScheduledAlarm> iterator = queue.iterator(); iterator.hasNext();) {
            ScheduledAlarm presentAlarm = iterator.next();
            if (presentAlarm.id == newAlarm.id) {
                iterator.remove();
                log.d(presentAlarm.id + " was removed");
            }
        }

        if (add) {
            queue.add(newAlarm);
        }

        fireAlarmsInThePast();

        ScheduledAlarm currentHead = queue.peek();
        // compare by reference!
        if (previousHead != currentHead) {
            if (!queue.isEmpty()) {
                setUpRTCAlarm(currentHead);
            } else {
                log.d("no more alarms to schedule, remove pending intent");
                removeRTCAlarm();
            }
            notifyListeners();
        }
    }

    /**
     * If two alarms were set for the same time, then the second alarm will be
     * processed in the past. In this case we remove it from the queue and fire
     * it.
     */

    private void fireAlarmsInThePast() {
        Calendar now = Calendar.getInstance();
        while (!queue.isEmpty() && queue.peek().calendar.before(now)) {
            // remove happens in fire
            ScheduledAlarm firedInThePastAlarm = queue.poll();
            log.d("In the past - " + firedInThePastAlarm.toString());
            Intent intent = new Intent(ACTION_FIRED);
            intent.putExtra(EXTRA_ID, firedInThePastAlarm.id);
            intent.putExtra(EXTRA_TYPE, firedInThePastAlarm.type.name());
            mContext.sendBroadcast(intent);
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
            // here it gets messy, but i could not care much because refactored
            // version is not far
            if (scheduledAlarm.type == CalendarType.PREALARM) {
                // we can only assume that the real one will be a little later,
                // namely:
                String asString = sp.getString("prealarm_duration", "30");
                int prealarmMinutes = Integer.parseInt(asString);
                int prealarmOffsetInMillis = prealarmMinutes * 60 * 1000;

                intent.putExtra(Intents.EXTRA_NEXT_NORMAL_TIME_IN_MILLIS, scheduledAlarm.calendar.getTimeInMillis()
                        + prealarmOffsetInMillis);
                intent.putExtra(Intents.EXTRA_IS_PREALARM, true);
            } else {
                intent.putExtra(Intents.EXTRA_NEXT_NORMAL_TIME_IN_MILLIS, scheduledAlarm.calendar.getTimeInMillis());
            }

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

    private void setUpRTCAlarm(ScheduledAlarm alarm) {
        log.d("Set " + alarm.toString());
        Intent intent = new Intent(ACTION_FIRED);
        intent.putExtra(EXTRA_ID, alarm.id);
        intent.putExtra(EXTRA_TYPE, alarm.type.name());
        PendingIntent sender = PendingIntent.getBroadcast(mContext, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        setAlarmStrategy.setRTCAlarm(alarm, sender);
    }

    private final class IceCreamSetter implements ISetAlarmStrategy {
        @Override
        public void setRTCAlarm(ScheduledAlarm alarm, PendingIntent sender) {
            am.set(AlarmManager.RTC_WAKEUP, alarm.calendar.getTimeInMillis(), sender);
        }
    }

    private final class KitKatSetter implements ISetAlarmStrategy {
        private final Method setExactMethod;

        private KitKatSetter(Method setExactMethod) {
            this.setExactMethod = setExactMethod;
        }

        @Override
        public void setRTCAlarm(ScheduledAlarm alarm, PendingIntent sender) {
            try {
                setExactMethod.invoke(am, new Object[] { AlarmManager.RTC_WAKEUP, alarm.calendar.getTimeInMillis(),
                        sender });
            } catch (Exception ex) {
                log.e("Failed to call setExact, falling back to set!");
                am.set(AlarmManager.RTC_WAKEUP, alarm.calendar.getTimeInMillis(), sender);
            }
        }
    }
}
