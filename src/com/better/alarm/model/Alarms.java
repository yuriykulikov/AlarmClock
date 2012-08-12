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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;


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
    private Map<Integer, Alarm> set;

    @SuppressLint("UseSparseArrays")
    Alarms(Context context, IAlarmsScheduler alarmsScheduler) {
        mContext = context;
        this.mAlarmsScheduler = alarmsScheduler;
        mAlarmListChangedListeners = new HashSet<IAlarmsManager.OnAlarmListChangedListener>();
        mContentResolver = mContext.getContentResolver();
        set = new HashMap<Integer, Alarm>();

        final Cursor cursor = mContentResolver.query(Columns.CONTENT_URI, Columns.ALARM_QUERY_COLUMNS,
                null, null, Columns.DEFAULT_SORT_ORDER);
        try {
            if (cursor.moveToFirst()) {
                do {
                    final Alarm a = new Alarm(cursor);
                    set.put(a.getId(), a);
                } while (cursor.moveToNext());
            }
        } finally {
            cursor.close();
        }
    }

    void init() {
        Calendar now = Calendar.getInstance();
        for (Alarm alarm : set.values()) {
            boolean isExpired = alarm.getNextTime().before(now);
            if (isExpired) {
                if (DBG) Log.d(TAG, "Alarm expired: " + alarm.toString());
                if (alarm.isEnabled() && alarm.getDaysOfWeek().isRepeatSet()) {
                    alarm.calculateCalendars();
                } else {
                    alarm.setEnabled(false);
                }
                writeToDb(alarm.getId());
            }
        }
        if (DBG) {
            Log.d(TAG, "Alarms:");
            for (Alarm alarm : set.values()) {
                Log.d(TAG, alarm.toString());
            }
        }
        setNextAlert();
    }

    private void writeToDb(int id) {
        ContentValues values = getAlarm(id).createContentValues();
        Uri uriWithAppendedId = ContentUris.withAppendedId(Columns.CONTENT_URI, id);
        mContentResolver.update(uriWithAppendedId, values, null, null);
    }

    private void deleteAlarm(int alarmId) {
        Alarm alarm = getAlarm(alarmId);
        set.remove(alarm);
        Uri uri = ContentUris.withAppendedId(Columns.CONTENT_URI, alarmId);
        mContentResolver.delete(uri, "", null);
    }

    @Override
    public int createNewAlarm() {
        Alarm alarm = new Alarm();
        Uri uri = mContentResolver.insert(Columns.CONTENT_URI, alarm.createContentValues());
        alarm.setId((int) ContentUris.parseId(uri));
        set.put(alarm.getId(), alarm);
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

        alarm.calculateCalendars();
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
        List<Alarm> alarms = new LinkedList<Alarm>(set.values());
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
        return set.get(alarmId);
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
        alarm.calculateCalendars();
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
        alarm.setSnoozedTime(calendar);
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

    /**
     * Called at system startup, on time/timezone change, and whenever the user
     * changes alarm settings. Activates snooze if set, otherwise loads all
     * alarms, activates next alert.
     */
    private void setNextAlert() {
        Alarm alarm = null;

        if (!getAlarmsList().isEmpty()) {
            alarm = Collections.min(getAlarmsList());
            if (!alarm.isEnabled() && !alarm.isSnoozed()) {
                alarm = null;
            }
        }
        if (DBG) Log.d(TAG, alarm == null ? "no alarms" : "next: " + alarm.toString());

        if (alarm != null) {
            Calendar calendar = alarm.chooseNextCalendar();
            mAlarmsScheduler.setUpRTCAlarm(alarm, calendar);
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
