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

package com.android.deskclock;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.Button;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.TimePicker.OnTimeChangedListener;

/**
 * Manages each alarm
 */
public class SetAlarm extends PreferenceActivity implements Preference.OnPreferenceChangeListener,
        OnTimeChangedListener {
    private static String KEY_CURRENT_ALARM = "currentAlarm";
    private static String KEY_ORIGINAL_ALARM = "originalAlarm";

    private EditTextPreference mLabel;
    private CheckBoxPreference mEnabledPref;
    private AlarmPreference mAlarmPref;
    private CheckBoxPreference mVibratePref;
    private RepeatPreference mRepeatPref;
    private TimePicker mTimePicker;

    private int     mId;
    private Alarm   mOriginalAlarm;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // Override the default content view.
        setContentView(R.layout.set_alarm);

        addPreferencesFromResource(R.xml.alarm_prefs);

        // Get each preference so we can retrieve the value later.
        mLabel = (EditTextPreference) findPreference("label");
        mLabel.setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener() {
                    public boolean onPreferenceChange(Preference p,
                            Object newValue) {
                        String val = (String) newValue;
                        // Set the summary based on the new label.
                        p.setSummary(val);
                        if (val != null && !val.equals(mLabel.getText())) {
                            // Call through to the generic listener.
                            return SetAlarm.this.onPreferenceChange(p,
                                newValue);
                        }
                        return true;
                    }
                });
        mEnabledPref = (CheckBoxPreference) findPreference("enabled");
        mEnabledPref.setOnPreferenceChangeListener(this);
        mAlarmPref = (AlarmPreference) findPreference("alarm");
        mAlarmPref.setOnPreferenceChangeListener(this);
        mVibratePref = (CheckBoxPreference) findPreference("vibrate");
        mVibratePref.setOnPreferenceChangeListener(this);
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (!v.hasVibrator()) {
            getPreferenceScreen().removePreference(mVibratePref);
        }
        mRepeatPref = (RepeatPreference) findPreference("setRepeat");
        mRepeatPref.setOnPreferenceChangeListener(this);
        mTimePicker = (TimePicker) findViewById(R.id.timePicker);
        mTimePicker.setOnTimeChangedListener(this);

        Intent i = getIntent();
        Alarm alarm = i.getParcelableExtra(Alarms.ALARM_INTENT_EXTRA);

        if (alarm == null) {
            // No alarm means create a new alarm.
            alarm = new Alarm();
        }
        mOriginalAlarm = alarm;

        // Populate the prefs with the original alarm data.  updatePrefs also
        // sets mId so it must be called before checking mId below.
        updatePrefs(mOriginalAlarm);

        // We have to do this to get the save/cancel buttons to highlight on
        // their own.
        getListView().setItemsCanFocus(true);

        // Attach actions to each button.
        Button b = (Button) findViewById(R.id.alarm_save);
        b.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    long time = saveAlarm(null);
                    popAlarmSetToast(SetAlarm.this, time);
                    finish();
                }
        });
        Button revert = (Button) findViewById(R.id.alarm_revert);
        revert.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                revert();
                finish();
            }
        });
        b = (Button) findViewById(R.id.alarm_delete);
        if (mId == -1) {
            b.setEnabled(false);
            b.setVisibility(View.GONE);
        } else {
            b.setVisibility(View.VISIBLE);
            b.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    deleteAlarm();
                }
            });
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_ORIGINAL_ALARM, mOriginalAlarm);
        outState.putParcelable(KEY_CURRENT_ALARM, buildAlarmFromUi());
    }

    @Override
    protected void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);

        Alarm alarmFromBundle = state.getParcelable(KEY_ORIGINAL_ALARM);
        if (alarmFromBundle != null) {
            mOriginalAlarm = alarmFromBundle;
        }

        alarmFromBundle = state.getParcelable(KEY_CURRENT_ALARM);
        if (alarmFromBundle != null) {
            updatePrefs(alarmFromBundle);
        }
    }

    // Used to post runnables asynchronously.
    private static final Handler sHandler = new Handler();

    public boolean onPreferenceChange(final Preference p, Object newValue) {
        // Asynchronously save the alarm since this method is called _before_
        // the value of the preference has changed.
        sHandler.post(new Runnable() {
            public void run() {
                // Editing any preference (except enable) enables the alarm.
                if (p != mEnabledPref) {
                    mEnabledPref.setChecked(true);
                }
                saveAlarm(null);
            }
        });
        return true;
    }

    private void updatePrefs(Alarm alarm) {
        mId = alarm.id;
        mEnabledPref.setChecked(alarm.enabled);
        mLabel.setText(alarm.label);
        mLabel.setSummary(alarm.label);
        mTimePicker.setCurrentHour(alarm.hour);
        mTimePicker.setCurrentMinute(alarm.minutes);
        mTimePicker.setIs24HourView(DateFormat.is24HourFormat(this));
        mRepeatPref.setDaysOfWeek(alarm.daysOfWeek);
        mVibratePref.setChecked(alarm.vibrate);
        // Give the alert uri to the preference.
        mAlarmPref.setAlert(alarm.alert);
    }

    @Override
    public void onBackPressed() {
        revert();
        finish();
    }

    @Override
    public void onTimeChanged(TimePicker timePicker, int hourOfDay, int minute) {
        // If the time has been changed, enable the alarm
        mEnabledPref.setChecked(true);
    }

    private long saveAlarm(Alarm alarm) {
        if (alarm == null) {
            alarm = buildAlarmFromUi();
        }

        long time;
        if (alarm.id == -1) {
            time = Alarms.addAlarm(this, alarm);
            // addAlarm populates the alarm with the new id. Update mId so that
            // changes to other preferences update the new alarm.
            mId = alarm.id;
        } else {
            time = Alarms.setAlarm(this, alarm);
        }
        return time;
    }

    private Alarm buildAlarmFromUi() {
        Alarm alarm = new Alarm();
        alarm.id = mId;
        alarm.enabled = mEnabledPref.isChecked();
        alarm.hour = mTimePicker.getCurrentHour();
        alarm.minutes = mTimePicker.getCurrentMinute();
        alarm.daysOfWeek = mRepeatPref.getDaysOfWeek();
        alarm.vibrate = mVibratePref.isChecked();
        alarm.label = mLabel.getText();
        alarm.alert = mAlarmPref.getAlert();
        return alarm;
    }

    private void deleteAlarm() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.delete_alarm))
                .setMessage(getString(R.string.delete_alarm_confirm))
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface d, int w) {
                                Alarms.deleteAlarm(SetAlarm.this, mId);
                                finish();
                            }
                        })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void revert() {
        int newId = mId;
        // "Revert" on a newly created alarm should delete it.
        if (mOriginalAlarm.id == -1) {
            Alarms.deleteAlarm(SetAlarm.this, newId);
        } else {
            saveAlarm(mOriginalAlarm);
        }
    }

    /**
     * Display a toast that tells the user how long until the alarm
     * goes off.  This helps prevent "am/pm" mistakes.
     */
    static void popAlarmSetToast(Context context, int hour, int minute,
                                 Alarm.DaysOfWeek daysOfWeek) {
        popAlarmSetToast(context,
                Alarms.calculateAlarm(hour, minute, daysOfWeek)
                .getTimeInMillis());
    }

    static void popAlarmSetToast(Context context, long timeInMillis) {
        String toastText = formatToast(context, timeInMillis);
        Toast toast = Toast.makeText(context, toastText, Toast.LENGTH_LONG);
        ToastMaster.setToast(toast);
        toast.show();
    }

    /**
     * format "Alarm set for 2 days 7 hours and 53 minutes from
     * now"
     */
    static String formatToast(Context context, long timeInMillis) {
        long delta = timeInMillis - System.currentTimeMillis();
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

        int index = (dispDays ? 1 : 0) |
                    (dispHour ? 2 : 0) |
                    (dispMinute ? 4 : 0);

        String[] formats = context.getResources().getStringArray(R.array.alarm_set);
        return String.format(formats[index], daySeq, hourSeq, minSeq);
    }
}
