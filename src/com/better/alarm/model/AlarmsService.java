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
package com.better.alarm.model;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import com.github.androidutils.logger.Logger;
import com.github.androidutils.wakelock.WakeLockManager;

public class AlarmsService extends Service {
    Alarms alarms;
    private Logger log;

    /**
     * Dispatches intents to the KlaxonService
     */
    public static class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            intent.setClass(context, AlarmsService.class);
            WakeLockManager.getWakeLockManager().acquirePartialWakeLock(intent, "AlarmsService");
            context.startService(intent);
        }
    }

    @Override
    public void onCreate() {
        alarms = AlarmsManager.getInstance();
        log = Logger.getDefaultLogger();
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        if (action.equals(AlarmsScheduler.ACTION_FIRED)) {
            int id = intent.getIntExtra(AlarmsScheduler.EXTRA_ID, -1);
            log.d("AlarmCore fired " + id);
            alarms.onAlarmFired(id, CalendarType.valueOf(intent.getExtras().getString(AlarmsScheduler.EXTRA_TYPE)));

        } else if (action.equals(Intent.ACTION_BOOT_COMPLETED) || action.equals(Intent.ACTION_TIMEZONE_CHANGED)
                || action.equals(Intent.ACTION_LOCALE_CHANGED) || action.equals(Intent.ACTION_TIME_CHANGED)) {
            log.d("Refreshing alarms because of " + action);
            alarms.refresh();
        }

        WakeLockManager.getWakeLockManager().releasePartialWakeLock(intent);

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
