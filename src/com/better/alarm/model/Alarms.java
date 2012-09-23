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

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.better.wakelock.Logger;

/**
 * The Alarms implements application domain logic
 */
@SuppressLint("UseSparseArrays")
public class Alarms implements IAlarmsManager {
    private final Context mContext;
    private final Logger log;

    private final IAlarmsScheduler mAlarmsScheduler;

    private final Set<IAlarmsManager.OnAlarmListChangedListener> mAlarmListChangedListeners;

    private final ContentResolver mContentResolver;
    private final Map<Integer, AlarmCore> alarms;

    Alarms(Context context, Logger logger, IAlarmsScheduler alarmsScheduler) {
        mContext = context;
        log = logger;
        mAlarmsScheduler = alarmsScheduler;

        mAlarmListChangedListeners = new HashSet<IAlarmsManager.OnAlarmListChangedListener>();
        mContentResolver = mContext.getContentResolver();
        alarms = new HashMap<Integer, AlarmCore>();

        final Cursor cursor = mContentResolver.query(Columns.CONTENT_URI, Columns.ALARM_QUERY_COLUMNS, null, null,
                Columns.DEFAULT_SORT_ORDER);
        try {
            if (cursor.moveToFirst()) {
                do {
                    final AlarmCore a = new AlarmCore(cursor, context, log, alarmsScheduler);
                    alarms.put(a.getId(), a);
                } while (cursor.moveToNext());
            }
        } finally {
            cursor.close();
        }

        log.d("Alarms:");
        for (Alarm alarm : alarms.values()) {
            log.d(alarm.toString());
        }
    }

    void reinit() {
        for (AlarmCore alarmCore : alarms.values()) {
            alarmCore.calculateCalendars();
        }

    }

    @Override
    public AlarmCore getAlarm(int alarmId) {
        return alarms.get(alarmId);
    }

    @Override
    public int createNewAlarm() {
        AlarmCore alarm = new AlarmCore(mContext, log, mAlarmsScheduler);
        alarms.put(alarm.getId(), alarm);
        notifyAlarmListChangedListeners();
        return alarm.getId();
    }

    @Override
    public void delete(int alarmId) {
        Alarm alarm = getAlarm(alarmId);
        alarm.delete();
        alarms.remove(alarmId);
        notifyAlarmListChangedListeners();
    }

    // Compatibility - passes calls to Alarms

    @Override
    public void changeAlarm(int id, boolean enabled, int hour, int minute, DaysOfWeek daysOfWeek, boolean vibrate,
            String label, Uri alert, boolean preAlarm) {
        Alarm alarm = getAlarm(id);
        alarm.change(enabled, hour, minute, daysOfWeek, vibrate, label, alert, preAlarm);
        notifyAlarmListChangedListeners();
    }

    @Override
    public List<Alarm> getAlarmsList() {
        List<Alarm> alarms = new LinkedList<Alarm>(this.alarms.values());
        return alarms;
    }

    void onAlarmFired(int id, CalendarType calendarType) {
        AlarmCore alarm = getAlarm(id);
        alarm.onAlarmFired(calendarType);
        notifyAlarmListChangedListeners();
    }

    /**
     * A convenience method to enable or disable an alarm.
     * 
     * @param id
     *            corresponds to the _id column
     * @param enabled
     *            corresponds to the ENABLED column
     */
    @Override
    public void enable(int id, boolean enable) {
        Alarm alarm = getAlarm(id);
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

    @Override
    public void registerOnAlarmListChangedListener(OnAlarmListChangedListener listener) {
        mAlarmListChangedListeners.add(listener);
        notifyAlarmListChangedListeners();
    }

    @Override
    public void unRegisterOnAlarmListChangedListener(OnAlarmListChangedListener listener) {
        mAlarmListChangedListeners.remove(listener);
    }

    private void notifyAlarmListChangedListeners() {
        for (OnAlarmListChangedListener listener : mAlarmListChangedListeners) {
            listener.onAlarmListChanged(getAlarmsList());
        }
    }
}
