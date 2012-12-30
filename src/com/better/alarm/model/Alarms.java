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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;

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
            cursor.close();
        }

        init();

        log.d("Alarms:");
        for (Alarm alarm : alarms.values()) {
            log.d(alarm.toString());
        }
    }

    void init() {
        for (AlarmCore alarmCore : alarms.values()) {
            alarmCore.refresh();
        }
        notifyAlarmListChangedListeners();
    }

    @Override
    public AlarmCore getAlarm(int alarmId) {
        return alarms.get(alarmId);
    }

    @Override
    public int createNewAlarm() {
        AlarmContainer container = new AlarmContainer(log, mContext);
        AlarmCore alarm = new AlarmCore(container, mContext, log, mAlarmsScheduler, broadcaster);
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

    private void notifyAlarmListChangedListeners() {
        mContext.sendBroadcast(new Intent(Intents.ACTION_ALARM_CHANGED));
    }
}
