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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.better.alarm.model.Intents;
import com.github.androidutils.wakelock.WakeLockManager;

/**
 * Dispatches intents to the KlaxonService
 */
public class AlarmKlaxonReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, final Intent intent) {
        int id = intent.getIntExtra(Intents.EXTRA_ID, -1);

        // Dispatch intent to the service
        Intent playAlarm = new Intent(intent.getAction());
        playAlarm.putExtra(Intents.EXTRA_ID, id);
        WakeLockManager.getWakeLockManager().acquirePartialWakeLock(playAlarm, "ForAlarmKlaxonService");
        context.startService(playAlarm);
    }
}
