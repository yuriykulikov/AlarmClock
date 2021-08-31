package com.better.alarm.wakelock

interface Wakelocks {
  fun acquireServiceLock()

  fun releaseServiceLock()
}
