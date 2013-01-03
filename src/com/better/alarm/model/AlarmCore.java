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
import java.util.WeakHashMap;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;

import com.better.alarm.R;
import com.github.androidutils.logger.Logger;
import com.github.androidutils.statemachine.ComplexTransition;
import com.github.androidutils.statemachine.IMessageWhatToStringConverter;
import com.github.androidutils.statemachine.IOnStateChangedListener;
import com.github.androidutils.statemachine.IState;
import com.github.androidutils.statemachine.State;
import com.github.androidutils.statemachine.StateMachine;

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
 * State RESCHEDULE
 * State ENABLED {
 * State PREALARM_SET
 * State SET
 * State FIRED
 * State PREALARM_FIRED
 * State SNOOZED
 * State PREALARM_SNOOZED
 * RESCHEDULE :Complex transitiontran
 * PREALARM_FIRED :timer
 * SNOOZED :timer
 * PREALARM_SNOOZED :timer
 * PREALARM_SET :timer
 * SET :timer
 * 
 * DISABLED -down-> RESCHEDULE :enable\nchange
 * ENABLED -up-> DISABLED :disable
 * ENABLED -up-> RESCHEDULE :change\ndismiss
 * 
 * PREALARM_SET -down-> PREALARM_FIRED :fired
 * PREALARM_FIRED -down-> PREALARM_SNOOZED :snooze
 * PREALARM_SNOOZED -up-> FIRED
 * SET -down-> FIRED :fired
 * PREALARM_FIRED --right--> FIRED :fired
 * FIRED -down->  SNOOZED :snooze
 * SNOOZED -up-> FIRED :fired
 * 
 * RESCHEDULE -up-> DISABLED :disabled
 * 
 * RESCHEDULE --down-> PREALARM_SET :prealarm enabled
 * RESCHEDULE -down-> SET :prealarm disabled
 * 
 * 
 * } 
 * @enduml
 * </pre>
 * 
 * @author Yuriy
 * 
 */
public final class AlarmCore implements Alarm {

    /**
     * Strategy used to notify other components about alarm state.
     */
    public interface IStateNotifier {
        public void broadcastAlarmState(int id, String action);

    }

    private final IAlarmsScheduler mAlarmsScheduler;
    private final Logger log;
    private final Context mContext;

    private final IStateNotifier broadcaster;

    private final IAlarmContainer container;

    /**
     * Used to calculate calendars. Is not synced with DB, because it is in the
     * settings
     */
    private int prealarmMinutes;

    private final AlarmStateMachine stateMachine;

