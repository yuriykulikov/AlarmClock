package com.better.alarm.model;

import android.database.Cursor;

public interface IAlarmsSetup {
    public void delete(int id);

    public long add(Alarm alarm);

    public void enable(Alarm alarm);

    public long set(Alarm alarm);

    public Cursor getCursor();
}
