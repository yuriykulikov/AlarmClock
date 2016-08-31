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

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.AlarmManager.AlarmClockInfo;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.text.format.DateFormat;

import com.better.alarm.model.interfaces.Intents;
import com.better.alarm.presenter.AlarmsListActivity;
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
    private static final Intent FAKE_INTENT_JUST_TO_DISPLAY_IN_ICON = new Intent("FAKE_ACTION_JUST_TO_DISPLAY_AN_ICON");

    @Override
    public void onReceive(Context context, Intent intent) {
        Logger.getDefaultLogger().d(intent.toString());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            doForLollipop(context, intent);
        } else {
            doForPreLollipop(context, intent);
        }
    }

    private void doForPreLollipop(Context context, Intent intent) {
        if (intent.getAction().equals(Intents.ACTION_ALARM_SCHEDULED)) {
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
            Settings.System.putString(context.getContentResolver(), Settings.System.NEXT_ALARM_FORMATTED, timeString);
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

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void doForLollipop(Context context, Intent intent) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (intent.getAction().equals(Intents.ACTION_ALARM_SCHEDULED)) {
            int id = intent.getIntExtra(Intents.EXTRA_ID, -1);

            Intent showList = new Intent(context, AlarmsListActivity.class);
            showList.putExtra(Intents.EXTRA_ID, id);
            PendingIntent showIntent = PendingIntent.getActivity(context, id, showList, 0);

            long milliseconds = intent.getLongExtra(Intents.EXTRA_NEXT_NORMAL_TIME_IN_MILLIS, -1);
            am.setAlarmClock(new AlarmClockInfo(milliseconds, showIntent),
                    PendingIntent.getBroadcast(context, 0, FAKE_INTENT_JUST_TO_DISPLAY_IN_ICON, 0));

        } else if (intent.getAction().equals(Intents.ACTION_ALARMS_UNSCHEDULED)) {
            am.cancel(PendingIntent.getBroadcast(context, 0, FAKE_INTENT_JUST_TO_DISPLAY_IN_ICON, 0));
        }
    }
}
