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

import com.better.alarm.configuration.Prefs;
import com.better.alarm.configuration.Store;
import com.better.alarm.interfaces.Alarm;
import com.better.alarm.interfaces.AlarmEditor;
import com.better.alarm.interfaces.Intents;
import com.better.alarm.logger.Logger;
import com.better.alarm.statemachine.ComplexTransition;
import com.better.alarm.statemachine.HandlerFactory;
import com.better.alarm.statemachine.IOnStateChangedListener;
import com.better.alarm.statemachine.IState;
import com.better.alarm.statemachine.Message;
import com.better.alarm.statemachine.State;
import com.better.alarm.statemachine.StateMachine;

import org.jetbrains.annotations.NotNull;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import io.reactivex.Observable;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;
import kotlin.jvm.functions.Function1;

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
public final class AlarmCore implements Alarm, Consumer<AlarmValue> {
    private final IAlarmsScheduler mAlarmsScheduler;
    private final Logger log;
    private final IStateNotifier broadcaster;
    private AlarmActiveRecord container;
    private final AlarmStateMachine stateMachine;
    private final DateFormat df;

    private final Observable<Integer> preAlarmDuration;
    private final Observable<Integer> snoozeDuration;
    private final Observable<Integer> autoSilence;

    private final Store store;
    private final Calendars calendars;

    public AlarmCore(AlarmActiveRecord container, Logger logger, IAlarmsScheduler alarmsScheduler, IStateNotifier broadcaster, HandlerFactory handlerFactory, Prefs prefs, Store store, Calendars calendars) {
        this.log = logger;
        this.calendars = calendars;
        this.mAlarmsScheduler = alarmsScheduler;
        this.container = container;
        this.broadcaster = broadcaster;
        this.df = new SimpleDateFormat("dd-MM-yy HH:mm:ss", Locale.GERMANY);

        this.preAlarmDuration = prefs.preAlarmDuration();
        this.snoozeDuration = prefs.snoozeDuration();
        this.autoSilence = prefs.autoSilence();

        this.store = store;

        stateMachine = new AlarmStateMachine(container.getState(), "Alarm " + container.getId(), handlerFactory);

        preAlarmDuration
                .skip(1)// not interested in the first update on startup
                .subscribe(new Consumer() {
                    @Override
                    public void accept(Object o) throws Exception {
                        stateMachine.sendMessage(AlarmStateMachine.PREALARM_DURATION_CHANGED);
                    }
                });
    }

    public void start() {
        // we always resume SM. This means that initial state will not receive
        // enter(), only resume()
        stateMachine.resume();
        updateListInStore();
    }

    /**
     * Strategy used to notify other components about alarm state.
     */
    public interface IStateNotifier {
        void broadcastAlarmState(int id, String action);
    }

    private static Function1<Integer, String> whatToString = new Function1<Integer, String>() {
        @Override
        public String invoke(Integer what) {
            switch (what) {
                case AlarmStateMachine.ENABLE:
                    return "ENABLE";
                case AlarmStateMachine.DISABLE:
                    return "DISABLE";
                case AlarmStateMachine.SNOOZE:
                    return "SNOOZE";
                case AlarmStateMachine.DISMISS:
                    return "DISMISS";
                case AlarmStateMachine.CHANGE:
                    return "CHANGE";
                case AlarmStateMachine.FIRED:
                    return "FIRED";
                case AlarmStateMachine.PREALARM_DURATION_CHANGED:
                    return "PREALARM_DURATION_CHANGED";
                case AlarmStateMachine.REFRESH:
                    return "REFRESH";
                case AlarmStateMachine.DELETE:
                    return "DELETE";
                case AlarmStateMachine.TIME_SET:
                    return "TIME_SET";
                case AlarmStateMachine.INEXACT_FIRED:
                    return "SKIP_FIRED";
                case AlarmStateMachine.SKIP:
                    return "SKIP";
            }
            return "" + what;
        }
    };

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
        public static final int REFRESH = 9;
        public static final int DELETE = 10;
        public static final int TIME_SET = 11;
        public static final int INEXACT_FIRED = 12;
        public static final int SKIP = 13;

        public final DisabledState disabledState;
        public final DeletedState deletedState;
        public final EnabledState enabledState;
        public final RescheduleTransition rescheduleTransition;
        public final EnableTransition enableTransition;
        public final EnabledState.SetState.PreAlarmSetState preAlarmSet;
        public final EnabledState.SetState.NormalSetState normalSet;
        public final EnabledState.SnoozedState snoozed;
        public final EnabledState.SkippingSetState skipping;
        public final EnabledState.PreAlarmFiredState preAlarmFired;
        public final EnabledState.PreAlarmSnoozedState preAlarmSnoozed;
        public final EnabledState.FiredState fired;

