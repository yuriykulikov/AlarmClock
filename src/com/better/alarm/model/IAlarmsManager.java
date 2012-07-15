package com.better.alarm.model;

import android.database.Cursor;

/**
 * An interface for Presenter-Model interaction. Presenters can invoke
 * {@link #dismiss(Alarm)}, {@link #snooze(Alarm)} as a result of user
 * interaction. Model broadcasts intents representing lifecycle of the
 * {@link Alarm}. Each intent contains an {@link Alarm} as a parceable extra
 * with the key {@link #ALARM_INTENT_EXTRA}
 * 
 * @author Yuriy
 * 
 */
public interface IAlarmsManager {
    /**
     * Tell the model that a certain alarm has to be snoozed because of the user
     * interaction
     * 
     * @param alarm
     */
    public void snooze(Alarm alarm);

    /**
     * Tell the model that a certain alarm has to be dismissed because of the
     * user interaction
     * 
     * @param alarm
     */
    public void dismiss(Alarm alarm);

    /**
     *  Delete an Alarm with the given id from the database
     * @param id
     */
    public void delete(int id);

    public long add(Alarm alarm);

    public void enable(Alarm alarm);

    public long set(Alarm alarm);

    public Cursor getCursor();
}
