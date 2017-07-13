package com.better.alarm.persistance;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

import com.better.alarm.BuildConfig;
import com.better.alarm.model.AlarmContainer;
import com.better.alarm.model.Calendars;
import com.better.alarm.model.ContainerFactory;
import com.better.alarm.model.DaysOfWeek;
import com.better.alarm.model.ImmutableAlarmContainer;
import com.google.inject.Inject;

import java.util.Calendar;

/**
 * Active record container for all alarm data.
 *
 * @author Yuriy
 */
public class PersistingContainerFactory implements ContainerFactory, AlarmContainer.Persistence {
    private final Calendars calendars;
    private final Context mContext;

    // ////////////////////////////
    // Column definitions
    // ////////////////////////////
    static class Columns implements BaseColumns {
        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = Uri.parse("content://" + BuildConfig.APPLICATION_ID + ".model/alarm");

        /**
         * Hour in 24-hour localtime 0 - 23.
         * <p>
         * Type: INTEGER
         * </P>
         */
        public static final String HOUR = "hour";

        /**
         * Minutes in localtime 0 - 59
         * <p>
         * Type: INTEGER
         * </P>
         */
        public static final String MINUTES = "minutes";

        /**
         * Days of week coded as integer
         * <p>
         * Type: INTEGER
         * </P>
         */
        public static final String DAYS_OF_WEEK = "daysofweek";

        /**
         * AlarmCore time in UTC milliseconds from the epoch.
         * <p>
         * Type: INTEGER
         * </P>
         */
        public static final String ALARM_TIME = "alarmtime";

        /**
         * True if alarm is active
         * <p>
         * Type: BOOLEAN
         * </P>
         */
        public static final String ENABLED = "enabled";

        /**
         * True if alarm should vibrate
         * <p>
         * Type: BOOLEAN
         * </P>
         */
        public static final String VIBRATE = "vibrate";

        /**
         * Message to show when alarm triggers Note: not currently used
         * <p>
         * Type: STRING
         * </P>
         */
        public static final String MESSAGE = "message";

        /**
         * Audio alert to play when alarm triggers
         * <p>
         * Type: STRING
         * </P>
         */
        public static final String ALERT = "alert";

        /**
         * Use prealarm
         * <p>
         * Type: STRING
         * </P>
         */
        public static final String PREALARM = "prealarm";

        /**
         * State machine state
         * <p>
         * Type: STRING
         * </P>
         */
        public static final String STATE = "state";

        /**
         * The default sort order for this table
         */
        public static final String DEFAULT_SORT_ORDER = HOUR + ", " + MINUTES + " ASC";

        public static final String[] ALARM_QUERY_COLUMNS = {_ID, HOUR, MINUTES, DAYS_OF_WEEK, ALARM_TIME, ENABLED,
                VIBRATE, MESSAGE, ALERT, PREALARM, STATE};

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
        public static final int ALARM_STATE_INDEX = 10;
    }

    @Inject
    public PersistingContainerFactory(Calendars calendars, Context context) {
        this.calendars = calendars;
        this.mContext = context;
    }

    @Override
    public AlarmContainer create(Cursor c) {
        ImmutableAlarmContainer.Builder builder = ImmutableAlarmContainer.builder()
                .id(c.getInt(Columns.ALARM_ID_INDEX))
                .isEnabled(c.getInt(Columns.ALARM_ENABLED_INDEX) == 1)
                .hour(c.getInt(Columns.ALARM_HOUR_INDEX))
                .minutes(c.getInt(Columns.ALARM_MINUTES_INDEX))
                .daysOfWeek(new DaysOfWeek(c.getInt(Columns.ALARM_DAYS_OF_WEEK_INDEX)))
                .isVibrate(c.getInt(Columns.ALARM_VIBRATE_INDEX) == 1)
                .isPrealarm(c.getInt(Columns.ALARM_PREALARM_INDEX) == 1)
                .state(c.getString(Columns.ALARM_STATE_INDEX));

        Calendar nextTime = calendars.now();
        nextTime.setTimeInMillis(c.getLong(Columns.ALARM_TIME_INDEX));
        builder.nextTime(nextTime);

        String nullableLabel = c.getString(Columns.ALARM_MESSAGE_INDEX);
        if (nullableLabel == null) {
            builder.label("");
        } else {
            builder.label(nullableLabel);
        }

        builder.alertString(c.getString(Columns.ALARM_ALERT_INDEX));

        builder.persistence(this);

        return builder.build();
    }

    @Override
    public AlarmContainer create() {
        Calendar now = calendars.now();
        ImmutableAlarmContainer defaultContainer = ImmutableAlarmContainer.builder()
                .id(-1)
                .isEnabled(false)
                .nextTime(now)
                .hour(now.get(Calendar.HOUR_OF_DAY))
                .minutes(now.get(Calendar.MINUTE))
                .isVibrate(true)
                .daysOfWeek(new DaysOfWeek(0))
                .alertString("")
                .isPrealarm(false)
                .label("")
                .state("")
                .persistence(AlarmContainer.PERSISTENCE_STUB)
                .build();

        //generate a new id
        Uri uri = mContext.getContentResolver().insert(Columns.CONTENT_URI, createContentValues(defaultContainer));
        //assign the id and return an onject with it. TODO here we have an annecassary DB flush
        return defaultContainer.withId((int) ContentUris.parseId(uri)).withPersistence(this);
    }

    /**
     * Persist data in the database
     */
    @Override
    public void persist(AlarmContainer container) {
        ContentValues values = createContentValues(container);
        Uri uriWithAppendedId = ContentUris.withAppendedId(PersistingContainerFactory.Columns.CONTENT_URI, container.getId());
        mContext.getContentResolver().update(uriWithAppendedId, values, null, null);
    }

    private ContentValues createContentValues(AlarmContainer container) {
        ContentValues values = new ContentValues(12);
        // id
        values.put(Columns.ENABLED, container.isEnabled());
        values.put(Columns.HOUR, container.getHour());
        values.put(Columns.MINUTES, container.getMinutes());
        values.put(Columns.DAYS_OF_WEEK, container.getDaysOfWeek().getCoded());
        values.put(Columns.VIBRATE, container.isVibrate());
        values.put(Columns.MESSAGE, container.getLabel());
        // A null alert Uri indicates a silent
        values.put(Columns.ALERT, container.alertString());
        values.put(Columns.PREALARM, container.isPrealarm());
        values.put(Columns.ALARM_TIME, container.getNextTime().getTimeInMillis());
        values.put(Columns.STATE, container.getState());

        return values;
    }

    @Override
    public void delete(AlarmContainer container) {
        Uri uri = ContentUris.withAppendedId(Columns.CONTENT_URI, container.getId());
        mContext.getContentResolver().delete(uri, "", null);
    }
}
