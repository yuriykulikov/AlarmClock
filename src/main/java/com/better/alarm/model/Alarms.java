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

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.acra.ACRA;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;

import com.better.alarm.Broadcasts;
import com.better.alarm.model.interfaces.Alarm;
import com.better.alarm.model.interfaces.AlarmNotFoundException;
import com.better.alarm.model.interfaces.IAlarmsManager;
import com.better.alarm.model.interfaces.Intents;
import com.better.alarm.model.persistance.AlarmContainer;
import com.github.androidutils.logger.Logger;

/**
 * The Alarms implements application domain logic
 */
@SuppressLint("UseSparseArrays")
public class Alarms implements IAlarmsManager {
    /**
     * in millis
     */
    private static final long RETRY_TOTAL_TIME = 61 * 1000;
    /**
     * in millis
     */
    private static final long RETRY_INTERVAL = 500;

    private final Context mContext;
    private final Logger log;

    private final IAlarmsScheduler mAlarmsScheduler;

    private final ContentResolver mContentResolver;
    private final Map<Integer, AlarmCore> alarms;
    private final AlarmStateNotifier broadcaster;

    private final DatabaseRetryCountDownTimer databaseRetryCountDownTimer;

    Alarms(Context context, Logger logger, IAlarmsScheduler alarmsScheduler) {
        mContext = context;
        log = logger;
        mAlarmsScheduler = alarmsScheduler;

        mContentResolver = mContext.getContentResolver();
        alarms = new HashMap<Integer, AlarmCore>();
        broadcaster = new AlarmStateNotifier(mContext);

        databaseRetryCountDownTimer = new DatabaseRetryCountDownTimer(RETRY_TOTAL_TIME, RETRY_INTERVAL);

        boolean hasInitlized = tryReadDb();
        if (!hasInitlized) {
            log.w("Scheduling retry");
            databaseRetryCountDownTimer.start();
        }
    }

    private boolean tryReadDb() {
        final Cursor cursor = mContentResolver.query(AlarmContainer.Columns.CONTENT_URI,
                AlarmContainer.Columns.ALARM_QUERY_COLUMNS, null, null, AlarmContainer.Columns.DEFAULT_SORT_ORDER);

        if (cursor == null) {
            log.e("Cursor was null!");
            return false;
        } else {
            try {
                if (cursor.moveToFirst()) {
                    do {
                        AlarmContainer container = new AlarmContainer(cursor, log, mContext);
                        final AlarmCore a = new AlarmCore(container, mContext, log, mAlarmsScheduler, broadcaster);
                        alarms.put(a.getId(), a);
                    } while (cursor.moveToNext());
                }
            } finally {
                cursor.close();
            }

            log.d("Alarms:");
            for (Alarm alarm : alarms.values()) {
                log.d(alarm.toString());
            }

            return true;
        }
    }

    void refresh() {
        for (AlarmCore alarmCore : alarms.values()) {
            alarmCore.refresh();
        }
        notifyAlarmListChangedListeners();
    }

    void onTimeSet() {
        for (AlarmCore alarmCore : alarms.values()) {
            alarmCore.onTimeSet();
        }
        notifyAlarmListChangedListeners();
    }

    @Override
    public AlarmCore getAlarm(int alarmId) throws AlarmNotFoundException {
        AlarmCore alarm = alarms.get(alarmId);
        if (alarm != null) return alarm;
        else throw new AlarmNotFoundException("AlarmID " + alarmId + " could not be resolved");
    }

    @Override
    public Alarm createNewAlarm() {
        AlarmContainer container = new AlarmContainer(log, mContext);
        AlarmCore alarm = new AlarmCore(container, mContext, log, mAlarmsScheduler, broadcaster);
        alarms.put(alarm.getId(), alarm);
        notifyAlarmListChangedListeners();
        return alarm;
    }

    @Override
    public void delete(Alarm alarm) {
        alarm.delete();
        alarms.remove(alarm.getId());
        notifyAlarmListChangedListeners();
    }

    // Compatibility - passes calls to Alarms

    @Override
    public List<Alarm> getAlarmsList() {
        List<Alarm> alarms = new LinkedList<Alarm>(this.alarms.values());
        Collections.sort(alarms, new MinuteComparator());
        Collections.sort(alarms, new HourComparator());
        Collections.sort(alarms, new RepeatComparator());

        return alarms;
    }

    private final class RepeatComparator implements Comparator<Alarm> {
        @Override
        public int compare(Alarm lhs, Alarm rhs) {
            return Integer.valueOf(getPrio(lhs)).compareTo(Integer.valueOf(getPrio(rhs)));
        }

        /**
         * First comes on Weekdays, than on weekends and then the rest
         *
         * @param alarm
         * @return
         */
        private int getPrio(Alarm alarm) {
            switch (alarm.getDaysOfWeek().getCoded()) {
                case 0x7F:
                    return 1;
                case 0x1F:
                    return 2;
                case 0x60:
                    return 3;
                default:
                    return 0;
            }
        }
    }

    private final class HourComparator implements Comparator<Alarm> {
        @Override
        public int compare(Alarm lhs, Alarm rhs) {
            return Integer.valueOf(lhs.getHour()).compareTo(Integer.valueOf(rhs.getHour()));
        }
    }

    private final class MinuteComparator implements Comparator<Alarm> {
        @Override
        public int compare(Alarm lhs, Alarm rhs) {
            return Integer.valueOf(lhs.getMinutes()).compareTo(Integer.valueOf(rhs.getMinutes()));
        }
    }

    void onAlarmFired(AlarmCore alarm, CalendarType calendarType) {
        mAlarmsScheduler.onAlarmFired(alarm.getId());
        alarm.onAlarmFired(calendarType);
        notifyAlarmListChangedListeners();
    }

    /**
     * A convenience method to enable or disable an alarm
     *
     * @param enable corresponds to the ENABLED column
     * @throws AlarmNotFoundException
     */
    @Override
    public void enable(Alarm alarm, boolean enable) {
        alarm.enable(enable);
        notifyAlarmListChangedListeners();
    }

    @Override
    public void snooze(Alarm alarm) {
        alarm.snooze();
        notifyAlarmListChangedListeners();
    }

    @Override
    public void dismiss(Alarm alarm) {
        alarm.dismiss();
        notifyAlarmListChangedListeners();
    }

    private void notifyAlarmListChangedListeners() {
        Broadcasts.sendLocal(mContext, new Intent(Intents.ACTION_ALARM_CHANGED));
    }

    private final class DatabaseRetryCountDownTimer extends CountDownTimer {
        private final Handler handler = new Handler();

        public DatabaseRetryCountDownTimer(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onFinish() {
            String message = "After " + RETRY_TOTAL_TIME + " still was not able to query";
            log.e(message);
            ACRA.getErrorReporter().handleSilentException(new Exception(message));
        }

        @Override
        public void onTick(long millisUntilFinished) {
            log.w("Retrying after " + (RETRY_TOTAL_TIME - millisUntilFinished));
            boolean hasInitlized = tryReadDb();
            if (hasInitlized) {
                log.w("Finished");
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        cancel();
                    }
                });
                for (AlarmCore alarmCore : alarms.values()) {
                    alarmCore.refresh();
                }
                notifyAlarmListChangedListeners();
            }
        }
    }
}
