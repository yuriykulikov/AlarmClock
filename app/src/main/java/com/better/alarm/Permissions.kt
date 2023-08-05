package com.better.alarm

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import com.better.alarm.background.PlayerWrapper
import com.better.alarm.configuration.globalLogger
import com.better.alarm.logger.Logger
import com.better.alarm.model.Alarmtone
import com.better.alarm.model.ringtoneManagerUri
import com.better.alarm.presenter.userFriendlyTitle

/** Checks if all ringtones can be played, and requests permissions if it is not the case */
fun checkPermissions(activity: Activity, tones: List<Alarmtone>) {
  checkSetAlarmPermissions(activity)
  checkMediaPermissions(activity, tones)
}

private fun checkMediaPermissions(activity: Activity, tones: List<Alarmtone>) {
  if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
  val mediaPermission =
      when {
        Build.VERSION.SDK_INT in 23..31 -> Manifest.permission.READ_EXTERNAL_STORAGE
        Build.VERSION.SDK_INT >= 33 -> Manifest.permission.READ_MEDIA_AUDIO
        else -> null
      }
  if (mediaPermission != null &&
      activity.checkSelfPermission(mediaPermission) != PackageManager.PERMISSION_GRANTED) {
    val logger: Logger by globalLogger("checkPermissions")

    val unplayable =
        tones
            .filter { alarmtone ->
              val uri = alarmtone.ringtoneManagerUri()
              uri != null &&
                  runCatching {
                        PlayerWrapper(
                                context = activity,
                                resources = activity.resources,
                                log = logger,
                            )
                            .setDataSource(uri)
                      }
                      .isFailure
            }
            .map { ringtone -> ringtone.userFriendlyTitle(activity) }

    if (unplayable.isNotEmpty()) {
      try {
        AlertDialog.Builder(activity)
            .setTitle(activity.getString(R.string.alert))
            .setMessage(
                activity.getString(
                    R.string.permissions_external_storage_text, unplayable.joinToString(", ")))
            .setPositiveButton(android.R.string.ok) { _, _ ->
              activity.requestPermissions(arrayOf(mediaPermission), 3)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
      } catch (e: Exception) {
        logger.e("Was not able to show dialog to request permission, continue without the dialog")
        activity.requestPermissions(arrayOf(mediaPermission), 3)
      }
    }
  }
}

fun checkSetAlarmPermissions(activity: Activity) {
  if (Build.VERSION.SDK_INT in 31..33 &&
      activity.getSystemService<AlarmManager>()?.canScheduleExactAlarms() != true) {
    AlertDialog.Builder(activity)
        .setTitle(activity.getString(R.string.set_exact_alarm_permission_title))
        .setMessage(activity.getString(R.string.set_exact_alarm_permission_text))
        .setPositiveButton(android.R.string.ok) { _, _ ->
          activity.startActivity(
              Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = ("package:${BuildConfig.APPLICATION_ID}").toUri()
              })
        }
        .setNegativeButton(android.R.string.cancel, null)
        .show()
  }
}
