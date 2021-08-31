package com.better.alarm.presenter

import android.content.ContentResolver
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Vibrator
import android.provider.Settings
import androidx.preference.CheckBoxPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import com.better.alarm.R
import com.better.alarm.checkPermissions
import com.better.alarm.configuration.Prefs
import com.better.alarm.configuration.globalInject
import com.better.alarm.lollipop
import com.better.alarm.model.Alarmtone
import com.better.alarm.view.VolumePreference
import io.reactivex.disposables.CompositeDisposable

/** Created by Yuriy on 24.07.2017. */
class SettingsFragment : PreferenceFragmentCompat() {
  private val alarmStreamTypeBit = 1 shl AudioManager.STREAM_ALARM
  private val vibrator: Vibrator by globalInject()
  private val prefs: Prefs by globalInject()
  private val disposables = CompositeDisposable()

  private val contentResolver: ContentResolver
    get() = requireActivity().contentResolver

  companion object {
    const val themeChangeReason = "theme change"
    private val retainedStrings =
        listOf(
            R.string.alarm_alert_snooze_set,
            R.string.volume_button_dialog_title,
            R.string.volume_button_setting_summary,
            R.array.volume_button_setting_entries,
            R.array.volume_button_setting_values,
            R.string.default_ringtone_setting_title,
        )
  }

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    addPreferencesFromResource(R.xml.preferences)

    val category: PreferenceCategory = findPreference("preference_category_sound_key")!!

    if (!vibrator.hasVibrator()) {
      findPreference<Preference>("vibrate")?.let { category.removePreference(it) }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      category.removePreference(findPreference(Prefs.KEY_ALARM_IN_SILENT_MODE))
    }

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
      findPreference<PreferenceCategory>("preference_category_ui")
          ?.removePreference(findPreference(Prefs.LIST_ROW_LAYOUT))
    }

    findPreference<Preference>("theme")?.onPreferenceChangeListener =
        Preference.OnPreferenceChangeListener { _, _ ->
          Handler().post {
            val intent =
                requireActivity()
                    .packageManager
                    .getLaunchIntentForPackage(requireActivity().packageName)
                    ?.apply {
                      addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                      putExtra("reason", themeChangeReason)
                    }
            startActivity(intent)
          }
          true
        }
  }

  override fun onPreferenceTreeClick(preference: Preference?): Boolean {
    when (preference?.key) {
      Prefs.KEY_ALARM_IN_SILENT_MODE -> {
        val pref = preference as CheckBoxPreference

        val ringerModeStreamTypes =
            when {
              pref.isChecked -> systemModeRingerStreamsAffected() and alarmStreamTypeBit.inv()
              else -> systemModeRingerStreamsAffected() or alarmStreamTypeBit
            }

        Settings.System.putInt(
            contentResolver, Settings.System.MODE_RINGER_STREAMS_AFFECTED, ringerModeStreamTypes)

        return true
      }
      else -> return super.onPreferenceTreeClick(preference)
    }
  }

  override fun onResume() {
    super.onResume()
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
      val alarmInSilentModePref =
          findPreference<CheckBoxPreference>(Prefs.KEY_ALARM_IN_SILENT_MODE)!!
      alarmInSilentModePref.isChecked =
          (systemModeRingerStreamsAffected() and alarmStreamTypeBit) == 0
    }

    findPreference<VolumePreference>("volume_preference")!!.onResume()

    checkPermissions(requireActivity(), listOf(Alarmtone.Default()))

    findListPreference(Prefs.KEY_ALARM_SNOOZE)
        .let { snoozePref ->
          prefs.snoozeDuration.observe().subscribe { newValue ->
            val idx = snoozePref.findIndexOfValue(newValue.toString())
            snoozePref.summary = snoozePref.entries[idx]
          }
        }
        .let { disposables.add(it) }

    findListPreference(Prefs.KEY_AUTO_SILENCE)
        .let { autoSilencePref ->
          prefs.autoSilence.observe().subscribe { newValue ->
            autoSilencePref.summary =
                when {
                  newValue == -1 -> getString(R.string.auto_silence_never)
                  else -> getString(R.string.auto_silence_summary, newValue.toInt())
                }
          }
        }
        .let { disposables.add(it) }

    findListPreference(Prefs.SKIP_DURATION_KEY)
        .let { skipDurationPref ->
          prefs.skipDuration.observe().subscribe {
            val indexOfValue = skipDurationPref.findIndexOfValue(skipDurationPref.value)
            skipDurationPref.summary = skipDurationPref.entries[indexOfValue]
          }
        }
        .let { disposables.add(it) }

    findListPreference(Prefs.KEY_PREALARM_DURATION)
        .let { prealarmDurationPref ->
          prefs.preAlarmDuration.observe().subscribe { newValue ->
            prealarmDurationPref.summary =
                when {
                  newValue == -1 -> getString(R.string.prealarm_off_summary)
                  else -> getString(R.string.prealarm_summary, newValue.toInt())
                }
          }
        }
        .let { disposables.add(it) }

    findListPreference(Prefs.KEY_FADE_IN_TIME_SEC)
        .let { fadeInPref ->
          prefs.fadeInTimeInSeconds.observe().subscribe { newValue ->
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
      prefs
          .listRowLayout
          .observe()
          .subscribe { findListPreference(Prefs.LIST_ROW_LAYOUT).run { summary = entry } }
          .let { disposables.add(it) }
    }
  }

  override fun onPause() {
    disposables.dispose()
    findPreference<VolumePreference>("volume_preference")?.onPause()
    super.onPause()
  }

  private fun findListPreference(key: String) = findPreference<ListPreference>(key)!!

  private fun systemModeRingerStreamsAffected(): Int {
    return Settings.System.getInt(contentResolver, Settings.System.MODE_RINGER_STREAMS_AFFECTED, 0)
  }
}
