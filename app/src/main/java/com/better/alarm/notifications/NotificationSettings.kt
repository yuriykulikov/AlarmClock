package com.better.alarm.notifications

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.app.AlertDialog
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import com.better.alarm.R
import com.better.alarm.bootstrap.globalLogger
import com.better.alarm.logger.Logger
import com.better.alarm.platform.oreo

object NotificationSettings {
  val logger: Logger by globalLogger("NotificationSettings")

  private fun Context.openAppNotificationSettings() {
    val intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
          }
        } else {
          Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
          }
        }
    startActivity(intent)
  }

  @TargetApi(Build.VERSION_CODES.O)
  fun Context.openChannelSettings(channelId: String) {
    val intent =
        Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            .putExtra(Settings.EXTRA_CHANNEL_ID, channelId)
    startActivity(intent)
  }

  fun checkNotificationPermissionsAndSettings(activity: Activity) =
      with(activity) {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED) {
          showOkButtonDialog(
              title = R.string.alert_permission_post_notifications_title,
              message = R.string.alert_permission_post_notifications_text,
              function = { requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 3) },
          )
        } else {
          checkNotificationChannelSettings()
        }
      }

  private fun Context.checkNotificationChannelSettings() {
    val context = this
    oreo {
      val notifications = getSystemService(NotificationManager::class.java)
      when {
        !NotificationManagerCompat.from(context).areNotificationsEnabled() -> {
          showOkButtonDialog(
              title = R.string.alert_notifications_title,
              message = R.string.alert_notifications_turned_off,
              function = { context.openAppNotificationSettings() },
          )
        }
        notifications.getNotificationChannel(CHANNEL_ID_HIGH_PRIO).importance <
            NotificationManager.IMPORTANCE_HIGH -> {
          showOkButtonDialog(
              title = R.string.alert_notifications_title,
              message = R.string.alert_notifications_high_prio_text,
              function = { context.openChannelSettings(CHANNEL_ID_HIGH_PRIO) },
          )
        }
        notifications.getNotificationChannel(CHANNEL_ID).importance <
            NotificationManager.IMPORTANCE_LOW -> {
          showOkButtonDialog(
              title = R.string.alert_notifications_title,
              message = R.string.alert_notifications_default_prio_text,
              function = { context.openChannelSettings(CHANNEL_ID) },
          )
        }
      }
    }
  }

  private fun Context.showOkButtonDialog(title: Int, message: Int, function: () -> Unit) {
    runCatching {
          AlertDialog.Builder(this)
              .setTitle(getString(title))
              .setMessage(getString(message))
              .setPositiveButton(android.R.string.ok) { _, _ -> function() }
              .setNegativeButton(android.R.string.cancel, null)
              .show()
        }
        .recover { e ->
          logger.error(e) { "Was not able to show dialog, continue without the dialog, $e" }
          function()
        }
  }
}
