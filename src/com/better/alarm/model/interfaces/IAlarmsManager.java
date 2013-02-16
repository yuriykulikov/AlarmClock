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
package com.better.alarm.model.interfaces;

import java.util.List;

import com.better.alarm.model.AlarmCore;

/**
 * An interface for Presenter-Model interaction. Presenters can invoke
 * {@link #dismiss(Alarm)}, {@link #snooze(Alarm)} as a result of user
 * interaction. Model broadcasts intents representing lifecycle of the
 * {@link AlarmCore}. Each intent contains an {@link Alarm} as a parceable extra
 * with the key {@link #EXTRA_ID}
 * 
 * @author Yuriy
 * 
 */
public interface IAlarmsManager {
    /**
     * Tell the model that a certain alarm has to be snoozed because of the user
     * interaction
     * 
     * @param alarm
     */
    public void snooze(Alarm alarm);

    /**
     * Tell the model that a certain alarm has to be dismissed because of the
     * user interaction
     * 
     * @param alarm
     */
    public void dismiss(Alarm alarm);

    /**
     * Delete an AlarmCore with the given id from the database
     * 
     * @param id
     */
    public void delete(int id);

    /**
     * Enable of disable an alarm with a given id
     * 
     * @param id
     * @param enable
     */
    public void enable(int id, boolean enable);

    /**
     * Return an AlarmCore object representing the alarm id in the database.
     * Returns null if no alarm exists.
     */
    public Alarm getAlarm(int alarmId);

    /**
     * Queries all alarms
     * 
     * @return List of all alarms as a copy
     */
    public List<Alarm> getAlarmsList();

    /**
     * Create new AlarmCore with default settings
     * 
     * @return id of newly created AlarmCore
     */
    int createNewAlarm();
}
