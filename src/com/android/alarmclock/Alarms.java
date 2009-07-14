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

package com.android.alarmclock;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.Settings;
import android.text.format.DateFormat;

import java.util.Calendar;
import java.text.DateFormatSymbols;

/**
 * The Alarms provider supplies info about Alarm Clock settings
 */
public class Alarms {

    final static String ALARM_ALERT_ACTION = "com.android.alarmclock.ALARM_ALERT";
    final static String ID = "alarm_id";
    final static String TIME = "alarm_time";
    final static String LABEL = "alarm_label";

    final static String PREF_SNOOZE_ID = "snooze_id";
    final static String PREF_SNOOZE_TIME = "snooze_time";
    final static String PREF_SNOOZE_LABEL = "snooze_label";

    private final static String DM12 = "E h:mm aa";
    private final static String DM24 = "E kk:mm";

    private final static String M12 = "h:mm aa";
    // Shared with DigitalClock
    final static String M24 = "kk:mm";

    /**
     * Mapping from days in this application (where Monday is 0) to
     * days in DateFormatSymbols (where Monday is 2).
     */
    private static int[] DAY_MAP = new int[] {
        Calendar.MONDAY,
        Calendar.TUESDAY,
        Calendar.WEDNESDAY,
        Calendar.THURSDAY,
        Calendar.FRIDAY,
        Calendar.SATURDAY,
        Calendar.SUNDAY,
    };

    static class DaysOfWeek {

        int mDays;

        /**
         * Days of week coded as single int, convenient for DB
         * storage:
         *
         * 0x00:  no day
         * 0x01:  Monday
         * 0x02:  Tuesday
         * 0x04:  Wednesday
         * 0x08:  Thursday
         * 0x10:  Friday
         * 0x20:  Saturday
         * 0x40:  Sunday
         */
        DaysOfWeek() {
            this(0);
        }

        DaysOfWeek(int days) {
            mDays = days;
        }

        public String toString(Context context, boolean showNever) {
            StringBuilder ret = new StringBuilder();

            /* no days */
            if (mDays == 0) return showNever ? context.getText(
                    R.string.never).toString() : "";

            /* every day */
            if (mDays == 0x7f) {
                return context.getText(R.string.every_day).toString();
            }

            /* count selected days */
            int dayCount = 0, days = mDays;
            while (days > 0) {
                if ((days & 1) == 1) dayCount++;
                days >>= 1;
            }

            /* short or long form? */
            DateFormatSymbols dfs = new DateFormatSymbols();
            String[] dayList = (dayCount > 1) ?
                                    dfs.getShortWeekdays() :
                                    dfs.getWeekdays();

            /* selected days */
            for (int i = 0; i < 7; i++) {
                if ((mDays & (1 << i)) != 0) {
                    ret.append(dayList[DAY_MAP[i]]);
                    dayCount -= 1;
                    if (dayCount > 0) ret.append(
                            context.getText(R.string.day_concat));
                }
            }
            return ret.toString();
        }

        /**
         * @param day Mon=0 ... Sun=6
         * @return true if given day is set
         */
        public boolean isSet(int day) {
            return ((mDays & (1 << day)) > 0);
        }

        public void set(int day, boolean set) {
            if (set) {
                mDays |= (1 << day);
            } else {
                mDays &= ~(1 << day);
            }
        }

        public void set(DaysOfWeek dow) {
            mDays = dow.mDays;
        }

        public int getCoded() {
            return mDays;
        }

        public boolean equals(DaysOfWeek dow) {
            return mDays == dow.mDays;
        }

        // Returns days of week encoded in an array of booleans.
        public boolean[] getBooleanArray() {
            boolean[] ret = new boolean[7];
            for (int i = 0; i < 7; i++) {
                ret[i] = isSet(i);
            }
            return ret;
        }

        public void setCoded(int days) {
            mDays = days;
        }

        /**
         * @return true if alarm is set to repeat
         */
        public boolean isRepeatSet() {
            return mDays != 0;
        }

        /**
         * @return true if alarm is set to repeat every day
         */
        public boolean isEveryDaySet() {
            return mDays == 0x7f;
        }


