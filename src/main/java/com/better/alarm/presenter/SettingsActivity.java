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

package com.better.alarm.presenter;

import android.content.Intent;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;

import com.better.alarm.AlarmApplication;
import com.better.alarm.R;
import com.better.alarm.view.AlarmPreference;
import com.google.inject.Inject;

/**
 * Settings for the Alarm Clock.
 */
public class SettingsActivity extends PreferenceActivity implements Preference.OnPreferenceChangeListener {
    private static final int ALARM_STREAM_TYPE_BIT = 1 << AudioManager.STREAM_ALARM;
    private static final String KEY_ALARM_IN_SILENT_MODE = "alarm_in_silent_mode";
    public static final String KEY_ALARM_SNOOZE = "snooze_duration";
    public static final String KEY_VOLUME_BEHAVIOR = "volume_button_setting";
    private static final String KEY_DEFAULT_RINGTONE = "default_ringtone";
    private static final String KEY_AUTO_SILENCE = "auto_silence";
    private static final String KEY_PREALARM_DURATION = "prealarm_duration";
    public static final String KEY_FADE_IN_TIME_SEC = "fade_in_time_sec";

    @Inject
    private Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(DynamicThemeHandler.getInstance().getIdForName(SettingsActivity.class.getName()));
        super.onCreate(savedInstanceState);
        AlarmApplication.guice().injectMembers(this);

        addPreferencesFromResource(R.xml.preferences);

        final AlarmPreference ringtone = (AlarmPreference) findPreference(KEY_DEFAULT_RINGTONE);
        Uri alert = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM);
        if (alert != null) {
            ringtone.setAlert(alert);
        }
        ringtone.setChangeDefault();

        boolean hasVibrator = vibrator.hasVibrator();
        // #65 we have to check if preference is present before we try to remove
        // it TODO this is very strange!
        if (!hasVibrator && findPreference("vibrate") != null) {
            getPreferenceScreen().removePreference(findPreference("vibrate"));
        }

        final Preference theme = findPreference("theme");
        theme.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                new Handler().post(new Runnable() {
                    @Override
                    public void run() {
                        Intent i = getBaseContext().getPackageManager().getLaunchIntentForPackage(
                                getBaseContext().getPackageName());
                        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(i);
                    }
                });
                return true;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getActionBar().setDisplayHomeAsUpEnabled(true);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            goBack();
            return true;
        } else return false;
    }

    private void goBack() {
        // This is called when the Home (Up) button is pressed
        // in the Action Bar.
        Intent parentActivityIntent = new Intent(this, AlarmsListActivity.class);
        parentActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(parentActivityIntent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (KEY_ALARM_IN_SILENT_MODE.equals(preference.getKey())) {
            CheckBoxPreference pref = (CheckBoxPreference) preference;
            int ringerModeStreamTypes = Settings.System.getInt(getContentResolver(),
                    Settings.System.MODE_RINGER_STREAMS_AFFECTED, 0);

            if (pref.isChecked()) {
                ringerModeStreamTypes &= ~ALARM_STREAM_TYPE_BIT;
            } else {
                ringerModeStreamTypes |= ALARM_STREAM_TYPE_BIT;
            }

            Settings.System.putInt(getContentResolver(), Settings.System.MODE_RINGER_STREAMS_AFFECTED,
                    ringerModeStreamTypes);

            return true;
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object newValue) {
        if (KEY_ALARM_SNOOZE.equals(pref.getKey())) {
            final ListPreference listPref = (ListPreference) pref;
            final int idx = listPref.findIndexOfValue((String) newValue);
            listPref.setSummary(listPref.getEntries()[idx]);
        } else if (KEY_AUTO_SILENCE.equals(pref.getKey())) {
            final ListPreference listPref = (ListPreference) pref;
            String delay = (String) newValue;
            updateAutoSnoozeSummary(listPref, delay);
        } else if (KEY_PREALARM_DURATION.equals(pref.getKey())) {
            updatePreAlarmDurationSummary((ListPreference) pref, (String) newValue);
        } else if (KEY_FADE_IN_TIME_SEC.equals(pref.getKey())) {
            updateFadeInTimeSummary((ListPreference) pref, (String) newValue);
        }
        return true;
    }

    private void updateFadeInTimeSummary(ListPreference listPref, String duration) {
        int i = Integer.parseInt(duration);
        listPref.setSummary(getString(R.string.fade_in_summary, i));
    }

    private void updateAutoSnoozeSummary(ListPreference listPref, String delay) {
        int i = Integer.parseInt(delay);
        if (i == -1) {
            listPref.setSummary(R.string.auto_silence_never);
        } else {
            listPref.setSummary(getString(R.string.auto_silence_summary, i));
        }
    }

    private void updatePreAlarmDurationSummary(ListPreference listPref, String duration) {
        int i = Integer.parseInt(duration);
        if (i == -1) {
            listPref.setSummary(getString(R.string.prealarm_off_summary));
        } else {
            listPref.setSummary(getString(R.string.prealarm_summary, i));
        }
    }

    private void refresh() {
        final CheckBoxPreference alarmInSilentModePref = (CheckBoxPreference) findPreference(KEY_ALARM_IN_SILENT_MODE);
        final int silentModeStreams = Settings.System.getInt(getContentResolver(),
                Settings.System.MODE_RINGER_STREAMS_AFFECTED, 0);
        alarmInSilentModePref.setChecked((silentModeStreams & ALARM_STREAM_TYPE_BIT) == 0);

        ListPreference listPref = (ListPreference) findPreference(KEY_ALARM_SNOOZE);
        listPref.setSummary(listPref.getEntry());
        listPref.setOnPreferenceChangeListener(this);

        listPref = (ListPreference) findPreference(KEY_AUTO_SILENCE);
        String delay = listPref.getValue();
        updateAutoSnoozeSummary(listPref, delay);
        listPref.setOnPreferenceChangeListener(this);

        listPref = (ListPreference) findPreference(KEY_PREALARM_DURATION);
        updatePreAlarmDurationSummary(listPref, listPref.getValue());
        listPref.setOnPreferenceChangeListener(this);

        listPref = (ListPreference) findPreference(KEY_FADE_IN_TIME_SEC);
        updateFadeInTimeSummary(listPref, listPref.getValue());
        listPref.setOnPreferenceChangeListener(this);

        ListPreference theme = (ListPreference) findPreference("theme");
        theme.setSummary(theme.getEntry());
    }
}
