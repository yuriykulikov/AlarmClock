/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.deskclock;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;

import java.util.Calendar;

public class ExternalAlarmProvider extends ContentProvider {
    private AlarmDatabaseHelper mOpenHelper;

    private static final int ALARMS_ADD_ALARM = 0;
    private static final UriMatcher sURLMatcher = new UriMatcher(
            UriMatcher.NO_MATCH);

    static {
        sURLMatcher.addURI("com.android.deskclock.alarmprovider", "add",
                           ALARMS_ADD_ALARM);
    }

    public ExternalAlarmProvider() {
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new AlarmDatabaseHelper(getContext());
        return true;
    }

    @Override
    public String getType(Uri url) {
        return null;
    }

    @Override
    public Cursor query(Uri url, String[] projectionIn, String selection,
            String[] selectionArgs, String sort) {
        throw new UnsupportedOperationException("Cannot query: " + url);
    }

    @Override
    public int update(Uri url, ContentValues values, String where, String[] whereArgs) {
        throw new UnsupportedOperationException("Cannot update: " + url);
    }

    @Override
    public int delete(Uri url, String where, String[] whereArgs) {
        throw new UnsupportedOperationException("Cannot delete: " + url);
    }

    @Override
    public Uri insert(Uri url, ContentValues initialValues) {
        int match = sURLMatcher.match(url);
        if (match != ALARMS_ADD_ALARM) {
            throw new IllegalArgumentException("Cannot insert into URL: " + url);
        }

        ContentValues values = new ContentValues(initialValues);

        Long time = values.getAsLong(Alarm.Columns.ALARM_TIME);
        if (time == null || time.longValue() == 0) {
            Integer hour = values.getAsInteger(Alarm.Columns.HOUR);
            Integer minutes = values.getAsInteger(Alarm.Columns.MINUTES);
            // If alarmtime is not set or is set to 0 (meaning repeated alarm),
            // verify that hour and minutes are set.
            if (hour == null || minutes == null) {
                throw new IllegalArgumentException("Cannot insert into " + url
                        + ": cannot determine alarm time.");
            }
            Integer repeat = values.getAsInteger(Alarm.Columns.DAYS_OF_WEEK);
            Alarm.DaysOfWeek dow = new Alarm.DaysOfWeek(repeat == null ? 0 :
                                                        repeat.intValue());

            // Ensure the alarmtime matches hour and minutes.
            Calendar c = Alarms.calculateAlarm(hour.intValue(),
                                               minutes.intValue(),
                                               dow);
            values.put(Alarm.Columns.ALARM_TIME, c.getTimeInMillis());
        } else {
            // Calculate hour and minute based on alarmtime to ensure the hour
            // and minutes match.
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(time.longValue());
            values.put(Alarm.Columns.HOUR, c.get(Calendar.HOUR_OF_DAY));
            values.put(Alarm.Columns.MINUTES, c.get(Calendar.MINUTE));
        }

        // By default, enable the alarm.
        if (!values.containsKey(Alarm.Columns.ENABLED)) {
            values.put(Alarm.Columns.ENABLED, 1);
        }

        // By default, turn on vibrate.
        if (!values.containsKey(Alarm.Columns.VIBRATE)) {
            values.put(Alarm.Columns.VIBRATE, 1);
        }

        final Uri newUrl = mOpenHelper.commonInsert(values);

        getContext().getContentResolver().notifyChange(newUrl, null);

        // Sending a blank intent broadcast to AlarmInitReceiver will trigger
        // the next alert to be set.  Can't set it here because we don't have
        // permission to read the alarms.
        final Context c = getContext();
        c.sendBroadcast(new Intent("", null, c, AlarmInitReceiver.class));

        return newUrl;
    }
}
