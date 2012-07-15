package com.better.alarm.model;

import java.util.Calendar;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.better.alarm.Log;
import com.better.alarm.R;
import com.better.alarm.model.AlarmsManager;
import com.better.alarm.presenter.AlarmAlertFullScreen;
import com.better.alarm.presenter.AlarmReceiver;
import com.better.alarm.presenter.SettingsActivity;

public class Temp {
    /**
     * Kills alarm audio after ALARM_TIMEOUT_SECONDS, so the alarm
     * won't run all day.
     *
     * This just cancels the audio, but leaves the notification
     * popped, so the user will know that the alarm tripped.
     */
    private void enableKiller(Alarm alarm) {
        final String autoSnooze =
                PreferenceManager.getDefaultSharedPreferences(this)
                .getString(SettingsActivity.KEY_AUTO_SILENCE,
                        DEFAULT_ALARM_TIMEOUT);
        int autoSnoozeMinutes = Integer.parseInt(autoSnooze);
        if (autoSnoozeMinutes != -1) {
            mHandler.sendMessageDelayed(mHandler.obtainMessage(KILLER, alarm),
                    1000 * autoSnoozeMinutes * 60);
        }
    }
}
