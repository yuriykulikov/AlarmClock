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

package com.better.alarm.alert

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.preference.PreferenceManager
import android.text.format.DateFormat
import com.better.alarm.BuildConfig
import com.better.alarm.R
import com.better.alarm.background.Event
import com.better.alarm.configuration.AlarmApplication.container
import com.better.alarm.interfaces.Alarm
import com.better.alarm.interfaces.IAlarmsManager
import com.better.alarm.interfaces.Intents
import com.better.alarm.interfaces.Intents.ACTION_CANCEL_NOTIFICATION
import com.better.alarm.interfaces.PresentationToModelIntents
import com.better.alarm.logger.Logger
import com.better.alarm.presenter.TransparentActivity
import java.util.*

/**
 * Glue class: connects AlarmAlert IntentReceiver to AlarmAlert activity. Passes
 * through Alarm ID.
 */
class BackgroundNotifications {
    private var mContext = container().context()
    private val nm = container().notificationManager()
    private val alarmsManager = container().alarms()
    private val prefs = container().prefs()

    init {
        val subscribe = container().store.events.subscribe { event ->
            when (event) {
                is Event.AlarmEvent -> nm.cancel(event.id + BACKGROUND_NOTIFICATION_OFFSET)
                is Event.PrealarmEvent -> nm.cancel(event.id + BACKGROUND_NOTIFICATION_OFFSET)
                is Event.DismissEvent -> nm.cancel(event.id + BACKGROUND_NOTIFICATION_OFFSET)
                is Event.CancelSnoozedEvent -> nm.cancel(event.id + BACKGROUND_NOTIFICATION_OFFSET)
                is Event.SnoozedEvent -> onSnoozed(event.id)
                is Event.Autosilenced -> onSoundExpired(event.id)
            }
        }
    }

    private fun onSnoozed(id: Int) {
        // What to do, when a user clicks on the notification bar
        val cancelSnooze = Intent(mContext, BackgroundNotifications::class.java)
        cancelSnooze.action = ACTION_CANCEL_NOTIFICATION
        cancelSnooze.putExtra(Intents.EXTRA_ID, id)
        val pCancelSnooze = PendingIntent.getBroadcast(mContext, id, cancelSnooze, 0)

        // When button Reschedule is clicked, the TransparentActivity with
        // TimePickerFragment to set new alarm time is launched
        val reschedule = Intent(mContext, TransparentActivity::class.java)
        reschedule.putExtra(Intents.EXTRA_ID, id)
        val pendingReschedule = PendingIntent.getActivity(mContext, id, reschedule, 0)

        val pendingDismiss = PresentationToModelIntents.createPendingIntent(mContext,
                PresentationToModelIntents.ACTION_REQUEST_DISMISS, id)

        val label = alarmsManager.alarmOrNull(id)?.labelOrDefault ?: ""

        //@formatter:off
        val contentText: String = alarmsManager.alarmOrNull(id)
                ?.let { mContext.getString(R.string.alarm_notify_snooze_text, it.formatTimeString()) }
                ?: ""

        val status = mContext.notificationBuilder(CHANNEL_ID, NotificationImportance.NORMAL) {
            // Get the display time for the snooze and update the notification.
            setContentTitle(mContext!!.getString(R.string.alarm_notify_snooze_label, label))
            setContentText(contentText)
            setSmallIcon(R.drawable.stat_notify_alarm)
            setContentIntent(pCancelSnooze)
            setOngoing(true)
            addAction(R.drawable.ic_action_reschedule_snooze, mContext!!.getString(R.string.alarm_alert_reschedule_text), pendingReschedule)
            addAction(R.drawable.ic_action_dismiss, mContext!!.getString(R.string.alarm_alert_dismiss_text), pendingDismiss)
            setDefaults(Notification.DEFAULT_LIGHTS)
        }
        //@formatter:on

        // Send the notification using the alarm id to easily identify the
        // correct notification.
        nm.notify(id + BACKGROUND_NOTIFICATION_OFFSET, status)
    }

    private fun Alarm.formatTimeString(): String {
        val format = if (prefs.is24HoutFormat().blockingGet()) DM24 else DM12
        val calendar = snoozedTime
        return DateFormat.format(format, calendar) as String
    }

    private fun onSoundExpired(id: Int) {
        val dismissAlarm = Intent(mContext, BackgroundNotifications::class.java)
        dismissAlarm.action = ACTION_CANCEL_NOTIFICATION
        dismissAlarm.putExtra(Intents.EXTRA_ID, id)
        val intent = PendingIntent.getBroadcast(mContext, id, dismissAlarm, 0)
        // Update the notification to indicate that the alert has been
        // silenced.
        val alarm = alarmsManager.getAlarm(id)
        val label = alarm.labelOrDefault
        val autoSilenceMinutes = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(mContext).getString(
                "auto_silence", "10"))
        val text = mContext!!.getString(R.string.alarm_alert_alert_silenced, autoSilenceMinutes)

        val notification = mContext.notificationBuilder(CHANNEL_ID, NotificationImportance.NORMAL) {
            setAutoCancel(true)
            setSmallIcon(R.drawable.stat_notify_alarm)
            setWhen(Calendar.getInstance().timeInMillis)
            setContentIntent(intent)
            setContentTitle(label)
            setContentText(text)
            setTicker(text)
        }

        nm.notify(BACKGROUND_NOTIFICATION_OFFSET + id, notification)
    }

    fun IAlarmsManager.alarmOrNull(id: Int): Alarm? {
        return try {
            getAlarm(id)
        } catch (e: Exception) {
            Logger.getDefaultLogger().e("Alarm not found")
            null
        }
    }

    companion object {
        private const val DM12 = "E h:mm aa"
        private const val DM24 = "E kk:mm"
        private const val BACKGROUND_NOTIFICATION_OFFSET = 1000
        private const val CHANNEL_ID = "${BuildConfig.APPLICATION_ID}.BackgroundNotifications"
    }
}
