/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.better.alarm.presenter;

import java.util.Calendar;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.better.alarm.R;
import com.better.alarm.model.Alarm;
import com.better.alarm.model.AlarmsManager;
import com.better.alarm.model.IAlarmsManager;
import com.better.alarm.model.Intents;

/**
 * Manages each alarm
 */
public class SetAlarmActivity extends PreferenceActivity implements Preference.OnPreferenceChangeListener,
        OnCancelListener, AlarmTimePickerDialogFragment.AlarmTimePickerDialogHandler {
    public final static String M12 = "h:mm aa";
    public final static String M24 = "kk:mm";

    private IAlarmsManager alarms;

    private CheckBoxPreference mEnabledPref;
    private Preference mTimePref;
    private AlarmPreference mAlarmPref;
    private RepeatPreference mRepeatPref;
    private CheckBoxPreference mPreAlarmPref;

    private int mId;
    private boolean isNewAlarm;
    // these are to get data from TimerPicker
    private int mHour;
    private int mMinute;
    private TimePickerDialog mTimePickerDialog;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        if (!getResources().getBoolean(R.bool.isTablet)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        // Override the default content view.
        setContentView(R.layout.set_alarm);

        // TODO Stop using preferences for this view. Save on done, not after
        // each change.
        addPreferencesFromResource(R.xml.alarm_prefs);

        alarms = AlarmsManager.getAlarmsManager();

        // Get each preference so we can retrieve the value later.
        mEnabledPref = (CheckBoxPreference) findPreference("enabled");
        // remove enable preference from screen
        getPreferenceScreen().removePreference(mEnabledPref);
        mTimePref = findPreference("time");
        mAlarmPref = (AlarmPreference) findPreference("alarm");
        mAlarmPref.setOnPreferenceChangeListener(this);
        mRepeatPref = (RepeatPreference) findPreference("setRepeat");
        mRepeatPref.setOnPreferenceChangeListener(this);
        mPreAlarmPref = (CheckBoxPreference) findPreference("prealarm");
        mPreAlarmPref.setOnPreferenceChangeListener(this);

        if (getIntent().hasExtra(Intents.EXTRA_ID)) {
            mId = getIntent().getIntExtra(Intents.EXTRA_ID, -1);
        } else {
            // No alarm means create a new alarm.
            mId = alarms.createNewAlarm();
            isNewAlarm = true;
        }

        // Populate the prefs with the original alarm data. updatePrefs also
        // sets mId so it must be called before checking mId below.
        updatePrefs();

        // We have to do this to get the save/cancel buttons to highlight on
        // their own.
        getListView().setItemsCanFocus(true);

        // Attach actions to each button.
        Button b = (Button) findViewById(R.id.alarm_save);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveAlarm();
                finish();
            }
        });
        Button revert = (Button) findViewById(R.id.alarm_revert);
        revert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                revert();
                finish();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // we inflate anyway even though currently we have only one entry. There
        // may be more in future
        getMenuInflater().inflate(R.menu.set_alarm_menu, menu);
        if (isNewAlarm) {
            menu.removeItem(R.id.set_alarm_menu_delete_alarm);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.set_alarm_menu_delete_alarm:
            deleteAlarm();
            break;

        default:
            break;
        }
        return true;
    }

    /*
     * @Override protected void onSaveInstanceState(Bundle outState) {
     * super.onSaveInstanceState(outState);
     * outState.putParcelable(KEY_ORIGINAL_ALARM, mOriginalAlarm);
     * outState.putParcelable(KEY_CURRENT_ALARM, buildAlarmFromUi()); if
     * (mTimePickerDialog != null) { if (mTimePickerDialog.isShowing()) {
     * outState.putParcelable(KEY_TIME_PICKER_BUNDLE,
     * mTimePickerDialog.onSaveInstanceState()); mTimePickerDialog.dismiss(); }
     * mTimePickerDialog = null; } }
     * 
     * @Override protected void onRestoreInstanceState(Bundle state) {
     * super.onRestoreInstanceState(state);
     * 
     * Alarm alarmFromBundle = state.getParcelable(KEY_ORIGINAL_ALARM); if
     * (alarmFromBundle != null) { mOriginalAlarm = alarmFromBundle; }
     * 
     * alarmFromBundle = state.getParcelable(KEY_CURRENT_ALARM); if
     * (alarmFromBundle != null) { updatePrefs(alarmFromBundle); }
     * 
     * Bundle b = state.getParcelable(KEY_TIME_PICKER_BUNDLE); if (b != null) {
     * showTimePicker(); mTimePickerDialog.onRestoreInstanceState(b); } }
     */

    @Override
    public boolean onPreferenceChange(final Preference p, Object newValue) {
        // this method is called _before_
        // the value of the preference has changed.
        // Editing any preference (except enable) enables the alarm.
        if (p != mEnabledPref) {
            mEnabledPref.setChecked(true);
        }
        return true;
    }

    private void updatePrefs() {
        Alarm alarm = alarms.getAlarm(mId);
        mEnabledPref.setChecked(alarm.isEnabled());
        mHour = alarm.getHour();
        mMinute = alarm.getMinutes();
        mRepeatPref.setDaysOfWeek(alarm.getDaysOfWeek());
        // Give the alert uri to the preference.
        mAlarmPref.setAlert(alarm.getAlert());
        mPreAlarmPref.setChecked(alarm.isPrealarm());
        updateTime();
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
        final FragmentTransaction ft = getFragmentManager().beginTransaction();
        final Fragment prev = getFragmentManager().findFragmentByTag("time_dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        final AlarmTimePickerDialogFragment fragment = AlarmTimePickerDialogFragment.newInstance(mId, this);
        fragment.show(ft, "time_dialog");
    }

    @Override
    public void onDialogTimeSet(Alarm alarm, int hourOfDay, int minute) {
        // onTimeSet is called when the user clicks "Set"
        mHour = hourOfDay;
        mMinute = minute;
        updateTime();
        // If the time has been changed, enable the alarm.
        mEnabledPref.setChecked(true);
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        mTimePickerDialog = null;
    }

    private void updateTime() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, mHour);
        c.set(Calendar.MINUTE, mMinute);
        String format = android.text.format.DateFormat.is24HourFormat(this) ? M24 : M12;
        CharSequence summary = (c == null) ? "" : (String) DateFormat.format(format, c);
        mTimePref.setSummary(summary);
    }

    private long saveAlarm() {
        alarms.changeAlarm(mId, mEnabledPref.isChecked(), mHour, mMinute, mRepeatPref.getDaysOfWeek(), true, "",
                mAlarmPref.getAlert(), mPreAlarmPref.isChecked());
        isNewAlarm = false;
        return alarms.getAlarm(mId).getNextTime().getTimeInMillis();
    }

    private void deleteAlarm() {
        new AlertDialog.Builder(this).setTitle(getString(R.string.delete_alarm))
                .setMessage(getString(R.string.delete_alarm_confirm))
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        alarms.delete(mId);
                        finish();
                    }
                }).setNegativeButton(android.R.string.cancel, null).show();
    }

    private void revert() {
        // "Revert" on a newly created alarm should delete it.
        if (isNewAlarm) {
            alarms.delete(mId);
        } else {
            // do not save changes
        }
    }
}
