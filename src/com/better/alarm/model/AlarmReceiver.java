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

import com.better.wakelock.WakeLockManager;

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

    @Override
    public void onReceive(Context context, Intent intent) {

        final String action = intent.getAction();
        if (DBG) Log.d(TAG, "Forwarding to the service: " + action);
        Intent serviceIntent = new Intent(action);
        serviceIntent.putExtras(intent);
        WakeLockManager.getWakeLockManager().acquirePartialWakeLock(serviceIntent, "ForAlarmService");
        context.startService(serviceIntent);
    }
}
