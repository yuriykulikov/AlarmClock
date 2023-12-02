/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.better.alarm.data.contentprovider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Helper class for opening the database from multiple providers. Also provides some common
 * functionality.
 */
public class AlarmDatabaseHelper extends SQLiteOpenHelper {
  private static final String DATABASE_NAME = "alarms.db";
  private static final int DATABASE_VERSION = 5;

  public AlarmDatabaseHelper(Context context) {
    super(context, DATABASE_NAME, null, DATABASE_VERSION);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    db.execSQL(
        "CREATE TABLE alarms ("
            + "_id INTEGER PRIMARY KEY,"
            + "hour INTEGER, "
            + "minutes INTEGER, "
            + "daysofweek INTEGER, "
            + "alarmtime INTEGER, "
            + "enabled INTEGER, "
            + "vibrate INTEGER, "
            + "message TEXT, "
            + "alert TEXT, "
            + "prealarm INTEGER, "
            + "state STRING);");
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int currentVersion) {
    db.execSQL("DROP TABLE IF EXISTS alarms");
    onCreate(db);
  }
}
