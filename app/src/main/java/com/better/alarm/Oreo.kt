package com.better.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.support.v4.app.NotificationCompat

enum class NotificationImportance {
    HIGH, NORMAL, LOW;
}

fun Context.notificationBuilder(
        channelId: String,
        importance: NotificationImportance = NotificationImportance.NORMAL,
        notificationBuilder: NotificationCompat.Builder.() -> Unit
): Notification {
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
        Build.VERSION.SDK_INT >= 26 -> NotificationCompat.Builder(this, channelId)
        else -> NotificationCompat.Builder(this, channelId)
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

fun <T> T.lollipop(action: T.() -> Unit): T {
    if (lollipop()) {
        this.action()
    }
    return this
}

fun lollipop(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
}