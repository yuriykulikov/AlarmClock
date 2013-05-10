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

import org.acra.ACRA;

import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;

import com.better.alarm.R;
import com.better.alarm.model.AlarmsManager;
import com.better.alarm.model.interfaces.Alarm;
import com.better.alarm.model.interfaces.AlarmNotFoundException;
import com.better.alarm.model.interfaces.IAlarmsManager;
import com.better.alarm.model.interfaces.Intents;
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

            if (action.equals(Intents.ALARM_ALERT_ACTION) || action.equals(Intents.ALARM_PREALARM_ACTION)) {
                // our alarm fired again, remove snooze notification
                nm.cancel(id + NOTIFICATION_OFFSET);
                onAlert(alarmsManager.getAlarm(id));

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
                alarmsManager.dismiss(alarmsManager.getAlarm(id));
            }
        } catch (AlarmNotFoundException e) {
            Logger.getDefaultLogger().e("oops", e);
            ACRA.getErrorReporter().handleSilentException(e);
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
        // keyguard.
        Class<? extends AlarmAlertFullScreen> c = AlarmAlert.class;
        KeyguardManager km = (KeyguardManager) mContext.getSystemService(Context.KEYGUARD_SERVICE);
        if (km.inKeyguardRestrictedInputMode()) {
            // Use the full screen activity for security.
            c = AlarmAlertFullScreen.class;
        }

        // Trigger a notification that, when clicked, will show the alarm
        // alert
        // dialog. No need to check for fullscreen since this will always be
        // launched from a user action.
        Intent notify = new Intent(mContext, AlarmAlert.class);
        notify.putExtra(Intents.EXTRA_ID, id);
        PendingIntent pendingNotify = PendingIntent.getActivity(mContext, id, notify, 0);

        // Use the alarm's label or the default label as the ticker text and
        // main text of the notification.
        String label = alarm.getLabelOrDefault(mContext);
        Notification n = new Notification(R.drawable.stat_notify_alarm, label, alarm.getNextTime().getTimeInMillis());
        n.setLatestEventInfo(mContext, label, mContext.getString(R.string.alarm_notify_text), pendingNotify);
        n.flags |= Notification.FLAG_SHOW_LIGHTS | Notification.FLAG_ONGOING_EVENT;
        n.defaults |= Notification.DEFAULT_LIGHTS;

        // NEW: Embed the full-screen UI here. The notification manager will
        // take care of displaying it if it's OK to do so.
        Intent alarmAlert = new Intent(mContext, c);
        alarmAlert.putExtra(Intents.EXTRA_ID, id);
        alarmAlert.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
        n.fullScreenIntent = PendingIntent.getActivity(mContext, id, alarmAlert, 0);

        // Send the notification using the alarm id to easily identify the
        // correct notification.
        nm.notify(id, n);
    }

    private void onSnoozed(int id) {
        // Get the display time for the snooze and update the notification.
        // Append (snoozed) to the label.
        String label = alarm.getLabelOrDefault(mContext);
        label = mContext.getString(R.string.alarm_notify_snooze_label, label);

        // Notify the user that the alarm has been snoozed.
        Intent cancelSnooze = new Intent(mContext, AlarmAlertReceiver.class);
        cancelSnooze.setAction(ACTION_CANCEL_NOTIFICATION);
        cancelSnooze.putExtra(Intents.EXTRA_ID, id);
        PendingIntent broadcast = PendingIntent.getBroadcast(mContext, id, cancelSnooze, 0);
        Notification n = new Notification(R.drawable.stat_notify_alarm, label, 0);

        n.setLatestEventInfo(mContext, label,
                mContext.getString(R.string.alarm_notify_snooze_text, formatTimeString()), broadcast);
        n.flags |= Notification.FLAG_AUTO_CANCEL | Notification.FLAG_ONGOING_EVENT;
        nm.notify(id + NOTIFICATION_OFFSET, n);
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
