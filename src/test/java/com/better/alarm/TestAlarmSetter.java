package com.better.alarm;

import com.better.alarm.model.AlarmSetter;
import com.better.alarm.model.AlarmsScheduler;

/**
 * Created by Yuriy on 25.06.2017.
 */
class TestAlarmSetter implements AlarmSetter {
    @Override
    public void removeRTCAlarm() {
        //NOP
    }

    @Override
    public void setUpRTCAlarm(AlarmsScheduler.ScheduledAlarm alarm) {
        //NOP
    }

    @Override
    public void fireNow(AlarmsScheduler.ScheduledAlarm firedInThePastAlarm) {
        //NOP
    }
}
