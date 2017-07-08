package com.better.alarm;

import android.database.Cursor;

import com.better.alarm.model.Calendars;
import com.better.alarm.model.ContainerFactory;
import com.better.alarm.model.DaysOfWeek;
import com.better.alarm.model.AlarmContainer;
import com.better.alarm.model.ImmutableAlarmContainer;
import com.google.inject.Inject;

import java.util.Calendar;

/**
 * Created by Yuriy on 25.06.2017.
 */
public class TestContainerFactory implements ContainerFactory {
    int idCounter;
    private Calendars calendars;

    @Inject
    public TestContainerFactory(Calendars calendars) {
        this.calendars = calendars;
    }

    @Override
    public AlarmContainer create() {
        Calendar now = calendars.now();
        return ImmutableAlarmContainer.builder()
                .id(-1)
                .isEnabled(false)
                .nextTime(now)
                .hour(now.get(Calendar.HOUR_OF_DAY))
                .minutes(now.get(Calendar.MINUTE))
                .isVibrate(true)
                .daysOfWeek(new DaysOfWeek(0))
                .alertString("")
                .isPrealarm(false)
                .label("")
                .state("")
                .id(idCounter++)
                .persistence(AlarmContainer.PERSISTENCE_STUB)
                .build();
    }

    @Override
    public AlarmContainer create(Cursor cursor) {
        throw new UnsupportedOperationException();
    }
}
