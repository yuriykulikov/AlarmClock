package com.better.alarm.persistance;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;

import com.better.alarm.AlarmApplication;
import com.better.alarm.BuildConfig;
import com.better.alarm.model.interfaces.Intents;
import com.google.inject.Inject;

public class DataBaseService extends IntentService {
    public static final String SAVE_ALARM_ACTION = BuildConfig.APPLICATION_ID + ".ACTION_SAVE_ALARM";

    @Inject
    ContentResolver mContentResolver;

    public DataBaseService() {
        super("DataBaseService");
    }

    @Override
    public void onCreate() {
        AlarmApplication.guice().injectMembers(this);
        super.onCreate();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent.getAction().equals(SAVE_ALARM_ACTION)) {
            int id = intent.getIntExtra(Intents.EXTRA_ID, -1);
            ContentValues values = intent.getParcelableExtra("extra_values");
            Uri uriWithAppendedId = ContentUris.withAppendedId(AlarmContainer.Columns.CONTENT_URI, id);
            mContentResolver.update(uriWithAppendedId, values, null, null);
            AlarmApplication.wakeLocks().releasePartialWakeLock(intent);
        }
    }
}
