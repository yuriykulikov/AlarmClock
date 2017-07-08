package com.better.alarm.model;

import com.google.common.base.Optional;

import org.immutables.value.Value;

/**
 * Created by Yuriy on 11.06.2017.
 */
public interface AlarmValue {
    int getId();

    boolean isEnabled();

    int getHour();

    int getMinutes();

    DaysOfWeek getDaysOfWeek();

    String getLabel();
}
