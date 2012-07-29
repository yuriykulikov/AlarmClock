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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * This receiver is a part of the model, but it has to be a separate class.
 * Application can be garbage collected, so we need to register a Receiver in
 * the manifest.
 * 
 * @author Yuriy
 * 
 */
public class AlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "AlarmReceiver";
    private static final boolean DBG = true;
    private static final String ACTION_FIRED = "com.better.alarm.ACTION_FIRED";
    private static final String ACTION_SNOOZED_FIRED = "com.better.alarm.ACTION_SNOOZED_FIRED";
    private static final String ACTION_SOUND_EXPIRED = "com.better.alarm.ACTION_SOUND_EXPIRED";

    private static final String EXTRA_ID = "intent.extra.alarm";

    @Override
    public void onReceive(Context context, Intent intent) {
        int id = intent.getIntExtra(EXTRA_ID, -1);
        Alarm alarm = AlarmsManager.getInstance().getAlarm(id);

        if (alarm == null) {
            Log.wtf(TAG, "Failed to parse the alarm from the intent");
            return;
        }

        String action = intent.getAction();
        if (action.equals(ACTION_FIRED)) {
            AlarmsManager.getInstance().onAlarmFired(alarm);

        } else if (action.equals(ACTION_SNOOZED_FIRED)) {
            AlarmsManager.getInstance().onAlarmSnoozedFired(alarm);

        } else if (action.equals(ACTION_SOUND_EXPIRED)) {
            AlarmsManager.getInstance().onAlarmSoundExpired(alarm);

        }
    }

    /**
     * @param context
     */
    static void removeRTCAlarm(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_FIRED),
                PendingIntent.FLAG_CANCEL_CURRENT);
        am.cancel(sender);
    }

    /**
     * @param context
     * @param alarm
     * @param atTimeInMillis
     */
    static void setUpRTCAlarm(Context context, final Alarm alarm, final long atTimeInMillis) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if (DBG) Log.d(TAG, "** setAlert id " + alarm.getId() + " atTime " + atTimeInMillis);

        Intent intent = new Intent(ACTION_FIRED);
        intent.putExtra(EXTRA_ID, alarm.getId());
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        am.set(AlarmManager.RTC_WAKEUP, atTimeInMillis, sender);
    }

}
