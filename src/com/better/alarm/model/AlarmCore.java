/*
 * Copyright (C) 2009 The Android Open Source Project
 * Copyright (C) 2012 Yuriy Kulikov yuriy.kulikov.87@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.better.alarm.model;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.preference.PreferenceManager;

import com.better.alarm.R;
import com.better.wakelock.Logger;

/**
 * Alarm is a class which models a real word alarm. It is a simple state
 * machine. External events (e.g. user input {@link #snooze()} or
 * {@link #dismiss()}) or timer events {@link #onAlarmFired(CalendarType)}
 * trigger transitions. Alarm notifies listeners when transitions happen by
 * broadcasting {@link Intent}s listed in {@link Intents}, e.g.
 * {@link Intents#ALARM_PREALARM_ACTION} or {@link Intents#ALARM_DISMISS_ACTION}
 * . State and properties of the alarm are stored in the database and are
 * updated every time when changes to alarm happen.
 * 
 * <pre>
 * @startuml
 * State DISABLED
 * [*] -down-> DISABLED
 * ENABLED -up-> [*]
 * DISABLED -up-> [*]
 * DISABLED -down-> ENABLED
 * ENABLED -up-> DISABLED
 * State ENABLED {
 * State PREALARM_PENDING
 * State PREALARM_FIRED
 * State MAIN_PENDING
 * State MAIN_FIRED {
 * M_ACTIVE -right-> M_KILLED
 * }
 * State SNOOZE_PENDING
 * State SNOOZE_FIRED {
 * S_ACTIVE -right-> S_KILLED
 * }
 * State SCHEDULE_REPEAT
 * SCHEDULE_REPEAT : Temporary state, immediately transitions to disabled in case no repeating is set
 * SCHEDULE_REPEAT : If repeating is set, transitions to one of the pending state
 * SCHEDULE_REPEAT : If prealarm is enabled, schedules next prealarm
 * SCHEDULE_REPEAT : If prealarm is enabled, but is in the past, schedules main alarm
 * 
 * [*] -down-> SCHEDULE_REPEAT
 * SCHEDULE_REPEAT -up-> [*]
 * 
 * PREALARM_PENDING -right-> PREALARM_FIRED : time
 * PREALARM_FIRED -down-> MAIN_PENDING : snooze
 * MAIN_PENDING -right-> MAIN_FIRED : time
 * PREALARM_FIRED -down->  MAIN_FIRED : time
 * MAIN_FIRED -down->  SNOOZE_PENDING : snooze
 * SNOOZE_PENDING -right-> SNOOZE_FIRED : time
 * SNOOZE_FIRED -left-> SNOOZE_PENDING : snooze
 * 
 * PREALARM_PENDING -up-> SCHEDULE_REPEAT : cancel today
 * PREALARM_FIRED -up-> SCHEDULE_REPEAT : dismiss
 * MAIN_FIRED -up-> SCHEDULE_REPEAT : dismiss
 * SNOOZE_FIRED -up-> SCHEDULE_REPEAT : dismiss
 * SNOOZE_PENDING -up-> SCHEDULE_REPEAT : notification
 * 
 * SCHEDULE_REPEAT -down-> PREALARM_PENDING : prealarm enabled
 * SCHEDULE_REPEAT -down-> MAIN_PENDING : prealarm disabled
 * PREALARM_PENDING -down-> MAIN_PENDING
 * } 
 * @enduml
 * </pre>
 * 
 * @author Yuriy
 * 
 */
public final class AlarmCore implements Alarm {
    // This string is used to indicate a silent alarm in the db.
    private static final String ALARM_ALERT_SILENT = "silent";

    private final IAlarmsScheduler mAlarmsScheduler;
    private final Logger log;
    private final Context mContext;
    private final IStateNotifier broadcaster;

    /**
     * Strategy used to notify other components about alarm state.
     */
    public interface IStateNotifier {
        public void broadcastAlarmState(int id, String action);
    }

