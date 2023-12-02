/*
 * Copyright (C) 2007 The Android Open Source Project
 * Copyright (C) 2012 Yuriy Kulikov yuriy.kulikov.87@gmail.com
 * Copyright (C) 2019 Yuriy Kulikov yuriy.kulikov.87@gmail.com
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
import android.text.format.DateFormat
import com.better.alarm.R
import com.better.alarm.data.Prefs
import com.better.alarm.domain.IAlarmsManager
import com.better.alarm.domain.Store
import com.better.alarm.platform.pendingIntentUpdateCurrentFlag
import com.better.alarm.receivers.Intents
import com.better.alarm.receivers.PresentationToModelIntents
import com.better.alarm.services.Event
import com.better.alarm.ui.alert.TransparentActivity
import com.better.alarm.util.subscribeForever
import java.util.Calendar

/**
 * Glue class: connects AlarmAlert IntentReceiver to AlarmAlert activity. Passes through Alarm ID.
 */
class BackgroundNotifications(
    private var mContext: Context,
    private val nm: NotificationManager,
    private val alarmsManager: IAlarmsManager,
    private val prefs: Prefs,
    private val store: Store
) {
  init {
    store.events.subscribeForever { event ->
      when (event) {
        is Event.AlarmEvent -> nm.cancel(event.id + SNOOZE_NOTIFICATION)
        is Event.PrealarmEvent -> nm.cancel(event.id + SNOOZE_NOTIFICATION)
        is Event.DismissEvent -> nm.cancel(event.id + SNOOZE_NOTIFICATION)
        is Event.CancelSnoozedEvent -> nm.cancel(event.id + SNOOZE_NOTIFICATION)
        is Event.SnoozedEvent -> onSnoozed(event.id, event.calendar)
        is Event.Autosilenced -> onSoundExpired(event.id)
        is Event.ShowSkip -> onShowSkip(event.id)
        is Event.HideSkip -> nm.cancel(SKIP_NOTIFICATION + event.id)
        is Event.DemuteEvent,
        is Event.MuteEvent,
        is Event.NullEvent -> Unit
      }
    }
  }

  private fun onSnoozed(id: Int, calendar: Calendar) {
    // When button Reschedule is clicked, the TransparentActivity with
    // TimePickerFragment to set new alarm time is launched
    val pendingReschedule =
        Intent()
            .apply {
              setClass(mContext, TransparentActivity::class.java)
              putExtra(Intents.EXTRA_ID, id)
            }
            .let { PendingIntent.getActivity(mContext, id, it, pendingIntentUpdateCurrentFlag()) }

    val pendingDismiss =
        PresentationToModelIntents.createPendingIntent(
            mContext, PresentationToModelIntents.ACTION_REQUEST_DISMISS, id)

    val label = alarmsManager.getAlarm(id)?.labelOrDefault ?: ""

    val contentText: String =
        alarmsManager.getAlarm(id)?.let {
          mContext.getString(R.string.alarm_notify_snooze_text, calendar.formatTimeString())
        }
            ?: ""

    val status =
        mContext.notificationBuilder(CHANNEL_ID) {
          // Get the display time for the snooze and update the notification.
          setContentTitle(getString(R.string.alarm_notify_snooze_label, label))
          setContentText(contentText)
          setSmallIcon(R.drawable.stat_notify_alarm)
          setContentIntent(pendingDismiss)
          setOngoing(true)
          addAction(
              R.drawable.ic_action_reschedule_snooze,
              getString(R.string.alarm_alert_reschedule_text),
              pendingReschedule)
          addAction(
              R.drawable.ic_action_dismiss,
              getString(R.string.alarm_alert_dismiss_text),
              pendingDismiss)
          setDefaults(Notification.DEFAULT_LIGHTS)
        }

    // Send the notification using the alarm id to easily identify the
    // correct notification.
    nm.notify(id + SNOOZE_NOTIFICATION, status)
  }

  private fun getString(id: Int, vararg args: String) = mContext.getString(id, *args)

  private fun getString(id: Int) = mContext.getString(id)

  private fun Calendar.formatTimeString(): String {
    val format = if (prefs.is24HourFormat.blockingGet()) DM24 else DM12
    return DateFormat.format(format, this) as String
  }

  private fun onSoundExpired(id: Int) {
    // Update the notification to indicate that the alert has been
    // silenced.
    val alarm = alarmsManager.getAlarm(id)
    val label: String = alarm?.labelOrDefault ?: ""
    val autoSilenceMinutes = prefs.autoSilence.value
    val text = mContext.getString(R.string.alarm_alert_alert_silenced, autoSilenceMinutes)

    val notification =
        mContext.notificationBuilder(CHANNEL_ID) {
          setAutoCancel(true)
          setSmallIcon(R.drawable.stat_notify_alarm)
          setWhen(Calendar.getInstance().timeInMillis)
          setContentTitle(label)
          setContentText(text)
          setTicker(text)
        }

    nm.notify(ONLY_MANUAL_DISMISS_OFFSET + id, notification)
  }

  private fun onShowSkip(id: Int) {
    val pendingSkip =
        PresentationToModelIntents.createPendingIntent(
            mContext, PresentationToModelIntents.ACTION_REQUEST_SKIP, id)

    alarmsManager.getAlarm(id)?.run {
      val label: String = labelOrDefault

      val notification =
          mContext.notificationBuilder(CHANNEL_ID) {
            setAutoCancel(true)
            addAction(
                R.drawable.ic_action_dismiss,
                when {
                  data.isRepeatSet -> getString(R.string.skip)
                  else -> getString(R.string.disable_alarm)
                },
                pendingSkip)
            setSmallIcon(R.drawable.stat_notify_alarm)
            setWhen(Calendar.getInstance().timeInMillis)
            setContentTitle(label)
            setContentText(
                "${getString(R.string.notification_alarm_is_about_to_go_off)} ${data.nextTime.formatTimeString()}")
            setTicker(
                "${getString(R.string.notification_alarm_is_about_to_go_off)} ${data.nextTime.formatTimeString()}")
          }

      nm.notify(SKIP_NOTIFICATION + id, notification)
    }
  }

  companion object {
    private const val DM12 = "E h:mm aa"
    private const val DM24 = "E kk:mm"
    private const val SNOOZE_NOTIFICATION = 1000
    private const val ONLY_MANUAL_DISMISS_OFFSET = 2000
    private const val SKIP_NOTIFICATION = 3000
  }
}
