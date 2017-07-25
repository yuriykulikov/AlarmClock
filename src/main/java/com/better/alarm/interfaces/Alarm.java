package com.better.alarm.interfaces;

import android.net.Uri;

import com.better.alarm.model.DaysOfWeek;

import java.util.Calendar;

public interface Alarm {
    void enable(boolean enable);

    void snooze();

    void snooze(int hourOfDay, int minute);

    void dismiss();

    void delete();

    ImmutableAlarmEditor edit();

    boolean isPrealarm();

    boolean isSilent();

    Uri getAlert();

    String getLabel();

    boolean isVibrate();

    DaysOfWeek getDaysOfWeek();

    int getMinutes();

    int getHour();

    boolean isEnabled();

    int getId();

    boolean isSnoozed();

    String getLabelOrDefault();

    /**
     * @deprecated
     * @return
     */
    @Deprecated
    public Calendar getSnoozedTime();

}
