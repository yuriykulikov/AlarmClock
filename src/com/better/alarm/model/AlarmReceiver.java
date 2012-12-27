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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.github.androidutils.logger.Logger;
import com.github.androidutils.wakelock.WakeLockManager;

/**
 * This receiver is a part of the model, but it has to be a separate class.
 * Application can be garbage collected, so we need to register a Receiver in
 * the manifest.
 * 
 * @author Yuriy
 * 
 */
public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        final String action = intent.getAction();
        Logger.getDefaultLogger().d("Forwarding to the service: " + action);
        Intent serviceIntent = new Intent(action);
        serviceIntent.putExtras(intent);
        WakeLockManager.getWakeLockManager().acquirePartialWakeLock(serviceIntent, "ForAlarmService");
        context.startService(serviceIntent);
    }
}
