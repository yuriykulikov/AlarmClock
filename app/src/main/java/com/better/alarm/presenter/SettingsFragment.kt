package com.better.alarm.presenter

import android.content.ContentResolver
import android.content.Intent
import android.media.AudioManager
import android.os.*
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
import com.better.alarm.stores.RxDataStore
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
  }

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    addPreferencesFromResource(R.xml.preferences)

    val category: PreferenceCategory = findPreference("preference_category_sound_key")!!

    if (!vibrator.hasVibrator()) {
      findPreference<Preference>(Prefs.KEY_VIBRATE)?.let { category.removePreference(it) }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      category.removePreference(findPreference(Prefs.KEY_ALARM_IN_SILENT_MODE))
    }

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
      findPreference<PreferenceCategory>("preference_category_ui")
          ?.removePreference(findPreference(Prefs.LIST_ROW_LAYOUT))

      findPreference<ListPreference>(Prefs.KEY_THEME)?.apply {
        entries = entries.take(2).toTypedArray()
        entryValues = entryValues.take(2).toTypedArray()
      }
    }

    findPreference<Preference>(Prefs.KEY_THEME)?.onPreferenceChangeListener =
        Preference.OnPreferenceChangeListener { _, _ ->
          Handler(Looper.getMainLooper()).post {
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

    findPreference<VolumePreference>(Prefs.KEY_VOLUME_PREFERENCE)?.onResume()

    checkPermissions(requireActivity(), listOf(Alarmtone.Default()))

    bindListPreference(Prefs.KEY_ALARM_SNOOZE, prefs.snoozeDuration) { duration ->
      val idx = findIndexOfValue(duration.toString())
      summary = entries[idx]
    }

    bindListPreference(Prefs.KEY_AUTO_SILENCE, prefs.autoSilence) { newValue ->
      summary =
          when (newValue) {
            -1 -> getString(R.string.auto_silence_never)
            else -> getString(R.string.auto_silence_summary, newValue)
          }
    }

    bindListPreference(Prefs.KEY_SKIP_DURATION, prefs.skipDuration) { skipDuration ->
      val indexOfValue = findIndexOfValue(skipDuration.toString())
      summary = entries[indexOfValue]
    }

    bindListPreference(Prefs.KEY_PREALARM_DURATION, prefs.preAlarmDuration) { newValue ->
      summary =
          when (newValue) {
            -1 -> getString(R.string.prealarm_off_summary)
            else -> getString(R.string.prealarm_summary, newValue)
          }
    }

    bindListPreference(Prefs.KEY_FADE_IN_TIME_SEC, prefs.fadeInTimeInSeconds) { newValue ->
      summary =
          when (newValue) {
            1 -> getString(R.string.fade_in_off_summary)
            else -> getString(R.string.fade_in_summary, newValue)
          }
    }

    bindListPreference(Prefs.KEY_THEME, prefs.theme) { summary = entry }

    lollipop { bindListPreference(Prefs.LIST_ROW_LAYOUT, prefs.listRowLayout) { summary = entry } }
  }

  override fun onPause() {
    disposables.dispose()
    findPreference<VolumePreference>(Prefs.KEY_VOLUME_PREFERENCE)?.onPause()
    super.onPause()
  }

  private fun <S> bindListPreference(
      key: String,
      store: RxDataStore<S>,
      func: (ListPreference).(S) -> Unit
  ) {
    val preference =
        requireNotNull(findPreference<ListPreference>(key)) {
          "Preference with key: $key not found"
        }
    disposables.add(store.observe().subscribe { next -> func(preference, next) })
  }

  private fun systemModeRingerStreamsAffected(): Int {
    return Settings.System.getInt(contentResolver, Settings.System.MODE_RINGER_STREAMS_AFFECTED, 0)
  }
}
