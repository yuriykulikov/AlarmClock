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
package com.better.alarm.bootstrap

import android.annotation.SuppressLint
import android.app.Application
import android.os.Build
import android.view.ViewConfiguration
import androidx.multidex.MultiDexApplication
import androidx.preference.PreferenceManager
import com.better.alarm.R
import com.better.alarm.bootstrap.AlarmApplicationInit.startOnce
import com.better.alarm.data.AlarmValue
import com.better.alarm.domain.Alarms
import com.better.alarm.domain.AlarmsScheduler
import com.better.alarm.domain.Store
import com.better.alarm.logger.BugReporter
import com.better.alarm.notifications.BackgroundNotifications
import com.better.alarm.notifications.createNotificationChannels
import com.better.alarm.receivers.ScheduledReceiver
import com.better.alarm.services.AlertServicePusher
import com.better.alarm.ui.toast.ToastPresenter
import java.util.concurrent.atomic.AtomicBoolean

class AlarmApplication : MultiDexApplication() {
  override fun onCreate() {
    startOnce(this)
    super.onCreate()
  }

  companion object {
    @JvmStatic
    fun startOnce(application: Application) {
      application.startOnce()
    }
  }
}

private object AlarmApplicationInit {
  private val started = AtomicBoolean(false)

  @SuppressLint("SoonBlockedPrivateApi")
  fun Application.startOnce() {
    if (started.getAndSet(true)) {
      return
    }

    runCatching {
      ViewConfiguration::class
          .java
          .getDeclaredField("sHasPermanentMenuKey")
          .apply { isAccessible = true }
          .setBoolean(ViewConfiguration.get(this), false)
    }

    val koin = startKoin(applicationContext)

    koin.get<BugReporter>().attachToMainThread(this)

    PreferenceManager.setDefaultValues(this, R.xml.preferences, false)

    koin.get<ScheduledReceiver>().start()
    koin.get<ToastPresenter>().start()
    koin.get<AlertServicePusher>()
    koin.get<BackgroundNotifications>()

    createNotificationChannels()

    // must be started the last, because otherwise we may loose intents from it.
    val alarmsLogger = koin.logger("Alarms")
    koin.get<Alarms>().start()
    alarmsLogger.debug { "Started alarms, SDK is " + Build.VERSION.SDK_INT }
    // start scheduling alarms after all alarms have been started
    koin.get<AlarmsScheduler>().start()

    with(koin.get<Store>()) {
      // register logging after startup has finished to avoid logging( O(n) instead of O(n log n) )
      alarms()
          .distinctUntilChanged()
          .map { it.toSet() }
          .startWith(emptySet<AlarmValue>())
          .buffer(2, 1)
          .map { (prev, next) -> next.minus(prev).map { it.toString() } }
          .distinctUntilChanged()
          .subscribe { lines -> lines.forEach { alarmsLogger.debug { it } } }
    }
  }
}
