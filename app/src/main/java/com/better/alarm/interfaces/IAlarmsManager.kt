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
package com.better.alarm.interfaces

import com.better.alarm.model.AlarmValue

/**
 * @author Yuriy
 */
interface IAlarmsManager {
    /**
     * Tell the model that a certain alarm has to be snoozed because of the user
     * interaction
     *
     * @param alarm
     */
    fun snooze(alarm: Alarm)

    /**
     * Tell the model that a certain alarm has to be dismissed because of the
     * user interaction
     *
     * @param alarm
     */
    fun dismiss(alarm: Alarm)

    /**
     * Delete an AlarmCore from the database
     *
     * @param alarm
     */
    fun delete(alarm: Alarm)

    /**
     * Enable of disable an alarm
     *
     * @param alarm
     * @param enable
     */
    fun enable(alarm: AlarmValue, enable: Boolean)

    /**
     * Return an AlarmCore object representing the alarm id in the database.
     * Returns null if no alarm exists.
     */
    fun getAlarm(alarmId: Int): Alarm?

    /**
     * Create new AlarmCore with default settings
     *
     * @return Alarm
     */
    fun createNewAlarm(): Alarm

    fun delete(alarm: AlarmValue)
}
