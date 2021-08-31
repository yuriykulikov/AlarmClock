package com.better.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

enum class NotificationImportance {
  HIGH,
  NORMAL,
  LOW
}

const val CHANNEL_ID_HIGH_PRIO = "${BuildConfig.APPLICATION_ID}.NotificationsPlugin"
const val CHANNEL_ID = "${BuildConfig.APPLICATION_ID}.BackgroundNotifications"
const val CHANNEL_RESERVED = "${BuildConfig.APPLICATION_ID}.AlertServiceWrapper"

fun Context.notificationBuilder(
    channelId: String,
    notificationBuilder: NotificationCompat.Builder.() -> Unit
): Notification {
  val builder =
      when {
        Build.VERSION.SDK_INT >= 26 -> NotificationCompat.Builder(this, channelId)
        else -> NotificationCompat.Builder(this, channelId)
      }

  notificationBuilder(builder)

  return builder.build()
}

fun Context.createNotificationChannels() {
  oreo {
    // Register the channel with the system; you can't change the importance
    // or other notification behaviors after this
    getSystemService(NotificationManager::class.java)?.run {
      createNotificationChannel(
          NotificationChannel(
                  CHANNEL_ID,
                  getString(R.string.notification_channel_default_prio),
                  NotificationManager.IMPORTANCE_DEFAULT)
              .apply { setSound(null, null) })
      createNotificationChannel(
          NotificationChannel(
                  CHANNEL_ID_HIGH_PRIO,
                  getString(R.string.notification_channel_high_prio),
                  NotificationManager.IMPORTANCE_HIGH)
              .apply { setSound(null, null) })
      createNotificationChannel(
          NotificationChannel(
                  CHANNEL_RESERVED,
                  getString(R.string.notification_channel_low_prio),
                  NotificationManager.IMPORTANCE_LOW)
              .apply { setSound(null, null) })
    }
  }
}

fun oreo(action: () -> Unit) {
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    action()
  }
}

fun isOreo() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

fun preOreo(action: () -> Unit) {
  if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
    action()
  }
}

fun lollipop(action: () -> Unit) {
  if (lollipop()) {
    action()
  }
}

fun preLollipop(action: () -> Unit) {
  if (!lollipop()) {
    action()
  }
}

fun lollipop(): Boolean {
  return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
}
