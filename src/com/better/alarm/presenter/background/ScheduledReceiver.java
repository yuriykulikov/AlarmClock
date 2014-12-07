/*
 * Copyright (C) 2012 Yuriy Kulikov yuriy.kulikov.87@gmail.com
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
package com.better.alarm.presenter.background;

import java.util.Calendar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.text.format.DateFormat;

import com.better.alarm.model.AlarmsManager;
import com.better.alarm.model.interfaces.Alarm;
import com.better.alarm.model.interfaces.AlarmNotFoundException;
import com.better.alarm.model.interfaces.Intents;
import com.github.androidutils.logger.Logger;

/**
 * This class reacts on {@link } and {@link } and
 * 
 * @author Yuriy
 * 
 */
public class ScheduledReceiver extends BroadcastReceiver {
    private static final String DM12 = "E h:mm aa";
    private static final String DM24 = "E kk:mm";

    @Override
    public void onReceive(Context context, Intent intent) {
        Logger.getDefaultLogger().d(intent.toString());
        if (intent.getAction().equals(Intents.ACTION_ALARM_SCHEDULED)) {
            int id = intent.getIntExtra(Intents.EXTRA_ID, -1);
            Alarm alarm;
            try {
                alarm = AlarmsManager.getAlarmsManager().getAlarm(id);
                // Broadcast intent for the notification bar
                Intent alarmChanged = new Intent("android.intent.action.ALARM_CHANGED");
                alarmChanged.putExtra("alarmSet", true);
                context.sendBroadcast(alarmChanged);

                // Update systems settings, so that interested Apps (like
                // KeyGuard)
                // will react accordingly
                String format = android.text.format.DateFormat.is24HourFormat(context) ? DM24 : DM12;
                Calendar calendar = Calendar.getInstance();
                long milliseconds = intent.getLongExtra(Intents.EXTRA_NEXT_NORMAL_TIME_IN_MILLIS, -1);
                calendar.setTimeInMillis(milliseconds);
                String timeString = (String) DateFormat.format(format, calendar);
                Settings.System.putString(context.getContentResolver(), Settings.System.NEXT_ALARM_FORMATTED,
                        timeString);
            } catch (AlarmNotFoundException e) {
                Logger.getDefaultLogger().d("Alarm not found");
            }

        } else if (intent.getAction().equals(Intents.ACTION_ALARMS_UNSCHEDULED)) {
            // Broadcast intent for the notification bar
            Intent alarmChanged = new Intent("android.intent.action.ALARM_CHANGED");
            alarmChanged.putExtra("alarmSet", false);
            context.sendBroadcast(alarmChanged);
            // Update systems settings, so that interested Apps (like KeyGuard)
            // will react accordingly
            Settings.System.putString(context.getContentResolver(), Settings.System.NEXT_ALARM_FORMATTED, "");
        }
    }

}
