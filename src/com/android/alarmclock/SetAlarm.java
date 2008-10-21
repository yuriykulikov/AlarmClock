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

import android.app.Activity;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.pim.DateFormat;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceScreen;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TimePicker;
import android.widget.Toast;

/**
 * Manages each alarm
 */
public class SetAlarm extends PreferenceActivity
        implements Alarms.AlarmSettings, TimePickerDialog.OnTimeSetListener {

    private CheckBoxPreference mAlarmOnPref;
    private Preference mTimePref;
    private AlarmPreference mAlarmPref;
    private CheckBoxPreference mVibratePref;
    private RepeatPreference mRepeatPref;
    private ContentObserver mAlarmsChangeObserver;
    private MenuItem mDeleteAlarmItem;

    private int mId;
    private int mHour;
    private int mMinutes;
    private Alarms.DaysOfWeek mDaysOfWeek = new Alarms.DaysOfWeek();

    private boolean mReportAlarmCalled;

    private static final int DIALOG_TIMEPICKER = 0;

    private class RingtoneChangedListener implements AlarmPreference.IRingtoneChangedListener {
        public void onRingtoneChanged(Uri ringtoneUri) {
            saveAlarm(false);
        }
    }

    private class OnRepeatChangeListener implements RepeatPreference.OnRepeatChangeListener {
        public void onRepeatChanged(Alarms.DaysOfWeek daysOfWeek) {
            if (!mDaysOfWeek.equals(daysOfWeek)) {
                mDaysOfWeek.set(daysOfWeek);
                saveAlarm(true);
            }
        }
    }

    private class AlarmsChangeObserver extends ContentObserver {
        public AlarmsChangeObserver() {
            super(new Handler());
        }
        @Override
        public void onChange(boolean selfChange) {
            Alarms.getAlarm(getContentResolver(), SetAlarm.this, mId);
        }
    }

    /**
     * Set an alarm.  Requires an Alarms.ID to be passed in as an
     * extra
     */
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.alarm_prefs);
        mAlarmOnPref = (CheckBoxPreference)findPreference("on");
        mTimePref = findPreference("time");
        mAlarmPref = (AlarmPreference) findPreference("alarm");
        mVibratePref = (CheckBoxPreference) findPreference("vibrate");
        mRepeatPref = (RepeatPreference) findPreference("setRepeat");

        Intent i = getIntent();
        mId = i.getIntExtra(Alarms.ID, -1);
        if (Log.LOGV) Log.v("In SetAlarm, alarm id = " + mId);

        mReportAlarmCalled = false;
        /* load alarm details from database */
        Alarms.getAlarm(getContentResolver(), this, mId);
        /* This should never happen, but does occasionally with the monkey.
         * I believe it's a race condition where a deleted alarm is opened
         * before the alarm list is refreshed. */
        if (!mReportAlarmCalled) {
            Log.e("reportAlarm never called!");
            finish();
        }

        mAlarmsChangeObserver = new AlarmsChangeObserver();
        getContentResolver().registerContentObserver(
                Alarms.AlarmColumns.CONTENT_URI, true, mAlarmsChangeObserver);

        mAlarmPref.setRingtoneChangedListener(new RingtoneChangedListener());
        mRepeatPref.setOnRepeatChangeListener(new OnRepeatChangeListener());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getContentResolver().unregisterContentObserver(mAlarmsChangeObserver);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog d;

        switch (id) {
        case DIALOG_TIMEPICKER:
            d = new TimePickerDialog(
                    SetAlarm.this,
                    this,
                    0,
                    0,
                    DateFormat.is24HourFormat(SetAlarm.this));
            d.setTitle(getResources().getString(R.string.time));
            break;
        default:
            d = null;
        }

        return d;
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        super.onPrepareDialog(id, dialog);

        switch (id) {
        case DIALOG_TIMEPICKER:
            TimePickerDialog timePicker = (TimePickerDialog)dialog;
            timePicker.updateTime(mHour, mMinutes);
            break;
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {

        if (preference == mTimePref) {
            showDialog(DIALOG_TIMEPICKER);
        } else if (preference == mAlarmOnPref) {
            saveAlarm(true);
        } else if (preference == mVibratePref) {
            saveAlarm(false);
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        mHour = hourOfDay;
        mMinutes = minute;
        mAlarmOnPref.setChecked(true);
        saveAlarm(true);
    }

    /**
     * Alarms.AlarmSettings implementation.  Database feeds current
     * settings in through this call
     */
    public void reportAlarm(
            int idx, boolean enabled, int hour, int minutes,
            Alarms.DaysOfWeek daysOfWeek, boolean vibrate,String message,
            String alert) {

        mHour = hour;
        mMinutes = minutes;
        mAlarmOnPref.setChecked(enabled);
        mDaysOfWeek.set(daysOfWeek);
        mRepeatPref.setDaysOfWeek(mDaysOfWeek);
        mVibratePref.setChecked(vibrate);

        if (alert == null || alert.length() == 0) {
            if (Log.LOGV) Log.v("****** reportAlarm null or 0-length alert");
            mAlarmPref.mAlert = getDefaultAlarm();
            if (mAlarmPref.mAlert == null) {
                Log.e("****** Default Alarm null");
            }
        } else {
            mAlarmPref.mAlert = Uri.parse(alert);
            if (mAlarmPref.mAlert == null) {
                Log.e("****** Parsed null alarm. URI: " + alert);
            }
        }
        if (Log.LOGV) Log.v("****** reportAlarm uri " + alert + " alert " +
                            mAlarmPref.mAlert);
        updateTime();
        updateRepeat();
        updateAlarm(mAlarmPref.mAlert);

        mReportAlarmCalled = true;
    }

    /**
     * picks the first alarm available
     */
    private Uri getDefaultAlarm() {
        RingtoneManager ringtoneManager = new RingtoneManager(this);
        ringtoneManager.setType(RingtoneManager.TYPE_ALARM);
        return ringtoneManager.getRingtoneUri(0);
    }

    private void updateTime() {
        if (Log.LOGV) Log.v("updateTime " + mId);
        mTimePref.setSummary(Alarms.formatTime(this, mHour, mMinutes, mDaysOfWeek));
    }

    private void updateAlarm(Uri ringtoneUri) {
        if (Log.LOGV) Log.v("updateAlarm " + mId);
        Ringtone ringtone = RingtoneManager.getRingtone(SetAlarm.this, ringtoneUri);
        if (ringtone != null) {
            mAlarmPref.setSummary(ringtone.getTitle(SetAlarm.this));
        }
    }

    private void updateRepeat() {
        if (Log.LOGV) Log.v("updateRepeat " + mId);
        mRepeatPref.setSummary(mDaysOfWeek.toString(this, true));
    }

    private void saveAlarm(boolean popToast) {
        if (mReportAlarmCalled && mAlarmPref.mAlert != null) {
            String alertString = mAlarmPref.mAlert.toString();
            saveAlarm(this, mId, mAlarmOnPref.isChecked(), mHour, mMinutes,
                      mDaysOfWeek, mVibratePref.isChecked(), alertString,
                      popToast);
        }
    }

    /**
     * Write alarm out to persistent store and pops toast if alarm
     * enabled
     */
    private static void saveAlarm(
            Context context, int id, boolean enabled, int hour, int minute,
            Alarms.DaysOfWeek daysOfWeek, boolean vibrate, String alert,
            boolean popToast) {
        if (Log.LOGV) Log.v("** saveAlarm " + id + " " + enabled + " " + hour +
                            " " + minute + " vibe " + vibrate);

        // Fix alert string first
        Alarms.setAlarm(context, id, enabled, hour, minute, daysOfWeek, vibrate,
                        "", alert);

        if (enabled && popToast) {
            popAlarmSetToast(context, hour, minute, daysOfWeek);
        }
    }

    /**
     * Display a toast that tells the user how long until the alarm
     * goes off.  This helps prevent "am/pm" mistakes.
     */
    static void popAlarmSetToast(Context context, int hour, int minute,
                                 Alarms.DaysOfWeek daysOfWeek) {

        String toastText = formatToast(context, hour, minute, daysOfWeek);
        Toast toast = Toast.makeText(context, toastText, Toast.LENGTH_LONG);
        ToastMaster.setToast(toast);
        toast.show();
    }

    /**
     * format "Alarm set for 2 days 7 hours and 53 minutes from
     * now"
     */
    static String formatToast(Context context, int hour, int minute,
                              Alarms.DaysOfWeek daysOfWeek) {
        long alarm = Alarms.calculateAlarm(hour, minute,
                                           daysOfWeek).getTimeInMillis();
        long delta = alarm - System.currentTimeMillis();;
        long hours = delta / (1000 * 60 * 60);
        long minutes = delta / (1000 * 60) % 60;
        long days = hours / 24;
        hours = hours % 24;

        String daySeq = (days == 0) ? "" :
                (days == 1) ? context.getString(R.string.day) :
                context.getString(R.string.days, Long.toString(days));

        String minSeq = (minutes == 0) ? "" :
                (minutes == 1) ? context.getString(R.string.minute) :
                context.getString(R.string.minutes, Long.toString(minutes));

        String hourSeq = (hours == 0) ? "" :
                (hours == 1) ? context.getString(R.string.hour) :
                context.getString(R.string.hours, Long.toString(hours));

        boolean dispDays = days > 0;
        boolean dispHour = hours > 0;
        boolean dispMinute = minutes > 0;

        String ret;
        if (!(dispDays || dispHour || dispMinute)) {
            ret = context.getString(R.string.subminute);
        } else {
            String parts[] = new String[5];
            parts[0] = daySeq;
            parts[1] = !dispDays ? "" :
                    dispHour && dispMinute ? context.getString(R.string.space) :
                    !dispHour && !dispMinute ? "" :
                    context.getString(R.string.and);
            parts[2] = dispHour ? hourSeq : "";
            parts[3] = dispHour && dispMinute ? context.getString(R.string.and) : "";
            parts[4] = dispMinute ? minSeq : "";
            ret = context.getString(R.string.combiner, (Object[])parts);
        }

        ret = context.getString(R.string.alarm_set, ret);
        /* if (Log.LOGV) Log.v("** TOAST daySeq " + daySeq + " hourSeq " + hourSeq +
           " minSeq " + minSeq + " ret " + ret); */
        return ret;
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        mDeleteAlarmItem = menu.add(0, 0, 0, R.string.delete_alarm);
        mDeleteAlarmItem.setIcon(android.R.drawable.ic_menu_delete);

        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item == mDeleteAlarmItem) {

            /* If alarm is snoozing, lose it */
            int id = Alarms.getSnoozeAlarmId(this);
            if (id == mId) Alarms.disableSnoozeAlert(this);

            Alarms.deleteAlarm(getContentResolver(), mId);
            Alarms.setNextAlert(this);
            finish();
            return true;
        }

        return false;
    }

}
