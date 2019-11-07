package com.better.alarm.interfaces;

import android.net.Uri;

import com.better.alarm.model.Alarmtone;

import java.util.Calendar;

public interface Alarm {
    void enable(boolean enable);

    void snooze();

    void snooze(int hourOfDay, int minute);

    void dismiss();

    void requestSkip();

    boolean isSkipping();

    void delete();

    AlarmEditor edit();

    boolean isSilent();

    Uri getAlert();

    int getId();

    String getLabelOrDefault();

    Alarmtone getAlarmtone();


    /**
     * @return
     * @deprecated
     */
    @Deprecated
    public Calendar getSnoozedTime();
}
