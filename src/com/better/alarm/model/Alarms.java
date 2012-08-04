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

import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

import com.better.alarm.model.Alarm.DaysOfWeek;

/**
 * The Alarms implements application domain logic
 */
public class Alarms implements IAlarmsManager {
    private static final String TAG = "Alarms";
    private static final boolean DBG = true;
    private Context mContext;
    private IAlarmsScheduler mAlarmsScheduler;
    private Set<IAlarmsManager.OnAlarmListChangedListener> mAlarmListChangedListeners;

    private ContentResolver mContentResolver;
    private Set<Alarm> set;

    Alarms(Context context, IAlarmsScheduler alarmsScheduler) {
        mContext = context;
        this.mAlarmsScheduler = alarmsScheduler;
        mAlarmListChangedListeners = new HashSet<IAlarmsManager.OnAlarmListChangedListener>();
        mContentResolver = mContext.getContentResolver();
        set = new HashSet<Alarm>();

        final Cursor cursor = mContentResolver.query(Alarm.Columns.CONTENT_URI, Alarm.Columns.ALARM_QUERY_COLUMNS,
                null, null, Alarm.Columns.DEFAULT_SORT_ORDER);
        try {
            if (cursor.moveToFirst()) {
                do {
                    final Alarm a = new Alarm(cursor);
                    set.add(a);
                } while (cursor.moveToNext());
            }
        } finally {
            cursor.close();
        }
        if (DBG) {
            Log.d(TAG, "Alarms:");
            for (Alarm alarm : set) {
                Log.d(TAG, alarm.toString());
            }
        }
    }

    void init() {
        // disable expired alarms
        long now = System.currentTimeMillis();
        for (Alarm alarm : set) {
            boolean isExpired = alarm.getNextTimeInMillis() < now;
            if (isExpired && !alarm.isEnabled()) {
                alarm.setEnabled(false);
                writeToDb(alarm.getId());
            }
        }
        setNextAlert();
    }

    private void writeToDb(int id) {
        ContentValues values = getAlarm(id).createContentValues();
        Uri uriWithAppendedId = ContentUris.withAppendedId(Alarm.Columns.CONTENT_URI, id);
        mContentResolver.update(uriWithAppendedId, values, null, null);
    }

    private void deleteAlarm(int alarmId) {
        Alarm alarm = getAlarm(alarmId);
        set.remove(alarm);
        Uri uri = ContentUris.withAppendedId(Alarm.Columns.CONTENT_URI, alarmId);
        mContentResolver.delete(uri, "", null);
    }

    @Override
    public int createNewAlarm() {
        Alarm alarm = new Alarm();
        Uri uri = mContentResolver.insert(Alarm.Columns.CONTENT_URI, alarm.createContentValues());
        alarm.setId((int) ContentUris.parseId(uri));
        set.add(alarm);
        // new alarm is not enabled, no need to check if it can fire. It can't
        notifyAlarmListChangedListeners();
        return alarm.getId();
    }

    @Override
    public void changeAlarm(int id, boolean enabled, int hour, int minute, DaysOfWeek daysOfWeek, boolean vibrate,
            String label, Uri alert, boolean preAlarm) {
        Alarm alarm = getAlarm(id);
        alarm.setEnabled(enabled);
        alarm.setHour(hour);
        alarm.setMinutes(minute);
        alarm.setDaysOfWeek(daysOfWeek);
        alarm.setVibrate(vibrate);
        alarm.setLabel(label);
        alarm.setAlert(alert);
        alarm.setPrealarm(preAlarm);
        writeToDb(alarm.getId());
        setNextAlert();
        notifyAlarmListChangedListeners();
    }

    @Override
    public void delete(int alarmId) {
        deleteAlarm(alarmId);
        broadcastAlarmState(alarmId, Intents.ALARM_DISMISS_ACTION);
        notifyAlarmListChangedListeners();
        setNextAlert();
    }

    @Override
    public List<Alarm> getAlarmsList() {
        List<Alarm> alarms = new LinkedList<Alarm>(set);
        return alarms;
    }

    void onAlarmFired(int id) {
        Alarm alarm = getAlarm(id);
        broadcastAlarmState(id, Intents.ALARM_ALERT_ACTION);
        alarm.setSnoozed(false);
        // Disable this alarm if it does not repeat.
        if (!alarm.getDaysOfWeek().isRepeatSet()) {
            alarm.setEnabled(false);
        }
        writeToDb(alarm.getId());
        setNextAlert();
        notifyAlarmListChangedListeners();
    }

    void onAlarmSnoozedFired(int id) {
        // TODO it is not used for now because, well, it will probably not be
        // used
    }

    void onAlarmSoundExpired(int id) {
        // TODO
    }

    @Override
    public Alarm getAlarm(int alarmId) {
        for (Alarm alarm : set) {
            if (alarm.getId() == alarmId) {
                return alarm;
            }
        }
        return null;
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
        alarm.setEnabled(enable);
        writeToDb(alarm.getId());
        setNextAlert();
        notifyAlarmListChangedListeners();
    }

    @Override
    public void snooze(Alarm alarm) {
        int snoozeMinutes = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(mContext).getString(
                "snooze_duration", "10"));
        alarm.setSnoozed(true);
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, snoozeMinutes);
        alarm.setSnoozedHour(calendar.get(Calendar.HOUR_OF_DAY));
        alarm.setSnoozedminutes(calendar.get(Calendar.MINUTE));
        broadcastAlarmState(alarm.getId(), Intents.ALARM_SNOOZE_ACTION);
        writeToDb(alarm.getId());
        setNextAlert();
        notifyAlarmListChangedListeners();
    }

    @Override
    public void dismiss(Alarm alarm) {
        broadcastAlarmState(alarm.getId(), Intents.ALARM_DISMISS_ACTION);
        alarm.setSnoozed(false);
        writeToDb(alarm.getId());
        setNextAlert();
        notifyAlarmListChangedListeners();
    }

    private Alarm calculateNextAlert() {
        Alarm alarm = null;

        if (getAlarmsList().isEmpty()) {
            if (DBG) Log.d(TAG, "no alarms");
        } else {
            alarm = Collections.min(getAlarmsList());
            if (!alarm.isEnabled() && !alarm.isSnoozed()) {
                alarm = null;
                if (DBG) Log.d(TAG, "no alarms");
            } else {
                if (DBG) Log.d(TAG, "next: " + alarm.toString());
            }
        }
        return alarm;
    }

    /**
     * Called at system startup, on time/timezone change, and whenever the user
     * changes alarm settings. Activates snooze if set, otherwise loads all
     * alarms, activates next alert.
     */
    private void setNextAlert() {
        final Alarm alarm = calculateNextAlert();
        if (alarm != null) {
            long timeInMillis = alarm.isSnoozed() ? alarm.getSnoozedTimeInMillis() : alarm.getTimeInMillis();
            mAlarmsScheduler.setUpRTCAlarm(alarm, timeInMillis);
            broadcastAlarmState(alarm.getId(), Intents.ACTION_ALARM_SCHEDULED);
        } else {
            mAlarmsScheduler.removeRTCAlarm();
            broadcastState(Intents.ACTION_ALARMS_UNSCHEDULED);
        }
    }

    private void broadcastAlarmState(int id, String action) {
        Intent intent = new Intent(action);
        intent.putExtra(Intents.EXTRA_ID, id);
        mContext.sendBroadcast(intent);
    }

    private void broadcastState(String action) {
        Intent intent = new Intent(action);
        mContext.sendBroadcast(intent);
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
