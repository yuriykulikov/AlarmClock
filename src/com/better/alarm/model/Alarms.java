/*
 * Copyright (C) 2007 The Android Open Source Project
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
import android.util.Log;

import com.better.alarm.model.Alarm.DaysOfWeek;

/**
 * The Alarms implements application domain logic
 */
public class Alarms implements IAlarmsManager {
    private static final String TAG = "Alarms";
    private static final boolean DBG = true;

    private Context mContext;
    private Set<IAlarmsManager.OnAlarmListChangedListener> mAlarmListChangedListeners;

    private ContentResolver mContentResolver;
    private Set<Alarm> set;

    Alarms(Context context) {
        mContext = context;
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
        changeAlarm(alarm);
    }

    @Override
    public void delete(int alarmId) {
        Alarm alarm = getAlarm(alarmId);
        set.remove(alarm);
        Uri uri = ContentUris.withAppendedId(Alarm.Columns.CONTENT_URI, alarmId);
        mContentResolver.delete(uri, "", null);

        broadcastAlarmState(getAlarm(alarmId), Intents.ALARM_DISMISS_ACTION);
        notifyAlarmListChangedListeners();

        setNextAlert();
    }

    @Override
    public List<Alarm> getAlarmsList() {
        List<Alarm> alarms = new LinkedList<Alarm>(set);
        return alarms;
    }

    void onAlarmFired(Alarm alarm) {
        broadcastAlarmState(alarm, Intents.ALARM_ALERT_ACTION);

        // Disable this alarm if it does not repeat.
        if (!alarm.getDaysOfWeek().isRepeatSet()) {
            enable(alarm.getId(), false);
        } else {
            setNextAlert();
        }
    }

    void onAlarmSnoozedFired(Alarm alarm) {
        // TODO
    }

    void onAlarmSoundExpired(Alarm alarm) {
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

    private int changeAlarm(Alarm alarm) {
        // first add to the DB
        ContentValues values = alarm.createContentValues();

        // this is an existing alarm
        mContentResolver.update(ContentUris.withAppendedId(Alarm.Columns.CONTENT_URI, alarm.getId()), values, null,
                null);
        set.remove(alarm);
        set.add(alarm);

        if (alarm.isEnabled()) {
            // Disable the snooze if we just changed the snoozed alarm. This
            // only does work if the snoozed alarm is the same as the given
            // alarm.
            // TODO: disableSnoozeAlert should have a better name.

            // Disable the snooze if this alarm fires before the snoozed alarm.
            // This works on every alarm since the user most likely intends to
            // have the modified alarm fire next.
            // clearSnoozeIfNeeded(timeInMillis);
        }
        setNextAlert();
        notifyAlarmListChangedListeners();
        return alarm.getId();
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
        changeAlarm(alarm);
    }

    @Override
    public void snooze(Alarm alarm) {
        // TODO
    }

    @Override
    public void dismiss(Alarm alarm) {
        broadcastAlarmState(alarm, Intents.ALARM_DISMISS_ACTION);
        setNextAlert();
        // TODO
    }

    private Alarm calculateNextAlert() {
        Alarm alarm = null;

        if (getAlarmsList().isEmpty()) {
            if (DBG) Log.d(TAG, "no alarms");
        } else {
            alarm = Collections.min(getAlarmsList());
            if (!alarm.isEnabled()) {
                alarm = null;
                if (DBG) Log.d(TAG, "no alarms");
            } else {
                if (DBG) Log.d(TAG, "next: " + alarm.toString());
            }
        }
        return alarm;
    }

    /**
     * Disables non-repeating alarms that have passed. Called at boot.
     */
    void disableExpiredAlarms() {
        long now = System.currentTimeMillis();
        for (Alarm alarm : set) {
            if (!alarm.isEnabled() && alarm.getTimeInMillis() != 0 && alarm.getTimeInMillis() < now) {
                enable(alarm.getId(), false);
            }
        }
    }

    /**
     * Called at system startup, on time/timezone change, and whenever the user
     * changes alarm settings. Activates snooze if set, otherwise loads all
     * alarms, activates next alert.
     */
    void setNextAlert() {
        final Alarm alarm = calculateNextAlert();
        if (alarm != null) {
            AlarmReceiver.setUpRTCAlarm(mContext, alarm, alarm.getTimeInMillis());
            broadcastAlarmState(alarm, Intents.ACTION_ALARM_SCHEDULED);
        } else {
            AlarmReceiver.removeRTCAlarm(mContext);
            broadcastAlarmState(alarm, Intents.ACTION_ALARMS_UNSCHEDULED);
        }
    }

    private void broadcastAlarmState(Alarm alarm, String action) {
        Intent intent = new Intent(action);
        intent.putExtra(Intents.EXTRA_ID, alarm == null ? -1 : alarm.getId());
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
