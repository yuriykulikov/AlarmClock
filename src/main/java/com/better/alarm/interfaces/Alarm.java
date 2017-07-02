package com.better.alarm.interfaces;

import java.util.Calendar;

import android.net.Uri;

import com.better.alarm.model.DaysOfWeek;

public interface Alarm {
    void enable(boolean enable);

    void snooze();

    void snooze(int hourOfDay, int minute);

    void dismiss();

    void delete();

    AlarmEditor edit();

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
