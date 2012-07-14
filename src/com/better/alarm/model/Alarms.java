/*
 * Copyright (C) 2007 The Android Open Source Project
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

import java.util.HashSet;
import java.util.Set;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;

/**
 * The Alarms provider supplies info about Alarm Clock settings
 */
public class Alarms implements IAlarmsManager, IAlarmsSetup {
    private static final String TAG = "Alarms";
    private static final boolean DBG = true;
    private static final String DEFAULT_SNOOZE = "10";

    private static final String PREF_SNOOZE_IDS = "snooze_ids";
    private static final String PREF_SNOOZE_TIME = "snooze_time";

    private static final int INVALID_ALARM_ID = -1;
    private Context mContext;

    private static Alarms sModelInstance;

    public static IAlarmsManager getAlarmsManager() {
        if (sModelInstance == null) {
            throw new NullPointerException("Alarms not initialized yet");
        }
        return sModelInstance;
    }

    public static IAlarmsSetup getAlarmsSetup() {
        if (sModelInstance == null) {
            throw new NullPointerException("Alarms not initialized yet");
        }
        return sModelInstance;
    }

    static Alarms getAlarms() {
        if (sModelInstance == null) {
            throw new NullPointerException("Alarms not initialized yet");
        }
        return sModelInstance;
    }

    static void init(Context context) {
        if (sModelInstance == null) {
            sModelInstance = new Alarms(context);
        }
        sModelInstance.disableExpiredAlarms();
        sModelInstance.setNextAlert();
    }

    private Alarms(Context context) {
        mContext = context;
    }

    /**
     * Creates a new Alarm and fills in the given alarm's id.
     */
    @Override
    public long add(Alarm alarm) {
        ContentValues values = alarm.createContentValues();
        Uri uri = mContext.getContentResolver().insert(Alarm.Columns.CONTENT_URI, values);
        alarm.id = (int) ContentUris.parseId(uri);

        long timeInMillis = alarm.getTimeInMillis();
        if (alarm.enabled) {
            //clearSnoozeIfNeeded(timeInMillis);
        }
        setNextAlert();
        return timeInMillis;
    }

    /**
     * Removes an existing Alarm. If this alarm is snoozing, disables snooze.
     * Sets next alert.
     */
    @Override
    public void delete(int alarmId) {
        if (alarmId == INVALID_ALARM_ID) return;

        ContentResolver contentResolver = mContext.getContentResolver();
        Uri uri = ContentUris.withAppendedId(Alarm.Columns.CONTENT_URI, alarmId);
        contentResolver.delete(uri, "", null);

        broadcastAlarmState(getAlarm(alarmId), Intents.ALARM_DISMISS_ACTION);

        setNextAlert();
    }

    /**
     * Queries all alarms
     * 
     * @return cursor over all alarms
     */
    @Override
    public Cursor getCursor() {
        return mContext.getContentResolver().query(Alarm.Columns.CONTENT_URI, Alarm.Columns.ALARM_QUERY_COLUMNS, null,
                null, Alarm.Columns.DEFAULT_SORT_ORDER);
    }

    // Private method to get a more limited set of alarms from the database.
    private Cursor getFilteredAlarmsCursor(ContentResolver contentResolver) {
        return contentResolver.query(Alarm.Columns.CONTENT_URI, Alarm.Columns.ALARM_QUERY_COLUMNS,
                Alarm.Columns.WHERE_ENABLED, null, null);
    }

    void onAlarmFired(Alarm alarm) {
        broadcastAlarmState(alarm, Intents.ALARM_ALERT_ACTION);

        // Disable this alarm if it does not repeat.
        if (!alarm.daysOfWeek.isRepeatSet()) {
            enableAlarmInternal(alarm, false);
        }

        setNextAlert();
    }

    void onAlarmSnoozedFired(Alarm alarm) {
        // TODO
    }

    void onAlarmSoundExpired(Alarm alarm) {
        // TODO
    }

