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

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;

import com.better.alarm.model.compat.JobIntentService;
import com.better.alarm.model.interfaces.AlarmNotFoundException;
import com.better.alarm.model.interfaces.PresentationToModelIntents;
import com.github.androidutils.logger.Logger;
import com.github.androidutils.wakelock.WakeLockManager;

public class AlarmsService extends JobIntentService implements Handler.Callback {
    /**
     * TODO SM should report when it is done
     */
    private static final int WAKELOCK_HOLD_TIME = 3000;
    private static final int EVENT_RELEASE_WAKELOCK = 1;
    private static int JOB_ID = 1;
    Alarms alarms;
    private Logger log;
    private Handler handler;

    /**
     * Dispatches intents to the KlaxonService
     */
    public static class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            Logger.getDefaultLogger().d("Got intent " + intent.getAction());
            intent.setClass(context, AlarmsService.class);
            WakeLockManager.getWakeLockManager().acquirePartialWakeLock(intent, "AlarmsService");
            JobIntentService.enqueueWork(context, AlarmsService.class, JOB_ID, intent, 200);
        }
    }

    @Override
    public void onCreate() {
        alarms = AlarmsManager.getInstance();
        log = Logger.getDefaultLogger();
        handler = new Handler(this);
        super.onCreate();
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        try {
            String action = intent.getAction();
            if (action.equals(AlarmsScheduler.ACTION_FIRED)) {
                int id = intent.getIntExtra(AlarmsScheduler.EXTRA_ID, -1);

                AlarmCore alarm = alarms.getAlarm(id);
                alarms.onAlarmFired(alarm,
                        CalendarType.valueOf(intent.getExtras().getString(AlarmsScheduler.EXTRA_TYPE)));
                log.d("AlarmCore fired " + id);

            } else if (action.equals(Intent.ACTION_BOOT_COMPLETED) || action.equals(Intent.ACTION_TIMEZONE_CHANGED)
                    || action.equals(Intent.ACTION_LOCALE_CHANGED) || action.equals(Intent.ACTION_MY_PACKAGE_REPLACED)) {
                log.d("Refreshing alarms because of " + action);
                alarms.refresh();

            } else if (action.equals(Intent.ACTION_TIME_CHANGED)) {
                alarms.onTimeSet();

            } else if (action.equals(PresentationToModelIntents.ACTION_REQUEST_SNOOZE)) {
                int id = intent.getIntExtra(AlarmsScheduler.EXTRA_ID, -1);
                alarms.getAlarm(id).snooze();

            } else if (action.equals(PresentationToModelIntents.ACTION_REQUEST_DISMISS)) {
                int id = intent.getIntExtra(AlarmsScheduler.EXTRA_ID, -1);
                alarms.getAlarm(id).dismiss();

            }
        } catch (AlarmNotFoundException e) {
            Logger.getDefaultLogger().d("Alarm not found");
        }

        Message msg = handler.obtainMessage(EVENT_RELEASE_WAKELOCK);
        msg.obj = intent;
        handler.sendMessageDelayed(msg, WAKELOCK_HOLD_TIME);
    }

    @Override
    public boolean handleMessage(Message msg) {
        WakeLockManager.getWakeLockManager().releasePartialWakeLock((Intent) msg.obj);
        return true;
    }
}
