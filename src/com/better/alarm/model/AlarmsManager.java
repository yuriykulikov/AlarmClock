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

package com.better.alarm.model;

import android.content.Context;

/**
 * The AlarmsManager provider supplies info about AlarmCore Clock settings
 */
public class AlarmsManager {
    private static final String TAG = "AlarmsManager";
    private static final boolean DBG = true;

    private static Alarms sModelInstance;

    public static IAlarmsManager getAlarmsManager() {
        if (sModelInstance == null) throw new NullPointerException("AlarmsManager not initialized yet");
        return sModelInstance;
    }

    static Alarms getInstance() {
        if (sModelInstance == null) throw new NullPointerException("AlarmsManager not initialized yet");
        return sModelInstance;
    }

    static void init(Context context) {
        if (sModelInstance == null) {
            sModelInstance = new Alarms(context, new AlarmsScheduler(context));
        } else throw new RuntimeException("Attept to reinitialize!");
    }

    public static void reinit() {
        sModelInstance.reinit();
    }
}
