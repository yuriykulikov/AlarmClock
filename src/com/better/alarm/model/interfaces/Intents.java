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
package com.better.alarm.model.interfaces;

public class Intents {
    /**
     * Broadcasted when an alarm fires.
     */
    public static final String ALARM_ALERT_ACTION = "com.better.alarm.ALARM_ALERT";

    /**
     * Broadcasted when an alarm fires.
     */
    public static final String ALARM_PREALARM_ACTION = "com.better.alarm.ALARM_PREALARM_ACTION";

    /**
     * Broadcasted when alarm is snoozed.
     */
    public static final String ALARM_SNOOZE_ACTION = "com.better.alarm.ALARM_SNOOZE";

    /**
     * Broadcasted when alarm is snoozed.
     */
    public static final String ACTION_CANCEL_SNOOZE = "com.better.alarm.ACTION_CANCEL_SNOOZE";

    /**
     * Broadcasted when alarm is dismissed.
     */
    public static final String ALARM_DISMISS_ACTION = "com.better.alarm.ALARM_DISMISS";

    /**
     * Broadcasted when alarm is scheduled
     */
    public static final String ACTION_SOUND_EXPIRED = "com.better.alarm.ACTION_SOUND_EXPIRED";

    /**
     * Broadcasted when alarm is scheduled
     */
    public static final String ACTION_ALARM_SCHEDULED = "com.better.alarm.model.Intents.ACTION_ALARM_SCHEDULED";

    /**
     * Broadcasted when alarm is scheduled
     */
    public static final String ACTION_ALARMS_UNSCHEDULED = "com.better.alarm.model.Intents.ACTION_ALARMS_UNSCHEDULED";

    /**
     * Broadcasted when alarm is scheduled
     */
    public static final String ACTION_ALARM_CHANGED = "com.better.alarm.model.Intents.ACTION_ALARM_CHANGED";

    /**
     * Broadcasted when alarm is set
     */
    public static final String ACTION_ALARM_SET = "com.better.alarm.model.Intents.ACTION_ALARM_SET";

    /**
     * Key of the AlarmCore attached as a parceble extra
     */
    public static final String EXTRA_ID = "intent.extra.alarm";

    public static final String ACTION_STOP_PREALARM_SAMPLE = "com.better.alarm.ACTION_STOP_PREALARM_SAMPLE";

    public static final String ACTION_START_PREALARM_SAMPLE = "com.better.alarm.ACTION_START_PREALARM_SAMPLE";

    public static final String ACTION_STOP_ALARM_SAMPLE = "com.better.alarm.ACTION_STOP_ALARM_SAMPLE";

    public static final String ACTION_START_ALARM_SAMPLE = "com.better.alarm.ACTION_START_ALARM_SAMPLE";

    public static final String ACTION_MUTE = "com.better.alarm.ACTION_MUTE";

    public static final String ACTION_DEMUTE = "com.better.alarm.ACTION_DEMUTE";

    public static final int MAX_ALARM_VOLUME = 10;

    public static final int MAX_PREALARM_VOLUME = 10;

    public static final int DEFAULT_PREALARM_VOLUME = 5;

    public static final int DEFAULT_ALARM_VOLUME = 10;

    public static final String KEY_PREALARM_VOLUME = "key_prealarm_volume";

    public static final String KEY_ALARM_VOLUME = "key_alarm_volume";

    /**
     * Broadcasted when someone wants to get a list of alarms.
     * 
     * @deprecated added as a draft
     */
    @Deprecated
    public static final String REQUEST_ALARMS_LIST = "com.better.alarm.model.interfaces.Intents.REQUEST_ALARMS_LIST";

    /**
     * Broadcasted when someone wants to get last scheduled alarm.
     */
    public static final String REQUEST_LAST_SCHEDULED_ALARM = "com.better.alarm.model.interfaces.Intents.REQUEST_LAST_SCHEDULED_ALARM";
}
