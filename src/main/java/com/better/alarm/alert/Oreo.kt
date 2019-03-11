package com.better.alarm.alert

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.better.alarm.R

enum class NotificationImportance {
    HIGH, NORMAL, LOW;
}

fun Context.notificationBuilder(channelId: String, importance: NotificationImportance = NotificationImportance.NORMAL, notificationBuilder: Notification.Builder.()->Unit): Notification {
    oreo {
        val name = getString(R.string.app_label)
        val channel = NotificationChannel(channelId, name, when (importance) {
            NotificationImportance.HIGH -> NotificationManager.IMPORTANCE_HIGH
            NotificationImportance.NORMAL -> NotificationManager.IMPORTANCE_DEFAULT
            NotificationImportance.LOW -> NotificationManager.IMPORTANCE_LOW
        })
        channel.setSound(null, null)

        // Register the channel with the system; you can't change the importance
        // or other notification behaviors after this
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager!!.createNotificationChannel(channel)
    }

    val builder = when {
        Build.VERSION.SDK_INT >= 26 -> Notification.Builder(this, channelId)
        else -> Notification.Builder(this)
    }

    notificationBuilder(builder)

    return builder.build()
}

fun oreo(action: () -> Unit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        action()
    }
}

fun preOreo(action: () -> Unit) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
        action()
    }
}