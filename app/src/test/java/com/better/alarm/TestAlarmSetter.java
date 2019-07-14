package com.better.alarm;

import com.better.alarm.model.AlarmSetter;
import com.better.alarm.model.AlarmsScheduler;

import org.jetbrains.annotations.NotNull;

import java.util.Calendar;

/**
 * Created by Yuriy on 25.06.2017.
 */
class TestAlarmSetter implements AlarmSetter {
    @Override
    public void removeRTCAlarm() {
        //NOP
    }

    @Override
    public void setUpRTCAlarm(int id, @NotNull String typeName, @NotNull Calendar calendar) {
        //NOP
    }

    @Override
    public void fireNow(int id, @NotNull String typeName) {
        //NOP
    }
}
