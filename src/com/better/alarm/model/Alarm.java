/*
 * Copyright (C) 2009 The Android Open Source Project
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

import java.text.DateFormatSymbols;
import java.util.Calendar;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

import com.better.alarm.R;

public final class Alarm implements Comparable<Alarm> {
    private static final String TAG = "Alarm";
    private static final boolean DBG = true;

    // ////////////////////////////
    // Column definitions
    // ////////////////////////////
    static class Columns implements BaseColumns {
        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = Uri.parse("content://com.better.alarm.model/alarm");

        /**
         * Hour in 24-hour localtime 0 - 23.
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String HOUR = "hour";

        /**
         * Minutes in localtime 0 - 59
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String MINUTES = "minutes";

        /**
         * Days of week coded as integer
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String DAYS_OF_WEEK = "daysofweek";

        /**
         * Alarm time in UTC milliseconds from the epoch.
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String ALARM_TIME = "alarmtime";

        /**
         * True if alarm is active
         * <P>
         * Type: BOOLEAN
         * </P>
         */
        public static final String ENABLED = "enabled";

        /**
         * True if alarm should vibrate
         * <P>
         * Type: BOOLEAN
         * </P>
         */
        public static final String VIBRATE = "vibrate";

        /**
         * Message to show when alarm triggers Note: not currently used
         * <P>
         * Type: STRING
         * </P>
         */
        public static final String MESSAGE = "message";

        /**
         * Audio alert to play when alarm triggers
         * <P>
         * Type: STRING
         * </P>
         */
        public static final String ALERT = "alert";

        /**
         * Use prealarm
         * <P>
         * Type: STRING
         * </P>
         */
        public static final String PREALARM = "prealarm";

        /**
         * The default sort order for this table
         */
        public static final String DEFAULT_SORT_ORDER = HOUR + ", " + MINUTES + " ASC";

        // Used when filtering enabled alarms.
        public static final String WHERE_ENABLED = ENABLED + "=1";

        static final String[] ALARM_QUERY_COLUMNS = { _ID, HOUR, MINUTES, DAYS_OF_WEEK, ALARM_TIME, ENABLED, VIBRATE,
                MESSAGE, ALERT, PREALARM };

        /**
         * These save calls to cursor.getColumnIndexOrThrow() THEY MUST BE KEPT
         * IN SYNC WITH ABOVE QUERY COLUMNS
         */
        public static final int ALARM_ID_INDEX = 0;
        public static final int ALARM_HOUR_INDEX = 1;
        public static final int ALARM_MINUTES_INDEX = 2;
        public static final int ALARM_DAYS_OF_WEEK_INDEX = 3;
        @Deprecated
        public static final int ALARM_TIME_INDEX = 4;
        public static final int ALARM_ENABLED_INDEX = 5;
        public static final int ALARM_VIBRATE_INDEX = 6;
        public static final int ALARM_MESSAGE_INDEX = 7;
        public static final int ALARM_ALERT_INDEX = 8;
        public static final int ALARM_PREALARM_INDEX = 9;
    }

    // ////////////////////////////
    // End column definitions
    // ////////////////////////////

    // This string is used to indicate a silent alarm in the db.
    private static final String ALARM_ALERT_SILENT = "silent";

    private int id;
    private boolean enabled;
    private int hour;
    private int minutes;
    private DaysOfWeek daysOfWeek;
    private boolean vibrate;
    private String label;
    private Uri alert;
    private boolean silent;
    private boolean prealarm;

    Alarm(Cursor c) {
        id = c.getInt(Columns.ALARM_ID_INDEX);
        enabled = c.getInt(Columns.ALARM_ENABLED_INDEX) == 1;
        hour = c.getInt(Columns.ALARM_HOUR_INDEX);
        minutes = c.getInt(Columns.ALARM_MINUTES_INDEX);
        daysOfWeek = new DaysOfWeek(c.getInt(Columns.ALARM_DAYS_OF_WEEK_INDEX));
        vibrate = c.getInt(Columns.ALARM_VIBRATE_INDEX) == 1;
        label = c.getString(Columns.ALARM_MESSAGE_INDEX);
        String alertString = c.getString(Columns.ALARM_ALERT_INDEX);
        prealarm = c.getInt(Columns.ALARM_PREALARM_INDEX) == 1;
        if (ALARM_ALERT_SILENT.equals(alertString)) {
            if (DBG) Log.d(TAG, "Alarm is marked as silent");
            silent = true;
        } else {
            if (alertString != null && alertString.length() != 0) {
                alert = Uri.parse(alertString);
            }

            // If the database alert is null or it failed to parse, use the
            // default alert.
            if (alert == null) {
                alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            }
        }
    }

    // Creates a default alarm at the current time.
    Alarm() {
        id = -1;
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(System.currentTimeMillis());
        hour = c.get(Calendar.HOUR_OF_DAY);
        minutes = c.get(Calendar.MINUTE);
        vibrate = true;
        daysOfWeek = new DaysOfWeek(0);
        alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        prealarm = false;
    }

    public String getLabelOrDefault(Context context) {
        if (label == null || label.length() == 0) {
            return context.getString(R.string.default_label);
        }
        return label;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Alarm)) return false;
        final Alarm other = (Alarm) o;
        return id == other.id;
    }

    /*
     * Days of week code as a single int. 0x00: no day 0x01: Monday 0x02:
     * Tuesday 0x04: Wednesday 0x08: Thursday 0x10: Friday 0x20: Saturday 0x40:
     * Sunday
     */
    public static final class DaysOfWeek {

        private static int[] DAY_MAP = new int[] { Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
                Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY, };

        // Bitmask of all repeating days
        private int mDays;

        public DaysOfWeek(int days) {
            mDays = days;
        }

        public String toString(Context context, boolean showNever) {
            StringBuilder ret = new StringBuilder();

            // no days
            if (mDays == 0) {
                return showNever ? context.getText(R.string.never).toString() : "";
            }

            // every day
            if (mDays == 0x7f) {
                return context.getText(R.string.every_day).toString();
            }

            // count selected days
            int dayCount = 0, days = mDays;
            while (days > 0) {
                if ((days & 1) == 1) dayCount++;
                days >>= 1;
            }

            // short or long form?
            DateFormatSymbols dfs = new DateFormatSymbols();
            String[] dayList = (dayCount > 1) ? dfs.getShortWeekdays() : dfs.getWeekdays();

            // selected days
            for (int i = 0; i < 7; i++) {
                if ((mDays & (1 << i)) != 0) {
                    ret.append(dayList[DAY_MAP[i]]);
                    dayCount -= 1;
                    if (dayCount > 0) ret.append(context.getText(R.string.day_concat));
                }
            }
            return ret.toString();
        }

        private boolean isSet(int day) {
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

        // Returns days of week encoded in an array of booleans.
        public boolean[] getBooleanArray() {
            boolean[] ret = new boolean[7];
            for (int i = 0; i < 7; i++) {
                ret[i] = isSet(i);
            }
            return ret;
        }

        public boolean isRepeatSet() {
            return mDays != 0;
        }

        /**
         * returns number of days from today until next alarm
         * 
         * @param c
         *            must be set to today
         */
        public int getNextAlarm(Calendar c) {
            if (mDays == 0) {
                return -1;
            }

            int today = (c.get(Calendar.DAY_OF_WEEK) + 5) % 7;

            int day = 0;
            int dayCount = 0;
            for (; dayCount < 7; dayCount++) {
                day = (today + dayCount) % 7;
                if (isSet(day)) {
                    break;
                }
            }
            return dayCount;
        }

        @Override
        public String toString() {
            if (mDays == 0) return "never";
            if (mDays == 0x7f) return "everyday";
            StringBuilder ret = new StringBuilder();
            String[] dayList = new DateFormatSymbols().getShortWeekdays();
            for (int i = 0; i < 7; i++) {
                if ((mDays & (1 << i)) != 0) {
                    ret.append(dayList[DAY_MAP[i]]);
                }
            }
            return ret.toString();
        }
    }

    /**
     * Given an alarm in hours and minutes, return a time suitable for setting
     * in AlarmManager.
     */
    public Calendar calculateCalendar() {

        // start with now
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(System.currentTimeMillis());

        int nowHour = c.get(Calendar.HOUR_OF_DAY);
        int nowMinute = c.get(Calendar.MINUTE);

        // if alarm is behind current time, advance one day
        if (hour < nowHour || hour == nowHour && minutes <= nowMinute) {
            c.add(Calendar.DAY_OF_YEAR, 1);
        }
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, minutes);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        int addDays = daysOfWeek.getNextAlarm(c);
        if (addDays > 0) c.add(Calendar.DAY_OF_WEEK, addDays);
        return c;
    }

    public long getTimeInMillis() {
        return calculateCalendar().getTimeInMillis();
    }

    public boolean isPrealarm() {
        return prealarm;
    }

    public boolean isSilent() {
        return silent;
    }

    public Uri getAlert() {
        return alert;
    }

    public String getLabel() {
        return label;
    }

    public boolean isVibrate() {
        return vibrate;
    }

    public DaysOfWeek getDaysOfWeek() {
        return daysOfWeek;
    }

    public int getMinutes() {
        return minutes;
    }

    public int getHour() {
        return hour;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getId() {
        return id;
    }

    ContentValues createContentValues() {
        ContentValues values = new ContentValues(8);
        // Set the alarm_time value if this alarm does not repeat. This will be
        // used later to disable expire alarms.
        long time = 0;
        if (!daysOfWeek.isRepeatSet()) {
            time = getTimeInMillis();
        }

        values.put(Alarm.Columns.ENABLED, enabled ? 1 : 0);
        values.put(Alarm.Columns.HOUR, hour);
        values.put(Alarm.Columns.MINUTES, minutes);
        values.put(Alarm.Columns.ALARM_TIME, time);
        values.put(Alarm.Columns.DAYS_OF_WEEK, daysOfWeek.getCoded());
        values.put(Alarm.Columns.VIBRATE, vibrate);
        values.put(Alarm.Columns.MESSAGE, label);
        values.put(Alarm.Columns.PREALARM, prealarm);

        // A null alert Uri indicates a silent
        values.put(Alarm.Columns.ALERT, alert == null ? ALARM_ALERT_SILENT : alert.toString());

        return values;
    }

    @Override
    public int compareTo(Alarm another) {
        if (!this.enabled && !another.enabled) {
            return 0;
        }
        if (!this.enabled && another.enabled) {
            return 1;
        }
        if (this.enabled && !another.enabled) {
            return -1;
        }
        if (this.enabled && another.enabled) {
            // both are enabled, let's see which one is closer
            if (getTimeInMillis() < another.getTimeInMillis()) {
                return -1;
            }
            if (getTimeInMillis() > another.getTimeInMillis()) {
                return 1;
            }
            // probably equal
            return 0;
        }
        throw new RuntimeException("should never get here!");
    }

    void setPrealarm(boolean prealarm) {
        this.prealarm = prealarm;
    }

    void setSilent(boolean silent) {
        this.silent = silent;
    }

    void setAlert(Uri alert) {
        this.alert = alert;
    }

    void setLabel(String label) {
        this.label = label;
    }

    void setVibrate(boolean vibrate) {
        this.vibrate = vibrate;
    }

    void setDaysOfWeek(DaysOfWeek daysOfWeek) {
        this.daysOfWeek = daysOfWeek;
    }

    void setMinutes(int minutes) {
        this.minutes = minutes;
    }

    void setHour(int hour) {
        this.hour = hour;
    }

    void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    void setId(int id) {
        this.id = id;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Alarm [");
        sb.append(enabled ? " enabled" : "disabled").append(", ");
        sb.append(hour).append(":").append(minutes).append(", ");
        sb.append(prealarm ? "prealarm" : " regular ").append(", ");
        sb.append(getTimeInMillis()).append(", ");
        sb.append(daysOfWeek.toString());
        sb.append("]");
        return sb.toString();
    }
}
