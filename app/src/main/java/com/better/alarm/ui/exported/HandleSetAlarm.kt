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

package com.better.alarm.ui.exported

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.AlarmClock
import com.better.alarm.bootstrap.AlarmApplication
import com.better.alarm.bootstrap.globalLogger
import com.better.alarm.data.AlarmsRepository
import com.better.alarm.domain.Alarm
import com.better.alarm.domain.IAlarmsManager
import com.better.alarm.domain.Store
import com.better.alarm.logger.Logger
import com.better.alarm.receivers.Intents
import com.better.alarm.ui.main.AlarmsListActivity
import org.koin.android.ext.android.inject

class HandleSetAlarm : Activity() {
  private val store: Store by inject()
  private val alarmsManager: IAlarmsManager by inject()
  private val logger: Logger by globalLogger("HandleSetAlarm")
  private val repository: AlarmsRepository by inject()

  override fun onCreate(icicle: Bundle?) {
    super.onCreate(icicle)
    AlarmApplication.startOnce(application)
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
        repository.awaitStored()
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

  /** A new alarm has to be created or an existing one edited based on the intent extras. */
  private fun createNewAlarmFromIntent(intent: Intent): Alarm {
    val hours = intent.getIntExtra(AlarmClock.EXTRA_HOUR, 0)
    val minutes = intent.getIntExtra(AlarmClock.EXTRA_MINUTES, 0)
    val msg = intent.getStringExtra(AlarmClock.EXTRA_MESSAGE)
    val label = msg ?: ""

    val alarms = store.alarms().blockingFirst()
    val sameAlarm =
        alarms.find {
          val hoursMatch = it.hour == hours
          val minutesMatch = it.minutes == minutes
          val labelsMatch = it.label == label
          val noRepeating = !it.isRepeatSet
          hoursMatch && minutesMatch && labelsMatch && noRepeating
        }

    return if (sameAlarm == null) {
      createNewAlarm(hours, minutes, label)
    } else {
      val edited = alarmsManager.getAlarm(sameAlarm.id)
      if (edited != null && !edited.data.isEnabled) {
        logger.debug { "Editing existing alarm ${sameAlarm.id}" }
        edited.apply { enable(true) }
      } else {
        createNewAlarm(hours, minutes, label)
      }
    }
  }

  private fun createNewAlarm(hours: Int, minutes: Int, label: String): Alarm {
    logger.debug { "No alarm found, creating a new one" }
    return alarmsManager.createNewAlarm().apply {
      edit {
        copy(
            hour = hours,
            minutes = minutes,
            isEnabled = true,
            label = label,
            isDeleteAfterDismiss = true,
        )
      }
    }
  }
}
