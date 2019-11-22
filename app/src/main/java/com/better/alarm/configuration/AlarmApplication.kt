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
package com.better.alarm.configuration

import android.app.Application
import android.preference.PreferenceManager
import android.view.ViewConfiguration
import com.better.alarm.BuildConfig
import com.better.alarm.R
import com.better.alarm.alert.BackgroundNotifications
import com.better.alarm.background.AlertServicePusher
import com.better.alarm.createNotificationChannels
import com.better.alarm.logger.LoggingExceptionHandler
import com.better.alarm.logger.StartupLogWriter
import com.better.alarm.model.Alarms
import com.better.alarm.model.AlarmsScheduler
import com.better.alarm.presenter.ScheduledReceiver
import com.better.alarm.presenter.ToastPresenter
import org.acra.ACRA
import org.acra.ReportField
import org.acra.annotation.ReportsCrashes

@ReportsCrashes(
        mailTo = BuildConfig.ACRA_EMAIL,
        applicationLogFileLines = 150,
        customReportContent = [
            ReportField.IS_SILENT,
            ReportField.APP_VERSION_CODE,
            ReportField.PHONE_MODEL,
            ReportField.ANDROID_VERSION,
            ReportField.CUSTOM_DATA,
            ReportField.STACK_TRACE,
            ReportField.SHARED_PREFERENCES
        ]
)
class AlarmApplication : Application() {

    override fun onCreate() {
        if (BuildConfig.ACRA_EMAIL.isNotEmpty()) {
            ACRA.init(this)
        }

        runCatching {
            ViewConfiguration::class.java
                    .getDeclaredField("sHasPermanentMenuKey")
                    .apply { isAccessible = true }
                    .setBoolean(ViewConfiguration.get(this), false)
        }

        val koin = startKoin(applicationContext)

        LoggingExceptionHandler.addLoggingExceptionHandlerToAllThreads(koin.rootScope.logger("default"))

        if (BuildConfig.ACRA_EMAIL.isNotEmpty()) {
            ACRA.getErrorReporter().setExceptionHandlerInitializer { reporter ->
                reporter.putCustomData("STARTUP_LOG", koin.rootScope.get<StartupLogWriter>().getMessagesAsString())
            }
        }

        // must be after sContainer
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)

        // TODO make it lazy
        koin.rootScope.get<ScheduledReceiver>().start()
        koin.rootScope.get<ToastPresenter>().start()
        koin.rootScope.get<AlertServicePusher>()
        koin.rootScope.get<BackgroundNotifications>()

        createNotificationChannels()

        // must be started the last, because otherwise we may loose intents from it.
        val alarmsLogger = koin.rootScope.logger("Alarms")
        alarmsLogger.d("Starting alarms")
        val alarms = koin.rootScope.get<Alarms>()
        alarms.start()
        // start scheduling alarms after all alarms have been started
        koin.rootScope.get<AlarmsScheduler>().start()

        with(koin.rootScope.get<Store>()) {
            // register logging after startup has finished to avoid logging( O(n) instead of O(n log n) )
            alarms()
                    .distinctUntilChanged()
                    .subscribe { alarmValues ->
                        for (alarmValue in alarmValues) {
                            alarmsLogger.d(alarmValue)
                        }
                    }

            next()
                    .distinctUntilChanged()
                    .subscribe { next -> alarmsLogger.d("## Next: $next") }
        }

        super.onCreate()
    }
}
