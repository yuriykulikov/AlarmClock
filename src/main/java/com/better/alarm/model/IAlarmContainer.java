package com.better.alarm.model;

import java.util.Calendar;

import android.net.Uri;

public interface IAlarmContainer {

    /**
     * Persist data in the database
     */
    public void writeToDb();

    public void delete();

    public int getId();

    public void setId(int id);

    public boolean isEnabled();

    public void setEnabled(boolean enabled);

    public int getHour();

    public void setHour(int hour);

    public int getMinutes();

    public void setMinutes(int minutes);

    public DaysOfWeek getDaysOfWeek();

    public void setDaysOfWeek(DaysOfWeek daysOfWeek);

    public boolean isVibrate();

    public void setVibrate(boolean vibrate);

    public String getLabel();

    public void setLabel(String label);

    public Uri getAlert();

    public void setAlert(Uri alert);

    public boolean isSilent();

    public void setSilent(boolean silent);

    public boolean isPrealarm();

    public void setPrealarm(boolean prealarm);

    public Calendar getNextTime();

    public void setNextTime(Calendar nextTime);

    public String getState();

    void setState(String state);
}