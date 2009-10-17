/*
 * Copyright (C) 2009 The Android Open Source Project
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
 
package com.android.deskclock;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

/**
 * BroadcastReceiver which receives {@link Intent#ACTION_DOCK_EVENT} events.
 * Launches the CarDockActivity if the device is placed into a car dock.
 *
 * TODO: This is the wrong way to launch, as this would cause contention
 * between multiple activities trying to launch if others did the same. Instead
 * register for a regular intent which should fire when placed into a car dock.
 */
public class DockEventReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {        
        Intent clockIntent = new Intent(Intent.ACTION_MAIN);
        clockIntent.setComponent(
                new ComponentName(context, AlarmClock.class));
        clockIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        
        String action = intent.getAction();
        if (Intent.ACTION_DOCK_EVENT.equals(action)) {
            // Code to control a sticky notification for the dock.
            /*
            NotificationManager notificationManager = (NotificationManager)
                    context.getSystemService(Context.NOTIFICATION_SERVICE);
            
            int dockState = intent.getIntExtra(Intent.EXTRA_DOCK_STATE, -1);
            if (dockState == Intent.EXTRA_DOCK_STATE_DESK) {
                Notification n = new Notification();
                n.icon = R.drawable.notification;
                n.defaults = Notification.DEFAULT_LIGHTS;
                n.flags = Notification.FLAG_ONGOING_EVENT;
                n.tickerText = context.getString(R.string.notification_title);
                n.when = 0;
                n.setLatestEventInfo(
                        context,
                        context.getString(R.string.notification_title),
                        context.getString(R.string.notification_text),
                        PendingIntent.getActivity(context, 0, clockIntent, 0));
                notificationManager.notify(0, n);
            } else if (dockState == Intent.EXTRA_DOCK_STATE_UNDOCKED) {
                notificationManager.cancelAll();
            }
            */
        } else if (android.provider.Telephony.Intents.SECRET_CODE_ACTION.equals(action)) {
            // The user dialed *#*#DESK#*#*
            context.startActivity(clockIntent);
        }
    }
}
