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
import android.content.Intent;
import android.os.IBinder;

public class AlarmsService extends Service {
    private static final String TAG = "AlarmsService";
    private static final boolean DBG = true;

    Alarms alarms;

    @Override
    public void onCreate() {
        alarms = AlarmsManager.getInstance();
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        if (action.equals(AlarmsScheduler.ACTION_FIRED)) {
            int id = intent.getIntExtra(AlarmsScheduler.EXTRA_ID, -1);
            alarms.onAlarmFired(id);

        } else if (action.equals(AlarmsScheduler.ACTION_SNOOZED_FIRED)) {
            int id = intent.getIntExtra(AlarmsScheduler.EXTRA_ID, -1);
            alarms.onAlarmSnoozedFired(id);

        } else if (action.equals(AlarmsScheduler.ACTION_SOUND_EXPIRED)) {
            int id = intent.getIntExtra(AlarmsScheduler.EXTRA_ID, -1);
            alarms.onAlarmSoundExpired(id);

        } else if (action.equals(Intent.ACTION_TIMEZONE_CHANGED) || action.equals(Intent.ACTION_TIME_CHANGED)
                || action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            AlarmsManager.init(getApplicationContext());

        } else if (action.equals(Intent.ACTION_LOCALE_CHANGED)) {
            // TODO
        }
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