        /**
         * returns number of days from today until next alarm
         * @param c must be set to today
         */
        public int getNextAlarm(Calendar c) {
            if (mDays == 0) return -1;
            int today = (c.get(Calendar.DAY_OF_WEEK) + 5) % 7;

            int day, dayCount;
            for (dayCount = 0; dayCount < 7; dayCount++) {
                day = (today + dayCount) % 7;
                if ((mDays & (1 << day)) > 0) {
                    break;
                }
            }
            return dayCount;
        }
    }

    public static class AlarmColumns implements BaseColumns {

        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI =
            Uri.parse("content://com.android.alarmclock/alarm");

        public static final String _ID = "_id";

        /**
         * The default sort order for this table
         */
        public static final String DEFAULT_SORT_ORDER = "_id ASC";

        /**
         * Hour in 24-hour localtime 0 - 23.
         * <P>Type: INTEGER</P>
         */
        public static final String HOUR = "hour";

        /**
         * Minutes in localtime 0 - 59
         * <P>Type: INTEGER</P>
         */
        public static final String MINUTES = "minutes";

        /**
         * Days of week coded as integer
         * <P>Type: INTEGER</P>
         */
        public static final String DAYS_OF_WEEK = "daysofweek";

        /**
         * Alarm time in UTC milliseconds from the epoch.
         * <P>Type: INTEGER</P>
         */
        public static final String ALARM_TIME = "alarmtime";

        /**
         * True if alarm is active
         * <P>Type: BOOLEAN</P>
         */
        public static final String ENABLED = "enabled";

        /**
         * True if alarm should vibrate
         * <P>Type: BOOLEAN</P>
         */
        public static final String VIBRATE = "vibrate";

        /**
         * Message to show when alarm triggers
         * Note: not currently used
         * <P>Type: STRING</P>
         */
        public static final String MESSAGE = "message";

        /**
         * Audio alert to play when alarm triggers
         * <P>Type: STRING</P>
         */
        public static final String ALERT = "alert";

        static final String[] ALARM_QUERY_COLUMNS = {
            _ID, HOUR, MINUTES, DAYS_OF_WEEK, ALARM_TIME,
            ENABLED, VIBRATE, MESSAGE, ALERT};

        /**
         * These save calls to cursor.getColumnIndexOrThrow()
         * THEY MUST BE KEPT IN SYNC WITH ABOVE QUERY COLUMNS
         */
        public static final int ALARM_ID_INDEX = 0;
        public static final int ALARM_HOUR_INDEX = 1;
        public static final int ALARM_MINUTES_INDEX = 2;
        public static final int ALARM_DAYS_OF_WEEK_INDEX = 3;
        public static final int ALARM_TIME_INDEX = 4;
        public static final int ALARM_ENABLED_INDEX = 5;
        public static final int ALARM_VIBRATE_INDEX = 6;
        public static final int ALARM_MESSAGE_INDEX = 7;
        public static final int ALARM_ALERT_INDEX = 8;
    }

    /**
     * getAlarm and getAlarms call this interface to report alarms in
     * the database
     */
    static interface AlarmSettings {
        void reportAlarm(
                int idx, boolean enabled, int hour, int minutes,
                DaysOfWeek daysOfWeek, boolean vibrate, String message,
                String alert);
    }

    /**
     * Creates a new Alarm.
     */
    public synchronized static Uri addAlarm(ContentResolver contentResolver) {
        ContentValues values = new ContentValues();
        values.put(Alarms.AlarmColumns.HOUR, 8);
        return contentResolver.insert(AlarmColumns.CONTENT_URI, values);
    }

    /**
     * Removes an existing Alarm.  If this alarm is snoozing, disables
     * snooze.  Sets next alert.
     */
    public synchronized static void deleteAlarm(
            Context context, int alarmId) {

        ContentResolver contentResolver = context.getContentResolver();
        /* If alarm is snoozing, lose it */
        int snoozeId = getSnoozeAlarmId(context);
        if (snoozeId == alarmId) disableSnoozeAlert(context);

        Uri uri = ContentUris.withAppendedId(AlarmColumns.CONTENT_URI, alarmId);
        deleteAlarm(contentResolver, uri);

        setNextAlert(context);
    }

