package com.better.alarm.presenter;

import android.content.ContentResolver;
import android.content.Intent;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.provider.Settings;

import com.better.alarm.AlarmApplication;
import com.better.alarm.R;
import com.better.alarm.view.AlarmPreference;
import com.google.inject.Inject;

import static com.better.alarm.Prefs.KEY_ALARM_IN_SILENT_MODE;
import static com.better.alarm.Prefs.KEY_ALARM_SNOOZE;
import static com.better.alarm.Prefs.KEY_AUTO_SILENCE;
import static com.better.alarm.Prefs.KEY_DEFAULT_RINGTONE;
import static com.better.alarm.Prefs.KEY_FADE_IN_TIME_SEC;
import static com.better.alarm.Prefs.KEY_PREALARM_DURATION;

/**
 * Created by Yuriy on 24.07.2017.
 */

public class SettingsFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {
    private static final int ALARM_STREAM_TYPE_BIT = 1 << AudioManager.STREAM_ALARM;

    @Inject
    private Vibrator vibrator;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AlarmApplication.guice().injectMembers(this);

        addPreferencesFromResource(R.xml.preferences);

        final AlarmPreference ringtone = (AlarmPreference) findPreference(KEY_DEFAULT_RINGTONE);
        Uri alert = RingtoneManager.getActualDefaultRingtoneUri(getActivity(), RingtoneManager.TYPE_ALARM);
        if (alert != null) {
            ringtone.setAlert(alert);
        }
        ringtone.setChangeDefault();

        PreferenceCategory category = (PreferenceCategory) findPreference("preference_category_sound_key");

        boolean hasVibrator = vibrator.hasVibrator();
        // #65 we have to check if preference is present before we try to remove
        // it TODO this is very strange!
        if (!hasVibrator && findPreference("vibrate") != null) {
            category.removePreference(findPreference("vibrate"));
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            category.removePreference(findPreference(KEY_ALARM_IN_SILENT_MODE));
        }

        final Preference theme = findPreference("theme");
        theme.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                new Handler().post(new Runnable() {
                    @Override
                    public void run() {
                        Intent i = getActivity().getPackageManager().getLaunchIntentForPackage(getActivity().getPackageName());
                        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(i);
                    }
                });
                return true;
            }
        });
    }


    @Override
    public void onResume() {
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
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            final CheckBoxPreference alarmInSilentModePref = (CheckBoxPreference) findPreference(KEY_ALARM_IN_SILENT_MODE);
            final int silentModeStreams = Settings.System.getInt(getContentResolver(),
                    Settings.System.MODE_RINGER_STREAMS_AFFECTED, 0);
            alarmInSilentModePref.setChecked((silentModeStreams & ALARM_STREAM_TYPE_BIT) == 0);
        }

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

    private ContentResolver getContentResolver() {
        return getActivity().getContentResolver();
    }
}
