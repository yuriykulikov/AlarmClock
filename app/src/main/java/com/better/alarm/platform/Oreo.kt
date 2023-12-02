package com.better.alarm.platform

import android.app.PendingIntent
import android.os.Build

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

fun pendingIntentUpdateCurrentFlag(): Int {
  return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
  } else {
    PendingIntent.FLAG_UPDATE_CURRENT
  }
}