    private synchronized static void deleteAlarm(
            ContentResolver contentResolver, Uri uri) {
        contentResolver.delete(uri, "", null);
    }

    /**
     * Queries all alarms
     * @return cursor over all alarms
     */
    public synchronized static Cursor getAlarmsCursor(
            ContentResolver contentResolver) {
        return contentResolver.query(
                AlarmColumns.CONTENT_URI, AlarmColumns.ALARM_QUERY_COLUMNS,
                null, null, AlarmColumns.DEFAULT_SORT_ORDER);
    }

    /**
     * Calls the AlarmSettings.reportAlarm interface on all alarms found in db.
     */
    public synchronized static void getAlarms(
            ContentResolver contentResolver, AlarmSettings alarmSettings) {
        Cursor cursor = getAlarmsCursor(contentResolver);
        getAlarms(alarmSettings, cursor);
        cursor.close();
    }

    private synchronized static void getAlarms(
            AlarmSettings alarmSettings, Cursor cur) {
        if (cur.moveToFirst()) {
            do {
                // Get the field values
                int id = cur.getInt(AlarmColumns.ALARM_ID_INDEX);
                int hour = cur.getInt(AlarmColumns.ALARM_HOUR_INDEX);
                int minutes = cur.getInt(AlarmColumns.ALARM_MINUTES_INDEX);
                int daysOfWeek = cur.getInt(AlarmColumns.ALARM_DAYS_OF_WEEK_INDEX);
                boolean enabled = cur.getInt(AlarmColumns.ALARM_ENABLED_INDEX) == 1 ? true : false;
                boolean vibrate = cur.getInt(AlarmColumns.ALARM_VIBRATE_INDEX) == 1 ? true : false;
                String message = cur.getString(AlarmColumns.ALARM_MESSAGE_INDEX);
                String alert = cur.getString(AlarmColumns.ALARM_ALERT_INDEX);
                alarmSettings.reportAlarm(
                        id, enabled, hour, minutes, new DaysOfWeek(daysOfWeek),
                        vibrate, message, alert);
            } while (cur.moveToNext());
        }
    }

    /**
     * Calls the AlarmSettings.reportAlarm interface on alarm with given
     * alarmId
     */
    public synchronized static void getAlarm(
            ContentResolver contentResolver, AlarmSettings alarmSetting,
            int alarmId) {
        Cursor cursor = contentResolver.query(
                ContentUris.withAppendedId(AlarmColumns.CONTENT_URI, alarmId),
                AlarmColumns.ALARM_QUERY_COLUMNS,
                null, null, AlarmColumns.DEFAULT_SORT_ORDER);

        getAlarms(alarmSetting, cursor);
        cursor.close();
    }


    /**
     * A convenience method to set an alarm in the Alarms
     * content provider.
     *
     * @param id             corresponds to the _id column
     * @param enabled        corresponds to the ENABLED column
     * @param hour           corresponds to the HOUR column
     * @param minutes        corresponds to the MINUTES column
     * @param daysOfWeek     corresponds to the DAYS_OF_WEEK column
     * @param time           corresponds to the ALARM_TIME column
     * @param vibrate        corresponds to the VIBRATE column
     * @param message        corresponds to the MESSAGE column
     * @param alert          corresponds to the ALERT column
     */
    public synchronized static void setAlarm(
            Context context, int id, boolean enabled, int hour, int minutes,
            DaysOfWeek daysOfWeek, boolean vibrate, String message,
            String alert) {

        ContentValues values = new ContentValues(8);
        ContentResolver resolver = context.getContentResolver();
        long time = calculateAlarm(hour, minutes, daysOfWeek).getTimeInMillis();

        if (Log.LOGV) Log.v(
                "**  setAlarm * idx " + id + " hour " + hour + " minutes " +
                minutes + " enabled " + enabled + " time " + time);

        values.put(AlarmColumns.ENABLED, enabled ? 1 : 0);
        values.put(AlarmColumns.HOUR, hour);
        values.put(AlarmColumns.MINUTES, minutes);
        values.put(AlarmColumns.ALARM_TIME, time);
        values.put(AlarmColumns.DAYS_OF_WEEK, daysOfWeek.getCoded());
        values.put(AlarmColumns.VIBRATE, vibrate);
        values.put(AlarmColumns.MESSAGE, message);
        values.put(AlarmColumns.ALERT, alert);
        resolver.update(ContentUris.withAppendedId(AlarmColumns.CONTENT_URI, id),
                        values, null, null);

        int aid = disableSnoozeAlert(context);
        if (aid != -1 && aid != id) enableAlarmInternal(context, aid, false);
        setNextAlert(context);
    }

