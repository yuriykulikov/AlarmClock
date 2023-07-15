package com.better.alarm.presenter

import android.content.Context
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.widget.Toast
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.better.alarm.R
import com.better.alarm.model.Alarmtone
import com.better.alarm.model.ringtoneManagerUri

/**
 * Shows the ringtone picker.
 *
 * @param current the current ringtone
 * @param ringtonePickerRequestCode the request code for the ringtone picker
 * @param defaultRingtone the default ringtone, which is the system default for settings and
 *   "internal default" for alarms
 */
fun Fragment.showRingtonePicker(
    current: Alarmtone,
    ringtonePickerRequestCode: Int,
    defaultRingtone: Alarmtone? = null
) {
  try {
    val pickerIntent =
        Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
          // only show alarms
          putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
          // also show the silent ringtone
          putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)

          // highlight the current ringtone
          val currentUri =
              if (current is Alarmtone.Default) defaultRingtone?.ringtoneManagerUri()
              else current.ringtoneManagerUri()

          putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, currentUri?.toUri())

          // show the default ringtone, which is the system default for settings and "internal
          // default" for alarms
          putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
          val defaultUri =
              if (defaultRingtone != null) {
                defaultRingtone
                    .ringtoneManagerUri()
                    ?.toUri()
                    ?.buildUpon()
                    ?.appendQueryParameter("default", "true")
                    ?.build()
              } else {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
              }
          putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, defaultUri)
        }
    startActivityForResult(pickerIntent, ringtonePickerRequestCode)
  } catch (e: Exception) {
    Toast.makeText(
            requireContext(), getString(R.string.details_no_ringtone_picker), Toast.LENGTH_LONG)
        .show()
  }
}

/**
 * Returns the ringtone that was picked in the ringtone picker.
 *
 * Can return [Alarmtone.Silent], [Alarmtone.Sound] everywhere, [Alarmtone.SystemDefault] in
 * settings and [Alarmtone.Default] in alarms.
 */
fun Intent.getPickedRingtone(): Alarmtone {
  val uriString: String? =
      getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)?.toString()

  val alarmtone: Alarmtone =
      when {
        uriString == null -> Alarmtone.Silent
        uriString.contains("default=true") -> Alarmtone.Default
        uriString == "silent" -> Alarmtone.Silent
        uriString == "default" -> Alarmtone.SystemDefault
        uriString == RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString() ->
            Alarmtone.SystemDefault
        else -> Alarmtone.Sound(uriString)
      }

  return alarmtone
}

fun Alarmtone.userFriendlyTitle(context: Context): CharSequence {
  return runCatching {
        when (this) {
          is Alarmtone.Silent -> context.getText(R.string.silent_alarm_summary)
          else ->
              RingtoneManager.getRingtone(context, this.ringtoneManagerUri()?.toUri())
                  .title(context)
        }
      }
      .getOrDefault("")
}

private fun Ringtone.title(context: Context): CharSequence {
  // this can fail, see
  // https://github.com/yuriykulikov/AlarmClock/issues/403
  return try {
    getTitle(context) ?: context.getText(R.string.silent_alarm_summary)
  } catch (e: Exception) {
    context.getText(R.string.silent_alarm_summary)
  } catch (e: NullPointerException) {
    null
  } ?: ""
}
