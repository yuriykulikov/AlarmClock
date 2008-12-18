/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.alarmclock;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;
import android.widget.TextView;

import java.util.Calendar;

/**
 * Alarm Clock alarm alert: pops visible indicator and plays alarm
 * tone
 */
public class AlarmAlert extends Activity {

    private final static int SNOOZE_MINUTES = 10;

    private KeyguardManager mKeyguardManager;
    private KeyguardManager.KeyguardLock mKeyguardLock = null;
    private Button mSnoozeButton;
    private boolean mSnoozed;

    private AlarmKlaxon mKlaxon;
    private int mAlarmId;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        /* FIXME Intentionally verbose: always log this until we've
           fully debugged the app failing to start up */
        Log.v("AlarmAlert.onCreate()");

        setContentView(R.layout.alarm_alert);

        mKlaxon = AlarmKlaxon.getInstance();

        // Popup alert over black screen
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        // XXX DO NOT COPY THIS!!!  THIS IS BOGUS!  Making an activity have
        // a system alert type is completely broken, because the activity
        // manager will still hide/show it as if it is part of the normal
        // activity stack.  If this is really what you want and you want it
        // to work correctly, you should create and show your own custom window.
        lp.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        lp.token = null;
        getWindow().setAttributes(lp);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

        mKeyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);

        /* set clock face */
        LayoutInflater mFactory = LayoutInflater.from(this);
        SharedPreferences settings = getSharedPreferences(AlarmClock.PREFERENCES, 0);
        int face = settings.getInt(AlarmClock.PREF_CLOCK_FACE, 0);
        if (face < 0 || face >= AlarmClock.CLOCKS.length) face = 0;
        View clockLayout = (View)mFactory.inflate(AlarmClock.CLOCKS[face], null);
        ViewGroup clockView = (ViewGroup)findViewById(R.id.clockView);
        clockView.addView(clockLayout);
        if (clockLayout instanceof DigitalClock) {
            ((DigitalClock)clockLayout).setAnimate();
        }

        mAlarmId = getIntent().getIntExtra(Alarms.ID, -1);

        /* allow next alarm to trigger while this activity is
           active */
        Alarms.disableSnoozeAlert(AlarmAlert.this);
        Alarms.disableAlert(AlarmAlert.this, mAlarmId);
        Alarms.setNextAlert(this);

        /* snooze behavior: pop a snooze confirmation view, kick alarm
           manager. */
        mSnoozeButton = (Button) findViewById(R.id.snooze);
        mSnoozeButton.requestFocus();
        mSnoozeButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                /* If next alarm is set for sooner than the snooze interval,
                   don't snooze: instead toast user that snooze will not be set */
                final long snoozeTarget = System.currentTimeMillis() + 1000 * 60 * SNOOZE_MINUTES;
                long nextAlarm = Alarms.calculateNextAlert(AlarmAlert.this).getAlert();
                if (nextAlarm < snoozeTarget) {
                    Calendar c = Calendar.getInstance();
                    c.setTimeInMillis(nextAlarm);
                    Toast.makeText(AlarmAlert.this,
                                   getString(R.string.alarm_alert_snooze_not_set,
                                             Alarms.formatTime(AlarmAlert.this, c)),
                                   Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(AlarmAlert.this,
                                   getString(R.string.alarm_alert_snooze_set,
                                             SNOOZE_MINUTES),
                                   Toast.LENGTH_LONG).show();

                    Alarms.saveSnoozeAlert(AlarmAlert.this, mAlarmId, snoozeTarget);
                    Alarms.setNextAlert(AlarmAlert.this);
                    mSnoozed = true;
                }
                mKlaxon.stop(AlarmAlert.this, mSnoozed);
                releaseLocks();
                finish();
            }
        });

        /* dismiss button: close notification */
        findViewById(R.id.dismiss).setOnClickListener(new Button.OnClickListener() {
                public void onClick(View v) {
                    mKlaxon.stop(AlarmAlert.this, mSnoozed);
                    releaseLocks();
                    finish();
                }
            });

        mKlaxon.setKillerCallback(new AlarmKlaxon.KillerCallback() {
            public void onKilled() {
                if (Log.LOGV) Log.v("onKilled()");
                TextView silenced = (TextView)findViewById(R.id.silencedText);
                silenced.setText(
                        getString(R.string.alarm_alert_alert_silenced,
                                  AlarmKlaxon.ALARM_TIMEOUT_SECONDS / 60));
                silenced.setVisibility(View.VISIBLE);

                /* don't allow snooze */
                mSnoozeButton.setEnabled(false);

                mKlaxon.stop(AlarmAlert.this, mSnoozed);
                releaseLocks();
            }
        });

        mKlaxon.restoreInstanceState(this, icicle);
    }

    /**
     * this is called when a second alarm is triggered while a
     * previous alert window is still active.
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (Log.LOGV) Log.v("AlarmAlert.OnNewIntent()");
        mSnoozeButton.setEnabled(true);
        disableKeyguard();

        mAlarmId = intent.getIntExtra(Alarms.ID, -1);

        /* unset silenced message */
        TextView silenced = (TextView)findViewById(R.id.silencedText);
        silenced.setVisibility(View.GONE);

        Alarms.setNextAlert(this);
        setIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Log.LOGV) Log.v("AlarmAlert.onResume()");
        disableKeyguard();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (Log.LOGV) Log.v("AlarmAlert.onStop()");
        mKlaxon.stop(this, mSnoozed);
        releaseLocks();
    }

    @Override
    protected void onSaveInstanceState(Bundle icicle) {
        mKlaxon.onSaveInstanceState(icicle);
    }

    private synchronized void enableKeyguard() {
        if (mKeyguardLock != null) {
            mKeyguardLock.reenableKeyguard();
            mKeyguardLock = null;
        }
    }

    private synchronized void disableKeyguard() {
        if (mKeyguardLock == null) {
            mKeyguardLock = mKeyguardManager.newKeyguardLock(Log.LOGTAG);
            mKeyguardLock.disableKeyguard();
        }
    }

    /**
     * release wake and keyguard locks
     */
    private synchronized void releaseLocks() {
        AlarmAlertWakeLock.release();
        enableKeyguard();
    }
}
