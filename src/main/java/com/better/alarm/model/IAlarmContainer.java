package com.better.alarm.model;

import java.util.Calendar;

import android.net.Uri;

public interface IAlarmContainer extends AlarmValue {

    /**
     * Persist data in the database
     */
    void writeToDb();

    void delete();

    void setId(int id);

    void setEnabled(boolean enabled);

    void setHour(int hour);

    void setMinutes(int minutes);

    void setDaysOfWeek(DaysOfWeek daysOfWeek);

    boolean isVibrate();

    void setVibrate(boolean vibrate);

    void setLabel(String label);

    Uri getAlert();

    void setAlert(Uri alert);

    boolean isSilent();

    void setSilent(boolean silent);

    boolean isPrealarm();

    void setPrealarm(boolean prealarm);

    Calendar getNextTime();

    void setNextTime(Calendar nextTime);

    String getState();

    void setState(String state);
}