    /**
     * Reference to a listener. We cannot use anonymous classes because
     * {@link PreferenceManager} stores {@link OnSharedPreferenceChangeListener}
     * in a {@link WeakHashMap}.
     */
    private final OnSharedPreferenceChangeListener mOnSharedPreferenceChangeListener = new OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals("prealarm_duration")) {
                fetchPreAlarmMinutes();
                if (container.isPrealarm()) {
                    refresh();
                }
            }
        }
    };

    private final IAlarmContainer container;

    /**
     * Used to calculate calendars. Is not synced with DB, because it is in the
     * settings
     */
    private int prealarmMinutes;

    public AlarmCore(IAlarmContainer container, Context context, Logger logger, IAlarmsScheduler alarmsScheduler,
            IStateNotifier broadcaster) {
        mContext = context;
        this.log = logger;
        mAlarmsScheduler = alarmsScheduler;
        this.container = container;
        this.broadcaster = broadcaster;

        Calendar now = Calendar.getInstance();

        boolean isExpired = getNextTime().before(now);
        if (isExpired) {
            log.d("AlarmCore expired: " + toString());
            container.setEnabled((isEnabled() && getDaysOfWeek().isRepeatSet()));
        }

        PreferenceManager.getDefaultSharedPreferences(mContext).registerOnSharedPreferenceChangeListener(
                mOnSharedPreferenceChangeListener);

        fetchPreAlarmMinutes();
    }

    public void onAlarmFired(CalendarType calendarType) {
        if (calendarType == CalendarType.PREALARM) {
            broadcastAlarmState(Intents.ALARM_PREALARM_ACTION);
        } else {
            broadcastAlarmState(Intents.ALARM_ALERT_ACTION);
            // Disable this alarm if it does not repeat.
            if (!getDaysOfWeek().isRepeatSet()) {
                container.setEnabled(false);
            }
        }

        container.setSnoozed(false);

        refresh();
        // TODO notifyAlarmListChangedListeners();
    }

    void refresh() {
        calculateCalendars();
        mAlarmsScheduler.setAlarm(container.getId(), getActiveCalendars());
        container.writeToDb();
    }

    // ++++++++++++++++++++++++++++++++++++++++++++++++++++++
    // ++++++ for GUI +++++++++++++++++++++++++++++++++
    // ++++++++++++++++++++++++++++++++++++++++++++++++++++++

    /**
     * A convenience method to enable or disable an
     * 
     * @param id
     *            corresponds to the _id column
     * @param enabled
     *            corresponds to the ENABLED column
     */
    @Override
    public void enable(boolean enable) {
        log.d(container.getId() + " is " + (enable ? "enabled" : "disabled"));
        container.setEnabled(enable);
        refresh();
    }

    @Override
    public void snooze() {
        int snoozeMinutes = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(mContext).getString(
                "snooze_duration", "10"));
        container.setSnoozed(true);
        container.setSnoozedTime(Calendar.getInstance());
        container.getSnoozedTime().add(Calendar.MINUTE, snoozeMinutes);
        log.d("scheduling snooze " + container.getId() + " at "
                + DateFormat.getDateTimeInstance().format(container.getSnoozedTime().getTime()));
        refresh();
        broadcastAlarmState(Intents.ALARM_SNOOZE_ACTION);
        // TODO notifyAlarmListChangedListeners();
    }

    @Override
    public void dismiss() {
        broadcastAlarmState(Intents.ALARM_DISMISS_ACTION);
        container.setSnoozed(false);
        refresh();
    }

    @Override
    public void delete() {
        container.delete();
        mAlarmsScheduler.removeAlarm(container.getId());
        broadcastAlarmState(Intents.ALARM_DISMISS_ACTION);
        // TODO notifyAlarmListChangedListeners();
    }

    @Override
    public void change(boolean enabled, int hour, int minute, DaysOfWeek daysOfWeek, boolean vibrate, String label,
            Uri alert, boolean preAlarm) {
        container.setPrealarm(preAlarm);
        container.setAlert(alert);
        container.setLabel(label);
        container.setVibrate(vibrate);
        container.setDaysOfWeek(daysOfWeek);
        container.setHour(hour);
        container.setMinutes(minute);
        container.setEnabled(enabled);

        log.d(container.getId() + " is changed");
        refresh();
        // TODO notifyAlarmListChangedListeners();
    }

    /**
     * Given an alarm in hours and minutes, return a time suitable for setting
     * in AlarmManager.
     */
    private void calculateCalendars() {
        // start with now
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(System.currentTimeMillis());

        int nowHour = c.get(Calendar.HOUR_OF_DAY);
        int nowMinute = c.get(Calendar.MINUTE);

        // if alarm is behind current time, advance one day
        if (container.getHour() < nowHour || container.getHour() == nowHour && container.getMinutes() <= nowMinute) {
            c.add(Calendar.DAY_OF_YEAR, 1);
        }
        c.set(Calendar.HOUR_OF_DAY, container.getHour());
        c.set(Calendar.MINUTE, container.getMinutes());
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        int addDays = container.getDaysOfWeek().getNextAlarm(c);
        if (addDays > 0) {
            c.add(Calendar.DAY_OF_WEEK, addDays);
        }

        container.setNextTime(c);

        container.setPrealarmTime(Calendar.getInstance());
        container.getPrealarmTime().setTimeInMillis(container.getNextTime().getTimeInMillis());
        container.getPrealarmTime().add(Calendar.MINUTE, -1 * prealarmMinutes);
    }

    private Map<CalendarType, Calendar> getActiveCalendars() {
        HashMap<CalendarType, Calendar> calendars = new HashMap<CalendarType, Calendar>();

        Calendar now = Calendar.getInstance();
        if (container.isEnabled() && container.getNextTime().after(now)) {
            calendars.put(CalendarType.NORMAL, container.getNextTime());
        }
        if (container.isSnoozed() && container.getSnoozedTime().after(now)) {
            calendars.put(CalendarType.SNOOZE, container.getSnoozedTime());
        }
        if (container.isEnabled() && container.isPrealarm() && container.getPrealarmTime().after(now)) {
            calendars.put(CalendarType.PREALARM, container.getPrealarmTime());
        }

        return calendars;
    }

    private void broadcastAlarmState(String action) {
        broadcaster.broadcastAlarmState(container.getId(), action);
    }

    // ++++++++++++++++++++++++++++++++++++++++++++++++++++++
    // ++++++ getters for GUI +++++++++++++++++++++++++++++++
    // ++++++++++++++++++++++++++++++++++++++++++++++++++++++

    private void fetchPreAlarmMinutes() {
        String asString = PreferenceManager.getDefaultSharedPreferences(mContext).getString("prealarm_duration", "30");
        prealarmMinutes = Integer.parseInt(asString);
    }

    /**
     * TODO calendar should be immutable
     * 
     * @return
     */
    @Override
    public Calendar getNextTime() {
        return container.getNextTime();
    }

    @Override
    public Calendar getSnoozedTime() {
        return container.getSnoozedTime();
    }

    @Override
    public Calendar getPrealarmTime() {
        return container.getPrealarmTime();
    }

    @Override
    public boolean isPrealarm() {
        return container.isPrealarm();
    }

    @Override
    public boolean isSilent() {
        return container.isSilent();
    }

    @Override
    public Uri getAlert() {
        return container.getAlert();
    }

    @Override
    public String getLabel() {
        return container.getLabel();
    }

    @Override
    public boolean isVibrate() {
        return container.isVibrate();
    }

    @Override
    public DaysOfWeek getDaysOfWeek() {
        return container.getDaysOfWeek();
    }

    @Override
    public int getMinutes() {
        return container.getMinutes();
    }

    @Override
    public int getHour() {
        return container.getHour();
    }

    @Override
    public boolean isEnabled() {
        return container.isEnabled();
    }

    @Override
    public int getId() {
        return container.getId();
    }

    @Override
    public boolean isSnoozed() {
        return container.isSnoozed();
    }

    @Override
    public String getLabelOrDefault(Context context) {
        if (container.getLabel() == null || container.getLabel().length() == 0)
            return context.getString(R.string.default_label);
        return container.getLabel();
    }

    @Override
    public int hashCode() {
        return container.getId();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AlarmCore)) return false;
        final AlarmCore other = (AlarmCore) o;
        return container.getId() == other.container.getId();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("AlarmCore ").append(container.getId());
        sb.append(", ");
        if (container.isEnabled()) {
            sb.append("enabled at ").append(container.getNextTime().getTime().toLocaleString());
        } else {
            sb.append("disabled");
        }
        sb.append(", ");
        if (container.isSnoozed()) {
            sb.append("snoozed at ").append(container.getSnoozedTime().getTime().toLocaleString());
        } else {
            sb.append("no snooze");
        }
        sb.append(", ");
        if (container.isPrealarm()) {
            sb.append("prealarm at ").append(container.getPrealarmTime().getTime().toLocaleString());
        } else {
            sb.append("no prealarm");
        }
        return sb.toString();
    }
}
