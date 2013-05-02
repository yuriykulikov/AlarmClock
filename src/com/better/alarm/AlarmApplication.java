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
package com.better.alarm;

import java.lang.reflect.Field;

import org.acra.ACRA;
import org.acra.ReportField;
import org.acra.annotation.ReportsCrashes;

import android.app.Application;
import android.view.ViewConfiguration;

import com.better.alarm.model.AlarmCore;
import com.better.alarm.model.Alarms;
import com.better.alarm.model.AlarmsManager;
import com.better.alarm.model.AlarmsScheduler;
import com.better.alarm.model.AlarmsService;
import com.better.alarm.model.persistance.AlarmDatabaseHelper;
import com.better.alarm.model.persistance.AlarmProvider;
import com.better.alarm.presenter.AlarmsListFragment;
import com.better.alarm.presenter.alert.AlarmAlertFullScreen;
import com.better.alarm.presenter.background.KlaxonService;
import com.better.alarm.presenter.background.VibrationService;
import com.github.androidutils.logger.FileLogWriter;
import com.github.androidutils.logger.LogcatLogWriterWithLines;
import com.github.androidutils.logger.Logger;
import com.github.androidutils.logger.Logger.LogLevel;
import com.github.androidutils.logger.LoggingExceptionHandler;
import com.github.androidutils.logger.StartupLogWriter;
import com.github.androidutils.statemachine.StateMachine;
import com.github.androidutils.wakelock.WakeLockManager;

// @formatter:off
@ReportsCrashes(
        formKey = "dFFwNk1yQ1lXeFNpVG1YVE1OZ05YNVE6MQ",
        applicationLogFile = "applog.log",
        applicationLogFileLines = 150,
        customReportContent = {
                ReportField.IS_SILENT,
                ReportField.APP_VERSION_CODE,
                ReportField.PHONE_MODEL,
                ReportField.DEVICE_ID,
                ReportField.ANDROID_VERSION,
                ReportField.CUSTOM_DATA,
                ReportField.LOGCAT,
                ReportField.STACK_TRACE,
                ReportField.APPLICATION_LOG,
                ReportField.SHARED_PREFERENCES,
                })
// @formatter:on
public class AlarmApplication extends Application {

    @Override
    public void onCreate() {
        // The following line triggers the initialization of ACRA
        ACRA.init(this);

        try {
            ViewConfiguration config = ViewConfiguration.get(this);
            Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
            if (menuKeyField != null) {
                menuKeyField.setAccessible(true);
                menuKeyField.setBoolean(config, false);
            }
        } catch (Exception ex) {
            // Ignore
        }

        Logger logger = Logger.getDefaultLogger();
        logger.addLogWriter(LogcatLogWriterWithLines.getInstance());
        logger.addLogWriter(FileLogWriter.getInstance(this, false));
        logger.addLogWriter(StartupLogWriter.getInstance());
        LoggingExceptionHandler.addLoggingExceptionHandlerToAllThreads(logger);

        logger.setLogLevel(WakeLockManager.class, LogLevel.ERR);
        logger.setLogLevel(AlarmDatabaseHelper.class, LogLevel.ERR);
        logger.setLogLevel(AlarmProvider.class, LogLevel.ERR);
        logger.setLogLevel(AlarmsScheduler.class, LogLevel.DBG);
        logger.setLogLevel(AlarmCore.class, LogLevel.DBG);
        logger.setLogLevel(Alarms.class, LogLevel.DBG);
        logger.setLogLevel(StateMachine.class, LogLevel.DBG);
        logger.setLogLevel(AlarmsService.class, LogLevel.DBG);
        logger.setLogLevel(KlaxonService.class, LogLevel.DBG);
        logger.setLogLevel(VibrationService.class, LogLevel.DBG);
        logger.setLogLevel(AlarmsListFragment.class, LogLevel.DBG);
        logger.setLogLevel(AlarmAlertFullScreen.class, LogLevel.DBG);

        WakeLockManager.init(getApplicationContext(), logger, true);
        AlarmsManager.init(getApplicationContext(), logger);

        logger.d("onCreate");
        super.onCreate();
    }
}
