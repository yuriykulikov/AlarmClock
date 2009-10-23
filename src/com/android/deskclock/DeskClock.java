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
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.animation.AnimationUtils;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.CheckBox;

import static android.os.BatteryManager.BATTERY_STATUS_CHARGING;
import static android.os.BatteryManager.BATTERY_STATUS_FULL;
import static android.os.BatteryManager.BATTERY_STATUS_UNKNOWN;

import java.text.DateFormat;
import java.util.Date;

/**
 * DeskClock clock view for desk docks.
 */
public class DeskClock extends Activity {

    private TextView mNextAlarm = null;
    private TextView mDate;
    private TextView mBatteryDisplay;
    private DigitalClock mTime;

    private boolean mDimmed = false;

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


    private DateFormat mDateFormat;
    
    private int mBatteryLevel;
    private boolean mPluggedIn;

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

    private void doDim() {
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
            tintView.startAnimation(AnimationUtils.loadAnimation(this, R.anim.dim));
        } else {
            winParams.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);
//            winParams.flags |= WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
            winParams.dimAmount = 0.2f; // lower contrast in normal mode
    
            // hide the window tint
            tintView.startAnimation(AnimationUtils.loadAnimation(this, R.anim.undim));
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

        doDim();
        refreshDate();
        refreshAlarm();
        refreshBattery();
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mIntentReceiver);
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setContentView(R.layout.desk_clock);

        mTime = (DigitalClock) findViewById(R.id.time);
        mDate = (TextView) findViewById(R.id.date);
        mBatteryDisplay = (TextView) findViewById(R.id.battery);

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
            }
        });

        final Button musicButton = (Button) findViewById(R.id.music_button);
        musicButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            }
        });

        final Button nightmodeButton = (Button) findViewById(R.id.nightmode_button);
        nightmodeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mDimmed = ! mDimmed;
                doDim();
            }
        });

    }

}
