package com.better.alarm.presenter

import android.content.ContentResolver
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Vibrator
import android.preference.*
import android.provider.Settings
import com.better.alarm.R
import com.better.alarm.checkPermissions
import com.better.alarm.configuration.Prefs
import com.better.alarm.configuration.globalInject
import com.better.alarm.lollipop
import com.better.alarm.model.Alarmtone
import com.better.alarm.view.VolumePreference
import com.f2prateek.rx.preferences2.RxSharedPreferences
import io.reactivex.disposables.CompositeDisposable

/**
 * Created by Yuriy on 24.07.2017.
 */

class SettingsFragment : PreferenceFragment() {
    private val alarmStreamTypeBit = 1 shl AudioManager.STREAM_ALARM
    private val vibrator: Vibrator by globalInject()
    private val rxSharedPreferences: RxSharedPreferences by globalInject()
    private val disposables = CompositeDisposable()

    private val contentResolver: ContentResolver
        get() = activity.contentResolver

    companion object {
        const val themeChangeReason = "theme change"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        addPreferencesFromResource(R.xml.preferences)

        val category = findPreference("preference_category_sound_key") as PreferenceCategory

        if (!vibrator.hasVibrator() && findPreference("vibrate") != null) {
            category.removePreference(findPreference("vibrate"))
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            category.removePreference(findPreference(Prefs.KEY_ALARM_IN_SILENT_MODE))
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            (findPreference("preference_category_ui") as PreferenceCategory)
                    .removePreference(findPreference(Prefs.LIST_ROW_LAYOUT))
        }

        findPreference("theme").onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, _ ->
            Handler().post {
                val intent = activity.packageManager.getLaunchIntentForPackage(activity.packageName)?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    putExtra("reason", themeChangeReason)
                }
                startActivity(intent)
            }
            true
        }
    }

    override fun onPreferenceTreeClick(preferenceScreen: PreferenceScreen, preference: Preference): Boolean {
        when (preference.key) {
            Prefs.KEY_ALARM_IN_SILENT_MODE -> {
                val pref = preference as CheckBoxPreference

                val ringerModeStreamTypes = when {
                    pref.isChecked -> systemModeRingerStreamsAffected() and alarmStreamTypeBit.inv()
                    else -> systemModeRingerStreamsAffected() or alarmStreamTypeBit
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
            val alarmInSilentModePref = findPreference(Prefs.KEY_ALARM_IN_SILENT_MODE) as CheckBoxPreference
            alarmInSilentModePref.isChecked = (systemModeRingerStreamsAffected() and alarmStreamTypeBit) == 0
        }

        (findPreference("volume_preference") as VolumePreference).onResume()

        checkPermissions(activity, listOf(Alarmtone.Default()))

        findListPreference(Prefs.KEY_ALARM_SNOOZE)
                .let { snoozePref ->
                    rxSharedPreferences.getString(Prefs.KEY_ALARM_SNOOZE)
                            .asObservable()
                            .subscribe { newValue ->
                                val idx = snoozePref.findIndexOfValue(newValue)
                                snoozePref.summary = snoozePref.entries[idx]
                            }
                }
                .let { disposables.add(it) }

        findListPreference(Prefs.KEY_AUTO_SILENCE)
                .let { autoSilencePref ->
                    rxSharedPreferences.getString(Prefs.KEY_AUTO_SILENCE)
                            .asObservable()
                            .subscribe { newValue ->
                                autoSilencePref.summary = when {
                                    newValue.toInt() == -1 -> getString(R.string.auto_silence_never)
                                    else -> getString(R.string.auto_silence_summary, newValue.toInt())
                                }
                            }
                }
                .let { disposables.add(it) }

        findListPreference(Prefs.KEY_PREALARM_DURATION)
                .let { prealarmDurationPref ->
                    rxSharedPreferences.getString(Prefs.KEY_PREALARM_DURATION)
                            .asObservable()
                            .subscribe { newValue ->
                                prealarmDurationPref.summary = when {
                                    newValue.toInt() == -1 -> getString(R.string.prealarm_off_summary)
                                    else -> getString(R.string.prealarm_summary, newValue.toInt())
                                }
                            }

                }
                .let { disposables.add(it) }

        findListPreference(Prefs.KEY_FADE_IN_TIME_SEC)
                .let { fadeInPref ->
                    rxSharedPreferences.getString(Prefs.KEY_FADE_IN_TIME_SEC)
                            .asObservable()
                            .subscribe { newValue ->
                                when {
                                    newValue.toInt() == 1 -> fadeInPref.summary = getString(R.string.fade_in_off_summary)
                                    else -> fadeInPref.summary = getString(R.string.fade_in_summary, newValue.toInt())
                                }
                            }
                }
                .let { disposables.add(it) }

        val theme = findListPreference("theme")
        theme.summary = theme.entry

        lollipop {
            rxSharedPreferences.getString(Prefs.LIST_ROW_LAYOUT)
                    .asObservable()
                    .subscribe {
                        findListPreference(Prefs.LIST_ROW_LAYOUT).run {
                            summary = entry
                        }
                    }
                    .let { disposables.add(it) }
        }
    }

    override fun onPause() {
        disposables.dispose()
        (findPreference("volume_preference") as VolumePreference).onPause()
        super.onPause()
    }

    private fun findListPreference(key: String) = findPreference(key) as ListPreference

    private fun systemModeRingerStreamsAffected(): Int {
        return Settings.System.getInt(contentResolver,
                Settings.System.MODE_RINGER_STREAMS_AFFECTED,
                0)
    }
}
