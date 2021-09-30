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

package com.better.alarm.persistance;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import com.better.alarm.logger.Logger;

/**
 * Helper class for opening the database from multiple providers. Also provides some common
 * functionality.
 */
public class AlarmDatabaseHelper extends SQLiteOpenHelper {
  private static final String DATABASE_NAME = "alarms.db";
  private static final int DATABASE_VERSION = 5;
  private final Logger log;

  public AlarmDatabaseHelper(Context context, Logger log) {
    super(context, DATABASE_NAME, null, DATABASE_VERSION);
    this.log = log;
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    // @formatter:off
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
    // @formatter:on
    // insert default alarms
    String insertMe =
        "INSERT INTO alarms "
            + "(hour, minutes, daysofweek, alarmtime, enabled, vibrate, "
            + "message, alert, prealarm, state) VALUES ";
    db.execSQL(insertMe + "(8, 30, 31, 0, 0, 1, '', '', 0, '');");
    db.execSQL(insertMe + "(9, 00, 96, 0, 0, 1, '', '', 0, '');");
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int currentVersion) {
    log.d(
        "Upgrading alarms database from version "
            + oldVersion
            + " to "
            + currentVersion
            + ", which will destroy all old data");
    db.execSQL("DROP TABLE IF EXISTS alarms");
    onCreate(db);
  }

  public Uri commonInsert(ContentValues values) {
    SQLiteDatabase db = getWritableDatabase();
    long rowId = db.insert("alarms", Columns.MESSAGE, values);
    if (rowId < 0) throw new SQLException("Failed to insert row");
    return ContentUris.withAppendedId(Columns.contentUri(), rowId);
  }
}
