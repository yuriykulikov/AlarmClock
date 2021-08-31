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
package com.better.alarm.interfaces;

import com.better.alarm.BuildConfig;

public interface Intents {
  /** Broadcasted when an alarm fires. */
  public static final String ALARM_ALERT_ACTION = BuildConfig.APPLICATION_ID + ".ALARM_ALERT";

  /** Broadcasted when an alarm fires. */
  public static final String ALARM_PREALARM_ACTION =
      BuildConfig.APPLICATION_ID + ".ALARM_PREALARM_ACTION";

  /** Broadcasted when alarm is snoozed. */
  public static final String ALARM_SNOOZE_ACTION = BuildConfig.APPLICATION_ID + ".ALARM_SNOOZE";

  /** Broadcasted when alarm is snoozed. */
  public static final String ACTION_CANCEL_SNOOZE =
      BuildConfig.APPLICATION_ID + ".ACTION_CANCEL_SNOOZE";

  /** Broadcasted when alarm is dismissed. */
  public static final String ALARM_DISMISS_ACTION = BuildConfig.APPLICATION_ID + ".ALARM_DISMISS";

  /** Broadcasted when alarm is scheduled */
  public static final String ACTION_SOUND_EXPIRED =
      BuildConfig.APPLICATION_ID + ".ACTION_SOUND_EXPIRED";

  /** Key of the AlarmCore attached as a parceble extra */
  public static final String EXTRA_ID = "intent.extra.alarm";

  public static final String ACTION_MUTE = BuildConfig.APPLICATION_ID + ".ACTION_MUTE";

  public static final String ACTION_DEMUTE = BuildConfig.APPLICATION_ID + ".ACTION_DEMUTE";

  public static final String ALARM_SHOW_SKIP = BuildConfig.APPLICATION_ID + ".ALARM_SHOW_SKIP";

  public static final String ALARM_REMOVE_SKIP = BuildConfig.APPLICATION_ID + ".ALARM_REMOVE_SKIP";
}