    /**
     * A convenience method to enable or disable an alarm.
     *
     * @param id             corresponds to the _id column
     * @param enabled        corresponds to the ENABLED column
     */

    public synchronized static void enableAlarm(
            final Context context, final int id, boolean enabled) {
        int aid = disableSnoozeAlert(context);
        if (aid != -1 && aid != id) enableAlarmInternal(context, aid, false);
        enableAlarmInternal(context, id, enabled);
        setNextAlert(context);
    }

    private synchronized static void enableAlarmInternal(
            final Context context, final int id, boolean enabled) {
        ContentResolver resolver = context.getContentResolver();

        class EnableAlarm implements AlarmSettings {
            public int mHour;
            public int mMinutes;
            public DaysOfWeek mDaysOfWeek;

            public void reportAlarm(
                    int idx, boolean enabled, int hour, int minutes,
                    DaysOfWeek daysOfWeek, boolean vibrate, String message,
                    String alert) {
                mHour = hour;
                mMinutes = minutes;
                mDaysOfWeek = daysOfWeek;
            }
        }

        ContentValues values = new ContentValues(2);
        values.put(AlarmColumns.ENABLED, enabled ? 1 : 0);

        /* If we are enabling the alarm, load hour/minutes/daysOfWeek
           from db, so we can calculate alarm time */
        if (enabled) {
            EnableAlarm enableAlarm = new EnableAlarm();
            getAlarm(resolver, enableAlarm, id);
            if (enableAlarm.mDaysOfWeek == null) {
                /* Under monkey, sometimes reportAlarm is never
                   called */
                Log.e("** enableAlarmInternal failed " + id + " h " +
                      enableAlarm.mHour + " m " + enableAlarm.mMinutes);
                return;
            }

            long time = calculateAlarm(enableAlarm.mHour, enableAlarm.mMinutes,
                                       enableAlarm.mDaysOfWeek).getTimeInMillis();
            values.put(AlarmColumns.ALARM_TIME, time);
        }

        resolver.update(ContentUris.withAppendedId(AlarmColumns.CONTENT_URI, id),
                        values, null, null);
    }


    /**
     * Calculates next scheduled alert
     */
    static class AlarmCalculator implements AlarmSettings {
        private long mMinAlert = Long.MAX_VALUE;
        private int mMinIdx = -1;
        private String mLabel;

        /**
         * returns next scheduled alert, MAX_VALUE if none
         */
        public long getAlert() {
            return mMinAlert;
        }
        public int getIndex() {
            return mMinIdx;
        }
        public String getLabel() {
            return mLabel;
        }

        public void reportAlarm(
                int idx, boolean enabled, int hour, int minutes,
                DaysOfWeek daysOfWeek, boolean vibrate, String message,
                String alert) {
            if (enabled) {
                long atTime = calculateAlarm(hour, minutes,
                                             daysOfWeek).getTimeInMillis();
                /* Log.i("**  SET ALERT* idx " + idx + " hour " + hour + " minutes " +
                   minutes + " enabled " + enabled + " calc " + atTime); */
                if (atTime < mMinAlert) {
                    mMinIdx = idx;
                    mMinAlert = atTime;
                    mLabel = message;
                }
            }
        }
    }

    static AlarmCalculator calculateNextAlert(final Context context) {
        ContentResolver resolver = context.getContentResolver();
        AlarmCalculator alarmCalc = new AlarmCalculator();
        getAlarms(resolver, alarmCalc);
        return alarmCalc;
    }

