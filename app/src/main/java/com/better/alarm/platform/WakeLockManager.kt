/*
 * Copyright (C) 2012 Yuriy Kulikov yuriy.kulikov.87@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.better.alarm.platform

import android.app.Application
import android.content.Intent
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import com.better.alarm.logger.Logger
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

/**
 * Utility class to pass [WakeLock] objects with intents. It contains a factory, which has to be
 * initialized in [Application.onCreate] or otherwise there will be an exception. Instance maintains
 * a [Map] of String tag to [WakeLock] wakelock
 */
class WakeLockManager(private val log: Logger, val pm: PowerManager) : Wakelocks {
  private val wakelockCounter = AtomicInteger(0)
  private val wakeLockIds = CopyOnWriteArrayList<Int>()
  private val serviceWakelock =
      pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SimpleAlarmClock:AlertServiceWrapper")
  private val transitionWakelock =
      pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SimpleAlarmClock:AlertServicePusher")

  override fun acquireServiceLock() {
    log.debug { "Acquired service wakelock" }
    serviceWakelock.acquire(60 * 60_000)
  }

  override fun releaseServiceLock() {
    if (serviceWakelock.isHeld) {
      log.debug { "Released service wakelock" }
      serviceWakelock.release()
    }
  }

  /**
   * Acquires a partial [WakeLock], stores it internally and puts the tag into the [Intent]. To be
   * used with [WakeLockManager.releaseTransitionWakeLock]
   */
  fun acquireTransitionWakeLock(intent: Intent) {
    transitionWakelock.acquire(60 * 1000)
    wakelockCounter.incrementAndGet().also { count ->
      wakeLockIds.add(count)
      intent.putExtra(COUNT, count)
      log.debug { "Acquired $transitionWakelock #$count" }
    }
  }

  /**
   * Releases a partial [WakeLock] with a tag contained in the given [Intent]
   *
   * @param intent
   */
  fun releaseTransitionWakeLock(intent: Intent) {
    val count = intent.getIntExtra(COUNT, -1)
    val wasRemoved = wakeLockIds.remove(count)
    if (wasRemoved && transitionWakelock.isHeld) {
      transitionWakelock.release()
      log.debug { "Released $transitionWakelock #$count" }
    }
  }

  companion object {
    const val COUNT = "WakeLockManager.COUNT"
  }
}
