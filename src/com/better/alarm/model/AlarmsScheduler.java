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

import java.util.Calendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

class AlarmsScheduler implements IAlarmsScheduler {
    private static final String TAG = "AlarmsScheduler";
    private static final boolean DBG = true;
    static final String ACTION_FIRED = "com.better.alarm.ACTION_FIRED";
    static final String ACTION_SNOOZED_FIRED = "com.better.alarm.ACTION_SNOOZED_FIRED";
    static final String ACTION_SOUND_EXPIRED = "com.better.alarm.ACTION_SOUND_EXPIRED";
    static final String EXTRA_ID = "intent.extra.alarm";
    private Context mContext;

    AlarmsScheduler(Context context) {
        mContext = context;
    }

    public void removeRTCAlarm() {
        AlarmManager am = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        PendingIntent sender = PendingIntent.getBroadcast(mContext, 0, new Intent(ACTION_FIRED),
                PendingIntent.FLAG_CANCEL_CURRENT);
        am.cancel(sender);
    }

    public void setUpRTCAlarm(final Alarm alarm, Calendar calendar) {
        AlarmManager am = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        if (DBG) Log.d(TAG, "Set alarm " + alarm.getId() + " on " + calendar.getTime().toLocaleString());

        if (DBG && calendar.before(Calendar.getInstance())) {
            throw new RuntimeException("Attempt to schedule alarm in the past: " + calendar.getTime().toLocaleString());
        }

        Intent intent = new Intent(ACTION_FIRED);
        intent.putExtra(EXTRA_ID, alarm.getId());
        PendingIntent sender = PendingIntent.getBroadcast(mContext, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        am.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), sender);
    }
}
