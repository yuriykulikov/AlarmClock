package com.better.alarm.presenter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.text.format.DateFormat;

import com.better.alarm.model.Alarm;
import com.better.alarm.model.Intents;

/**
 * This class reacts on {@link } and {@link } and 
 * @author Yuriy
 *
 */
public class ScheduledReceiver extends BroadcastReceiver {
    private static final String DM12 = "E h:mm aa";
    private static final String DM24 = "E kk:mm";

    @Override
    public void onReceive(Context context, Intent intent) {
        
        if (intent.getAction().equals(Intents.ACTION_ALARM_SCHEDULED)) {
            Alarm alarm = intent.getParcelableExtra(Intents.ALARM_INTENT_EXTRA);

            // Broadcast intent for the notification bar
            Intent alarmChanged = new Intent("android.intent.action.ALARM_CHANGED");
            alarmChanged.putExtra("alarmSet", true);
            context.sendBroadcast(alarmChanged);

            // Update systems settings, so that interested Apps (like KeyGuard) will react accordingly
            String format = android.text.format.DateFormat.is24HourFormat(context) ? DM24 : DM12;
            String timeString = (String) DateFormat.format(format, alarm.calculateCalendar());
            Settings.System.putString(context.getContentResolver(), Settings.System.NEXT_ALARM_FORMATTED, timeString);
           
        } else if (intent.getAction().equals(Intents.ACTION_ALARMS_UNSCHEDULED)) {
            // Broadcast intent for the notification bar
            Intent alarmChanged = new Intent("android.intent.action.ALARM_CHANGED");
            alarmChanged.putExtra("alarmSet", false);
            context.sendBroadcast(alarmChanged);
            // Update systems settings, so that interested Apps (like KeyGuard) will react accordingly
            Settings.System.putString(context.getContentResolver(), Settings.System.NEXT_ALARM_FORMATTED, "");
        }
    }

}
