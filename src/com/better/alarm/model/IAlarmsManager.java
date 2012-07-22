package com.better.alarm.model;

import java.util.List;

import android.net.Uri;

import com.better.alarm.model.Alarm.DaysOfWeek;

/**
 * An interface for Presenter-Model interaction. Presenters can invoke
 * {@link #dismiss(Alarm)}, {@link #snooze(Alarm)} as a result of user
 * interaction. Model broadcasts intents representing lifecycle of the
 * {@link Alarm}. Each intent contains an {@link Alarm} as a parceable extra
 * with the key {@link #EXTRA_ID}
 * 
 * @author Yuriy
 * 
 */
public interface IAlarmsManager {
    public interface OnAlarmListChangedListener {
        void onAlarmListChanged(List<Alarm> newList);
    }

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
     * Delete an Alarm with the given id from the database
     * 
     * @param id
     */
    public void delete(int id);

    /**
     * Enable of disable an alarm with a given id
     * @param id
     * @param enable
     */
    public void enable(int id, boolean enable);

    /**
     * Return an Alarm object representing the alarm id in the database. Returns
     * null if no alarm exists.
     */
    public Alarm getAlarm(int alarmId);

    /**
     * Queries all alarms
     * 
     * @return List of all alarms as a copy
     */
    public List<Alarm> getAlarmsList();

    /**
     * Register a listener. Newly registered listener will be notified within
     * the call
     * 
     * @param listener
     */
    public void registerOnAlarmListChangedListener(OnAlarmListChangedListener listener);

    /**
     * Unregister the listener
     * 
     * @param listener
     */
    public void unRegisterOnAlarmListChangedListener(OnAlarmListChangedListener listener);

    /**
     * Create new Alarm with default settings
     * 
     * @return id of newly created Alarm
     */
    int createNewAlarm();

    /**
     * A convenience method to change an existing alarm
     * 
     * @param id
     *            - Alarm which has to be changed
     * @param enabled
     * @param hour
     * @param minute
     * @param daysOfWeek
     * @param vibrate
     * @param label
     * @param alert
     * @param preAlarm
     */
    void changeAlarm(int id, boolean enabled, int hour, int minute, DaysOfWeek daysOfWeek, boolean vibrate,
            String label, Uri alert, boolean preAlarm);
}
