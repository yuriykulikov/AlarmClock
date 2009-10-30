/*
 * Copyright (C) 2009 The Android Open Source Project
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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import static android.os.BatteryManager.BATTERY_STATUS_CHARGING;
import static android.os.BatteryManager.BATTERY_STATUS_FULL;
import static android.os.BatteryManager.BATTERY_STATUS_UNKNOWN;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.util.Date;

/**
 * DeskClock clock view for desk docks.
 */
public class DeskClock extends Activity {
    private static final boolean DEBUG = true;

    private static final String LOG_TAG = "DeskClock";

    private static final String MUSIC_NOW_PLAYING_ACTIVITY = "com.android.music.PLAYBACK_VIEWER";

    private final long FETCH_WEATHER_DELAY = 5 * 60 * 1000; // 5 min.
    private final int FETCH_WEATHER_DATA_MSG = 10000;
    private final int UPDATE_WEATHER_DISPLAY_MSG = 10001;

    private static final String GENIE_PACKAGE_ID = "com.google.android.apps.genie.geniewidget";
    private static final String WEATHER_CONTENT_AUTHORITY = GENIE_PACKAGE_ID + ".weather";
    private static final String WEATHER_CONTENT_PATH = "/weather/current";
    private static final String[] WEATHER_CONTENT_COLUMNS = new String[] {
            "location",
            "timestamp",
            "highTemperature",
            "lowTemperature",
            "iconUrl",
            "iconResId",
            "description",
        };

    private DigitalClock mTime;
    private TextView mDate;

    private TextView mNextAlarm = null;
    private TextView mBatteryDisplay;

    private TextView mWeatherTemperature;
    private TextView mWeatherLocation;
    private ImageView mWeatherIcon;

    private String mWeatherTemperatureString;
    private String mWeatherLocationString;
    private Drawable mWeatherIconDrawable;

    private Resources mGenieResources = null;

    private boolean mDimmed = false;

    private DateFormat mDateFormat;
    
    private int mBatteryLevel;
    private boolean mPluggedIn;

