/*
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

import com.better.alarm.BuildConfig;
import com.better.alarm.configuration.ImmutableNext;
import com.better.alarm.configuration.Prefs;
import com.better.alarm.configuration.Store;
import com.better.alarm.logger.Logger;
import com.google.common.base.Optional;
import com.google.inject.Inject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Locale;
import java.util.PriorityQueue;

import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;

public class AlarmsScheduler implements IAlarmsScheduler {
    public static final String ACTION_FIRED = BuildConfig.APPLICATION_ID + ".ACTION_FIRED";
    public static final String EXTRA_ID = "intent.extra.alarm";
    public static final String EXTRA_TYPE = "intent.extra.type";

    private final Store store;
    private final Prefs prefs;

    private final AlarmSetter setter;
    private final PriorityQueue<ScheduledAlarm> queue;
    private final Logger log;
    private final Calendars calendars;

    public class ScheduledAlarm implements Comparable<ScheduledAlarm> {
        public final int id;
        public final Calendar calendar;
        public final CalendarType type;
        private final DateFormat df;
        private final Optional<AlarmValue> alarmValue;

        public ScheduledAlarm(int id, Calendar calendar, CalendarType type, AlarmValue alarmValue) {
            this.id = id;
            this.calendar = calendar;
            this.type = type;
            this.alarmValue = Optional.of(alarmValue);
            this.df = new SimpleDateFormat("dd-MM-yy HH:mm:ss", Locale.GERMANY);
        }

        public ScheduledAlarm(int id) {
            this.id = id;
            this.calendar = null;
            this.type = null;
            this.alarmValue = Optional.absent();
            this.df = new SimpleDateFormat("dd-MM-yy HH:mm:ss", Locale.GERMANY);
        }

        @Override
        public boolean equals(Object o) {
            if (((ScheduledAlarm) o).id == id) return true;
            else return false;
        }

        @Override
        public int compareTo(ScheduledAlarm another) {
            return this.calendar.compareTo(another.calendar);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(id).append(" ");
            sb.append(type != null ? type.toString() : "null").append(" ");
            sb.append("on ").append(calendar != null ? df.format(calendar.getTime()) : "null");
            return sb.toString();
        }
    }

    @Inject
    public AlarmsScheduler(AlarmSetter setter, Logger logger, Store store, Prefs prefs, Calendars calendars) {
        this.setter = setter;
        this.store = store;
        this.prefs = prefs;
        queue = new PriorityQueue<ScheduledAlarm>();
        this.log = logger;
        this.calendars = calendars;
    }

    @Override
    public void setAlarm(int id, CalendarType type, Calendar calendar, AlarmValue alarmValue) {
        ScheduledAlarm scheduledAlarm = new ScheduledAlarm(id, calendar, type, alarmValue);
        replaceAlarm(scheduledAlarm, true);
    }

    @Override
    public void removeAlarm(int id) {
        replaceAlarm(new ScheduledAlarm(id), false);
    }

    @Override
    public void onAlarmFired(int id) {
        replaceAlarm(new ScheduledAlarm(id), false);
    }

    private void replaceAlarm(ScheduledAlarm newAlarm, boolean add) {
        ScheduledAlarm previousHead = queue.peek();

        // remove if we have already an alarm
        for (Iterator<ScheduledAlarm> iterator = queue.iterator(); iterator.hasNext(); ) {
            ScheduledAlarm presentAlarm = iterator.next();
            if (presentAlarm.id == newAlarm.id) {
                iterator.remove();
                log.d(presentAlarm.id + " was removed");
            }
        }

        if (add) {
            queue.add(newAlarm);
        }

        fireAlarmsInThePast();

        ScheduledAlarm currentHead = queue.peek();
        // compare by reference!
        if (previousHead != currentHead) {
            if (!queue.isEmpty()) {
                setter.setUpRTCAlarm(currentHead);
            } else {
                log.d("no more alarms to schedule, remove pending intent");
                setter.removeRTCAlarm();
            }
            notifyListeners();
        }
    }

    /**
     * If two alarms were set for the same time, then the second alarm will be
     * processed in the past. In this case we remove it from the queue and fire
     * it.
     */

    private void fireAlarmsInThePast() {
        Calendar now = calendars.now();
        while (!queue.isEmpty() && queue.peek().calendar.before(now)) {
            // remove happens in fire
            ScheduledAlarm firedInThePastAlarm = queue.poll();
            log.d("In the past - " + firedInThePastAlarm.toString());
            setter.fireNow(firedInThePastAlarm);
        }
    }

    /**
     * TODO the whole mechanism has to be revised. Currently we can only know
     * when next alarm is scheduled and we do not know what is the reason for
     * that. Maybe create a separate component for that or notify from the alarm
     * SM. Actually notify this component from SM :-) and he can notify the
     * rest.
     */
    private void notifyListeners() {
        findNextNormalAlarm()
                .map(new Function<ScheduledAlarm, Optional<Store.Next>>() {
                    @Override
                    public Optional<Store.Next> apply(@NonNull ScheduledAlarm scheduledAlarm) throws Exception {
                        boolean isPrealarm = scheduledAlarm.type == CalendarType.PREALARM;
                        return Optional.of((Store.Next) ImmutableNext.builder()
                                .alarm(scheduledAlarm.alarmValue.get())
                                .isPrealarm(isPrealarm)
                                .nextNonPrealarmTime(isPrealarm ? findNormalTime(scheduledAlarm) : scheduledAlarm.calendar.getTimeInMillis())
                                .build());
                    }

                    private long findNormalTime(ScheduledAlarm scheduledAlarm) {
                        // we can only assume that the real one will be a little later,
                        // namely:
                        int prealarmOffsetInMillis = prefs.preAlarmDuration().blockingFirst() * 60 * 1000;
                        return scheduledAlarm.calendar.getTimeInMillis() + prealarmOffsetInMillis;
                    }
                })
                .defaultIfEmpty(Optional.<Store.Next>absent())
                .subscribe(new Consumer<Optional<Store.Next>>() {
                    @Override
                    public void accept(@NonNull Optional<Store.Next> nextOptional) throws Exception {
                        store.next().onNext(nextOptional);
                    }
                });
    }

    private Maybe<ScheduledAlarm> findNextNormalAlarm() {
        return Observable.fromIterable(queue)
                .sorted()
                .filter(new Predicate<ScheduledAlarm>() {
                    @Override
                    public boolean test(@NonNull ScheduledAlarm scheduledAlarm) throws Exception {
                        return scheduledAlarm.type != CalendarType.AUTOSILENCE;
                    }
                }).firstElement();
    }
}