    /**
     * Disables non-repeating alarms that have passed.  Called at
     * boot.
     */
    public static void disableExpiredAlarms(final Context context) {
        Cursor cur = getAlarmsCursor(context.getContentResolver());
        long now = System.currentTimeMillis();

        if (cur.moveToFirst()) {
            do {
                // Get the field values
                int id = cur.getInt(AlarmColumns.ALARM_ID_INDEX);
                boolean enabled = cur.getInt(
                        AlarmColumns.ALARM_ENABLED_INDEX) == 1 ? true : false;
                DaysOfWeek daysOfWeek = new DaysOfWeek(
                        cur.getInt(AlarmColumns.ALARM_DAYS_OF_WEEK_INDEX));
                long time = cur.getLong(AlarmColumns.ALARM_TIME_INDEX);

                if (enabled && !daysOfWeek.isRepeatSet() && time < now) {
                    if (Log.LOGV) Log.v(
                            "** DISABLE " + id + " now " + now +" set " + time);
                    enableAlarmInternal(context, id, false);
                }
            } while (cur.moveToNext());
        }
        cur.close();
    }

    private static NotificationManager getNotificationManager(
            final Context context) {
        return (NotificationManager) context.getSystemService(
                context.NOTIFICATION_SERVICE);
    }

    /**
     * Called at system startup, on time/timezone change, and whenever
     * the user changes alarm settings.  Activates snooze if set,
     * otherwise loads all alarms, activates next alert.
     */
    public static void setNextAlert(final Context context) {
        int snoozeId = getSnoozeAlarmId(context);
        if (snoozeId == -1) {
            AlarmCalculator ac = calculateNextAlert(context);
            int id = ac.getIndex();
            long atTime = ac.getAlert();

            if (atTime < Long.MAX_VALUE) {
                enableAlert(context, id, ac.getLabel(), atTime);
            } else {
                disableAlert(context, id);
            }
        } else {
            enableSnoozeAlert(context);
        }
    }

    /**
     * Sets alert in AlarmManger and StatusBar.  This is what will
     * actually launch the alert when the alarm triggers.
     *
     * Note: In general, apps should call setNextAlert() instead of
     * this method.  setAlert() is only used outside this class when
     * the alert is not to be driven by the state of the db.  "Snooze"
     * uses this API, as we do not want to alter the alarm in the db
     * with each snooze.
     *
     * @param id Alarm ID.
     * @param atTimeInMillis milliseconds since epoch
     */
    static void enableAlert(Context context, int id, String label,
           long atTimeInMillis) {
        AlarmManager am = (AlarmManager)
                context.getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(ALARM_ALERT_ACTION);
        if (Log.LOGV) Log.v("** setAlert id " + id + " atTime " + atTimeInMillis);
        intent.putExtra(ID, id);
        intent.putExtra(LABEL, label);
        intent.putExtra(TIME, atTimeInMillis);
        PendingIntent sender = PendingIntent.getBroadcast(
                context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        if (true) {
            am.set(AlarmManager.RTC_WAKEUP, atTimeInMillis, sender);
        } else {
            // a five-second alarm, for testing
            am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 5000,
                   sender);
        }

        setStatusBarIcon(context, true);

        Calendar c = Calendar.getInstance();
        c.setTime(new java.util.Date(atTimeInMillis));
        String timeString = formatDayAndTime(context, c);
        saveNextAlarm(context, timeString);
    }

