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

package com.better.alarm.presenter;

import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.better.alarm.R;
import com.better.alarm.model.Alarm;
import com.better.alarm.model.AlarmsManager;
import com.better.alarm.model.Intents;

/**
 * Glue class: connects AlarmAlert IntentReceiver to AlarmAlert activity. Passes
 * through Alarm ID.
 */
public class AlarmAlertReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, final Intent intent) {
        String action = intent.getAction();
        int id = intent.getIntExtra(Intents.EXTRA_ID, -1);

        if (action.equals(Intents.ALARM_ALERT_ACTION)) {
            /* Close dialogs and window shade */
            Intent closeDialogs = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
            context.sendBroadcast(closeDialogs);

            // Decide which activity to start based on the state of the
            // keyguard.
            Class<? extends AlarmAlertFullScreen> c = AlarmAlert.class;
            KeyguardManager km = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
            if (km.inKeyguardRestrictedInputMode()) {
                // Use the full screen activity for security.
                c = AlarmAlertFullScreen.class;
            }

            // Trigger a notification that, when clicked, will show the alarm
            // alert
            // dialog. No need to check for fullscreen since this will always be
            // launched from a user action.
            Intent notify = new Intent(context, AlarmAlert.class);
            notify.putExtra(Intents.EXTRA_ID, id);
            PendingIntent pendingNotify = PendingIntent.getActivity(context, id, notify, 0);

            Alarm alarm = AlarmsManager.getAlarmsManager().getAlarm(id);
            // Use the alarm's label or the default label as the ticker text and
            // main text of the notification.
            String label = alarm.getLabelOrDefault(context);
            Notification n = new Notification(R.drawable.stat_notify_alarm, label, alarm.getNextTime().getTimeInMillis());
            n.setLatestEventInfo(context, label, context.getString(R.string.alarm_notify_text), pendingNotify);
            n.flags |= Notification.FLAG_SHOW_LIGHTS | Notification.FLAG_ONGOING_EVENT;
            n.defaults |= Notification.DEFAULT_LIGHTS;

            // NEW: Embed the full-screen UI here. The notification manager will
            // take care of displaying it if it's OK to do so.
            Intent alarmAlert = new Intent(context, c);
            alarmAlert.putExtra(Intents.EXTRA_ID, id);
            alarmAlert.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
            n.fullScreenIntent = PendingIntent.getActivity(context, id, alarmAlert, 0);

            // Send the notification using the alarm id to easily identify the
            // correct notification.
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            nm.notify(id, n);

        } else if (action.equals(Intents.ALARM_DISMISS_ACTION) || action.equals(Intents.ALARM_SNOOZE_ACTION)) {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            nm.cancel(id);
        }
    }
}
