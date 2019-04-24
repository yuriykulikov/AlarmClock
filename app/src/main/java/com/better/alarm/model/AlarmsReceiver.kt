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
package com.better.alarm.model

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.better.alarm.alert.BackgroundNotifications
import com.better.alarm.configuration.AlarmApplication.container
import com.better.alarm.interfaces.Intents
import com.better.alarm.interfaces.PresentationToModelIntents
import com.better.alarm.logger.Logger

class AlarmsReceiver : BroadcastReceiver() {
    private val alarms = container().rawAlarms()
    private val log = container().logger()

    override fun onReceive(context: Context, intent: Intent) {
        try {
            val action = intent.action
            when (action) {
                AlarmsScheduler.ACTION_FIRED -> {
                    val id = intent.getIntExtra(AlarmsScheduler.EXTRA_ID, -1)

                    val alarm = alarms.getAlarm(id)
                    alarms.onAlarmFired(alarm, CalendarType.valueOf(intent.extras!!.getString(AlarmsScheduler.EXTRA_TYPE)))
                    log.d("AlarmCore fired $id")
                }
                Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_TIMEZONE_CHANGED,
                Intent.ACTION_LOCALE_CHANGED,
                Intent.ACTION_MY_PACKAGE_REPLACED -> {
                    log.d("Refreshing alarms because of $action")
                    alarms.refresh()
                }
                Intent.ACTION_TIME_CHANGED -> alarms.onTimeSet()

                PresentationToModelIntents.ACTION_REQUEST_SNOOZE -> {
                    val id = intent.getIntExtra(AlarmsScheduler.EXTRA_ID, -1)
                    alarms.getAlarm(id).snooze()
                }

                PresentationToModelIntents.ACTION_REQUEST_DISMISS -> {
                    val id = intent.getIntExtra(AlarmsScheduler.EXTRA_ID, -1)
                    alarms.getAlarm(id).dismiss()
                }
            }
        } catch (e: Exception) {
            Logger.getDefaultLogger().d("Alarm not found")
        }
    }
}