    /**
     * Disables alert in AlarmManger and StatusBar.
     *
     * @param id Alarm ID.
     */
    static void disableAlert(Context context, int id) {
        AlarmManager am = (AlarmManager)
                context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(ALARM_ALERT_ACTION);
        intent.putExtra(ID, id);
        PendingIntent sender = PendingIntent.getBroadcast(
                context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        am.cancel(sender);
        setStatusBarIcon(context, false);
        saveNextAlarm(context, "");
    }

    static void saveSnoozeAlert(final Context context, int id,
                                long atTimeInMillis, String label) {
        SharedPreferences prefs = context.getSharedPreferences(
                AlarmClock.PREFERENCES, 0);
        SharedPreferences.Editor ed = prefs.edit();
        ed.putInt(PREF_SNOOZE_ID, id);
        ed.putLong(PREF_SNOOZE_TIME, atTimeInMillis);
        ed.putString(PREF_SNOOZE_LABEL, label);
        ed.commit();
    }

    /**
     * @return ID of alarm disabled, if disabled, -1 otherwise
     */
    static int disableSnoozeAlert(final Context context) {
        int id = getSnoozeAlarmId(context);
        if (id == -1) return -1;
        saveSnoozeAlert(context, -1, 0, null);
        return id;
    }

    /**
     * @return alarm ID of snoozing alarm, -1 if snooze unset
     */
    private static int getSnoozeAlarmId(final Context context) {
        SharedPreferences prefs = context.getSharedPreferences(
                AlarmClock.PREFERENCES, 0);
        return prefs.getInt(PREF_SNOOZE_ID, -1);
    }

    /**
     * If there is a snooze set, enable it in AlarmManager
     * @return true if snooze is set
     */
    private static boolean enableSnoozeAlert(final Context context) {
        SharedPreferences prefs = context.getSharedPreferences(
                AlarmClock.PREFERENCES, 0);

        int id = prefs.getInt(PREF_SNOOZE_ID, -1);
        if (id == -1) return false;
        long atTimeInMillis = prefs.getLong(PREF_SNOOZE_TIME, -1);
        if (id == -1) return false;
        // Try to get the label from the snooze preference. If null, use the
        // default label.
        String label = prefs.getString(PREF_SNOOZE_LABEL, null);
        if (label == null) {
            label = context.getString(R.string.default_label);
        }
        enableAlert(context, id, label, atTimeInMillis);
        return true;
    }


    /**
     * Tells the StatusBar whether the alarm is enabled or disabled
     */
    private static void setStatusBarIcon(Context context, boolean enabled) {
        Intent alarmChanged = new Intent(Intent.ACTION_ALARM_CHANGED);
        alarmChanged.putExtra("alarmSet", enabled);
        context.sendBroadcast(alarmChanged);
    }

    /**
     * Given an alarm in hours and minutes, return a time suitable for
     * setting in AlarmManager.
     * @param hour Always in 24 hour 0-23
     * @param minute 0-59
     * @param daysOfWeek 0-59
     */
    static Calendar calculateAlarm(int hour, int minute, DaysOfWeek daysOfWeek) {

        // start with now
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(System.currentTimeMillis());

        int nowHour = c.get(Calendar.HOUR_OF_DAY);
        int nowMinute = c.get(Calendar.MINUTE);

        // if alarm is behind current time, advance one day
        if (hour < nowHour  ||
            hour == nowHour && minute <= nowMinute) {
            c.add(Calendar.DAY_OF_YEAR, 1);
        }
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, minute);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        int addDays = daysOfWeek.getNextAlarm(c);
        /* Log.v("** TIMES * " + c.getTimeInMillis() + " hour " + hour +
           " minute " + minute + " dow " + c.get(Calendar.DAY_OF_WEEK) + " from now " +
           addDays); */
        if (addDays > 0) c.add(Calendar.DAY_OF_WEEK, addDays);
        return c;
    }

    static String formatTime(final Context context, int hour, int minute,
                             DaysOfWeek daysOfWeek) {
        Calendar c = calculateAlarm(hour, minute, daysOfWeek);
        return formatTime(context, c);
    }

    /* used by AlarmAlert */
    static String formatTime(final Context context, Calendar c) {
        String format = get24HourMode(context) ? M24 : M12;
        return (c == null) ? "" : (String)DateFormat.format(format, c);
    }

    /**
     * Shows day and time -- used for lock screen
     */
    private static String formatDayAndTime(final Context context, Calendar c) {
        String format = get24HourMode(context) ? DM24 : DM12;
        return (c == null) ? "" : (String)DateFormat.format(format, c);
    }

    /**
     * Save time of the next alarm, as a formatted string, into the system
     * settings so those who care can make use of it.
     */
    static void saveNextAlarm(final Context context, String timeString) {
        Settings.System.putString(context.getContentResolver(),
                                  Settings.System.NEXT_ALARM_FORMATTED,
                                  timeString);
    }

    /**
     * @return true if clock is set to 24-hour mode
     */
    static boolean get24HourMode(final Context context) {
        return android.text.format.DateFormat.is24HourFormat(context);
    }
}
