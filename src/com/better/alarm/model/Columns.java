package com.better.alarm.model;

import android.net.Uri;
import android.provider.BaseColumns;

// ////////////////////////////
// Column definitions
// ////////////////////////////
class Columns implements BaseColumns {
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
     * AlarmCore time in UTC milliseconds from the epoch.
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
     * AlarmCore time in UTC milliseconds from the epoch.
     * <P>
     * Type: INTEGER
     * </P>
     */
    public static final String PREALARM_TIME = "prealarm_TIME";

    /**
     * True if alarm is snoozed
     * <P>
     * Type: BOOLEAN
     * </P>
     */
    public static final String SNOOZED = "snoozed";

    /**
     * AlarmCore time in UTC milliseconds from the epoch.
     * <P>
     * Type: INTEGER
     * </P>
     */
    public static final String SNOOZE_TIME = "snooze_time";

    /**
     * The default sort order for this table
     */
    public static final String DEFAULT_SORT_ORDER = HOUR + ", " + MINUTES + " ASC";

    // Used when filtering enabled alarms.
    public static final String WHERE_ENABLED = ENABLED + "=1";

    static final String[] ALARM_QUERY_COLUMNS = { _ID, HOUR, MINUTES, DAYS_OF_WEEK, ALARM_TIME, ENABLED, VIBRATE,
            MESSAGE, ALERT, PREALARM, PREALARM_TIME, SNOOZED, SNOOZE_TIME };

    /**
     * These save calls to cursor.getColumnIndexOrThrow() THEY MUST BE KEPT
     * IN SYNC WITH ABOVE QUERY COLUMNS
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
    public static final int ALARM_PREALARM_INDEX = 9;
    public static final int ALARM_PREALARM_TIME_INDEX = 10;
    public static final int ALARM_SNOOZED_INDEX = 11;
    public static final int ALARM_SNOOZE_TIME_INDEX = 12;
}