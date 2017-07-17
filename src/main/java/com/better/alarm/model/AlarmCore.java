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

import android.net.Uri;

import com.better.alarm.ImmutableAlarmSet;
import com.better.alarm.Prefs;
import com.better.alarm.Store;
import com.better.alarm.interfaces.Alarm;
import com.better.alarm.interfaces.ImmutableAlarmEditor;
import com.better.alarm.interfaces.Intents;
import com.better.alarm.logger.Logger;
import com.better.alarm.statemachine.ComplexTransition;
import com.better.alarm.statemachine.HandlerFactory;
import com.better.alarm.statemachine.IOnStateChangedListener;
import com.better.alarm.statemachine.IState;
import com.better.alarm.statemachine.Message;
import com.better.alarm.statemachine.State;
import com.better.alarm.statemachine.StateMachine;
import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import io.reactivex.Observable;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;

/**
 * Alarm is a class which models a real word alarm. It is a simple state
 * machine. External events (e.g. user input {@link #snooze()} or
 * {@link #dismiss()}) or timer events {@link #onAlarmFired(CalendarType)}
 * trigger transitions. Alarm notifies listeners when transitions happen by
 * broadcasting Intents listed in {@link Intents}, e.g.
 * {@link Intents#ALARM_PREALARM_ACTION} or {@link Intents#ALARM_DISMISS_ACTION}
 * . State and properties of the alarm are stored in the database and are
 * updated every time when changes to alarm happen.
 * <p>
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
 */
public final class AlarmCore implements Alarm, Consumer<AlarmChangeData> {
    private final IAlarmsScheduler mAlarmsScheduler;
    private final Logger log;
    private final IStateNotifier broadcaster;
    private ImmutableAlarmContainer container;
    private final AlarmStateMachine stateMachine;
    private final DateFormat df;

    private final Observable<Integer> preAlarmDuration;
    private final Observable<Integer> snoozeDuration;
    private final Observable<Integer> autoSilence;

    private final Store store;
    private final Calendars calendars;

    @AutoFactory
    public AlarmCore(AlarmContainer container, @Provided Logger logger, @Provided IAlarmsScheduler alarmsScheduler,
                     @Provided IStateNotifier broadcaster, @Provided HandlerFactory handlerFactory, @Provided Prefs prefs, @Provided Store store, @Provided Calendars calendars) {
        this.log = logger;
        this.calendars = calendars;
        this.mAlarmsScheduler = alarmsScheduler;
        this.container = ImmutableAlarmContainer.copyOf(container);
        this.broadcaster = broadcaster;
        this.df = new SimpleDateFormat("dd-MM-yy HH:mm:ss", Locale.GERMANY);

        this.preAlarmDuration = prefs.preAlarmDuration();
        this.snoozeDuration = prefs.snoozeDuration();
        this.autoSilence = prefs.autoSilence();

        this.store = store;

        stateMachine = new AlarmStateMachine(container.getState(), "Alarm " + container.getId(), handlerFactory);
        // we always resume SM. This means that initial state will not receive
        // enter(), only resume()
        stateMachine.resume();

        updateListInStore();

        preAlarmDuration.subscribe(new Consumer() {
            @Override
            public void accept(Object o) throws Exception {
                stateMachine.sendMessage(AlarmStateMachine.PREALARM_DURATION_CHANGED);
            }
        });
    }

    /**
     * Strategy used to notify other components about alarm state.
     */
    public interface IStateNotifier {
        void broadcastAlarmState(int id, String action);
    }

    /**
     * SM to handle Alarm states
     */
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
        public static final int TIME_SET = 11;

        public final DisabledState disabledState;
        public final DeletedState deletedState;
        public final EnabledState enabledState;
        public final RescheduleTransition rescheduleTransition;
        public final EnableTransition enableTransition;
        public final PreAlarmSetState preAlarmSet;
        public final SetState set;
        public final SnoozedState snoozed;
        public final PreAlarmFiredState preAlarmFired;
        public final PreAlarmSnoozedState preAlarmSnoozed;
        public final FiredState fired;

        public AlarmStateMachine(String initialState, String name, HandlerFactory handlerFactory) {
            super(name, handlerFactory, log);
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
            deletedState = new DeletedState();

            addState(disabledState);
            addState(enabledState);
            addState(deletedState);
            addState(rescheduleTransition);
            addState(enableTransition);
            addState(preAlarmSet, enabledState);
            addState(set, enabledState);
            addState(snoozed, enabledState);
            addState(preAlarmFired, enabledState);
            addState(fired, enabledState);
            addState(preAlarmSnoozed, enabledState);

            addOnStateChangedListener(new IOnStateChangedListener() {
                @Override
                public void onStateChanged(IState state) {
                    if (state != enabledState && !(state instanceof ComplexTransition)) {
                        log.d("saving state " + state.getName());
                        container = container.withState(state.getName());
                    }
                }
            });

            setInitialState(stringToState(initialState));
        }

