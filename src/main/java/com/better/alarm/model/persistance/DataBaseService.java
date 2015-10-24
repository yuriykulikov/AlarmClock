package com.better.alarm.model.persistance;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;

import com.better.alarm.BuildConfig;
import com.better.alarm.model.interfaces.Intents;
import com.github.androidutils.wakelock.WakeLockManager;

public class DataBaseService extends IntentService {
    public static final String SAVE_ALARM_ACTION = BuildConfig.APPLICATION_ID + ".ACTION_SAVE_ALARM";

    ContentResolver mContentResolver;

    public DataBaseService() {
        super("DataBaseService");
    }

    @Override
    public void onCreate() {
        mContentResolver = getContentResolver();
        super.onCreate();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent.getAction().equals(SAVE_ALARM_ACTION)) {
            int id = intent.getIntExtra(Intents.EXTRA_ID, -1);
            ContentValues values = intent.getParcelableExtra("extra_values");
            Uri uriWithAppendedId = ContentUris.withAppendedId(AlarmContainer.Columns.CONTENT_URI, id);
            mContentResolver.update(uriWithAppendedId, values, null, null);
            WakeLockManager.getWakeLockManager().releasePartialWakeLock(intent);
        }
    }
}
