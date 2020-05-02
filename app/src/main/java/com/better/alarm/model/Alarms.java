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
package com.better.alarm.model;

import android.annotation.SuppressLint;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.better.alarm.interfaces.Alarm;
import com.better.alarm.interfaces.IAlarmsManager;
import com.better.alarm.logger.Logger;
import com.better.alarm.persistance.DatabaseQuery;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.functions.Consumer;

/**
 * The Alarms implements application domain logic
 */
@SuppressLint("UseSparseArrays")
public class Alarms implements IAlarmsManager {
    private final IAlarmsScheduler mAlarmsScheduler;

    private final Map<Integer, AlarmCore> alarms;
    private DatabaseQuery query;
    private final AlarmCoreFactory factory;
    private final ContainerFactory containerFactory;
    private final Logger logger;

    public Alarms(IAlarmsScheduler alarmsScheduler, DatabaseQuery query, final AlarmCoreFactory factory, ContainerFactory containerFactory, Logger logger) {
        this.mAlarmsScheduler = alarmsScheduler;
        this.query = query;
        this.factory = factory;
        this.containerFactory = containerFactory;
        this.logger = logger;
        this.alarms = new HashMap<Integer, AlarmCore>();
    }

    public void start() {
        query.query().subscribe(new Consumer<List<AlarmStore>>() {
            @Override
            public void accept(@NonNull List<AlarmStore> alarmRecords) throws Exception {
                for (AlarmStore container : alarmRecords) {
                    final AlarmCore a = factory.create(container);
                    alarms.put(a.getId(), a);
                    a.start();
                    //TODO a.refresh();, but with a delay or something. We do not want to refresh the alarms that have just fired, right?
                }
            }
        });
    }

    public void refresh() {
        for (AlarmCore alarmCore : alarms.values()) {
            alarmCore.refresh();
        }
    }

    public void onTimeSet() {
        for (AlarmCore alarmCore : alarms.values()) {
            alarmCore.onTimeSet();
        }
    }

    @Override
    @Nullable
    public AlarmCore getAlarm(int alarmId) {
        AlarmCore alarmCore = alarms.get(alarmId);
        if (alarmCore == null) {
            RuntimeException exception = new RuntimeException("Alarm with id " + alarmId + " not found!");
            exception.fillInStackTrace();
            logger.e("Alarm with id " + alarmId + " not found!", exception);
        }
        return alarmCore;
    }

    @Override
    @NonNull
    public Alarm createNewAlarm() {
        AlarmCore alarm = factory.create(containerFactory.create());
        alarms.put(alarm.getId(), alarm);
        alarm.start();
        return alarm;
    }

    @Override
    public void delete(AlarmValue alarm) {
        alarms.get(alarm.getId()).delete();
        alarms.remove(alarm.getId());
    }

    @Override
    public void delete(Alarm alarm) {
        alarm.delete();
        alarms.remove(alarm.getId());
    }

    public void onAlarmFired(@NonNull AlarmCore alarm, CalendarType calendarType) {
        //TODO this should not be needed
        mAlarmsScheduler.removeAlarm(alarm.getId());
        alarm.onAlarmFired(calendarType);
    }

    @Override
    public void enable(AlarmValue alarm, boolean enable) {
        alarms.get(alarm.getId()).enable(enable);
    }

    @Override
    public void snooze(Alarm alarm) {
        alarm.snooze();
    }

    @Override
    public void dismiss(Alarm alarm) {
        alarm.dismiss();
    }
}
