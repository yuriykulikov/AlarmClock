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
import com.better.alarm.model.interfaces.Alarm;
import com.better.alarm.model.interfaces.AlarmEditor;
import com.better.alarm.model.interfaces.AlarmEditor.AlarmChangeData;
import com.better.alarm.model.interfaces.Intents;
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
 * State ENABLE
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
 * DISABLED -down-> ENABLE :enable\nchange
 * ENABLED -up-> DISABLED :disable
 * ENABLED -up-> RESCHEDULE :dismiss
 * ENABLED -up-> ENABLE :change\nrefresh
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
 * RESCHEDULE -down-> PREALARM_SET :PA
 * RESCHEDULE -down-> SET :nPA
 * ENABLE -down-> PREALARM_SET :PA
 * ENABLE -down-> SET :nPA
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
        // we always resume SM. This means that initial state will not receive
        // enter(), only resume()
        stateMachine.resume();
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
                stateMachine.sendMessage(AlarmStateMachine.PREALARM_DURATION_CHANGED);
            }
        }
    };

    private void fetchPreAlarmMinutes() {
        String asString = PreferenceManager.getDefaultSharedPreferences(mContext).getString("prealarm_duration", "30");
        prealarmMinutes = Integer.parseInt(asString);
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
        public final EnableTransition enableTransition;
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
            enableTransition = new EnableTransition();
            preAlarmSet = new PreAlarmSetState();
            set = new SetState();
            snoozed = new SnoozedState();
            preAlarmFired = new PreAlarmFiredState();
            preAlarmSnoozed = new PreAlarmSnoozedState();
            fired = new FiredState();

            addState(disabledState);
            addState(enabledState);
            addState(rescheduleTransition);
            addState(enableTransition);
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
                    if (state != enabledState && !(state instanceof ComplexTransition)) {
                        log.d("saving state " + state.getName());
                        container.setState(state.getName());
                    }
                }
            });

            setInitialState(stringToState(initialState));
        }

        private class DisabledState extends AlarmState {
            @Override
            protected void onChange(AlarmChangeData changeData) {
                writeChangeData(changeData);
                broadcastAlarmState(Intents.ACTION_ALARM_CHANGED);
                if (container.isEnabled()) {
                    transitionTo(enableTransition);
                }
            }

            @Override
            protected void onEnable() {
                container.setEnabled(true);
                transitionTo(enableTransition);
            }

            @Override
            protected void onDelete() {
                container.delete();
            }

            @Override
            protected void onRefresh() {
                // nothing to do
            }

            @Override
            protected void onPreAlarmDurationChanged() {
                // nothing to do
            }
        }

        /** Master state for all enabled states. Handles disable and delete */
        private class EnabledState extends AlarmState {
            @Override
            protected void onChange(AlarmChangeData changeData) {
                writeChangeData(changeData);
                broadcastAlarmState(Intents.ACTION_ALARM_CHANGED);
                if (container.isEnabled()) {
                    transitionTo(enableTransition);
                } // else nothing to do
            }

            @Override
            protected void onDismiss() {
                broadcastAlarmState(Intents.ALARM_DISMISS_ACTION);
                transitionTo(rescheduleTransition);
            }

            @Override
            protected void onDisable() {
                container.setEnabled(false);
                broadcastAlarmState(Intents.ALARM_DISMISS_ACTION);
                transitionTo(disabledState);
            }

            @Override
            protected void onRefresh() {
                transitionTo(enableTransition);
            }

            @Override
            protected void onDelete() {
                container.delete();
                removeAlarm();
                broadcastAlarmState(Intents.ALARM_DISMISS_ACTION);
            }

            @Override
            protected void onPreAlarmDurationChanged() {
                // nothing to do
            }

            @Override
            public void exit(Message reason) {
                // TODO maybe this is not necessary here?
                broadcastAlarmState(Intents.ALARM_DISMISS_ACTION);
            }
        }

        private class RescheduleTransition extends ComplexTransition {
            @Override
            public void performComplexTransition() {
                if (container.getDaysOfWeek().isRepeatSet()) {
                    if (container.isPrealarm()) {
                        transitionTo(preAlarmSet);
                    } else {
                        transitionTo(set);
                    }
                } else {
                    log.d("Repeating is not set, disabling the alarm");
                    container.setEnabled(false);
                    transitionTo(disabledState);
                }
            }
        }

        /**
         * Transition checks if preAlarm for the next alarm is in the future.
         * This is required to prevent the situation when user sets alarm in
         * time which is less than preAlarm duration. In this case main alarm
         * should be set.
         */
        private class EnableTransition extends ComplexTransition {
            @Override
            public void performComplexTransition() {
                Calendar preAlarm = calculateNextTime();
                preAlarm.add(Calendar.MINUTE, -1 * prealarmMinutes);
                if (container.isPrealarm() && preAlarm.after(Calendar.getInstance())) {
                    transitionTo(preAlarmSet);
                } else {
                    transitionTo(set);
                }
            }
        }

        private class SetState extends AlarmState {
            @Override
            public void enter(Message reason) {
                broadcastAlarmState(Intents.ACTION_ALARM_SET);
            }

            @Override
            public void resume() {
                Calendar nextTime = calculateNextTime();
                setAlarm(nextTime);
            }

            @Override
            protected void onFired() {
                transitionTo(fired);
            }

            @Override
            protected void onPreAlarmDurationChanged() {
                transitionTo(enableTransition);
            }

            @Override
            public void exit(Message reason) {
                removeAlarm();
            }
        }

        /** handles both snoozed and main for now */
        private class FiredState extends AlarmState {
            @Override
            public void enter(Message reason) {
                broadcastAlarmState(Intents.ALARM_ALERT_ACTION);
                int autoSilenceMinutes = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(mContext)
                        .getString("auto_silence", "10"));
                if (autoSilenceMinutes > 0) {
                    // -1 means OFF
                    Calendar nextTime = Calendar.getInstance();
                    nextTime.add(Calendar.MINUTE, autoSilenceMinutes);
                    setAlarm(nextTime, CalendarType.AUTOSILENCE);
                }
            }

            @Override
            protected void onFired() {
                // TODO actually we have to create a new state for this
                // or maybe not :-)
                broadcastAlarmState(Intents.ACTION_SOUND_EXPIRED);
            }

            @Override
            protected void onSnooze() {
                transitionTo(snoozed);
            }

            @Override
            public void exit(Message reason) {
                broadcastAlarmState(Intents.ALARM_DISMISS_ACTION);
                removeAlarm();
            }
        }

        private class SnoozedState extends AlarmState {
            @Override
            public void enter(Message reason) {
                Calendar nextTime = Calendar.getInstance();
                int snoozeMinutes = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(mContext).getString(
                        "snooze_duration", "10"));
                nextTime.add(Calendar.MINUTE, snoozeMinutes);
                setAlarm(nextTime);
                broadcastAlarmState(Intents.ALARM_SNOOZE_ACTION);
            }

            @Override
            protected void onFired() {
                transitionTo(fired);
            }

            @Override
            public void exit(Message reason) {
                removeAlarm();
                broadcastAlarmState(Intents.ALARM_DISMISS_ACTION);
            }
        }

        // enabled states
        private class PreAlarmSetState extends AlarmState {

            @Override
            public void enter(Message reason) {
                broadcastAlarmState(Intents.ACTION_ALARM_SET);
            }

            @Override
            public void resume() {
                Calendar c = calculateNextTime();
                c.add(Calendar.MINUTE, -1 * prealarmMinutes);
                // since prealarm is before main alarm, it can be already in the
                // past, so it has to be adjusted.
                advanceCalendar(c);
                if (c.after(Calendar.getInstance())) {
                    setAlarm(c);
                } else {
                    // TODO this should never happen
                    log.e("PreAlarm is still in the past!");
                    transitionTo(container.isEnabled() ? enableTransition : disabledState);
                }
            }

            @Override
            protected void onFired() {
                transitionTo(preAlarmFired);
            }

            @Override
            protected void onPreAlarmDurationChanged() {
                transitionTo(enableTransition);
            }

            @Override
            public void exit(Message reason) {
                removeAlarm();
            }
        }

        private class PreAlarmFiredState extends AlarmState {
            @Override
            public void enter(Message reason) {
                broadcastAlarmState(Intents.ALARM_PREALARM_ACTION);
                setAlarm(calculateNextTime());
            }

            @Override
            protected void onFired() {
                transitionTo(fired);
            }

            @Override
            protected void onSnooze() {
                transitionTo(preAlarmSnoozed);
            }

            @Override
            protected void onPreAlarmTimedOut() {
                transitionTo(fired);
            }

            @Override
            public void exit(Message reason) {
                removeAlarm();
                broadcastAlarmState(Intents.ALARM_DISMISS_ACTION);
            }
        }

        private class PreAlarmSnoozedState extends AlarmState {
            @Override
            public void enter(Message reason) {
                setAlarm(calculateNextTime());
                broadcastAlarmState(Intents.ALARM_SNOOZE_ACTION);
            }

            @Override
            protected void onFired() {
                transitionTo(fired);
            }

            @Override
            public void exit(Message reason) {
                removeAlarm();
                broadcastAlarmState(Intents.ALARM_DISMISS_ACTION);
            }
        }

        private void broadcastAlarmState(String action) {
            log.d(container.getId() + " - " + action);
            broadcaster.broadcastAlarmState(container.getId(), action);
        }

        private void setAlarm(Calendar calendar) {
            setAlarm(calendar, CalendarType.NORMAL);
        }

        private void setAlarm(Calendar calendar, CalendarType calendarType) {
            mAlarmsScheduler.setAlarm(container.getId(), calendarType, calendar);
            container.setNextTime(calendar);
        }

        private void removeAlarm() {
            mAlarmsScheduler.removeAlarm(container.getId());
        }

        private void writeChangeData(AlarmChangeData data) {
            container.setHour(data.hour);
            container.setAlert(data.alert);
            container.setDaysOfWeek(data.daysOfWeek);
            container.setLabel(data.label);
            container.setMinutes(data.minutes);
            container.setPrealarm(data.prealarm);
            container.setVibrate(data.vibrate);
            // this will cause a DB flush
            // TODO better solution, e.g. edit/commit
            container.setEnabled(data.enabled);
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

        private class AlarmState extends State {
            private boolean handled;

            @Override
            public final boolean processMessage(Message msg) {
                handled = true;
                switch (msg.what) {
                case ENABLE:
                    onEnable();
                    break;
                case DISABLE:
                    onDisable();
                    break;
                case SNOOZE:
                    onSnooze();
                    break;
                case DISMISS:
                    onDismiss();
                    break;
                case CHANGE:
                    onChange((AlarmChangeData) msg.obj);
                    break;
                case FIRED:
                    onFired();
                    break;
                case PREALARM_DURATION_CHANGED:
                    onPreAlarmDurationChanged();
                    break;
                case PREALARM_TIMED_OUT:
                    onPreAlarmTimedOut();
                    break;
                case REFRESH:
                    onRefresh();
                    break;
                case DELETE:
                    onDelete();
                    break;
                default:
                    throw new RuntimeException("Handling of message code " + msg.what + " is not implemented");
                }
                return handled;
            }

            protected final void markNotHandled() {
                handled = false;
            }

            protected void onEnable() {
                markNotHandled();
            }

            protected void onDisable() {
                markNotHandled();
            }

            protected void onSnooze() {
                markNotHandled();
            }

            protected void onDismiss() {
                markNotHandled();
            }

            protected void onChange(AlarmChangeData changeData) {
                markNotHandled();
            }

            protected void onFired() {
                markNotHandled();
            }

            protected void onPreAlarmDurationChanged() {
                markNotHandled();
            }

            protected void onPreAlarmTimedOut() {
                markNotHandled();
            }

            protected void onRefresh() {
                markNotHandled();
            }

            protected void onDelete() {
                markNotHandled();
            }
        }
    }

    public void onAlarmFired(CalendarType calendarType) {
        stateMachine.sendMessage(AlarmStateMachine.FIRED);
    }

    public void refresh() {
        stateMachine.sendMessage(AlarmStateMachine.REFRESH);
    }

    public void change(AlarmChangeData data) {
        Message msg = stateMachine.obtainMessage();
        msg.what = AlarmStateMachine.CHANGE;
        msg.obj = data;
        msg.sendToTarget();
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

    @Override
    public AlarmEditor edit() {
        return new AlarmEditor(this);
    }
}
