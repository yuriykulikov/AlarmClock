/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.better.alarm.presenter.alert;

import java.util.Calendar;

import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.text.format.DateFormat;

import com.better.alarm.R;
import com.better.alarm.model.AlarmsManager;
import com.better.alarm.model.interfaces.Alarm;
import com.better.alarm.model.interfaces.AlarmNotFoundException;
import com.better.alarm.model.interfaces.IAlarmsManager;
import com.better.alarm.model.interfaces.Intents;
import com.better.alarm.model.interfaces.PresentationToModelIntents;
import com.better.alarm.presenter.TransparentActivity;
import com.github.androidutils.logger.Logger;

/**
 * Glue class: connects AlarmAlert IntentReceiver to AlarmAlert activity. Passes
 * through Alarm ID.
 */
public class AlarmAlertReceiver extends BroadcastReceiver {

    private static final String ACTION_CANCEL_NOTIFICATION = "AlarmAlertReceiver.ACTION_CANCEL_NOTIFICATION";
    private static final String DM12 = "E h:mm aa";
    private static final String DM24 = "E kk:mm";
    private static final int NOTIFICATION_OFFSET = 1000;
    Context mContext;
    NotificationManager nm;
    IAlarmsManager alarmsManager;
    Alarm alarm;

    @Override
    public void onReceive(final Context context, final Intent intent) {
        mContext = context;
        alarmsManager = AlarmsManager.getAlarmsManager();
        nm = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        String action = intent.getAction();
        int id = intent.getIntExtra(Intents.EXTRA_ID, -1);
        try {
            if (id != -1) {
                alarm = alarmsManager.getAlarm(id);
            }

            if (action.equals(Intents.ALARM_ALERT_ACTION) || action.equals(Intents.ALARM_PREALARM_ACTION)) {
                // our alarm fired again, remove snooze notification
                nm.cancel(id + NOTIFICATION_OFFSET);
                onAlert(alarm);

            } else if (action.equals(Intents.ALARM_DISMISS_ACTION)) {
                nm.cancel(id);
                nm.cancel(id + NOTIFICATION_OFFSET);

            } else if (action.equals(Intents.ACTION_CANCEL_SNOOZE)) {
                nm.cancel(id);
                nm.cancel(id + NOTIFICATION_OFFSET);

            } else if (action.equals(Intents.ALARM_SNOOZE_ACTION)) {
                nm.cancel(id);
                onSnoozed(id);

            } else if (action.equals(Intents.ACTION_SOUND_EXPIRED)) {
                onSoundExpired(id);

            } else if (action.equals(ACTION_CANCEL_NOTIFICATION)) {
                alarmsManager.dismiss(alarm);
            }
        } catch (AlarmNotFoundException e) {
            Logger.getDefaultLogger().d("Alarm not found");
            nm.cancel(id);
            nm.cancel(id + NOTIFICATION_OFFSET);
        }
    }

    private void onAlert(Alarm alarm) {
        int id = alarm.getId();

        /* Close dialogs and window shade */
        Intent closeDialogs = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        mContext.sendBroadcast(closeDialogs);

        // Decide which activity to start based on the state of the
        // keyguard - is the screen locked or not.
        Class<? extends AlarmAlertFullScreen> c = AlarmAlert.class;
        KeyguardManager km = (KeyguardManager) mContext.getSystemService(Context.KEYGUARD_SERVICE);
        if (km.inKeyguardRestrictedInputMode()) {
            // Use the full screen activity to unlock the screen.
            c = AlarmAlertFullScreen.class;
        }

        // Trigger a notification that, when clicked, will show the alarm
        // alert dialog. No need to check for fullscreen since this will always
        // be launched from a user action.
        Intent notify = new Intent(mContext, c);
        notify.putExtra(Intents.EXTRA_ID, id);
        PendingIntent pendingNotify = PendingIntent.getActivity(mContext, id, notify, 0);
        PendingIntent pendingSnooze = PresentationToModelIntents.createPendingIntent(mContext,
                PresentationToModelIntents.ACTION_REQUEST_SNOOZE, id);
        PendingIntent pendingDismiss = PresentationToModelIntents.createPendingIntent(mContext,
                PresentationToModelIntents.ACTION_REQUEST_DISMISS, id);

        // When button Reschedule is clicked, the TransparentActivity with
        // TimePickerFragment to set new alarm time is launched
        Intent reschedule = new Intent(mContext, TransparentActivity.class);
        reschedule.putExtra(Intents.EXTRA_ID, id);
        PendingIntent pendingReschedule = PendingIntent.getActivity(mContext, id, reschedule, 0);

        //@formatter:off
        Notification status = new NotificationCompat.Builder(mContext)
                .setContentTitle(alarm.getLabelOrDefault(mContext))
                .setContentText(mContext.getString(R.string.alarm_notify_text))
                .setSmallIcon(R.drawable.stat_notify_alarm)
                // setFullScreenIntent to show the user AlarmAlert dialog at the same time 
                // when the Notification Bar was created.
                .setFullScreenIntent(pendingNotify, true)
                // setContentIntent to show the user AlarmAlert dialog  
                // when he will click on the Notification Bar.
                .setContentIntent(pendingNotify)
                .setOngoing(true)
                .addAction(R.drawable.ic_action_snooze, mContext.getString(R.string.alarm_alert_snooze_text), pendingSnooze)
                .addAction(R.drawable.ic_action_reschedule_snooze, mContext.getString(R.string.alarm_alert_reschedule_text), pendingReschedule)
                .addAction(R.drawable.ic_action_dismiss, mContext.getString(R.string.alarm_alert_dismiss_text), pendingDismiss)
                .setDefaults(Notification.DEFAULT_LIGHTS)
                .build();
        //@formatter:on

        // Send the notification using the alarm id to easily identify the
        // correct notification.
        nm.notify(id, status);
    }

