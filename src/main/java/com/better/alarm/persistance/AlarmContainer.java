package com.better.alarm.persistance;

import java.util.Calendar;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.provider.BaseColumns;

import com.better.alarm.AlarmApplication;
import com.better.alarm.BuildConfig;
import com.better.alarm.model.DaysOfWeek;
import com.better.alarm.model.IAlarmContainer;
import com.better.alarm.interfaces.Intents;
import com.better.alarm.logger.Logger;

/**
 * Active record container for all alarm data.
 * 
 * @author Yuriy
 * 
 */
public class AlarmContainer implements IAlarmContainer {
    // ////////////////////////////
    // Column definitions
    // ////////////////////////////
    public static class Columns implements BaseColumns {
        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = Uri.parse("content://" + BuildConfig.APPLICATION_ID + ".model/alarm");

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
         * State machine state
         * <P>
         * Type: STRING
         * </P>
         */
        public static final String STATE = "state";

        /**
         * The default sort order for this table
         */
        public static final String DEFAULT_SORT_ORDER = HOUR + ", " + MINUTES + " ASC";

        // Used when filtering enabled alarms.
        public static final String WHERE_ENABLED = ENABLED + "=1";

        public static final String[] ALARM_QUERY_COLUMNS = { _ID, HOUR, MINUTES, DAYS_OF_WEEK, ALARM_TIME, ENABLED,
                VIBRATE, MESSAGE, ALERT, PREALARM, STATE };

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

    // This string is used to indicate a silent alarm in the db.
    private static final String ALARM_ALERT_SILENT = "silent";

    private int id;
    private boolean enabled;
    private int hour;
    private int minutes;
    private DaysOfWeek daysOfWeek;
    private boolean vibrate;
    private String label;
    private String alertString;
    private boolean silent;
    private boolean prealarm;
    /**
     * Time when AlarmCore would normally go next time. Used to disable expired
     * alarms if devicee was off-line when they were supposed to fire
     * 
     */
    private Calendar nextTime;
    private String state;

    private final Logger log;
    private final Context mContext;

    public AlarmContainer(Cursor c, Logger logger, Context context) {
        log = logger;
        mContext = context;
        id = c.getInt(Columns.ALARM_ID_INDEX);
        enabled = c.getInt(Columns.ALARM_ENABLED_INDEX) == 1;
        hour = c.getInt(Columns.ALARM_HOUR_INDEX);
        minutes = c.getInt(Columns.ALARM_MINUTES_INDEX);
        daysOfWeek = new DaysOfWeek(c.getInt(Columns.ALARM_DAYS_OF_WEEK_INDEX));
        vibrate = c.getInt(Columns.ALARM_VIBRATE_INDEX) == 1;
        label = c.getString(Columns.ALARM_MESSAGE_INDEX);
        if (label == null) label = "";
        alertString = c.getString(Columns.ALARM_ALERT_INDEX);
        silent = ALARM_ALERT_SILENT.equals(alertString);
        prealarm = c.getInt(Columns.ALARM_PREALARM_INDEX) == 1;
        nextTime = Calendar.getInstance();
        nextTime.setTimeInMillis(c.getLong(Columns.ALARM_TIME_INDEX));
        state = c.getString(Columns.ALARM_STATE_INDEX);
    }

    public AlarmContainer(Logger logger, Context context) {
        log = logger;
        mContext = context;

        id = -1;
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(System.currentTimeMillis());
        hour = c.get(Calendar.HOUR_OF_DAY);
        minutes = c.get(Calendar.MINUTE);
        vibrate = true;
        daysOfWeek = new DaysOfWeek(0);
        nextTime = c;
        alertString = null;
        prealarm = false;
        label = "";

        Uri uri = mContext.getContentResolver().insert(Columns.CONTENT_URI, createContentValues());
        id = (int) ContentUris.parseId(uri);
        state = "";
    }

    /**
     * Persist data in the database
     */
    @Override
    public void writeToDb() {
        ContentValues values = createContentValues();
        Intent intent = new Intent(mContext, DataBaseService.class);
        intent.setAction(DataBaseService.SAVE_ALARM_ACTION);
        intent.putExtra("extra_values", values);
        intent.putExtra(Intents.EXTRA_ID, id);
        AlarmApplication.wakeLocks().acquirePartialWakeLock(intent, "forDBService");
        mContext.startService(intent);
    }

    private ContentValues createContentValues() {
        ContentValues values = new ContentValues(12);
        // id
        values.put(Columns.ENABLED, enabled);
        values.put(Columns.HOUR, hour);
        values.put(Columns.MINUTES, minutes);
        values.put(Columns.DAYS_OF_WEEK, daysOfWeek.getCoded());
        values.put(Columns.VIBRATE, vibrate);
        values.put(Columns.MESSAGE, label);
        // A null alert Uri indicates a silent
        values.put(Columns.ALERT, alertString);
        values.put(Columns.PREALARM, prealarm);
        values.put(Columns.ALARM_TIME, nextTime.getTimeInMillis());
        values.put(Columns.STATE, state);

        return values;
    }

    @Override
    public void delete() {
        Uri uri = ContentUris.withAppendedId(Columns.CONTENT_URI, id);
        mContext.getContentResolver().delete(uri, "", null);
        log.d(id + " is deleted");
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        writeToDb();
    }

    @Override
    public int getHour() {
        return hour;
    }

    @Override
    public void setHour(int hour) {
        this.hour = hour;
    }

    @Override
    public int getMinutes() {
        return minutes;
    }

    @Override
    public void setMinutes(int minutes) {
        this.minutes = minutes;
    }

    @Override
    public DaysOfWeek getDaysOfWeek() {
        return daysOfWeek;
    }

    @Override
    public void setDaysOfWeek(DaysOfWeek daysOfWeek) {
        this.daysOfWeek = daysOfWeek;
    }

    @Override
    public boolean isVibrate() {
        return vibrate;
    }

    @Override
    public void setVibrate(boolean vibrate) {
        this.vibrate = vibrate;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public Uri getAlert() {
        if (alertString != null && alertString.length() != 0) {
            try {
                return Uri.parse(alertString);
            } catch (Exception e){
                // If the database alert is null or it failed to parse, use the
                return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            }
        } else {
        // default alert.
            return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        }
    }

    @Override
    public void setAlert(Uri alert) {
        this.alertString = alert == null ? null : alert.toString();
    }

    @Override
    public boolean isSilent() {
        return silent;
    }

    @Override
    public void setSilent(boolean silent) {
        this.silent = silent;
    }

    @Override
    public boolean isPrealarm() {
        return prealarm;
    }

    @Override
    public void setPrealarm(boolean prealarm) {
        this.prealarm = prealarm;
    }

    @Override
    public Calendar getNextTime() {
        return nextTime;
    }

    @Override
    public void setNextTime(Calendar nextTime) {
        this.nextTime = nextTime;
        writeToDb();
    }

    @Override
    public String getState() {
        return state;
    }

    @Override
    public void setState(String state) {
        this.state = state;
        writeToDb();
    }

    @Override
    public String toString() {
        return "AlarmContainer [id=" + id + ", enabled=" + enabled + ", hour=" + hour + ", minutes=" + minutes
                + ", daysOfWeek=" + daysOfWeek + ", vibrate=" + vibrate + ", label=" + label + ", alert=" + alertString
                + ", silent=" + silent + ", prealarm=" + prealarm + ", nextTime=" + nextTime + ", state=" + state + "]";
    }
}
