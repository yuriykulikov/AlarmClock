package com.better.alarm.interfaces;

import android.net.Uri;

import java.util.Calendar;

public interface Alarm {
    void enable(boolean enable);

    void snooze();

    void snooze(int hourOfDay, int minute);

    void dismiss();

    void delete();

    AlarmEditor edit();

    boolean isSilent();

    Uri getAlert();

    int getId();

    String getLabelOrDefault();

    /**
     * @return
     * @deprecated
     */
    @Deprecated
    public Calendar getSnoozedTime();

}