    private void onSnoozed(int id) {

        // What to do, when a user clicks on the notification bar
        Intent cancelSnooze = new Intent(mContext, AlarmAlertReceiver.class);
        cancelSnooze.setAction(ACTION_CANCEL_NOTIFICATION);
        cancelSnooze.putExtra(Intents.EXTRA_ID, id);
        PendingIntent pCancelSnooze = PendingIntent.getBroadcast(mContext, id, cancelSnooze, 0);

        // When button Reschedule is clicked, the TransparentActivity with
        // TimePickerFragment to set new alarm time is launched
        Intent reschedule = new Intent(mContext, TransparentActivity.class);
        reschedule.putExtra(Intents.EXTRA_ID, id);
        PendingIntent pendingReschedule = PendingIntent.getActivity(mContext, id, reschedule, 0);

        PendingIntent pendingDismiss = PresentationToModelIntents.createPendingIntent(mContext,
                PresentationToModelIntents.ACTION_REQUEST_DISMISS, id);

        String label = alarm.getLabelOrDefault(mContext);

        //@formatter:off
        Notification status = new NotificationCompat.Builder(mContext)
                // Get the display time for the snooze and update the notification.
                .setContentTitle(mContext.getString(R.string.alarm_notify_snooze_label, label))
                .setContentText(mContext.getString(R.string.alarm_notify_snooze_text, formatTimeString()))
                .setSmallIcon(R.drawable.stat_notify_alarm)
                .setContentIntent(pCancelSnooze)
                .setOngoing(true)
                .addAction(R.drawable.ic_action_reschedule_snooze, mContext.getString(R.string.alarm_alert_reschedule_text), pendingReschedule)
                .addAction(R.drawable.ic_action_dismiss, mContext.getString(R.string.alarm_alert_dismiss_text), pendingDismiss)
                .setDefaults(Notification.DEFAULT_LIGHTS)
                .build();
        //@formatter:on

        // Send the notification using the alarm id to easily identify the
        // correct notification.
        nm.notify(id + NOTIFICATION_OFFSET, status);
    }

    private String formatTimeString() {
        String format = android.text.format.DateFormat.is24HourFormat(mContext) ? DM24 : DM12;
        Calendar calendar = alarm.getSnoozedTime();
        String timeString = (String) DateFormat.format(format, calendar);
        return timeString;
    }

    private void onSoundExpired(int id) {
        Intent dismissAlarm = new Intent(mContext, AlarmAlertReceiver.class);
        dismissAlarm.setAction(ACTION_CANCEL_NOTIFICATION);
        dismissAlarm.putExtra(Intents.EXTRA_ID, id);
        PendingIntent intent = PendingIntent.getBroadcast(mContext, id, dismissAlarm, 0);
        // Update the notification to indicate that the alert has been
        // silenced.
        String label = alarm.getLabelOrDefault(mContext);
        int autoSilenceMinutes = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(mContext).getString(
                "auto_silence", "10"));
        String text = mContext.getString(R.string.alarm_alert_alert_silenced, autoSilenceMinutes);

        Notification.Builder nb = new Notification.Builder(mContext);
        nb.setAutoCancel(true);
        nb.setSmallIcon(R.drawable.stat_notify_alarm);
        nb.setWhen(Calendar.getInstance().getTimeInMillis());
        nb.setContentIntent(intent);
        nb.setContentTitle(label);
        nb.setContentText(text);
        nb.setTicker(text);
        Notification n = nb.getNotification();

        // We have to cancel the original notification since it is in the
        // ongoing section and we want the "killed" notification to be a plain
        // notification.
        nm.cancel(id);
        nm.notify(id, n);
    }
}
