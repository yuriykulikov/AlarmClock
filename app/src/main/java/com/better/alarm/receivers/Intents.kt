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
package com.better.alarm.receivers

import com.better.alarm.BuildConfig

object Intents {
  /** Alarm fires */
  const val SNOOZE_ALARM_ALERT_ACTION = BuildConfig.APPLICATION_ID + ".SNOOZE_ALARM_ALERT"

  /** Alarm fires */
  const val CHECK_ALARM_ALERT_ACTION = BuildConfig.APPLICATION_ID + ".CHECK_ALARM_ALERT"

  /** Alarm fires */
  const val ALARM_ALERT_ACTION = BuildConfig.APPLICATION_ID + ".ALARM_ALERT"

  /** Alarm fires */
  const val ALARM_ALERT_PAUSE_ACTION = BuildConfig.APPLICATION_ID + ".ALARM_ALERT_PAUSE"

  /** Alarm fires */
  const val ALARM_ALERT_RESUME_ACTION = BuildConfig.APPLICATION_ID + ".ALARM_ALERT_RESUME"

  /** Alarm fires */
  const val ALARM_ALERT_START_WAKING_ACTION = BuildConfig.APPLICATION_ID + ".ALARM_ALERT_START_WAKING"

  /** Alarm fires */
  const val ALARM_PREALARM_ACTION = BuildConfig.APPLICATION_ID + ".ALARM_PREALARM_ACTION"

  /** Alarm is snoozed */
  const val ALARM_SNOOZE_ACTION = BuildConfig.APPLICATION_ID + ".ALARM_SNOOZE"

  /** Alarm is check*/
  const val ALARM_CHECK_ACTION = BuildConfig.APPLICATION_ID + ".ALARM_CHECK"

  /** Cancel a snoozed alarm */
  const val ACTION_CANCEL_SNOOZE = BuildConfig.APPLICATION_ID + ".ACTION_CANCEL_SNOOZE"

  /** Cancel a check alarm */
  const val ACTION_CANCEL_CHECK = BuildConfig.APPLICATION_ID + ".ACTION_CANCEL_CHECK"

  /** Alarm is dismissed */
  const val ALARM_DISMISS_ACTION = BuildConfig.APPLICATION_ID + ".ALARM_DISMISS"

  /** Alarm sound expired */
  const val ACTION_SOUND_EXPIRED = BuildConfig.APPLICATION_ID + ".ACTION_SOUND_EXPIRED"

  const val EXTRA_ID = "intent.extra.alarm"
  const val EXTRA_TYPE = "intent.extra.type"
  const val TYPE_NORMAL_ALARM= "NORMAL_ALARM"
  const val TYPE_SNOOZE_ALARM= "SNOOZE_ALARM"
  const val TYPE_CHECK_ALARM= "CHECK_ALARM"
  const val ACTION_MUTE = BuildConfig.APPLICATION_ID + ".ACTION_MUTE"
  const val ACTION_DEMUTE = BuildConfig.APPLICATION_ID + ".ACTION_DEMUTE"
  const val ALARM_SHOW_SKIP = BuildConfig.APPLICATION_ID + ".ALARM_SHOW_SKIP"
  const val ALARM_REMOVE_SKIP = BuildConfig.APPLICATION_ID + ".ALARM_REMOVE_SKIP"
}
