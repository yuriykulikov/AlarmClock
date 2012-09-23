/*
 * Copyright (C) 2009 The Android Open Source Project
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
import java.util.HashMap;
import java.util.Map;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.preference.PreferenceManager;

import com.better.alarm.R;
import com.better.wakelock.Logger;
import com.better.wakelock.WakeLockManager;

public final class AlarmCore implements Alarm {

    // This string is used to indicate a silent alarm in the db.
    private static final String ALARM_ALERT_SILENT = "silent";

    private final IAlarmsScheduler mAlarmsScheduler;
    private final Logger log;
    private final Context mContext;

    private int id;
    private boolean enabled;
    private int hour;
    private int minutes;
    /**
     * Time when AlarmCore would normally go next time. Used to disable expired
     * alarms if devicee was off-line when they were supposed to fire
     * 
     */
    private Calendar nextTime;
    private DaysOfWeek daysOfWeek;
    private boolean vibrate;
    private String label;
    private Uri alert;
    private boolean silent;
    private boolean prealarm;
    private final Calendar prealarmTime;
    private boolean snoozed;
    private Calendar snoozedTime;

    AlarmCore(Cursor c, Context context, Logger logger, IAlarmsScheduler alarmsScheduler) {
        mContext = context;
        this.log = logger;
        mAlarmsScheduler = alarmsScheduler;
        id = c.getInt(Columns.ALARM_ID_INDEX);
        enabled = c.getInt(Columns.ALARM_ENABLED_INDEX) == 1;
        hour = c.getInt(Columns.ALARM_HOUR_INDEX);
        minutes = c.getInt(Columns.ALARM_MINUTES_INDEX);
        nextTime = Calendar.getInstance();
        nextTime.setTimeInMillis(c.getLong(Columns.ALARM_TIME_INDEX));
        daysOfWeek = new DaysOfWeek(c.getInt(Columns.ALARM_DAYS_OF_WEEK_INDEX));
        vibrate = c.getInt(Columns.ALARM_VIBRATE_INDEX) == 1;
        label = c.getString(Columns.ALARM_MESSAGE_INDEX);
        String alertString = c.getString(Columns.ALARM_ALERT_INDEX);
        prealarm = c.getInt(Columns.ALARM_PREALARM_INDEX) == 1;
        prealarmTime = Calendar.getInstance();
        prealarmTime.setTimeInMillis(c.getLong(Columns.ALARM_PREALARM_TIME_INDEX));
        snoozed = c.getInt(Columns.ALARM_SNOOZED_INDEX) == 1;
        snoozedTime = Calendar.getInstance();
        snoozedTime.setTimeInMillis(c.getLong(Columns.ALARM_SNOOZE_TIME_INDEX));
        if (ALARM_ALERT_SILENT.equals(alertString)) {
            log.d("AlarmCore is marked as silent");
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

        Calendar now = Calendar.getInstance();

        boolean isExpired = getNextTime().before(now);
        if (isExpired) {
            log.d("AlarmCore expired: " + toString());
            enabled = (isEnabled() && getDaysOfWeek().isRepeatSet());
        }

        calculateCalendars();

        writeToDb();

        mAlarmsScheduler.setAlarm(id, getActiveCalendars());

    }

    // Creates a default alarm at the current time.
    AlarmCore(Context context, Logger logger, IAlarmsScheduler alarmsScheduler) {
        mContext = context;
        log = logger;
        mAlarmsScheduler = alarmsScheduler;
        id = -1;
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(System.currentTimeMillis());
        hour = c.get(Calendar.HOUR_OF_DAY);
        minutes = c.get(Calendar.MINUTE);
        vibrate = true;
        daysOfWeek = new DaysOfWeek(0);
        nextTime = c;
        alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        prealarm = false;
        prealarmTime = c;
        snoozed = false;
        snoozedTime = c;

        Uri uri = mContext.getContentResolver().insert(Columns.CONTENT_URI, createContentValues());
        id = (int) ContentUris.parseId(uri);
    }

    void onAlarmFired(CalendarType calendarType) {
        broadcastAlarmState(id, Intents.ALARM_ALERT_ACTION);

        snoozed = false;

        calculateCalendars();
        mAlarmsScheduler.setAlarm(id, getActiveCalendars());
        writeToDb();
        // TODO notifyAlarmListChangedListeners();
    }

    // ++++++++++++++++++++++++++++++++++++++++++++++++++++++
    // ++++++ for GUI +++++++++++++++++++++++++++++++++
    // ++++++++++++++++++++++++++++++++++++++++++++++++++++++

    /**
     * A convenience method to enable or disable an
     * 
     * @param id
     *            corresponds to the _id column
     * @param enabled
     *            corresponds to the ENABLED column
     */
    @Override
    public void enable(boolean enable) {
        enabled = enable;
        calculateCalendars();
        mAlarmsScheduler.setAlarm(id, getActiveCalendars());
        writeToDb();
        // TODO notifyAlarmListChangedListeners();
    }

    @Override
    public void snooze() {
        int snoozeMinutes = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(mContext).getString(
                "snooze_duration", "10"));
        snoozed = true;
        snoozedTime = Calendar.getInstance();
        snoozedTime.add(Calendar.MINUTE, snoozeMinutes);
        calculateCalendars();
        mAlarmsScheduler.setAlarm(id, getActiveCalendars());
        broadcastAlarmState(id, Intents.ALARM_SNOOZE_ACTION);
        writeToDb();
        // TODO notifyAlarmListChangedListeners();
    }

    @Override
    public void dismiss() {
        broadcastAlarmState(id, Intents.ALARM_DISMISS_ACTION);
        snoozed = false;
        calculateCalendars();
        mAlarmsScheduler.setAlarm(id, getActiveCalendars());
        writeToDb();
        // TODO notifyAlarmListChangedListeners();
    }

    @Override
    public void delete() {
        Uri uri = ContentUris.withAppendedId(Columns.CONTENT_URI, id);
        mContext.getContentResolver().delete(uri, "", null);
        mAlarmsScheduler.removeAlarm(id);
        broadcastAlarmState(id, Intents.ALARM_DISMISS_ACTION);
        // TODO notifyAlarmListChangedListeners();
    }

    @Override
    public void change(boolean enabled, int hour, int minute, DaysOfWeek daysOfWeek, boolean vibrate, String label,
            Uri alert, boolean preAlarm) {
        this.prealarm = preAlarm;
        this.alert = alert;
        this.label = label;
        this.vibrate = vibrate;
        this.daysOfWeek = daysOfWeek;
        this.hour = hour;
        this.minutes = minute;
        this.enabled = enabled;

        calculateCalendars();

        writeToDb();
        mAlarmsScheduler.setAlarm(id, getActiveCalendars());
        // TODO notifyAlarmListChangedListeners();
    }

    /**
     * Given an alarm in hours and minutes, return a time suitable for setting
     * in AlarmManager.
     */
    void calculateCalendars() {
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
        if (addDays > 0) {
            c.add(Calendar.DAY_OF_WEEK, addDays);
        }

        nextTime = c;
    }

    private Map<CalendarType, Calendar> getActiveCalendars() {
        HashMap<CalendarType, Calendar> calendars = new HashMap<CalendarType, Calendar>();

        Calendar now = Calendar.getInstance();
        if (enabled && nextTime.after(now)) {
            calendars.put(CalendarType.NORMAL, nextTime);
        }
        if (snoozed && snoozedTime.after(now)) {
            calendars.put(CalendarType.SNOOZE, snoozedTime);
        }

        return calendars;
    }

    private ContentValues createContentValues() {
        ContentValues values = new ContentValues(12);

        values.put(Columns.ENABLED, enabled ? 1 : 0);
        values.put(Columns.HOUR, hour);
        values.put(Columns.MINUTES, minutes);
        values.put(Columns.ALARM_TIME, nextTime.getTimeInMillis());
        values.put(Columns.DAYS_OF_WEEK, daysOfWeek.getCoded());
        values.put(Columns.VIBRATE, vibrate);
        values.put(Columns.MESSAGE, label);
        values.put(Columns.PREALARM, prealarm);
        values.put(Columns.PREALARM_TIME, prealarmTime.getTimeInMillis());
        values.put(Columns.SNOOZED, snoozed);
        values.put(Columns.SNOOZE_TIME, snoozedTime.getTimeInMillis());

        // A null alert Uri indicates a silent
        values.put(Columns.ALERT, alert == null ? ALARM_ALERT_SILENT : alert.toString());

        return values;
    }

    private void writeToDb() {
        ContentValues values = createContentValues();
        Intent intent = new Intent(DataBaseService.SAVE_ALARM_ACTION);
        intent.putExtra("extra_values", values);
        intent.putExtra(Intents.EXTRA_ID, id);
        WakeLockManager.getWakeLockManager().acquirePartialWakeLock(intent, "forDBService");
        mContext.startService(intent);
    }

    private void broadcastAlarmState(int id, String action) {
        Intent intent = new Intent(action);
        intent.putExtra(Intents.EXTRA_ID, id);
        mContext.sendBroadcast(intent);
    }

    // ++++++++++++++++++++++++++++++++++++++++++++++++++++++
    // ++++++ getters for GUI +++++++++++++++++++++++++++++++
    // ++++++++++++++++++++++++++++++++++++++++++++++++++++++

    /**
     * TODO calendar should be immutable
     * 
     * @return
     */
    @Override
    public Calendar getNextTime() {
        return nextTime;
    }

    @Override
    public Calendar getSnoozedTime() {
        return snoozedTime;
    }

    @Override
    public Calendar getPrealarmTime() {
        return prealarmTime;
    }

    @Override
    public boolean isPrealarm() {
        return prealarm;
    }

    @Override
    public boolean isSilent() {
        return silent;
    }

    @Override
    public Uri getAlert() {
        return alert;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public boolean isVibrate() {
        return vibrate;
    }

    @Override
    public DaysOfWeek getDaysOfWeek() {
        return daysOfWeek;
    }

    @Override
    public int getMinutes() {
        return minutes;
    }

    @Override
    public int getHour() {
        return hour;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public boolean isSnoozed() {
        return snoozed;
    }

    @Override
    public String getLabelOrDefault(Context context) {
        if (label == null || label.length() == 0) return context.getString(R.string.default_label);
        return label;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AlarmCore)) return false;
        final AlarmCore other = (AlarmCore) o;
        return id == other.id;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("AlarmCore ").append(id);
        sb.append(", ");
        if (enabled) {
            sb.append("enabled at ").append(nextTime.getTime().toLocaleString());
        } else {
            sb.append("disabled");
        }
        sb.append(", ");
        if (snoozed) {
            sb.append("snoozed at ").append(snoozedTime.getTime().toLocaleString());
        } else {
            sb.append("no snooze");
        }
        sb.append(", ");
        if (prealarm) {
            sb.append("prealarm at ").append(prealarmTime.getTime().toLocaleString());
        } else {
            sb.append("no prealarm");
        }
        return sb.toString();
    }
}