    public AlarmCore(IAlarmContainer container, Context context, Logger logger, IAlarmsScheduler alarmsScheduler,
            IStateNotifier broadcaster) {
        mContext = context;
        this.log = logger;
        mAlarmsScheduler = alarmsScheduler;
        this.container = container;
        this.broadcaster = broadcaster;

        PreferenceManager.getDefaultSharedPreferences(mContext).registerOnSharedPreferenceChangeListener(
                mOnSharedPreferenceChangeListener);

        stateMachine = new AlarmStateMachine(container.getState());
        stateMachine.start();
        fetchPreAlarmMinutes();
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
            }
        }
    };

    private void fetchPreAlarmMinutes() {
        String asString = PreferenceManager.getDefaultSharedPreferences(mContext).getString("prealarm_duration", "30");
        prealarmMinutes = Integer.parseInt(asString);
        stateMachine.sendMessage(AlarmStateMachine.PREALARM_DURATION_CHANGED);
    }

    /** SM to handle Alarm states */
    private class AlarmStateMachine extends StateMachine {
        public static final int ENABLE = 1;
        public static final int DISABLE = 2;
        public static final int SNOOZE = 3;
        public static final int DISMISS = 4;
        public static final int CHANGE = 5;
        public static final int FIRED = 6;
        public static final int PREALARM_DURATION_CHANGED = 7;
        public static final int PREALARM_TIMED_OUT = 8;
        public static final int REFRESH = 9;
        public static final int DELETE = 10;

        public final DisabledState disabledState;
        public final EnabledState enabledState;
        public final RescheduleTransition rescheduleTransition;
        public final PreAlarmSetState preAlarmSet;
        public final SetState set;
        public final SnoozedState snoozed;
        public final PreAlarmFiredState preAlarmFired;
        public final PreAlarmSnoozedState preAlarmSnoozed;
        public final FiredState fired;

        public AlarmStateMachine(String initialState) {
            super("AlarmSM", Looper.getMainLooper(), log);
            disabledState = new DisabledState();
            enabledState = new EnabledState();
            rescheduleTransition = new RescheduleTransition();
            preAlarmSet = new PreAlarmSetState();
            set = new SetState();
            snoozed = new SnoozedState();
            preAlarmFired = new PreAlarmFiredState();
            preAlarmSnoozed = new PreAlarmSnoozedState();
            fired = new FiredState();

            addState(disabledState);
            addState(enabledState);
            addState(rescheduleTransition);
            addState(preAlarmSet, enabledState);
            addState(set, enabledState);
            addState(snoozed, enabledState);
            addState(preAlarmFired, enabledState);
            addState(fired, enabledState);
            addState(preAlarmSnoozed, enabledState);

            setDbg(true);
            setMessageWhatToStringConverter(new IMessageWhatToStringConverter() {
                @Override
                public String messageWhatToString(int what) {
                    switch (what) {
                    case ENABLE:
                        return "ENABLE";
                    case DISABLE:
                        return "DISABLE";
                    case SNOOZE:
                        return "SNOOZE";
                    case DISMISS:
                        return "DISMISS";
                    case CHANGE:
                        return "CHANGE";
                    case FIRED:
                        return "FIRED";
                    case PREALARM_DURATION_CHANGED:
                        return "PREALARM_DURATION_CHANGED";
                    case PREALARM_TIMED_OUT:
                        return "PREALARM_TIMED_OUT";
                    case REFRESH:
                        return "REFRESH";
                    case DELETE:
                        return "DELETE";
                    default:
                        return "UNKNOWN";
                    }
                }
            });

            addOnStateChangedListener(new IOnStateChangedListener() {
                @Override
                public void onStateChanged(IState state) {
                    if (state != enabledState && state != rescheduleTransition) {
                        log.d("saving state " + state.getName());
                        container.setState(state.getName());
                        container.writeToDb();
                    }
                }
            });

            setInitialState(stringToState(initialState));
        }

        private class DisabledState extends State {
            @Override
            public boolean processMessage(Message msg) {
                switch (msg.what) {
                case CHANGE:
                    AlarmChangeData data = (AlarmChangeData) msg.obj;
                    writeChangeData(data);
                    transitionTo(rescheduleTransition);
                    return HANDLED;
                case ENABLE:
                    container.setEnabled(true);
                    transitionTo(rescheduleTransition);
                    return HANDLED;
                case DELETE:
                    container.delete();
                    return HANDLED;
                }
                return NOT_HANDLED;
            }
        }

        /** Master state for all enabled states. Handles disable and delete */
        private class EnabledState extends State {
            @Override
            public boolean processMessage(Message msg) {
                switch (msg.what) {
                case CHANGE:
                    AlarmChangeData data = (AlarmChangeData) msg.obj;
                    writeChangeData(data);
                    transitionTo(rescheduleTransition);
                    return HANDLED;
                case DISMISS:
                    broadcastAlarmState(Intents.ALARM_DISMISS_ACTION);
                    rescheduleRepeatingAlarm();
                    return HANDLED;
                case DISABLE:
                    container.setEnabled(false);
                    broadcastAlarmState(Intents.ALARM_DISMISS_ACTION);
                    transitionTo(disabledState);
                    return HANDLED;
                case REFRESH:
                    Calendar now = Calendar.getInstance();
                    boolean isExpired = getNextTime().before(now);
                    if (isExpired) {
                        log.d("AlarmCore expired: " + toString());
                        rescheduleRepeatingAlarm();
                    }
                    return HANDLED;
                case DELETE:
                    container.delete();
                    mAlarmsScheduler.removeAlarm(container.getId());
                    broadcastAlarmState(Intents.ALARM_DISMISS_ACTION);
                    return HANDLED;
                }
                return NOT_HANDLED;
            }

            @Override
            public void exit() {
                // TODO maybe this is not necessary here?
                broadcastAlarmState(Intents.ALARM_DISMISS_ACTION);
            }

            private void rescheduleRepeatingAlarm() {
                if (container.getDaysOfWeek().isRepeatSet()) {
                    transitionTo(rescheduleTransition);
                } else {
                    transitionTo(disabledState);
                }
            }
        }

        private class RescheduleTransition extends ComplexTransition {
            @Override
            public void performComplexTransition() {
                if (container.isEnabled()) {
                    if (container.isPrealarm()) {
                        transitionTo(preAlarmSet);
                    } else {
                        transitionTo(set);
                    }
                } else {
                    transitionTo(disabledState);
                }
            }
        }

        private class SetState extends State {
            @Override
            public void enter() {
                Calendar nextTime = calculateNextTime();
                setAlarm(nextTime);
            }

            @Override
            public boolean processMessage(Message msg) {
                switch (msg.what) {
                case FIRED:
                    transitionTo(fired);
                    return HANDLED;
                case PREALARM_DURATION_CHANGED:
                    // TODO
                    log.d("here we do somehting");
                    return HANDLED;
                }
                return NOT_HANDLED;
            }

            @Override
            public void exit() {
                removeAlarm();
            }
        }

        /** handles both snoozed and main for now */
        private class FiredState extends State {
            @Override
            public void enter() {
                broadcastAlarmState(Intents.ALARM_ALERT_ACTION);
            }

            @Override
            public boolean processMessage(Message msg) {
                switch (msg.what) {
                case SNOOZE:
                    transitionTo(snoozed);
                    return HANDLED;
                }
                return NOT_HANDLED;
            }

            @Override
            public void exit() {
                broadcastAlarmState(Intents.ALARM_DISMISS_ACTION);
            }
        }

        private class SnoozedState extends State {
            @Override
            public void enter() {
                Calendar nextTime = Calendar.getInstance();
                int snoozeMinutes = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(mContext).getString(
                        "snooze_duration", "10"));
                nextTime.add(Calendar.MINUTE, snoozeMinutes);
                setAlarm(nextTime);
                broadcastAlarmState(Intents.ALARM_SNOOZE_ACTION);
            }

            @Override
            public boolean processMessage(Message msg) {
                switch (msg.what) {
                case FIRED:
                    transitionTo(fired);
                    return HANDLED;
                }
                return NOT_HANDLED;
            }

            @Override
            public void exit() {
                removeAlarm();
                broadcastAlarmState(Intents.ALARM_DISMISS_ACTION);
            }
        }

        // enabled states
        private class PreAlarmSetState extends State {
            @Override
            public void enter() {
                Calendar c = calculateNextTime();
                c.add(Calendar.MINUTE, -1 * prealarmMinutes);
                if (c.after(Calendar.getInstance())) {
                    setAlarm(c);
                } else {
                    // if prealarm is already in the past
                    transitionTo(set);
                }
            }

            @Override
            public boolean processMessage(Message msg) {
                switch (msg.what) {
                case FIRED:
                    transitionTo(preAlarmFired);
                    return HANDLED;
                case PREALARM_DURATION_CHANGED:
                    // TODO
                    log.d("here we do somehting");
                    return HANDLED;
                }
                return NOT_HANDLED;
            }

            @Override
            public void exit() {
                removeAlarm();
            }
        }

        private class PreAlarmFiredState extends State {
            @Override
            public void enter() {
                broadcastAlarmState(Intents.ALARM_PREALARM_ACTION);
                setAlarm(calculateNextTime());
            }

            @Override
            public boolean processMessage(Message msg) {
                switch (msg.what) {
                case FIRED:
                    transitionTo(fired);
                    return HANDLED;
                case SNOOZE:
                    transitionTo(preAlarmSnoozed);
                    return HANDLED;
                case PREALARM_TIMED_OUT:
                    transitionTo(fired);
                    return HANDLED;
                }
                return NOT_HANDLED;
            }

            @Override
            public void exit() {
                removeAlarm();
                broadcastAlarmState(Intents.ALARM_DISMISS_ACTION);
            }
        }

        private class PreAlarmSnoozedState extends State {
            @Override
            public void enter() {
                setAlarm(calculateNextTime());
                broadcastAlarmState(Intents.ALARM_SNOOZE_ACTION);
            }

            @Override
            public boolean processMessage(Message msg) {
                switch (msg.what) {
                case FIRED:
                    transitionTo(fired);
                    return HANDLED;
                }
                return NOT_HANDLED;
            }

            @Override
            public void exit() {
                removeAlarm();
                broadcastAlarmState(Intents.ALARM_DISMISS_ACTION);
            }
        }

        private void broadcastAlarmState(String action) {
            log.d(container.getId() + " - " + action);
            broadcaster.broadcastAlarmState(container.getId(), action);
        }

        private void setAlarm(Calendar calendar) {
            HashMap<CalendarType, Calendar> calendars = new HashMap<CalendarType, Calendar>();
            calendars.put(CalendarType.NORMAL, calendar);
            mAlarmsScheduler.setAlarm(container.getId(), calendars);
            container.setNextTime(calendar);
            container.writeToDb();
        }

        private void removeAlarm() {
            mAlarmsScheduler.removeAlarm(container.getId());
        }

        private void writeChangeData(AlarmChangeData data) {
            container.setEnabled(data.enabled);
            container.setHour(data.hour);
            container.setAlert(data.alert);
            container.setDaysOfWeek(data.daysOfWeek);
            container.setLabel(data.label);
            container.setMinutes(data.minutes);
            container.setPrealarm(data.prealarm);
            container.setVibrate(data.vibrate);
            container.writeToDb();
        }

        private Calendar calculateNextTime() {
            Calendar c = Calendar.getInstance();
            c.set(Calendar.HOUR_OF_DAY, container.getHour());
            c.set(Calendar.MINUTE, container.getMinutes());
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);
            advanceCalendar(c);
            return c;
        }

        private void advanceCalendar(Calendar calendar) {
            Calendar now = Calendar.getInstance();
            // if alarm is behind current time, advance one day
            if (calendar.before(now)) {
                calendar.add(Calendar.DAY_OF_YEAR, 1);
            }
            int addDays = container.getDaysOfWeek().getNextAlarm(calendar);
            if (addDays > 0) {
                calendar.add(Calendar.DAY_OF_WEEK, addDays);
            }
        }

        private State stringToState(String initialState) {
            if ("".equals(initialState)) {
                log.d("new Alarm - DisabledState");
                return disabledState;
            }
            for (State state : getStates()) {
                if (state.getName().equals(initialState)) return state;
            }
            log.d("wtf? state not found");
            return disabledState;
        }
    }

    private class AlarmChangeData {
        public boolean prealarm;
        public Uri alert;
        public String label;
        public boolean vibrate;
        public DaysOfWeek daysOfWeek;
        public int hour;
        public int minutes;
        public boolean enabled;
    }

    public void onAlarmFired(CalendarType calendarType) {
        stateMachine.sendMessage(AlarmStateMachine.FIRED);
    }

    public void refresh() {
        stateMachine.sendMessage(AlarmStateMachine.REFRESH);
    }

    @Override
    public void enable(boolean enable) {
        stateMachine.sendMessage(enable ? AlarmStateMachine.ENABLE : AlarmStateMachine.DISABLE);
    }

    @Override
    public void snooze() {
        stateMachine.sendMessage(AlarmStateMachine.SNOOZE);
    }

    @Override
    public void dismiss() {
        stateMachine.sendMessage(AlarmStateMachine.DISMISS);
    }

    @Override
    public void delete() {
        stateMachine.sendMessage(AlarmStateMachine.DELETE);
    }

    @Override
    public void change(boolean enabled, int hour, int minute, DaysOfWeek daysOfWeek, boolean vibrate, String label,
            Uri alert, boolean preAlarm) {
        AlarmChangeData data = new AlarmChangeData();
        data.prealarm = preAlarm;
        data.alert = alert;
        data.label = label;
        data.vibrate = vibrate;
        data.daysOfWeek = daysOfWeek;
        data.hour = hour;
        data.minutes = minute;
        data.enabled = enabled;

        Message msg = stateMachine.obtainMessage();
        msg.what = AlarmStateMachine.CHANGE;
        msg.obj = data;
        msg.sendToTarget();
    }

    // ++++++++++++++++++++++++++++++++++++++++++++++++++++++
    // ++++++ getters for GUI +++++++++++++++++++++++++++++++
    // ++++++++++++++++++++++++++++++++++++++++++++++++++++++

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
        // TODO this might not work :-)
        // actually these getters should be replaced with extras to intents
        return container.getNextTime();
    }

    @Override
    public Calendar getPrealarmTime() {
        // TODO this might not work :-)
        // actually these getters should be replaced with extras to intents
        return container.getNextTime();
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

    /**
     * this is only valid from the main thread
     */
    @Override
    public boolean isSnoozed() {
        return stateMachine.getCurrentState().equals(stateMachine.snoozed);
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
        sb.append(" in ").append(stateMachine.getCurrentState().getName());
        DateFormat df = DateFormat.getDateTimeInstance();
        sb.append(" on ").append(df.format(container.getNextTime().getTime()));
        return sb.toString();
    }
}
