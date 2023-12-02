package com.better.alarm.platform

interface Wakelocks {
  fun acquireServiceLock()

  fun releaseServiceLock()
}
