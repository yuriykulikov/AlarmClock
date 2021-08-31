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

import android.annotation.SuppressLint
import com.better.alarm.interfaces.Alarm
import com.better.alarm.interfaces.IAlarmsManager
import com.better.alarm.logger.Logger
import com.better.alarm.persistance.DatabaseQuery
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** The Alarms implements application domain logic */
@SuppressLint("UseSparseArrays")
class Alarms(
    private val mAlarmsScheduler: IAlarmsScheduler,
    private val query: DatabaseQuery,
    private val factory: AlarmCoreFactory,
    private val containerFactory: ContainerFactory,
    private val logger: Logger
) : IAlarmsManager {
  private val alarms: MutableMap<Int, AlarmCore> = mutableMapOf()
  private val scope = CoroutineScope(Dispatchers.Main.immediate)
  fun start() {
    scope.launch {
      val created =
          query.query().map { container -> container.value.id to factory.create(container) }.toMap()
      alarms.putAll(created)
      created.values.forEach { it.start() }
    }
  }

  fun refresh() {
    alarms.values.forEach { alarmCore -> alarmCore.refresh() }
  }

  fun onTimeSet() {
    alarms.values.forEach { alarmCore -> alarmCore.onTimeSet() }
  }

  override fun getAlarm(alarmId: Int): AlarmCore? {
    return alarms[alarmId].also {
      if (it == null) {
        val exception =
            RuntimeException("Alarm with id $alarmId not found!").apply { fillInStackTrace() }
        logger.error(exception) { "Alarm with id $alarmId not found!" }
      }
    }
  }

  override fun createNewAlarm(): Alarm {
    val alarm = factory.create(containerFactory.create())
    alarms[alarm.id] = alarm
    alarm.start()
    return alarm
  }

  fun onAlarmFired(alarm: AlarmCore, calendarType: CalendarType?) {
    // TODO this should not be needed
    mAlarmsScheduler.removeAlarm(alarm.id)
    alarm.onAlarmFired()
  }

  override fun enable(alarm: AlarmValue, enable: Boolean) {
    alarms.getValue(alarm.id).enable(enable)
  }
}