    private boolean mWeatherFetchScheduled = false;

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (Intent.ACTION_DATE_CHANGED.equals(action)) {
                refreshDate();
            } else if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                handleBatteryUpdate(
                    intent.getIntExtra("status", BATTERY_STATUS_UNKNOWN),
                    intent.getIntExtra("level", 0));
            }
        }
    };

    private final Handler mHandy = new Handler() {
        @Override
        public void handleMessage(Message m) {
            if (DEBUG) Log.d(LOG_TAG, "handleMessage: " + m.toString());

            if (m.what == FETCH_WEATHER_DATA_MSG) {
                if (!mWeatherFetchScheduled) return;
                mWeatherFetchScheduled = false;
                new Thread() { public void run() { fetchWeatherData(); } }.start();
                scheduleWeatherFetchDelayed(FETCH_WEATHER_DELAY);
            } else if (m.what == UPDATE_WEATHER_DISPLAY_MSG) {
                updateWeatherDisplay();
            }
        }
    };

    private boolean supportsWeather() {
        return (mGenieResources != null);
    }

    private void scheduleWeatherFetchDelayed(long delay) {
        if (mWeatherFetchScheduled) return;

        if (DEBUG) Log.d(LOG_TAG, "scheduling weather fetch message for " + delay + "ms from now");

        mWeatherFetchScheduled = true;

        mHandy.sendEmptyMessageDelayed(FETCH_WEATHER_DATA_MSG, delay);
    }

    private void unscheduleWeatherFetch() {
        mWeatherFetchScheduled = false;
    }

    private void fetchWeatherData() {
        // if we couldn't load the weather widget's resources, we simply
        // assume it's not present on the device.
        if (mGenieResources == null) return;

        Uri queryUri = new Uri.Builder()
            .scheme(android.content.ContentResolver.SCHEME_CONTENT)
            .authority(WEATHER_CONTENT_AUTHORITY)
            .path(WEATHER_CONTENT_PATH)
            .appendPath(new Long(System.currentTimeMillis()).toString())
            .build();

        if (DEBUG) Log.d(LOG_TAG, "querying genie: " + queryUri);

        Cursor cur;
        try {
            cur = managedQuery(
                queryUri,
                WEATHER_CONTENT_COLUMNS,
                null,
                null,
                null);
        } catch (RuntimeException e) {
            Log.e(LOG_TAG, "Weather query failed", e);
            cur = null;
        }

        if (cur != null && cur.moveToFirst()) {
            mWeatherIconDrawable = mGenieResources.getDrawable(cur.getInt(
                cur.getColumnIndexOrThrow("iconResId")));
            mWeatherTemperatureString = cur.getString(
                cur.getColumnIndexOrThrow("highTemperature"));
            mWeatherLocationString = cur.getString(
                cur.getColumnIndexOrThrow("location"));
        } else {
            Log.w(LOG_TAG, "No weather information available (cur=" 
                + cur +")");
            mWeatherIconDrawable = null;
            mWeatherTemperatureString = "";
            mWeatherLocationString = "Weather data unavailable."; // TODO: internationalize
        }

        mHandy.sendEmptyMessage(UPDATE_WEATHER_DISPLAY_MSG);
    }

    private void refreshWeather() {
        if (supportsWeather())
            scheduleWeatherFetchDelayed(0);
    }

    private void updateWeatherDisplay() {
        mWeatherTemperature.setText(mWeatherTemperatureString);
        mWeatherLocation.setText(mWeatherLocationString);
        mWeatherIcon.setImageDrawable(mWeatherIconDrawable);
    }

    // Adapted from KeyguardUpdateMonitor.java
    private void handleBatteryUpdate(int plugStatus, int batteryLevel) {
        final boolean pluggedIn = (plugStatus == BATTERY_STATUS_CHARGING || plugStatus == BATTERY_STATUS_FULL);
        if (pluggedIn != mPluggedIn || batteryLevel != mBatteryLevel) {
            mBatteryLevel = batteryLevel;
            mPluggedIn = pluggedIn;
            refreshBattery();
        }
    }

    private void refreshBattery() {
        if (mPluggedIn /* || mBatteryLevel < LOW_BATTERY_THRESHOLD */) {
            mBatteryDisplay.setCompoundDrawablesWithIntrinsicBounds(
                0, 0, android.R.drawable.ic_lock_idle_charging, 0);
            mBatteryDisplay.setText(
                getString(R.string.battery_charging_level, mBatteryLevel));
            mBatteryDisplay.setVisibility(View.VISIBLE);
        } else {
            mBatteryDisplay.setVisibility(View.INVISIBLE);
        }
    }

    private void refreshDate() {
        mDate.setText(mDateFormat.format(new Date()));
    }

    private void refreshAlarm() {
        String nextAlarm = Settings.System.getString(getContentResolver(),
                Settings.System.NEXT_ALARM_FORMATTED);
        if (!TextUtils.isEmpty(nextAlarm)) {
            mNextAlarm.setText(nextAlarm);
            //mNextAlarm.setCompoundDrawablesWithIntrinsicBounds(
            //    android.R.drawable.ic_lock_idle_alarm, 0, 0, 0);
            mNextAlarm.setVisibility(View.VISIBLE);
        } else {
            mNextAlarm.setVisibility(View.INVISIBLE);
        }
    }

    private void refreshAll() {
        refreshDate();
        refreshAlarm();
        refreshBattery();
        refreshWeather();
    }

    private void doDim(boolean fade) {
        View tintView = findViewById(R.id.window_tint);

        Window win = getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();

        // dim the wallpaper somewhat (how much is determined below)
        winParams.flags |= (WindowManager.LayoutParams.FLAG_DIM_BEHIND);

        if (mDimmed) {
            winParams.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
//            winParams.flags &= (~WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
            winParams.dimAmount = 0.5f; // pump up contrast in dim mode

            // show the window tint
            tintView.startAnimation(AnimationUtils.loadAnimation(this, 
                fade ? R.anim.dim
                     : R.anim.dim_instant));
        } else {
            winParams.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);
//            winParams.flags |= WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
            winParams.dimAmount = 0.2f; // lower contrast in normal mode
    
            // hide the window tint
            tintView.startAnimation(AnimationUtils.loadAnimation(this, 
                fade ? R.anim.undim
                     : R.anim.undim_instant));
        }
        
        win.setAttributes(winParams);
    }

    @Override
    public void onResume() {
        super.onResume();

        // reload the date format in case the user has changed settings
        // recently
        mDateFormat = java.text.DateFormat.getDateInstance(java.text.DateFormat.FULL);

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_DATE_CHANGED);
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(mIntentReceiver, filter);

        doDim(false);
        refreshAll();
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mIntentReceiver);
        unscheduleWeatherFetch();
    }


    private void initViews() {
        setContentView(R.layout.desk_clock);

        mTime = (DigitalClock) findViewById(R.id.time);
        mDate = (TextView) findViewById(R.id.date);
        mBatteryDisplay = (TextView) findViewById(R.id.battery);

        mWeatherTemperature = (TextView) findViewById(R.id.weather_temperature);
        mWeatherLocation = (TextView) findViewById(R.id.weather_location);
        mWeatherIcon = (ImageView) findViewById(R.id.weather_icon);

        mNextAlarm = (TextView) findViewById(R.id.nextAlarm);

        final Button alarmButton = (Button) findViewById(R.id.alarm_button);
        alarmButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(DeskClock.this, AlarmClock.class));
            }
        });

        final Button galleryButton = (Button) findViewById(R.id.gallery_button);
        galleryButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    startActivity(new Intent(
                        Intent.ACTION_VIEW,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI));
                } catch (android.content.ActivityNotFoundException e) {
                    Log.e(LOG_TAG, "Couldn't launch image browser", e);
                }
            }
        });

        final Button musicButton = (Button) findViewById(R.id.music_button);
        musicButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    startActivity(new Intent(MUSIC_NOW_PLAYING_ACTIVITY));
                } catch (android.content.ActivityNotFoundException e) {
                    Log.e(LOG_TAG, "Couldn't launch music browser", e);
                }
            }
        });

        final Button homeButton = (Button) findViewById(R.id.home_button);
        homeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(
                    new Intent(Intent.ACTION_MAIN)
                        .addCategory(Intent.CATEGORY_HOME));
            }
        });

        final Button nightmodeButton = (Button) findViewById(R.id.nightmode_button);
        nightmodeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mDimmed = ! mDimmed;
                doDim(true);
            }
        });
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        initViews();
        doDim(false);
        refreshAll();
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        try {
            mGenieResources = getPackageManager().getResourcesForApplication(GENIE_PACKAGE_ID);
        } catch (PackageManager.NameNotFoundException e) {
            // no weather info available
            Log.w(LOG_TAG, "Can't find "+GENIE_PACKAGE_ID+". Weather forecast will not be available.");
        }

        initViews();
    }

}
