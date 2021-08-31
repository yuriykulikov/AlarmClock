package com.better.alarm

import android.annotation.TargetApi
import android.app.AlertDialog
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

class NotificationSettings {

  fun Context.openAppNotificationSettings() {
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
            .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName())
            .putExtra(Settings.EXTRA_CHANNEL_ID, channelId)
    startActivity(intent)
  }

  fun checkSettings(context: Context) {
    oreo {
      val notifications = context.getSystemService(NotificationManager::class.java)
      when {
        !NotificationManagerCompat.from(context).areNotificationsEnabled() -> {
          AlertDialog.Builder(context)
              .setTitle(context.getString(R.string.alert_notifications_title))
              .setMessage(context.getString(R.string.alert_notifications_turned_off))
              .setPositiveButton(android.R.string.ok) { _, _ ->
                context.openAppNotificationSettings()
              }
              .setNegativeButton(android.R.string.cancel, null)
              .show()
        }
        notifications.getNotificationChannel(CHANNEL_ID_HIGH_PRIO).importance !=
            NotificationManager.IMPORTANCE_HIGH -> {
          AlertDialog.Builder(context)
              .setTitle(context.getString(R.string.alert_notifications_title))
              .setMessage(context.getString(R.string.alert_notifications_high_prio_text))
              .setPositiveButton(android.R.string.ok) { _, _ ->
                context.openChannelSettings(CHANNEL_ID_HIGH_PRIO)
              }
              .setNegativeButton(android.R.string.cancel, null)
              .show()
        }
        notifications.getNotificationChannel(CHANNEL_ID).importance !=
            NotificationManager.IMPORTANCE_DEFAULT -> {
          AlertDialog.Builder(context)
              .setTitle(context.getString(R.string.alert_notifications_title))
              .setMessage(context.getString(R.string.alert_notifications_default_prio_text))
              .setPositiveButton(android.R.string.ok) { _, _ ->
                context.openChannelSettings(CHANNEL_ID)
              }
              .setNegativeButton(android.R.string.cancel, null)
              .show()
        }
      }
    }
  }
}
