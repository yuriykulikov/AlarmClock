package com.better.alarm.model;

import java.util.Calendar;

import android.content.Context;
import android.net.Uri;

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

    public void dismiss();

    public void delete();

    public void change(boolean enabled, int hour, int minute, DaysOfWeek daysOfWeek, boolean vibrate, String label,
            Uri alert, boolean preAlarm);

    /**
     * TODO calendar should be immutable
     * 
     * @return
     */
    public Calendar getNextTime();

    public Calendar getSnoozedTime();

    public Calendar getPrealarmTime();

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

}
