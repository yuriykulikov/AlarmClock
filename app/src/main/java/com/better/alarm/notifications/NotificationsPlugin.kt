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

package com.better.alarm.notifications

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.better.alarm.R
import com.better.alarm.logger.Logger
import com.better.alarm.platform.isOreo
import com.better.alarm.platform.pendingIntentUpdateCurrentFlag
import com.better.alarm.receivers.Intents
import com.better.alarm.receivers.PresentationToModelIntents
import com.better.alarm.services.EnclosingService
import com.better.alarm.services.PluginAlarmData
import com.better.alarm.ui.alert.AlarmAlertFullScreen

/**
 * Glue class: connects AlarmAlert IntentReceiver to AlarmAlert activity. Passes through Alarm ID.
 */
class NotificationsPlugin(
    private val logger: Logger,
    private val mContext: Context,
    private val nm: NotificationManager,
    private val enclosingService: EnclosingService
) {
  fun show(alarm: PluginAlarmData, index: Int, startForeground: Boolean) {
    // Trigger a notification that, when clicked, will show the alarm
    // alert dialog. No need to check for fullscreen since this will always
    // be launched from a user action.
    val notify = Intent(mContext, AlarmAlertFullScreen::class.java)
    notify.putExtra(Intents.EXTRA_ID, alarm.id)
    val pendingNotify =
        PendingIntent.getActivity(mContext, alarm.id, notify, pendingIntentUpdateCurrentFlag())
    val pendingSnooze =
        PresentationToModelIntents.createPendingIntent(
            mContext, PresentationToModelIntents.ACTION_REQUEST_SNOOZE, alarm.id)
    val pendingDismiss =
        PresentationToModelIntents.createPendingIntent(
            mContext, PresentationToModelIntents.ACTION_REQUEST_DISMISS, alarm.id)

    val notification =
        mContext.notificationBuilder(CHANNEL_ID_HIGH_PRIO) {
          setContentTitle(alarm.label)
          setContentText(getString(R.string.alarm_notify_text))
          setSmallIcon(R.drawable.stat_notify_alarm)
          setWhen(0)
          priority = NotificationCompat.PRIORITY_HIGH
          setCategory(NotificationCompat.CATEGORY_ALARM)
          setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
          setLocalOnly(true)
          // setFullScreenIntent to show the user AlarmAlert dialog at the same time
          // when the Notification Bar was created.
          setFullScreenIntent(pendingNotify, true)
          // setContentIntent to show the user AlarmAlert dialog
          // when he will click on the Notification Bar.
          setContentIntent(pendingNotify)
          setOngoing(true)
          addAction(
              R.drawable.ic_action_snooze,
              getString(R.string.alarm_alert_snooze_text),
              pendingSnooze)
          addAction(
              R.drawable.ic_action_dismiss,
              getString(R.string.alarm_alert_dismiss_text),
              pendingDismiss)
          setDefaults(Notification.DEFAULT_LIGHTS)
          setSound(null)
          setVibrate(longArrayOf(0))
        }

    if (startForeground && isOreo()) {
      logger.debug { "startForeground() for ${alarm.id}" }
      enclosingService.startForeground(index + OFFSET, notification)
    } else {
      logger.debug { "nm.notify() for ${alarm.id}" }
      nm.notify(index + OFFSET, notification)
    }
  }

  fun cancel(index: Int) {
    nm.cancel(index + OFFSET)
  }

  private fun getString(id: Int) = mContext.getString(id)

  companion object {
    private const val OFFSET = 100000
  }
}
