package com.better.alarm.persistance

import android.net.Uri
import android.provider.BaseColumns

import com.better.alarm.BuildConfig

// ////////////////////////////
// Column definitions
// ////////////////////////////
class Columns : BaseColumns {
    companion object {
        /**
         * The content:// style URL for this table
         */
        val CONTENT_URI by lazy { Uri.parse("content://" + BuildConfig.APPLICATION_ID + ".model/alarm") }

        @JvmStatic
        fun contentUri() = CONTENT_URI

        /**
         * Hour in 24-hour localtime 0 - 23.
         *
         *
         * Type: INTEGER
         *
         */
        @JvmField
        val HOUR = "hour"

        /**
         * Minutes in localtime 0 - 59
         *
         *
         * Type: INTEGER
         *
         */
        @JvmField
        val MINUTES = "minutes"

        /**
         * Days of week coded as integer
         *
         *
         * Type: INTEGER
         *
         */
        @JvmField
        val DAYS_OF_WEEK = "daysofweek"

        /**
         * AlarmCore time in UTC milliseconds from the epoch.
         *
         *
         * Type: INTEGER
         *
         */
        @JvmField
        val ALARM_TIME = "alarmtime"

        /**
         * True if alarm is active
         *
         *
         * Type: BOOLEAN
         *
         */
        @JvmField
        val ENABLED = "enabled"

        /**
         * True if alarm should vibrate
         *
         *
         * Type: BOOLEAN
         *
         */
        @JvmField
        val VIBRATE = "vibrate"

        /**
         * Message to show when alarm triggers Note: not currently used
         *
         *
         * Type: STRING
         *
         */
        @JvmField
        val MESSAGE = "message"

        /**
         * Audio alert to play when alarm triggers
         *
         *
         * Type: STRING
         *
         */
        @JvmField
        val ALERT = "alert"

        /**
         * Use prealarm
         *
         *
         * Type: STRING
         *
         */
        @JvmField
        val PREALARM = "prealarm"

        /**
         * State machine state
         *
         *
         * Type: STRING
         *
         */
        @JvmField
        val STATE = "state"

        /**
         * The default sort order for this table
         */
        @JvmField
        val DEFAULT_SORT_ORDER = "$HOUR, $MINUTES ASC"
        @JvmField
        val ALARM_QUERY_COLUMNS = arrayOf(BaseColumns._ID, HOUR, MINUTES, DAYS_OF_WEEK, ALARM_TIME, ENABLED, VIBRATE, MESSAGE, ALERT, PREALARM, STATE)

        /**
         * These save calls to cursor.getColumnIndexOrThrow() THEY MUST BE KEPT
         * IN SYNC WITH ABOVE QUERY COLUMNS
         */
        @JvmField
        val ALARM_ID_INDEX = 0
        @JvmField
        val ALARM_HOUR_INDEX = 1
        @JvmField
        val ALARM_MINUTES_INDEX = 2
        @JvmField
        val ALARM_DAYS_OF_WEEK_INDEX = 3
        @JvmField
        val ALARM_TIME_INDEX = 4
        @JvmField
        val ALARM_ENABLED_INDEX = 5
        @JvmField
        val ALARM_VIBRATE_INDEX = 6
        @JvmField
        val ALARM_MESSAGE_INDEX = 7
        @JvmField
        val ALARM_ALERT_INDEX = 8
        @JvmField
        val ALARM_PREALARM_INDEX = 9
        @JvmField
        val ALARM_STATE_INDEX = 10
    }
}
