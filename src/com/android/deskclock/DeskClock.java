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
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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

import java.util.Calendar;
import java.text.DateFormatSymbols;

/**
 * DeskClock clock view for desk docks.
 */
public class DeskClock extends Activity {

    private TextView mNextAlarm = null;
    private Drawable mAlarmIcon = null;

    private TextView mDate;
    private DigitalClock mTime;

    private boolean mDimmed = false;

    private void refreshAlarm() {
        String nextAlarm = Settings.System.getString(getContentResolver(),
                Settings.System.NEXT_ALARM_FORMATTED);
        if (nextAlarm != null && TextUtils.isEmpty(nextAlarm)) {
            mNextAlarm.setText(nextAlarm);
            mAlarmIcon = getResources().getDrawable(android.R.drawable.ic_lock_idle_alarm);
            mNextAlarm.setCompoundDrawablesWithIntrinsicBounds(
                mAlarmIcon, null, null, null);
            mNextAlarm.setVisibility(View.VISIBLE);
        } else {
            mNextAlarm.setVisibility(View.INVISIBLE);
        }
    }

    private void doDim() {
        View tintView = findViewById(R.id.window_tint);

        Window win = getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        if (mDimmed) {
            winParams.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
            tintView.startAnimation(AnimationUtils.loadAnimation(this, R.anim.dim));
        } else {
            winParams.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);
            tintView.startAnimation(AnimationUtils.loadAnimation(this, R.anim.undim));
        }

        win.setAttributes(winParams);
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setContentView(R.layout.desk_clock);

        mTime = (DigitalClock) findViewById(R.id.time);
        mDate = (TextView) findViewById(R.id.date);

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

        doDim();
        refreshAlarm();
    }

}