        private class DeletedState extends AlarmState {
            @Override
            public void enter() {
                removeAlarm();
                container.delete();
                removeFromStore();
            }

            @Override
            protected void onRefresh() {
                // nothing to do
            }

            @Override
            protected void onTimeSet() {
                // nothing to do
            }

            @Override
            protected void onPreAlarmDurationChanged() {
                // nothing to do
            }
        }

        private class DisabledState extends AlarmState {
            @Override
            public void enter() {
                updateListInStore();
            }

            @Override
            protected void onChange(AlarmChangeData changeData) {
                writeChangeData(changeData);
                updateListInStore();
                if (container.isEnabled()) {
                    transitionTo(enableTransition);
                }
            }

            @Override
            protected void onEnable() {
                container = container.withIsEnabled(true);
                transitionTo(enableTransition);
            }

            @Override
            protected void onDelete() {
                transitionTo(deletedState);
            }

            @Override
            protected void onRefresh() {
                // nothing to do
            }

            @Override
            protected void onTimeSet() {
                // nothing to do
            }

            @Override
            protected void onPreAlarmDurationChanged() {
                // nothing to do
            }
        }

        /**
         * Master state for all enabled states. Handles disable and delete
         */
        private class EnabledState extends AlarmState {
            @Override
            public void enter() {
                updateListInStore();
            }

            @Override
            protected void onChange(AlarmChangeData changeData) {
                writeChangeData(changeData);
                updateListInStore();
                if (container.isEnabled()) {
                    transitionTo(enableTransition);
                } // else nothing to do
            }

            @Override
            protected void onDismiss() {
                transitionTo(rescheduleTransition);
            }

            @Override
            protected void onDisable() {
                container = container.withIsEnabled(false);
                transitionTo(disabledState);
            }

            @Override
            protected void onRefresh() {
                transitionTo(enableTransition);
            }

            @Override
            protected void onTimeSet() {
                transitionTo(enableTransition);
            }

            @Override
            protected void onDelete() {
                transitionTo(deletedState);
            }

            @Override
            protected void onPreAlarmDurationChanged() {
                // nothing to do
            }
        }

        private class RescheduleTransition extends ComplexTransition {
            @Override
            public void performComplexTransition() {
                if (container.getDaysOfWeek().isRepeatSet()) {
                    if (container.isPrealarm() && preAlarmDuration.blockingFirst() != -1) {
                        transitionTo(preAlarmSet);
                    } else {
                        transitionTo(set);
                    }
                } else {
                    log.d("Repeating is not set, disabling the alarm");
                    container = container.withIsEnabled(false);
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
                Integer preAlarmMinutes = preAlarmDuration.blockingFirst();
                preAlarm.add(Calendar.MINUTE, -1 * preAlarmMinutes);
                if (container.isPrealarm() && preAlarm.after(calendars.now()) && preAlarmMinutes != -1) {
                    transitionTo(preAlarmSet);
                } else {
                    transitionTo(set);
                }
            }
        }

        private class SetState extends AlarmState {
            @Override
            public void enter() {
                int what = getCurrentMessage().what();
                if (what == DISMISS || what == SNOOZE || what == CHANGE) {
                    broadcastAlarmSetWithNormalTime(calculateNextTime().getTimeInMillis());
                }
            }

            @Override
            public void resume() {
                Calendar nextTime = calculateNextTime();
                setAlarm(nextTime, CalendarType.NORMAL);
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
            public void exit() {
                if (!alarmWillBeRescheduled(getCurrentMessage())) {
                    removeAlarm();
                }
            }
        }

