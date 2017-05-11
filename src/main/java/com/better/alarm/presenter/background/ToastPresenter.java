/*
 * Copyright (C) 2008 The Android Open Source Project
 * Copyright (C) 2013 Yuriy Kulikov
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

package com.better.alarm.presenter.background;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.better.alarm.AlarmApplication;
import com.better.alarm.R;
import com.better.alarm.model.interfaces.Alarm;
import com.better.alarm.model.interfaces.Intents;
import com.github.androidutils.logger.Logger;

public class ToastPresenter extends BroadcastReceiver {

    private static Toast sToast = null;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (Intents.ACTION_ALARM_SET.equals(action)) {
            Alarm alarm;
            int id = intent.getIntExtra(Intents.EXTRA_ID, -1);
            try {
                alarm = AlarmApplication.alarms().getAlarm(id);
                if (alarm.isEnabled()) {
                    popAlarmSetToast(context, alarm, intent);
                } else {
                    Logger.getDefaultLogger().w("Alarm " + id + " is already disabled");
                }
            } catch (Exception e) {
                Logger.getDefaultLogger().w("Alarm " + id + " could not be found. Must be deleted");
            }
        }
    }

    public static void setToast(Toast toast) {
        if (sToast != null) {
            sToast.cancel();
        }
        sToast = toast;
    }

    public static void cancelToast() {
        if (sToast != null) {
            sToast.cancel();
        }
        sToast = null;
    }

    static void popAlarmSetToast(Context context, Alarm alarm, Intent intent) {
        String toastText;
        long timeInMillis = intent.getLongExtra(Intents.EXTRA_NEXT_NORMAL_TIME_IN_MILLIS, -1);
        toastText = formatToast(context, timeInMillis);
        Toast toast = Toast.makeText(context, toastText, Toast.LENGTH_LONG);
        ToastPresenter.setToast(toast);
        toast.show();
    }

    /**
     * format "Alarm set for 2 days 7 hours and 53 minutes from now"
     * 
     * If prealarm is on it will be
     * 
     * "Alarm set for 2 days 7 hours and 53 minutes from now. Prealarm will
     * start 30 minutes before the main alarm".
     */
    static String formatToast(Context context, long timeInMillis) {
        long delta = timeInMillis - System.currentTimeMillis();
        long hours = delta / (1000 * 60 * 60);
        long minutes = delta / (1000 * 60) % 60;
        long days = hours / 24;
        hours = hours % 24;

        String daySeq = days == 0 ? "" : days == 1 ? context.getString(R.string.day) : context.getString(R.string.days,
                Long.toString(days));

        String minSeq = minutes == 0 ? "" : minutes == 1 ? context.getString(R.string.minute) : context.getString(
                R.string.minutes, Long.toString(minutes));

        String hourSeq = hours == 0 ? "" : hours == 1 ? context.getString(R.string.hour) : context.getString(
                R.string.hours, Long.toString(hours));

        boolean dispDays = days > 0;
        boolean dispHour = hours > 0;
        boolean dispMinute = minutes > 0;

        int index = (dispDays ? 1 : 0) | (dispHour ? 2 : 0) | (dispMinute ? 4 : 0);

        String[] formats = context.getResources().getStringArray(R.array.alarm_set);
        return String.format(formats[index], daySeq, hourSeq, minSeq);
    }
}
