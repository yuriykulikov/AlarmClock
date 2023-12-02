/*
 * Copyright (C) 2007 The Android Open Source Project
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

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import com.better.alarm.BuildConfig;
import com.better.alarm.util.Preconditions;

public class AlarmProvider extends ContentProvider {
  private AlarmDatabaseHelper mOpenHelper;

  private static final int ALARMS = 1;
  private static final int ALARMS_ID = 2;
  private static final UriMatcher sURLMatcher = new UriMatcher(UriMatcher.NO_MATCH);

  static {
    sURLMatcher.addURI(BuildConfig.APPLICATION_ID + ".model", "alarm", ALARMS);
    sURLMatcher.addURI(BuildConfig.APPLICATION_ID + ".model", "alarm/#", ALARMS_ID);
  }

  @Override
  public boolean onCreate() {
    mOpenHelper = new AlarmDatabaseHelper(getContext());
    return true;
  }

  @Override
  public Cursor query(
      Uri url, String[] projectionIn, String selection, String[] selectionArgs, String sort) {
    SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

    // Generate the body of the query
    Preconditions.checkArgument(sURLMatcher.match(url) == ALARMS, "Invalid URL %s", url);
    qb.setTables("alarms");

    SQLiteDatabase db = mOpenHelper.getReadableDatabase();
    Cursor ret;
    try {
      ret = qb.query(db, projectionIn, selection, selectionArgs, null, null, sort);
    } catch (SQLException e) {
      db.execSQL("DROP TABLE IF EXISTS alarms");
      mOpenHelper.onCreate(db);
      ret = qb.query(db, projectionIn, selection, selectionArgs, null, null, sort);
    }
    if (ret != null) {
      ret.setNotificationUri(getContext().getContentResolver(), url);
    }

    return ret;
  }

  @Override
  public String getType(Uri url) {
    int match = sURLMatcher.match(url);
    switch (match) {
      case ALARMS:
        return "vnd.android.cursor.dir/alarms";
      case ALARMS_ID:
        return "vnd.android.cursor.item/alarms";
      default:
        throw new IllegalArgumentException("Invalid URL");
    }
  }

  @Override
  public int update(Uri url, ContentValues values, String where, String[] whereArgs) {
    throw new UnsupportedOperationException("Not supported");
  }

  @Override
  public Uri insert(Uri url, ContentValues initialValues) {
    throw new UnsupportedOperationException("Not supported");
  }

  @Override
  public int delete(Uri url, String where, String[] whereArgs) {
    Preconditions.checkArgument(sURLMatcher.match(url) == ALARMS_ID, "Invalid URL %s", url);

    SQLiteDatabase db = mOpenHelper.getWritableDatabase();
    final int count;
    String segment = url.getPathSegments().get(1);
    if (TextUtils.isEmpty(where)) {
      count = db.delete("alarms", "_id=" + segment, whereArgs);
    } else {
      count = db.delete("alarms", "_id=" + segment + " AND (" + where + ")", whereArgs);
    }

    getContext().getContentResolver().notifyChange(url, null);
    return count;
  }
}
