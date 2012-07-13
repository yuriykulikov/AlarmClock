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

import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

import com.better.alarm.presenter.AlarmsListActivity;
import com.better.alarm.presenter.SettingsActivity;

/**
 * The Alarms provider supplies info about Alarm Clock settings
 */
public class Alarms implements IAlarmsManager, IAlarmsSetup {
    private static final String TAG = "Alarms";
    private static final boolean DBG = true;
    private static final String DEFAULT_SNOOZE = "10";
    // This string is used to indicate a silent alarm in the db.
    public static final String ALARM_ALERT_SILENT = "silent";

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
        // TODO do init stuff here sModelInstance
    }

    private Alarms(Context context) {
        mContext = context;
    }

    /**
     * Creates a new Alarm and fills in the given alarm's id.
     */
    @Override
    public long add(Alarm alarm) {
        ContentValues values = createContentValues(alarm);
        Uri uri = mContext.getContentResolver().insert(Alarm.Columns.CONTENT_URI, values);
        alarm.id = (int) ContentUris.parseId(uri);

        long timeInMillis = alarm.getTimeInMillis();
        if (alarm.enabled) {
            clearSnoozeIfNeeded(timeInMillis);
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
        /* If alarm is snoozing, lose it */
        disableSnoozeAlert(alarmId);

        Uri uri = ContentUris.withAppendedId(Alarm.Columns.CONTENT_URI, alarmId);
        contentResolver.delete(uri, "", null);

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

        // Disable the snooze alert if this alarm is the snooze.
        disableSnoozeAlert(alarm.id);
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

    private ContentValues createContentValues(Alarm alarm) {
        ContentValues values = new ContentValues(8);
        // Set the alarm_time value if this alarm does not repeat. This will be
        // used later to disable expire alarms.
        long time = 0;
        if (!alarm.daysOfWeek.isRepeatSet()) {
            time = alarm.getTimeInMillis();
        }

        values.put(Alarm.Columns.ENABLED, alarm.enabled ? 1 : 0);
        values.put(Alarm.Columns.HOUR, alarm.hour);
        values.put(Alarm.Columns.MINUTES, alarm.minutes);
        values.put(Alarm.Columns.ALARM_TIME, time);
        values.put(Alarm.Columns.DAYS_OF_WEEK, alarm.daysOfWeek.getCoded());
        values.put(Alarm.Columns.VIBRATE, alarm.vibrate);
        values.put(Alarm.Columns.MESSAGE, alarm.label);

        // A null alert Uri indicates a silent alarm.
        values.put(Alarm.Columns.ALERT, alarm.alert == null ? ALARM_ALERT_SILENT : alarm.alert.toString());

        return values;
    }

    private void clearSnoozeIfNeeded(long alarmTime) {
        // If this alarm fires before the next snooze, clear the snooze to
        // enable this alarm.
        SharedPreferences prefs = mContext.getSharedPreferences(AlarmsListActivity.PREFERENCES, 0);

        // Get the list of snoozed alarms
        final Set<String> snoozedIds = prefs.getStringSet(PREF_SNOOZE_IDS, new HashSet<String>());
        for (String snoozedAlarm : snoozedIds) {
            final long snoozeTime = prefs.getLong(getAlarmPrefSnoozeTimeKey(snoozedAlarm), 0);
            if (alarmTime < snoozeTime) {
                final int alarmId = Integer.parseInt(snoozedAlarm);
                clearSnoozePreference(prefs, alarmId);
            }
        }
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
        ContentValues values = createContentValues(alarm);
        ContentResolver resolver = mContext.getContentResolver();
        resolver.update(ContentUris.withAppendedId(Alarm.Columns.CONTENT_URI, alarm.id), values, null, null);

        long timeInMillis = alarm.getTimeInMillis();

        if (alarm.enabled) {
            // Disable the snooze if we just changed the snoozed alarm. This
            // only does work if the snoozed alarm is the same as the given
            // alarm.
            // TODO: disableSnoozeAlert should have a better name.
            disableSnoozeAlert(alarm.id);

            // Disable the snooze if this alarm fires before the snoozed alarm.
            // This works on every alarm since the user most likely intends to
            // have the modified alarm fire next.
            clearSnoozeIfNeeded(timeInMillis);
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
        final String snooze = PreferenceManager.getDefaultSharedPreferences(mContext).getString(
                SettingsActivity.KEY_ALARM_SNOOZE, DEFAULT_SNOOZE);
        int snoozeMinutes = Integer.parseInt(snooze);

        final long snoozeTime = System.currentTimeMillis() + (1000 * 60 * snoozeMinutes);
        saveSnoozeAlert(alarm, snoozeTime);
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
            disableSnoozeAlert(alarm.id);
        }

        resolver.update(ContentUris.withAppendedId(Alarm.Columns.CONTENT_URI, alarm.id), values, null, null);
    }

    private Alarm calculateNextAlert() {
        long minTime = Long.MAX_VALUE;
        long now = System.currentTimeMillis();
        final SharedPreferences prefs = mContext.getSharedPreferences(AlarmsListActivity.PREFERENCES, 0);

        Set<Alarm> alarms = new HashSet<Alarm>();

        // We need to to build the list of alarms from both the snoozed list and
        // the scheduled
        // list. For a non-repeating alarm, when it goes of, it becomes
        // disabled. A snoozed
        // non-repeating alarm is not in the active list in the database.

        // first go through the snoozed alarms
        final Set<String> snoozedIds = prefs.getStringSet(PREF_SNOOZE_IDS, new HashSet<String>());
        for (String snoozedAlarm : snoozedIds) {
            final int alarmId = Integer.parseInt(snoozedAlarm);
            final Alarm a = getAlarm(alarmId);
            alarms.add(a);
        }

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

            // Update the alarm if it has been snoozed
            updateAlarmTimeForSnooze(prefs, a);

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

    private void saveSnoozeAlert(Alarm alarm, long time) {
        SharedPreferences prefs = mContext.getSharedPreferences(AlarmsListActivity.PREFERENCES, 0);
        if (alarm.id == INVALID_ALARM_ID) {
            clearAllSnoozePreferences(prefs);
        } else {
            final Set<String> snoozedIds = prefs.getStringSet(PREF_SNOOZE_IDS, new HashSet<String>());
            snoozedIds.add(Integer.toString(alarm.id));
            final SharedPreferences.Editor ed = prefs.edit();
            ed.putStringSet(PREF_SNOOZE_IDS, snoozedIds);
            ed.putLong(getAlarmPrefSnoozeTimeKey(alarm.id), time);
            ed.apply();
        }
        // Set the next alert after updating the snooze.
        setNextAlert();
    }

    private String getAlarmPrefSnoozeTimeKey(int id) {
        return getAlarmPrefSnoozeTimeKey(Integer.toString(id));
    }

    private String getAlarmPrefSnoozeTimeKey(String id) {
        return PREF_SNOOZE_TIME + id;
    }

    /**
     * Disable the snooze alert if the given id matches the snooze id.
     */
    private void disableSnoozeAlert(final int id) {
        SharedPreferences prefs = mContext.getSharedPreferences(AlarmsListActivity.PREFERENCES, 0);
        if (hasAlarmBeenSnoozed(prefs, id)) {
            // This is the same id so clear the shared prefs.
            clearSnoozePreference(prefs, id);
        }
    }

    // XXX Helper to remove the snooze preference. Do not use clear because that
    // will erase the clock preferences. Also clear the snooze notification in
    // the window shade.
    private void clearSnoozePreference(final SharedPreferences prefs, final int id) {
        final String alarmStr = Integer.toString(id);
        final Set<String> snoozedIds = prefs.getStringSet(PREF_SNOOZE_IDS, new HashSet<String>());
        if (snoozedIds.contains(alarmStr)) {
            NotificationManager nm = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
            nm.cancel(id);
        }

        final SharedPreferences.Editor ed = prefs.edit();
        snoozedIds.remove(alarmStr);
        ed.putStringSet(PREF_SNOOZE_IDS, snoozedIds);
        ed.remove(getAlarmPrefSnoozeTimeKey(alarmStr));
        ed.apply();
    }

    // XXX
    private void clearAllSnoozePreferences(final SharedPreferences prefs) {
        NotificationManager nm = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        final Set<String> snoozedIds = prefs.getStringSet(PREF_SNOOZE_IDS, new HashSet<String>());
        final SharedPreferences.Editor ed = prefs.edit();
        for (String snoozeId : snoozedIds) {
            nm.cancel(Integer.parseInt(snoozeId));
            ed.remove(getAlarmPrefSnoozeTimeKey(snoozeId));
        }

        ed.remove(PREF_SNOOZE_IDS);
        ed.apply();
    }

    private boolean hasAlarmBeenSnoozed(final SharedPreferences prefs, final int alarmId) {
        final Set<String> snoozedIds = prefs.getStringSet(PREF_SNOOZE_IDS, null);

        // Return true if there a valid snoozed alarmId was saved
        return snoozedIds != null && snoozedIds.contains(Integer.toString(alarmId));
    }

    /**
     * Updates the specified Alarm with the additional snooze time. Returns a
     * boolean indicating whether the alarm was updated.
     */
    private boolean updateAlarmTimeForSnooze(final SharedPreferences prefs, final Alarm alarm) {
        if (!hasAlarmBeenSnoozed(prefs, alarm.id)) {
            // No need to modify the alarm
            return false;
        }

        final long time = prefs.getLong(getAlarmPrefSnoozeTimeKey(alarm.id), -1);
        // The time in the database is either 0 (repeating) or a specific time
        // for a non-repeating alarm. Update this value so the AlarmReceiver
        // has the right time to compare.
        alarm.time = time;

        return true;
    }

    private void broadcastAlarmState(Alarm alarm, String action) {
        Intent intent = new Intent(action);
        intent.putExtra(Intents.ALARM_INTENT_EXTRA, alarm);
        mContext.sendBroadcast(intent);
    }
}