        /**
         * handles both snoozed and main for now
         */
        private class FiredState extends AlarmState {
            @Override
            public void enter() {
                broadcastAlarmState(Intents.ALARM_ALERT_ACTION);
                int autoSilenceMinutes = autoSilence.blockingFirst();
                if (autoSilenceMinutes > 0) {
                    // -1 means OFF
                    Calendar nextTime = calendars.now();
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
            protected void onTimeSet() {
                // nothing to do
            }

            @Override
            public void exit() {
                broadcastAlarmState(Intents.ALARM_DISMISS_ACTION);
                removeAlarm();
            }
        }

        private class SnoozedState extends AlarmState {
            @Override
            public void enter() {
                Calendar nextTime;
                Calendar now = calendars.now();
                Message reason = getCurrentMessage();
                if (reason.obj().isPresent()) {
                    Calendar customTime = calendars.now();
                    //TODO pass an object, dont misuse these poor args
                    customTime.set(Calendar.HOUR_OF_DAY, reason.arg1().get());
                    customTime.set(Calendar.MINUTE, reason.arg2().get());
                    if (customTime.after(now)) {
                        nextTime = customTime;
                    } else {
                        nextTime = getNextRegualarSnoozeCalendar();
                    }
                } else {
                    nextTime = getNextRegualarSnoozeCalendar();
                }

                setAlarm(nextTime, CalendarType.NORMAL);
                broadcastAlarmState(Intents.ALARM_SNOOZE_ACTION);
            }

            private Calendar getNextRegualarSnoozeCalendar() {
                Calendar nextTime = calendars.now();
                int snoozeMinutes = snoozeDuration.blockingFirst();
                nextTime.add(Calendar.MINUTE, snoozeMinutes);
                return nextTime;
            }

            @Override
            protected void onFired() {
                transitionTo(fired);
            }

            @Override
            protected void onTimeSet() {
                // Do nothing
            }

            @Override
            protected void onSnooze() {
                enter();
            }

            @Override
            public void exit() {
                removeAlarm();
                broadcastAlarmState(Intents.ACTION_CANCEL_SNOOZE);
            }
        }

        // enabled states
        private class PreAlarmSetState extends AlarmState {

            @Override
            public void enter() {
                int what = getCurrentMessage().what();
                if (what == DISMISS || what == SNOOZE || what == CHANGE) {
                    broadcastAlarmSetWithNormalTime(calculateNextTime().getTimeInMillis());
                }

                updateListInStore();
            }

            @Override
            public void resume() {
                Calendar c = calculateNextTime();
                c.add(Calendar.MINUTE, -1 * preAlarmDuration.blockingFirst());
                // since prealarm is before main alarm, it can be already in the
                // past, so it has to be adjusted.
                advanceCalendar(c);
                if (c.after(calendars.now())) {
                    setAlarm(c, CalendarType.PREALARM);
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
            public void exit() {
                if (!alarmWillBeRescheduled(getCurrentMessage())) {
                    removeAlarm();
                }
            }
        }

        private class PreAlarmFiredState extends AlarmState {
            @Override
            public void enter() {
                broadcastAlarmState(Intents.ALARM_PREALARM_ACTION);
                setAlarm(calculateNextTime(), CalendarType.NORMAL);
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
            protected void onTimeSet() {
                // nothing to do
            }

            @Override
            public void exit() {
                removeAlarm();
                broadcastAlarmState(Intents.ALARM_DISMISS_ACTION);
            }
        }

        private class PreAlarmSnoozedState extends AlarmState {
            @Override
            public void enter() {
                Calendar nextTime;
                Calendar now = calendars.now();
                Message reason = getCurrentMessage();
                if (reason.obj().isPresent()) {
                    Calendar customTime = calendars.now();
                    customTime.set(Calendar.HOUR_OF_DAY, reason.arg1().get());
                    customTime.set(Calendar.MINUTE, reason.arg2().get());
                    if (customTime.after(now)) {
                        nextTime = customTime;
                    } else {
                        nextTime = calculateNextTime();
                    }
                } else {
                    nextTime = calculateNextTime();
                }

                setAlarm(nextTime, CalendarType.NORMAL);
                broadcastAlarmState(Intents.ALARM_SNOOZE_ACTION);
            }

            @Override
            protected void onFired() {
                transitionTo(fired);
            }

            @Override
            protected void onSnooze() {
                enter();
            }

            @Override
            protected void onTimeSet() {
                // nothing to do
            }

            @Override
            public void exit() {
                removeAlarm();
                broadcastAlarmState(Intents.ACTION_CANCEL_SNOOZE);
            }
        }

        private void broadcastAlarmState(String action) {
            log.d(container.getId() + " - " + action);
            broadcaster.broadcastAlarmState(container.getId(), action);
            updateListInStore();
        }

        private void broadcastAlarmSetWithNormalTime(long millis) {
            store.sets().onNext(ImmutableAlarmSet.of(container, millis));
            updateListInStore();
        }

        private void setAlarm(Calendar calendar, CalendarType calendarType) {
            mAlarmsScheduler.setAlarm(container.getId(), calendarType, calendar, container);
            container = container.withNextTime(calendar);
        }

        private void removeAlarm() {
            mAlarmsScheduler.removeAlarm(container.getId());
        }

        private void removeFromStore() {
            store.alarms().take(1).subscribe(new Consumer<List<AlarmValue>>() {
                @Override
                public void accept(@NonNull List<AlarmValue> alarmValues) throws Exception {
                    List<AlarmValue> copy = new ArrayList<AlarmValue>(alarmValues);

                    AlarmValue old = Collections2.filter(alarmValues, new Predicate<AlarmValue>() {
                        @Override
                        public boolean apply(AlarmValue input) {
                            return input.getId() == container.getId();
                        }
                    }).iterator().next();
                    copy.remove(alarmValues.indexOf(old));

                    store.alarms().onNext(copy);
                }
            });
        }

        private void writeChangeData(AlarmChangeData data) {
            container = ImmutableAlarmContainer.builder()
                    .from(container)
                    .from(data)
                    .build();
        }

        private Calendar calculateNextTime() {
            Calendar c = calendars.now();
            c.set(Calendar.HOUR_OF_DAY, container.getHour());
            c.set(Calendar.MINUTE, container.getMinutes());
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);
            advanceCalendar(c);
            return c;
        }

        private void advanceCalendar(Calendar calendar) {
            Calendar now = calendars.now();
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

        private boolean alarmWillBeRescheduled(Message reason) {
            boolean alarmWillBeRescheduled = reason.what() == CHANGE && ((AlarmChangeData) reason.obj().get()).isEnabled();
            return alarmWillBeRescheduled;
        }

        private class AlarmState extends State {
            private boolean handled;

            @Override
            public final boolean processMessage(Message msg) {
                handled = true;
                switch (msg.what()) {
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
                        onChange((AlarmChangeData) msg.obj().get());
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
                    case TIME_SET:
                        onTimeSet();
                        break;
                    case DELETE:
                        onDelete();
                        break;
                    default:
                        throw new RuntimeException("Handling of message code " + msg.what() + " is not implemented");
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

            protected void onTimeSet() {
                markNotHandled();
            }

            protected void onDelete() {
                markNotHandled();
            }
        }
    }

    private void updateListInStore() {
        final AlarmValue toStore = container;

        log.d("Storing " + toStore);

        store.alarms().take(1).subscribe(new Consumer<List<AlarmValue>>() {
            @Override
            public void accept(@NonNull List<AlarmValue> alarmValues) throws Exception {

                Iterator<AlarmValue> optional = Collections2.filter(alarmValues, new Predicate<AlarmValue>() {
                    @Override
                    public boolean apply(AlarmValue input) {
                        return input.getId() == container.getId();
                    }
                }).iterator();

                List<AlarmValue> copy = new ArrayList<AlarmValue>(alarmValues);

                if (optional.hasNext()) {
                    AlarmValue old = optional.next();
                    copy.set(alarmValues.indexOf(old), toStore);
                } else {
                    copy.add(toStore);
                }

                store.alarms().onNext(copy);
            }
        });
    }

    /**
     * for {@link #edit()}
     */
    @Override
    public void accept(@NonNull AlarmChangeData alarmChangeData) throws Exception {
        change(alarmChangeData);
    }

    public void onAlarmFired(CalendarType calendarType) {
        stateMachine.sendMessage(AlarmStateMachine.FIRED);
    }

    public void refresh() {
        stateMachine.sendMessage(AlarmStateMachine.REFRESH);
    }

    public void onTimeSet() {
        stateMachine.sendMessage(AlarmStateMachine.TIME_SET);
    }

    public void change(AlarmChangeData data) {
        stateMachine.obtainMessage(AlarmStateMachine.CHANGE)
                .withObj(data)
                .send();
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
    public void snooze(int hourOfDay, int minute) {
        stateMachine.obtainMessage(AlarmStateMachine.SNOOZE)
                .withArg1(hourOfDay)
                .withArg2(minute)
                //This is a marker, sick stuff
                .withObj(new Object())
                .send();
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
    public String getLabelOrDefault() {
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
        sb.append(" on ").append(df.format(container.getNextTime().getTime()));
        return sb.toString();
    }

    @Override
    public ImmutableAlarmEditor edit() {
        return ImmutableAlarmEditor.builder().from(container).callback(this).build();
    }

    @Override
    @Deprecated
    public Calendar getSnoozedTime() {
        return container.getNextTime();
    }
}
