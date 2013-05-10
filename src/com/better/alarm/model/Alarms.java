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

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.acra.ACRA;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;

import com.better.alarm.model.interfaces.Alarm;
import com.better.alarm.model.interfaces.AlarmNotFoundException;
import com.better.alarm.model.interfaces.IAlarmsManager;
import com.better.alarm.model.interfaces.Intents;
import com.better.alarm.model.persistance.AlarmContainer;
import com.github.androidutils.logger.Logger;

/**
 * The Alarms implements application domain logic
 */
@SuppressLint("UseSparseArrays")
public class Alarms implements IAlarmsManager {
    private final Context mContext;
    private final Logger log;

    private final IAlarmsScheduler mAlarmsScheduler;

    private final ContentResolver mContentResolver;
    private final Map<Integer, AlarmCore> alarms;
    private final AlarmStateNotifier broadcaster;

    Alarms(Context context, Logger logger, IAlarmsScheduler alarmsScheduler) {
        mContext = context;
        log = logger;
        mAlarmsScheduler = alarmsScheduler;

        mContentResolver = mContext.getContentResolver();
        alarms = new HashMap<Integer, AlarmCore>();
        broadcaster = new AlarmStateNotifier(mContext);

        final Cursor cursor = mContentResolver.query(AlarmContainer.Columns.CONTENT_URI,
                AlarmContainer.Columns.ALARM_QUERY_COLUMNS, null, null, AlarmContainer.Columns.DEFAULT_SORT_ORDER);
        try {
            if (cursor.moveToFirst()) {
                do {
                    AlarmContainer container = new AlarmContainer(cursor, log, context);
                    final AlarmCore a = new AlarmCore(container, context, log, alarmsScheduler, broadcaster);
                    alarms.put(a.getId(), a);
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            } else {
                log.e("Cursor was null!");
                ACRA.getErrorReporter().handleSilentException(new Exception("Cursor was null!"));
            }
        }

        log.d("Alarms:");
        for (Alarm alarm : alarms.values()) {
            log.d(alarm.toString());
        }
    }

    void refresh() {
        for (AlarmCore alarmCore : alarms.values()) {
            alarmCore.refresh();
        }
        notifyAlarmListChangedListeners();
    }

    void onTimeSet() {
        for (AlarmCore alarmCore : alarms.values()) {
            alarmCore.onTimeSet();
        }
        notifyAlarmListChangedListeners();
    }

    @Override
    public AlarmCore getAlarm(int alarmId) throws AlarmNotFoundException {
        AlarmCore alarm = alarms.get(alarmId);
        if (alarm != null) return alarm;
        else throw new AlarmNotFoundException("AlarmID " + alarmId + " could not be resolved");
    }

    @Override
    public Alarm createNewAlarm() {
        AlarmContainer container = new AlarmContainer(log, mContext);
        AlarmCore alarm = new AlarmCore(container, mContext, log, mAlarmsScheduler, broadcaster);
        alarms.put(alarm.getId(), alarm);
        notifyAlarmListChangedListeners();
        return alarm;
    }

    @Override
    public void delete(Alarm alarm) {
        alarm.delete();
        alarms.remove(alarm.getId());
        notifyAlarmListChangedListeners();
    }

    // Compatibility - passes calls to Alarms

    @Override
    public List<Alarm> getAlarmsList() {
        List<Alarm> alarms = new LinkedList<Alarm>(this.alarms.values());
        Collections.sort(alarms, new MinuteComparator());
        Collections.sort(alarms, new HourComparator());
        Collections.sort(alarms, new RepeatComparator());

        return alarms;
    }

    private final class RepeatComparator implements Comparator<Alarm> {
        @Override
        public int compare(Alarm lhs, Alarm rhs) {
            return Integer.valueOf(getPrio(lhs)).compareTo(Integer.valueOf(getPrio(rhs)));
        }

        /**
         * First comes on Weekdays, than on weekends and then the rest
         * 
         * @param alarm
         * @return
         */
        private int getPrio(Alarm alarm) {
            switch (alarm.getDaysOfWeek().getCoded()) {
            case 0x7F:
                return 0;
            case 0x1F:
                return 1;
            case 0x60:
                return 2;
            default:
                return 3;
            }
        }
    }

    private final class HourComparator implements Comparator<Alarm> {
        @Override
        public int compare(Alarm lhs, Alarm rhs) {
            return Integer.valueOf(lhs.getHour()).compareTo(Integer.valueOf(rhs.getHour()));
        }
    }

    private final class MinuteComparator implements Comparator<Alarm> {
        @Override
        public int compare(Alarm lhs, Alarm rhs) {
            return Integer.valueOf(lhs.getMinutes()).compareTo(Integer.valueOf(rhs.getMinutes()));
        }
    }

    void onAlarmFired(AlarmCore alarm, CalendarType calendarType) {
        mAlarmsScheduler.onAlarmFired(alarm.getId());
        alarm.onAlarmFired(calendarType);
        notifyAlarmListChangedListeners();
    }

    /**
     * A convenience method to enable or disable an alarm
     * 
     * @param enabled
     *            corresponds to the ENABLED column
     * @throws AlarmNotFoundException
     */
    @Override
    public void enable(Alarm alarm, boolean enable) {
        alarm.enable(enable);
        notifyAlarmListChangedListeners();
    }

    @Override
    public void snooze(Alarm alarm) {
        alarm.snooze();
        notifyAlarmListChangedListeners();
    }

    @Override
    public void dismiss(Alarm alarm) {
        alarm.dismiss();
        notifyAlarmListChangedListeners();
    }

    private void notifyAlarmListChangedListeners() {
        mContext.sendBroadcast(new Intent(Intents.ACTION_ALARM_CHANGED));
    }
}
