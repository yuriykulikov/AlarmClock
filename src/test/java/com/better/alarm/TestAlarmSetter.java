package com.better.alarm;

import com.better.alarm.model.AlarmSetter;
import com.better.alarm.model.AlarmsScheduler;
import com.google.inject.Inject;

/**
 * Created by Yuriy on 25.06.2017.
 */
class TestAlarmSetter implements AlarmSetter {
    @Inject
    public TestAlarmSetter() {

    }

    @Override
    public void removeRTCAlarm() {

    }

    @Override
    public void setUpRTCAlarm(AlarmsScheduler.ScheduledAlarm alarm) {

    }

    @Override
    public void fireNow(AlarmsScheduler.ScheduledAlarm firedInThePastAlarm) {
//nothing for now
    }
}
