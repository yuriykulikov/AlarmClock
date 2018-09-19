package com.better.alarm.presenter;

import android.content.ContentResolver;
import android.content.Intent;
import android.media.AudioManager;
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
import android.preference.RingtonePreference;
import android.provider.Settings;

import com.better.alarm.R;
import com.f2prateek.rx.preferences2.RxSharedPreferences;

import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

import static com.better.alarm.configuration.AlarmApplication.container;
import static com.better.alarm.configuration.Prefs.KEY_ALARM_IN_SILENT_MODE;
import static com.better.alarm.configuration.Prefs.KEY_ALARM_SNOOZE;
import static com.better.alarm.configuration.Prefs.KEY_AUTO_SILENCE;
import static com.better.alarm.configuration.Prefs.KEY_DEFAULT_RINGTONE;
import static com.better.alarm.configuration.Prefs.KEY_FADE_IN_TIME_SEC;
import static com.better.alarm.configuration.Prefs.KEY_PREALARM_DURATION;

/**
 * Created by Yuriy on 24.07.2017.
 */

public class SettingsFragment extends PreferenceFragment {
    private static final int ALARM_STREAM_TYPE_BIT = 1 << AudioManager.STREAM_ALARM;

    private final Vibrator vibrator = container().vibrator();
    private final RxSharedPreferences rxSharedPreferences = container().rxPrefs();

    private final CompositeDisposable dispoables = new CompositeDisposable();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);

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
    public void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            final CheckBoxPreference alarmInSilentModePref = (CheckBoxPreference) findPreference(KEY_ALARM_IN_SILENT_MODE);
            final int silentModeStreams = Settings.System.getInt(getContentResolver(),
                    Settings.System.MODE_RINGER_STREAMS_AFFECTED, 0);
            alarmInSilentModePref.setChecked((silentModeStreams & ALARM_STREAM_TYPE_BIT) == 0);
        }

        {
            final ListPreference snoozePref = (ListPreference) findPreference(KEY_ALARM_SNOOZE);
            Disposable disposable = rxSharedPreferences.getString(KEY_ALARM_SNOOZE)
                    .asObservable()
                    .subscribe(new Consumer<String>() {
                        @Override
                        public void accept(@NonNull String newValue) throws Exception {
                            final int idx = snoozePref.findIndexOfValue(newValue);
                            snoozePref.setSummary(snoozePref.getEntries()[idx]);
                        }
                    });
            dispoables.add(disposable);
        }
        {
            final ListPreference autoSilence = (ListPreference) findPreference(KEY_AUTO_SILENCE);
            Disposable disposable = rxSharedPreferences.getString(KEY_AUTO_SILENCE)
                    .asObservable()
                    .subscribe(new Consumer<String>() {
                        @Override
                        public void accept(@NonNull String newValue) throws Exception {
                            int i = Integer.parseInt(newValue);
                            if (i == -1) {
                                autoSilence.setSummary(R.string.auto_silence_never);
                            } else {
                                autoSilence.setSummary(getString(R.string.auto_silence_summary, i));
                            }
                        }
                    });
            dispoables.add(disposable);
        }
        {
            final ListPreference preAlarmDuration = (ListPreference) findPreference(KEY_PREALARM_DURATION);
            Disposable disposable = rxSharedPreferences.getString(KEY_PREALARM_DURATION)
                    .asObservable()
                    .subscribe(new Consumer<String>() {
                        @Override
                        public void accept(@NonNull String newValue) throws Exception {
                            int i = Integer.parseInt(newValue);
                            if (i == -1) {
                                preAlarmDuration.setSummary(getString(R.string.prealarm_off_summary));
                            } else {
                                preAlarmDuration.setSummary(getString(R.string.prealarm_summary, i));
                            }
                        }
                    });
            dispoables.add(disposable);
        }
        {
            final ListPreference fadeInDuration = (ListPreference) findPreference(KEY_FADE_IN_TIME_SEC);
            Disposable disposable = rxSharedPreferences.getString(KEY_FADE_IN_TIME_SEC)
                    .asObservable()
                    .subscribe(new Consumer<String>() {
                        @Override
                        public void accept(@NonNull String newValue) throws Exception {
                            int i = Integer.parseInt(newValue);
                            if (i == 1) {
                                fadeInDuration.setSummary(getString(R.string.fade_in_off_summary));
                            } else {
                                fadeInDuration.setSummary(getString(R.string.fade_in_summary, i));
                            }
                        }
                    });
            dispoables.add(disposable);
        }
        {
            Disposable disposable = AlarmDetailsFragmentKt.bindPreferenceSummary((RingtonePreference) findPreference(KEY_DEFAULT_RINGTONE), rxSharedPreferences, getActivity());
            dispoables.add(disposable);
        }

        ListPreference theme = (ListPreference) findPreference("theme");
        theme.setSummary(theme.getEntry());
    }

    @Override
    public void onPause() {
        dispoables.dispose();
        super.onPause();
    }

    private ContentResolver getContentResolver() {
        return getActivity().getContentResolver();
    }
}
