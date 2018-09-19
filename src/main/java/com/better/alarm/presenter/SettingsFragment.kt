package com.better.alarm.presenter

import android.content.ContentResolver
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.preference.*
import android.provider.Settings
import com.better.alarm.R
import com.better.alarm.configuration.AlarmApplication.container
import com.better.alarm.configuration.Prefs.*
import io.reactivex.disposables.CompositeDisposable

/**
 * Created by Yuriy on 24.07.2017.
 */

class SettingsFragment : PreferenceFragment() {
    private val ALARM_STREAM_TYPE_BIT = 1 shl AudioManager.STREAM_ALARM
    private val vibrator = container().vibrator()
    private val rxSharedPreferences = container().rxPrefs()
    private val disposables = CompositeDisposable()

    private val contentResolver: ContentResolver
        get() = activity.contentResolver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        addPreferencesFromResource(R.xml.preferences)

        val category = findPreference("preference_category_sound_key") as PreferenceCategory

        if (!vibrator.hasVibrator() && findPreference("vibrate") != null) {
            category.removePreference(findPreference("vibrate"))
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            category.removePreference(findPreference(KEY_ALARM_IN_SILENT_MODE))
        }

        findPreference("theme").onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, _ ->
            Handler().post {
                val intent = activity.packageManager.getLaunchIntentForPackage(activity.packageName).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                startActivity(intent)
            }
            true
        }
    }

    override fun onPreferenceTreeClick(preferenceScreen: PreferenceScreen, preference: Preference): Boolean {
        when (preference.key) {
            KEY_ALARM_IN_SILENT_MODE -> {
                val pref = preference as CheckBoxPreference

                val ringerModeStreamTypes = when {
                    pref.isChecked -> systemModeRingerStreamsAffected() and ALARM_STREAM_TYPE_BIT.inv()
                    else -> systemModeRingerStreamsAffected() or ALARM_STREAM_TYPE_BIT
                }

                Settings.System.putInt(contentResolver,
                        Settings.System.MODE_RINGER_STREAMS_AFFECTED,
                        ringerModeStreamTypes)

                return true
            }
            else -> return super.onPreferenceTreeClick(preferenceScreen, preference)
        }
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            val alarmInSilentModePref = findPreference(KEY_ALARM_IN_SILENT_MODE) as CheckBoxPreference
            alarmInSilentModePref.isChecked = (systemModeRingerStreamsAffected() and ALARM_STREAM_TYPE_BIT) == 0
        }

        findListPreference(KEY_ALARM_SNOOZE)
                .let { snoozePref ->
                    rxSharedPreferences.getString(KEY_ALARM_SNOOZE)
                            .asObservable()
                            .subscribe { newValue ->
                                val idx = snoozePref.findIndexOfValue(newValue)
                                snoozePref.summary = snoozePref.entries[idx]
                            }
                }
                .let { disposables.add(it) }

        findListPreference(KEY_AUTO_SILENCE)
                .let { autoSilencePref ->
                    rxSharedPreferences.getString(KEY_AUTO_SILENCE)
                            .asObservable()
                            .subscribe { newValue ->
                                autoSilencePref.summary = when {
                                    newValue.toInt() == -1 -> getString(R.string.auto_silence_never)
                                    else -> getString(R.string.auto_silence_summary, newValue.toInt())
                                }
                            }
                }
                .let { disposables.add(it) }

        findListPreference(KEY_PREALARM_DURATION)
                .let { prealarmDurationPref ->
                    rxSharedPreferences.getString(KEY_PREALARM_DURATION)
                            .asObservable()
                            .subscribe { newValue ->
                                prealarmDurationPref.summary = when {
                                    newValue.toInt() == -1 -> getString(R.string.prealarm_off_summary)
                                    else -> getString(R.string.prealarm_summary, newValue.toInt())
                                }
                            }

                }
                .let { disposables.add(it) }

        findListPreference(KEY_FADE_IN_TIME_SEC)
                .let { fadeInPref ->
                    rxSharedPreferences.getString(KEY_FADE_IN_TIME_SEC)
                            .asObservable()
                            .subscribe { newValue ->
                                when {
                                    newValue.toInt() == 1 -> fadeInPref.summary = getString(R.string.fade_in_off_summary)
                                    else -> fadeInPref.summary = getString(R.string.fade_in_summary, newValue.toInt())
                                }
                            }
                }
                .let { disposables.add(it) }


        (findPreference(KEY_DEFAULT_RINGTONE) as RingtonePreference)
                .bindPreferenceSummary(rxSharedPreferences, activity)
                .let { disposables.add(it) }

        val theme = findListPreference("theme")
        theme.summary = theme.entry
    }

    override fun onPause() {
        disposables.dispose()
        super.onPause()
    }

    private fun findListPreference(key: String) = findPreference(key) as ListPreference

    private fun systemModeRingerStreamsAffected(): Int {
        return Settings.System.getInt(contentResolver,
                Settings.System.MODE_RINGER_STREAMS_AFFECTED,
                0)
    }
}
