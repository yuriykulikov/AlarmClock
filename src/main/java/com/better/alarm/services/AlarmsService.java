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
package com.better.alarm.services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

import com.better.alarm.AlarmApplication;
import com.better.alarm.interfaces.PresentationToModelIntents;
import com.better.alarm.logger.Logger;
import com.better.alarm.model.AlarmCore;
import com.better.alarm.model.Alarms;
import com.better.alarm.model.AlarmsScheduler;
import com.better.alarm.model.CalendarType;
import com.google.inject.Inject;

public class AlarmsService extends Service {
    /**
     * TODO SM should report when it is done
     */
    private static final int WAKELOCK_HOLD_TIME = 5000;
    private static final int EVENT_RELEASE_WAKELOCK = 1;
    @Inject
    private Alarms alarms;
    @Inject
    private Logger log;

    private Handler handler;

    /**
     * Dispatches intents to the KlaxonService
     */
    public static class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            intent.setClass(context, AlarmsService.class);
            AlarmApplication.wakeLocks().acquirePartialWakeLock(intent, "AlarmsService");
            context.startService(intent);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                AlarmApplication.wakeLocks().releasePartialWakeLock((Intent) msg.obj);
                return true;
            }
        });
        AlarmApplication.guice().injectMembers(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            String action = intent.getAction();
            if (action.equals(AlarmsScheduler.ACTION_FIRED)) {
                int id = intent.getIntExtra(AlarmsScheduler.EXTRA_ID, -1);

                AlarmCore alarm = alarms.getAlarm(id);
                alarms.onAlarmFired(alarm,
                        CalendarType.valueOf(intent.getExtras().getString(AlarmsScheduler.EXTRA_TYPE)));
                log.d("AlarmCore fired " + id);

            } else if (action.equals(Intent.ACTION_BOOT_COMPLETED) || action.equals(Intent.ACTION_TIMEZONE_CHANGED)
                    || action.equals(Intent.ACTION_LOCALE_CHANGED)) {
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
        } catch (Exception e) {
            Logger.getDefaultLogger().d("Alarm not found");
        }

        Message msg = handler.obtainMessage(EVENT_RELEASE_WAKELOCK);
        msg.obj = intent;
        handler.sendMessageDelayed(msg, WAKELOCK_HOLD_TIME);

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
