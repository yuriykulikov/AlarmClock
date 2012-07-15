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

package com.better.alarm.presenter;

import java.util.Calendar;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TimePicker;

import com.better.alarm.R;
import com.better.alarm.model.Alarm;
import com.better.alarm.model.AlarmsManager;
import com.better.alarm.model.IAlarmsManager;
import com.better.alarm.model.Intents;

/**
 * Manages each alarm
 */
public class SetAlarmActivity extends PreferenceActivity implements Preference.OnPreferenceChangeListener,
        TimePickerDialog.OnTimeSetListener, OnCancelListener {
    private static final String TAG = "SetAlarmActivity";
    private static final String KEY_CURRENT_ALARM = "currentAlarm";
    private static final String KEY_ORIGINAL_ALARM = "originalAlarm";
    private static final String KEY_TIME_PICKER_BUNDLE = "timePickerBundle";

    public final static String M12 = "h:mm aa";
    public final static String M24 = "kk:mm";

    private IAlarmsManager alarms;

    private CheckBoxPreference mEnabledPref;
    private Preference mTimePref;
    private AlarmPreference mAlarmPref;
    private RepeatPreference mRepeatPref;

    private int mId;
    private int mHour;
    private int mMinute;
    private TimePickerDialog mTimePickerDialog;
    private Alarm mOriginalAlarm;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // Override the default content view.
        setContentView(R.layout.set_alarm);

        // TODO Stop using preferences for this view. Save on done, not after
        // each change.
        addPreferencesFromResource(R.xml.alarm_prefs);

        alarms = AlarmsManager.getAlarmsManager();

        // Get each preference so we can retrieve the value later.
        mEnabledPref = (CheckBoxPreference) findPreference("enabled");
        mEnabledPref.setOnPreferenceChangeListener(this);
        // remove enable preference from screen
        getPreferenceScreen().removePreference(mEnabledPref);
        mTimePref = findPreference("time");
        mAlarmPref = (AlarmPreference) findPreference("alarm");
        mAlarmPref.setOnPreferenceChangeListener(this);
        mRepeatPref = (RepeatPreference) findPreference("setRepeat");
        mRepeatPref.setOnPreferenceChangeListener(this);

        Intent i = getIntent();
        Alarm alarm = i.getParcelableExtra(Intents.ALARM_INTENT_EXTRA);

        if (alarm == null) {
            // No alarm means create a new alarm.
            alarm = new Alarm();
        }
        mOriginalAlarm = alarm;

        // Populate the prefs with the original alarm data. updatePrefs also
        // sets mId so it must be called before checking mId below.
        updatePrefs(mOriginalAlarm);

        // We have to do this to get the save/cancel buttons to highlight on
        // their own.
        getListView().setItemsCanFocus(true);

        // Attach actions to each button.
        Button b = (Button) findViewById(R.id.alarm_save);
        b.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                saveAlarm(null);
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
        if (mTimePickerDialog != null) {
            if (mTimePickerDialog.isShowing()) {
                outState.putParcelable(KEY_TIME_PICKER_BUNDLE, mTimePickerDialog.onSaveInstanceState());
                mTimePickerDialog.dismiss();
            }
            mTimePickerDialog = null;
        }
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

        Bundle b = state.getParcelable(KEY_TIME_PICKER_BUNDLE);
        if (b != null) {
            showTimePicker();
            mTimePickerDialog.onRestoreInstanceState(b);
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
        mHour = alarm.hour;
        mMinute = alarm.minutes;
        mRepeatPref.setDaysOfWeek(alarm.daysOfWeek);
        // Give the alert uri to the preference.
        mAlarmPref.setAlert(alarm.alert);
        updateTime(alarm);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mTimePref) {
            showTimePicker();
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public void onBackPressed() {
        revert();
        finish();
    }

    private void showTimePicker() {
        if (mTimePickerDialog != null) {
            if (mTimePickerDialog.isShowing()) {
                Log.e(TAG, "mTimePickerDialog is already showing.");
                mTimePickerDialog.dismiss();
            } else {
                Log.e(TAG, "mTimePickerDialog is not null");
            }
            mTimePickerDialog.dismiss();
        }

        mTimePickerDialog = new TimePickerDialog(this, this, mHour, mMinute, DateFormat.is24HourFormat(this));
        mTimePickerDialog.setOnCancelListener(this);
        mTimePickerDialog.show();
    }

    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        // onTimeSet is called when the user clicks "Set"
        mTimePickerDialog = null;
        mHour = hourOfDay;
        mMinute = minute;
        updateTime(buildAlarmFromUi());
        // If the time has been changed, enable the alarm.
        mEnabledPref.setChecked(true);
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        mTimePickerDialog = null;
    }

    private void updateTime(Alarm alarm) {
        Calendar c = alarm.calculateCalendar();
        String format = android.text.format.DateFormat.is24HourFormat(this) ? M24 : M12;
        CharSequence summary = (c == null) ? "" : (String) DateFormat.format(format, c);
        mTimePref.setSummary(summary);
    }

    private long saveAlarm(Alarm alarm) {
        if (alarm == null) {
            alarm = buildAlarmFromUi();
        }

        long time;
        if (alarm.id == -1) {
            time = alarms.add(alarm);
            // addAlarm populates the alarm with the new id. Update mId so that
            // changes to other preferences update the new alarm.
            mId = alarm.id;
        } else {
            time = alarms.set(alarm);
        }
        return time;
    }

    private Alarm buildAlarmFromUi() {
        Alarm alarm = new Alarm();
        alarm.id = mId;
        alarm.enabled = mEnabledPref.isChecked();
        alarm.hour = mHour;
        alarm.minutes = mMinute;
        alarm.daysOfWeek = mRepeatPref.getDaysOfWeek();
        // TODO fetch from settings
        alarm.vibrate = true;
        alarm.label = "";
        alarm.alert = mAlarmPref.getAlert();
        return alarm;
    }

    private void deleteAlarm() {
        new AlertDialog.Builder(this).setTitle(getString(R.string.delete_alarm))
                .setMessage(getString(R.string.delete_alarm_confirm))
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        alarms.delete(mId);
                        finish();
                    }
                }).setNegativeButton(android.R.string.cancel, null).show();
    }

    private void revert() {
        int newId = mId;
        // "Revert" on a newly created alarm should delete it.
        if (mOriginalAlarm.id == -1) {
            alarms.delete(newId);
        } else {
            saveAlarm(mOriginalAlarm);
        }
    }
}