        public AlarmStateMachine(String initialState, String name, HandlerFactory handlerFactory) {
            super(name, handlerFactory, log, whatToString);
            disabledState = new DisabledState();
            rescheduleTransition = new RescheduleTransition();
            enableTransition = new EnableTransition();

            enabledState = new EnabledState();
            EnabledState.SetState set = enabledState.new SetState();
            normalSet = set.new NormalSetState();
            preAlarmSet = set.new PreAlarmSetState();
            skipping = enabledState.new SkippingSetState();

            snoozed = enabledState.new SnoozedState();
            preAlarmFired = enabledState.new PreAlarmFiredState();
            preAlarmSnoozed = enabledState.new PreAlarmSnoozedState();
            fired = enabledState.new FiredState();
            deletedState = new DeletedState();

            addState(disabledState);
            addState(enabledState);
            addState(deletedState);
            addState(rescheduleTransition);
            addState(enableTransition);

            addState(set, enabledState);
            addState(preAlarmSet, set);
            addState(normalSet, set);
            addState(snoozed, enabledState);
            addState(skipping, enabledState);
            addState(preAlarmFired, enabledState);
            addState(fired, enabledState);
            addState(preAlarmSnoozed, enabledState);

            addOnStateChangedListener(new IOnStateChangedListener() {
                @Override
                public void onStateChanged(IState state) {
                    if (state != enabledState && !(state instanceof ComplexTransition)) {
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
                //TODO unregister from updates
            }
        }

        private class DisabledState extends AlarmState {
            @Override
            public void enter() {
                updateListInStore();
            }

            @Override
            protected void onChange(AlarmValue alarmValue) {
                writeChangeData(alarmValue);
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
        }


        private class RescheduleTransition extends ComplexTransition {
            @Override
            public void performComplexTransition() {
                if (container.getDaysOfWeek().isRepeatSet()) {
                    if (container.isPrealarm() && preAlarmDuration.blockingFirst() != -1) {
                        transitionTo(preAlarmSet);
                    } else {
                        transitionTo(normalSet);
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
                    transitionTo(normalSet);
                }
            }
        }

        /**
         * Master state for all enabled states. Handles disable and delete
         */
        private class EnabledState extends AlarmState {
            @Override
            public void enter() {
                if (!container.isEnabled()) {
                    // if due to an exception during development an alarm is not enabled but the state is
                    container = container.withIsEnabled(true);
                }
                updateListInStore();
            }

            @Override
            protected void onChange(AlarmValue alarmValue) {
                writeChangeData(alarmValue);
                updateListInStore();
                if (container.isEnabled()) {
                    transitionTo(enableTransition);
                } else {
                    transitionTo(disabledState);
                }
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
                // nothing to do
            }

            @Override
            protected void onDelete() {
                transitionTo(deletedState);
            }

            private class SetState extends AlarmState {

                private class NormalSetState extends AlarmState {
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
                        showSkipNotification(nextTime);
                    }

                    @Override
                    protected void onFired() {
                        transitionTo(fired);
                    }

                    @Override
                    protected void onPreAlarmDurationChanged() {
                        transitionTo(enableTransition);
                    }
                }

                private class PreAlarmSetState extends AlarmState {

                    @Override
                    public void enter() {
                        int what = getCurrentMessage().what();
                        if (what == DISMISS || what == SNOOZE || what == CHANGE) {
                            broadcastAlarmSetWithNormalTime(calculateNextPrealarmTime().getTimeInMillis());
                        }

                        updateListInStore();
                    }

                    @Override
                    public void resume() {
                        Calendar nextPrealarmTime = calculateNextPrealarmTime();
                        if (nextPrealarmTime.after(calendars.now())) {
                            setAlarm(nextPrealarmTime, CalendarType.PREALARM);
                            showSkipNotification(nextPrealarmTime);
                        } else {
                            // TODO this should never happen
                            log.e("PreAlarm is still in the past!");
                            transitionTo(container.isEnabled() ? enableTransition : disabledState);
                        }
                    }

                    @NotNull
                    private Calendar calculateNextPrealarmTime() {
                        Calendar c = calculateNextTime();
                        c.add(Calendar.MINUTE, -1 * preAlarmDuration.blockingFirst());
                        // since prealarm is before main alarm, it can be already in the
                        // past, so it has to be adjusted.
                        advanceCalendar(c);
                        return c;
                    }

                    @Override
                    protected void onFired() {
                        transitionTo(preAlarmFired);
                    }

                    @Override
                    protected void onPreAlarmDurationChanged() {
                        transitionTo(enableTransition);
                    }
                }

                private void showSkipNotification(Calendar c) {
                    Calendar calendar = (Calendar) c.clone();
                    calendar.add(Calendar.MINUTE, -120);
                    if (calendar.after(calendars.now())) {
                        mAlarmsScheduler.setInexactAlarm(getId(), calendar);
                    } else {
                        log.d("Alarm " + getId() + " is due in less than 2 hours - show notification");
                        broadcastAlarmState(Intents.ALARM_SHOW_SKIP);
                    }
                }

                @Override
                public void enter() {
                    updateListInStore();
                }

                @Override
                protected void onSkipFired() {
                    broadcastAlarmState(Intents.ALARM_SHOW_SKIP);
                }

                @Override
                protected void onSkipped() {
                    if (container.getDaysOfWeek().isRepeatSet()) {
                        transitionTo(skipping);
                    } else {
                        transitionTo(rescheduleTransition);
                    }
                }

                @Override
                public void exit() {
                    broadcastAlarmState(Intents.ALARM_REMOVE_SKIP);
                    if (!alarmWillBeRescheduled(getCurrentMessage())) {
                        removeAlarm();
                    }
                    mAlarmsScheduler.removeInexactAlarm(getId());
                }

                @Override
                protected void onTimeSet() {
                    transitionTo(enableTransition);
                }
            }

            private class SkippingSetState extends AlarmState {
                @Override
                public void enter() {
                    updateListInStore();
                }

                @Override
                public void resume() {
                    Calendar nextTime = calculateNextTime();
                    if (nextTime.after(calendars.now())) {
                        mAlarmsScheduler.setInexactAlarm(getId(), nextTime);

                        Calendar nextAfterSkip = calculateNextTime();
                        nextAfterSkip.add(Calendar.DAY_OF_YEAR, 1);
                        int addDays = container.getDaysOfWeek().getNextAlarm(nextAfterSkip);
                        if (addDays > 0) {
                            nextAfterSkip.add(Calendar.DAY_OF_WEEK, addDays);
                        }

                        // this will never (hopefully) fire, but in order to display it everywhere...
                        setAlarm(nextAfterSkip, CalendarType.NORMAL);
                    } else {
                        transitionTo(container.isEnabled() ? enableTransition : disabledState);
                    }
                }

                @Override
                protected void onFired() {
                    // yeah should never happen
                    transitionTo(fired);
                }

                @Override
                protected void onSkipFired() {
                    transitionTo(enableTransition);
                }

                @Override
                public void exit() {
                    mAlarmsScheduler.removeInexactAlarm(getId());
                    // avoids flicker of the icon
                    if (getCurrentMessage().what() != SKIP) {
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
                public void exit() {
                    broadcastAlarmState(Intents.ALARM_DISMISS_ACTION);
                    removeAlarm();
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
                    if (getCurrentMessage().obj().isPresent()) {
                        //snooze to time with prealarm -> go to snoozed
                        transitionTo(snoozed);
                    } else {
                        transitionTo(preAlarmSnoozed);
                    }
                }

                @Override
                public void exit() {
                    removeAlarm();
                    if (getCurrentMessage().what() != FIRED) {
                        // do not dismiss because we will immediately fire another event at the service
                        broadcastAlarmState(Intents.ALARM_DISMISS_ACTION);
                    }
                }
            }

            private class SnoozedState extends AlarmState {
                Calendar nextTime;

                @Override
                public void enter() {
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
                    // change the next time to show notification properly
                    container = container.withNextTime(nextTime);
                    broadcastAlarmState(Intents.ALARM_SNOOZE_ACTION); // Yar. 18.08
                }

                @Override
                public void resume() {
                    // alarm was started again while snoozed alarm was hanging in there
                    if (nextTime == null) {
                        nextTime = getNextRegualarSnoozeCalendar();
                    }

                    setAlarm(nextTime, CalendarType.NORMAL);
                    //broadcastAlarmState(Intents.ALARM_SNOOZE_ACTION); Yar_18.08-2038: nafig broadcast iz resume
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
                protected void onSnooze() {
                    //reschedule from notification
                    enter();
                    resume();
                }

                @Override
                public void exit() {
                    removeAlarm();
                    broadcastAlarmState(Intents.ACTION_CANCEL_SNOOZE);
                }
            }

            private class PreAlarmSnoozedState extends AlarmState {
                @Override
                public void enter() {
                    //Yar 18.08: setAlarm -> resume; setAlarm(calculateNextTime(), CalendarType.NORMAL);
                    broadcastAlarmState(Intents.ALARM_SNOOZE_ACTION);
                }

                @Override
                protected void onFired() {
                    transitionTo(fired);
                }

                @Override
                protected void onSnooze() {
                    //reschedule from notification
                    transitionTo(snoozed);
                }

                @Override
                public void exit() {
                    removeAlarm();
                    broadcastAlarmState(Intents.ACTION_CANCEL_SNOOZE);
                }

                @Override
                public void resume() {
                    setAlarm(calculateNextTime(), CalendarType.NORMAL);
                    super.resume();
                }
            }
        }

        private void broadcastAlarmState(String action) {
            log.d(container.getId() + " - " + action);
            broadcaster.broadcastAlarmState(container.getId(), action);
            updateListInStore();
        }

        private void broadcastAlarmSetWithNormalTime(long millis) {
            store.sets().onNext(new Store.AlarmSet(container, millis));
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
                public void accept(@NonNull List<AlarmValue> alarmValues) {
                    List<AlarmValue> withoutId = AlarmUtilsKt.removeWithId(alarmValues, container.getId());
                    store.alarmsSubject().onNext(withoutId);
                }
            });
        }

        private void writeChangeData(AlarmValue data) {
            container = container.withChangeData(data);
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
            boolean alarmWillBeRescheduled = reason.what() == CHANGE && ((AlarmValue) reason.obj().get()).isEnabled();
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
                        onChange((AlarmValue) msg.obj().get());
                        break;
                    case FIRED:
                        onFired();
                        break;
                    case PREALARM_DURATION_CHANGED:
                        onPreAlarmDurationChanged();
                        break;
                    case REFRESH:
                        onRefresh();
                        break;
                    case TIME_SET:
                        onTimeSet();
                        break;
                    case INEXACT_FIRED:
                        onSkipFired();
                        break;
                    case SKIP:
                        onSkipped();
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

            protected void onChange(AlarmValue alarmValue) {
                markNotHandled();
            }

            protected void onFired() {
                markNotHandled();
            }

            protected void onSkipFired() {
                markNotHandled();
            }

            protected void onSkipped() {
                markNotHandled();
            }

            protected void onPreAlarmDurationChanged() {
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
        store.alarms().take(1).subscribe(new Consumer<List<AlarmValue>>() {
            @Override
            public void accept(@NonNull List<AlarmValue> alarmValues) {
                List<AlarmValue> copy = AlarmUtilsKt.addOrReplace(alarmValues, container);

                store.alarmsSubject().onNext(copy);
            }
        });
    }

    /**
     * for {@link #edit()}
     */
    @Override
    public void accept(@NonNull AlarmValue alarmChangeData) throws Exception {
        change(alarmChangeData);
    }

    public void onAlarmFired(CalendarType calendarType) {
        stateMachine.sendMessage(AlarmStateMachine.FIRED);
    }

    public void onInexactAlarmFired() {
        stateMachine.sendMessage(AlarmStateMachine.INEXACT_FIRED);
    }

    public void requestSkip() {
        stateMachine.sendMessage(AlarmStateMachine.SKIP);
    }

    @Override
    public boolean isSkipping() {
        return container.getSkipping();
    }

    public void refresh() {
        stateMachine.sendMessage(AlarmStateMachine.REFRESH);
    }

    public void onTimeSet() {
        stateMachine.sendMessage(AlarmStateMachine.TIME_SET);
    }

    public void change(AlarmValue data) {
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
    public int getId() {
        return container.getId();
    }

    @Override
    public String getLabelOrDefault() {
        return container.getLabel();
    }

    @Override
    public Alarmtone getAlarmtone() {
        return container.getAlarmtone();
    }

    @Override
    public String toString() {
        return "AlarmCore " + container.getId() +
                " in " + stateMachine.getCurrentState().getName() +
                " on " + df.format(container.getNextTime().getTime());
    }

    @Override
    public AlarmEditor edit() {
        return new AlarmEditor((Consumer<AlarmValue>) this, container.getAlarmValue());
    }

    @Override
    @Deprecated
    public Calendar getSnoozedTime() {
        return container.getNextTime();
    }
}
