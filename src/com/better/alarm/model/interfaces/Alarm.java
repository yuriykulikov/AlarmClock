package com.better.alarm.model.interfaces;

import java.util.Calendar;

import android.content.Context;
import android.net.Uri;

import com.better.alarm.model.DaysOfWeek;

public interface Alarm {

    /**
     * A convenience method to enable or disable an
     * 
     * @param id
     *            corresponds to the _id column
     * @param enabled
     *            corresponds to the ENABLED column
     */
    public void enable(boolean enable);

    public void snooze();

    public void snooze(int hourOfDay, int minute);

    public void dismiss();

    public void delete();

    public AlarmEditor edit();

    public boolean isPrealarm();

    public boolean isSilent();

    public Uri getAlert();

    public String getLabel();

    public boolean isVibrate();

    public DaysOfWeek getDaysOfWeek();

    public int getMinutes();

    public int getHour();

    public boolean isEnabled();

    public int getId();

    public boolean isSnoozed();

    public String getLabelOrDefault(Context context);

    /**
     * @deprecated
     * @return
     */
    @Deprecated
    public Calendar getSnoozedTime();

}