    /**
     * Return an Alarm object representing the alarm id in the database. Returns
     * null if no alarm exists.
     */
    private Alarm getAlarm(int alarmId) {
        Cursor cursor = mContext.getContentResolver().query(
                ContentUris.withAppendedId(Alarm.Columns.CONTENT_URI, alarmId), Alarm.Columns.ALARM_QUERY_COLUMNS,
                null, null, null);
        Alarm alarm = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                alarm = new Alarm(cursor);
            }
            cursor.close();
        }
        return alarm;
    }

    /**
     * A convenience method to set an alarm in the Alarms content provider.
     * 
     * @return Time when the alarm will fire.
     */
    @Override
    public long set(Alarm alarm) {
        ContentValues values = alarm.createContentValues();
        ContentResolver resolver = mContext.getContentResolver();
        resolver.update(ContentUris.withAppendedId(Alarm.Columns.CONTENT_URI, alarm.id), values, null, null);

        long timeInMillis = alarm.getTimeInMillis();

        if (alarm.enabled) {
            // Disable the snooze if we just changed the snoozed alarm. This
            // only does work if the snoozed alarm is the same as the given
            // alarm.
            // TODO: disableSnoozeAlert should have a better name.

            // Disable the snooze if this alarm fires before the snoozed alarm.
            // This works on every alarm since the user most likely intends to
            // have the modified alarm fire next.
        }

        setNextAlert();

        return timeInMillis;
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
    public void enable(Alarm alarm) {
        enableAlarmInternal(getAlarm(alarm.id), !alarm.enabled);
        setNextAlert();
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

    private void enableAlarmInternal(final Alarm alarm, boolean enabled) {
        if (alarm == null) {
            return;
        }
        ContentResolver resolver = mContext.getContentResolver();

        ContentValues values = new ContentValues(2);
        values.put(Alarm.Columns.ENABLED, enabled ? 1 : 0);

        // If we are enabling the alarm, calculate alarm time since the time
        // value in Alarm may be old.
        if (enabled) {
            long time = 0;
            if (!alarm.daysOfWeek.isRepeatSet()) {
                time = alarm.getTimeInMillis();
            }
            values.put(Alarm.Columns.ALARM_TIME, time);
        } else {
            // Clear the snooze if the id matches.
        }

        resolver.update(ContentUris.withAppendedId(Alarm.Columns.CONTENT_URI, alarm.id), values, null, null);
    }

    private Alarm calculateNextAlert() {
        long minTime = Long.MAX_VALUE;
        long now = System.currentTimeMillis();

        Set<Alarm> alarms = new HashSet<Alarm>();

        // We need to to build the list of alarms from both the snoozed list and
        // the scheduled
        // list. For a non-repeating alarm, when it goes of, it becomes
        // disabled. A snoozed
        // non-repeating alarm is not in the active list in the database.

        // Now add the scheduled alarms
        final Cursor cursor = getFilteredAlarmsCursor(mContext.getContentResolver());
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    do {
                        final Alarm a = new Alarm(cursor);
                        alarms.add(a);
                    } while (cursor.moveToNext());
                }
            } finally {
                cursor.close();
            }
        }

        Alarm alarm = null;

        for (Alarm a : alarms) {
            // A time of 0 indicates this is a repeating alarm, so
            // calculate the time to get the next alert.
            if (a.time == 0) {
                a.time = a.getTimeInMillis();
            }

            if (a.time < now) {
                // Expired alarm, disable it and move along.
                enableAlarmInternal(a, false);
                continue;
            }
            if (a.time < minTime) {
                minTime = a.time;
                alarm = a;
            }
        }

        return alarm;
    }

    /**
     * Disables non-repeating alarms that have passed. Called at boot.
     */
    private void disableExpiredAlarms() {
        Cursor cur = getFilteredAlarmsCursor(mContext.getContentResolver());
        long now = System.currentTimeMillis();

        try {
            if (cur.moveToFirst()) {
                do {
                    Alarm alarm = new Alarm(cur);
                    // A time of 0 means this alarm repeats. If the time is
                    // non-zero, check if the time is before now.
                    if (alarm.time != 0 && alarm.time < now) {
                        enableAlarmInternal(alarm, false);
                    }
                } while (cur.moveToNext());
            }
        } finally {
            cur.close();
        }
    }

    /**
     * Called at system startup, on time/timezone change, and whenever the user
     * changes alarm settings. Activates snooze if set, otherwise loads all
     * alarms, activates next alert.
     */
    private void setNextAlert() {
        final Alarm alarm = calculateNextAlert();
        if (alarm != null) {
            AlarmReceiver.setUpRTCAlarm(mContext, alarm, alarm.time);
            broadcastAlarmState(alarm, Intents.ACTION_ALARM_SCHEDULED);
        } else {
            AlarmReceiver.removeRTCAlarm(mContext);
            broadcastAlarmState(alarm, Intents.ACTION_ALARMS_UNSCHEDULED);
        }
    }

    private void broadcastAlarmState(Alarm alarm, String action) {
        Intent intent = new Intent(action);
        intent.putExtra(Intents.ALARM_INTENT_EXTRA, alarm);
        mContext.sendBroadcast(intent);
    }
}
