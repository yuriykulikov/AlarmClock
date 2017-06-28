package com.better.alarm;

import android.net.Uri;

import com.better.alarm.model.DaysOfWeek;
import com.better.alarm.model.IAlarmContainer;

import java.util.Calendar;

/**
 * Created by Yuriy on 25.06.2017.
 */
class TestAlarmContainer implements IAlarmContainer {
    private int id;
    private boolean enabled;
    private int hour;
    private int minutes;
    private DaysOfWeek daysOfWeek;
    private boolean vibrate;
    private String label;
    private boolean silent;
    private boolean prealarm;
    private Calendar nextTime;
    private String state;

    public TestAlarmContainer(int id) {
        hour = 18;
        minutes = 0;
        vibrate = true;
        daysOfWeek = new DaysOfWeek(0);
        //nextTime = c;
        prealarm = false;
        label = "";
    }

    @Override
    public void writeToDb() {
        //NOP
    }

    @Override
    public void delete() {
        //NOP
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void setHour(int hour) {
        this.hour = hour;
    }

    @Override
    public void setMinutes(int minutes) {
        this.minutes = minutes;
    }

    @Override
    public void setDaysOfWeek(DaysOfWeek daysOfWeek) {
        this.daysOfWeek = daysOfWeek;
    }

    @Override
    public boolean isVibrate() {
        return vibrate;
    }

    @Override
    public void setVibrate(boolean vibrate) {
        this.vibrate = vibrate;
    }

    @Override
    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public Uri getAlert() {
        return null;
    }

    @Override
    public void setAlert(Uri alert) {
        //NOP
    }

    @Override
    public boolean isSilent() {
        return silent;
    }

    @Override
    public void setSilent(boolean silent) {

        this.silent = silent;
    }

    @Override
    public boolean isPrealarm() {
        return prealarm;
    }

    @Override
    public void setPrealarm(boolean prealarm) {
        this.prealarm = prealarm;
    }

    @Override
    public Calendar getNextTime() {
        return nextTime;
    }

    @Override
    public void setNextTime(Calendar nextTime) {
        this.nextTime = nextTime;
    }

    @Override
    public String getState() {
        return state;
    }

    @Override
    public void setState(String state) {
        this.state = state;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public int getHour() {
        return hour;
    }

    @Override
    public int getMinutes() {
        return minutes;
    }

    @Override
    public DaysOfWeek getDaysOfWeek() {
        return daysOfWeek;
    }

    @Override
    public String getLabel() {
        return label;
    }
}
