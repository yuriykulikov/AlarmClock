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

import java.io.File;
import java.lang.reflect.Field;

import org.acra.ACRA;
import org.acra.ErrorReporter;
import org.acra.ExceptionHandlerInitializer;
import org.acra.ReportField;
import org.acra.annotation.ReportsCrashes;

import android.app.Application;
import android.content.Context;
import android.preference.PreferenceManager;
import android.view.ViewConfiguration;

import com.better.alarm.model.AlarmsManager;
import com.better.alarm.presenter.DynamicThemeHandler;
import com.github.androidutils.logger.LogcatLogWriterWithLines;
import com.github.androidutils.logger.Logger;
import com.github.androidutils.logger.LoggingExceptionHandler;
import com.github.androidutils.logger.StartupLogWriter;
import com.github.androidutils.wakelock.WakeLockManager;

// @formatter:off
@ReportsCrashes(
        formKey = "",
        mailTo = "yuriy.kulikov.87@gmail.com",
        applicationLogFileLines = 150,
        customReportContent = {
                ReportField.IS_SILENT,
                ReportField.APP_VERSION_CODE,
                ReportField.PHONE_MODEL,
                ReportField.ANDROID_VERSION,
                ReportField.CUSTOM_DATA,
                ReportField.STACK_TRACE,
                ReportField.SHARED_PREFERENCES,
                })
// @formatter:on
public class AlarmApplication extends Application {

    @Override
    public void onCreate() {
        // The following line triggers the initialization of ACRA
        ACRA.init(this);
        DynamicThemeHandler.init(this);
        setTheme(DynamicThemeHandler.getInstance().getIdForName(DynamicThemeHandler.DEFAULT));

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
        logger.addLogWriter(StartupLogWriter.getInstance());
        LoggingExceptionHandler.addLoggingExceptionHandlerToAllThreads(logger);

        WakeLockManager.init(getApplicationContext(), new Logger(), true);
        AlarmsManager.init(getApplicationContext(), logger);

        ACRA.getErrorReporter().setExceptionHandlerInitializer(new ExceptionHandlerInitializer() {
            @Override
            public void initializeExceptionHandler(ErrorReporter reporter) {
                reporter.putCustomData("STARTUP_LOG", StartupLogWriter.getInstance().getMessagesAsString());
            }
        });

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        deleteLogs(logger, getApplicationContext());

        logger.d("onCreate");
        super.onCreate();
    }

    private void deleteLogs(Logger logger, Context context) {
        final File logFile = new File(context.getFilesDir(), "applog.log");
        if (logFile.exists()) {
            logFile.delete();
            logger.d("Deleted log file");
        } else {
            logger.d("Log file was already deleted");
        }
    }
}
