/*
 * Copyright (C) 2010 The Android Open Source Project
 * Copyright (C) 2013 Yuriy Kulikov yuriy.kulikov.87@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.better.alarm.presenter

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.AlarmClock

import com.better.alarm.interfaces.Alarm
import com.better.alarm.interfaces.Intents
import com.better.alarm.logger.Logger
import com.better.alarm.model.AlarmValue

import com.better.alarm.configuration.AlarmApplication.container


class HandleSetAlarm : Activity() {
    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
        val intent = intent
        when {
            intent == null || intent.action != AlarmClock.ACTION_SET_ALARM -> {
                finish()
            }
            !intent.hasExtra(AlarmClock.EXTRA_HOUR) -> {
                // no extras - start list activity
                startActivity(Intent(this, AlarmsListActivity::class.java))
                finish()
            }
            intent.getBooleanExtra(AlarmClock.EXTRA_SKIP_UI, false) -> {
                createNewAlarmFromIntent(intent)
                finish()
            }
            else -> {
                val alarm = createNewAlarmFromIntent(intent)

                val startDetailsIntent = Intent(this, AlarmsListActivity::class.java)
                startDetailsIntent.putExtra(Intents.EXTRA_ID, alarm.id)
                startActivity(startDetailsIntent)

                finish()
            }
        }

    }

    /**
     * A new alarm has to be created or an existing one edited based on the
     * intent extras.
     */
    private fun createNewAlarmFromIntent(intent: Intent): Alarm {
        val hours = intent.getIntExtra(AlarmClock.EXTRA_HOUR, 0)
        val minutes = intent.getIntExtra(AlarmClock.EXTRA_MINUTES, 0)
        val msg = intent.getStringExtra(AlarmClock.EXTRA_MESSAGE)
        val label = msg ?: ""

        val alarms = container().store().alarms().blockingFirst()
        val sameAlarms = alarms.filter {
            val hoursMatch = it.hour == hours
            val minutesMatch = it.minutes == minutes
            val labelsMatch = it.label != null && it.label == label
            val noRepeating = !it.daysOfWeek.isRepeatSet
            hoursMatch && minutesMatch && labelsMatch && noRepeating
        }

        val alarm: Alarm
        if (sameAlarms.isEmpty()) {
            Logger.getDefaultLogger().d("No alarm found, creating a new one")
            alarm = container().alarms().createNewAlarm()
            alarm.edit()
                    .with(hour = hours, minutes = minutes, enabled = true)
                    .withLabel(label)
                    .commit()
        } else {
            Logger.getDefaultLogger().d("Enable existing alarm")
            alarm = container().alarms().getAlarm(sameAlarms.iterator().next().id)
            alarm.enable(true)
        }
        return alarm
    }
}