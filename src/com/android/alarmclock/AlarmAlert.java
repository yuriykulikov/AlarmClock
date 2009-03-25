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
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.view.KeyEvent;
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

    private static final int SNOOZE_MINUTES = 10;
    private static final int UNKNOWN = 0;
    private static final int SNOOZE = 1;
    private static final int DISMISS = 2;
    private static final int KILLED = 3;

    private KeyguardManager mKeyguardManager;
    private KeyguardManager.KeyguardLock mKeyguardLock;
    private Button mSnoozeButton;
    private int mState = UNKNOWN;

    private AlarmKlaxon mKlaxon;
    private int mAlarmId;
    private String mLabel;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // Maintain a lock during the playback of the alarm. This lock may have
        // already been acquired in AlarmReceiver. If the process was killed,
        // the global wake lock is gone. Acquire again just to be sure.
        AlarmAlertWakeLock.acquire(this);

        /* FIXME Intentionally verbose: always log this until we've
           fully debugged the app failing to start up */
        Log.v("AlarmAlert.onCreate()");

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

        Intent i = getIntent();
        mAlarmId = i.getIntExtra(Alarms.ID, -1);

        mKlaxon = new AlarmKlaxon();
        mKlaxon.postPlay(this, mAlarmId);

        /* Set the title from the passed in label */
        setTitleFromIntent(i);

        /* allow next alarm to trigger while this activity is
           active */
        Alarms.disableSnoozeAlert(AlarmAlert.this);
        Alarms.disableAlert(AlarmAlert.this, mAlarmId);
        Alarms.setNextAlert(this);

        mKlaxon.setKillerCallback(new AlarmKlaxon.KillerCallback() {
            public void onKilled() {
                if (Log.LOGV) Log.v("onKilled()");
                updateSilencedText();

                /* don't allow snooze */
                mSnoozeButton.setEnabled(false);

                // Dismiss the alarm but mark the state as killed so if the
                // config changes, we show the silenced message and disable
                // snooze.
                dismiss();
                mState = KILLED;
            }
        });

        updateLayout();
    }

    private void setTitleFromIntent(Intent i) {
        mLabel = i.getStringExtra(Alarms.LABEL);
        if (mLabel == null || mLabel.length() == 0) {
            mLabel = getString(R.string.default_label);
        }
        setTitle(mLabel);
    }

    private void updateSilencedText() {
        TextView silenced = (TextView) findViewById(R.id.silencedText);
        silenced.setText(getString(R.string.alarm_alert_alert_silenced,
                    AlarmKlaxon.ALARM_TIMEOUT_SECONDS / 60));
        silenced.setVisibility(View.VISIBLE);
    }

    private void updateLayout() {
        setContentView(R.layout.alarm_alert);

        /* set clock face */
        LayoutInflater mFactory = LayoutInflater.from(this);
        SharedPreferences settings =
                getSharedPreferences(AlarmClock.PREFERENCES, 0);
        int face = settings.getInt(AlarmClock.PREF_CLOCK_FACE, 0);
        if (face < 0 || face >= AlarmClock.CLOCKS.length) {
            face = 0;
        }
        View clockLayout =
                (View) mFactory.inflate(AlarmClock.CLOCKS[face], null);
        ViewGroup clockView = (ViewGroup) findViewById(R.id.clockView);
        clockView.addView(clockLayout);
        if (clockLayout instanceof DigitalClock) {
            ((DigitalClock) clockLayout).setAnimate();
        }

        /* snooze behavior: pop a snooze confirmation view, kick alarm
           manager. */
        mSnoozeButton = (Button) findViewById(R.id.snooze);
        mSnoozeButton.requestFocus();
        // If this was a configuration change, keep the silenced text if the
        // alarm was killed.
        if (mState == KILLED) {
            updateSilencedText();
            mSnoozeButton.setEnabled(false);
        } else {
            mSnoozeButton.setOnClickListener(new Button.OnClickListener() {
                public void onClick(View v) {
                    snooze();
                    finish();
                }
            });
        }

        /* dismiss button: close notification */
        findViewById(R.id.dismiss).setOnClickListener(
                new Button.OnClickListener() {
                    public void onClick(View v) {
                        dismiss();
                        finish();
                    }
                });
    }

    // Attempt to snooze this alert.
    private void snooze() {
        if (mState != UNKNOWN) {
            return;
        }
        // If the next alarm is set for sooner than the snooze interval, don't
        // snooze. Instead, toast the user that the snooze will not be set.
        final long snoozeTime = System.currentTimeMillis()
                + (1000 * 60 * SNOOZE_MINUTES);
        final long nextAlarm =
                Alarms.calculateNextAlert(AlarmAlert.this).getAlert();
        String displayTime = null;
        if (nextAlarm < snoozeTime) {
            final Calendar c = Calendar.getInstance();
            c.setTimeInMillis(nextAlarm);
            displayTime = getString(R.string.alarm_alert_snooze_not_set,
                    Alarms.formatTime(AlarmAlert.this, c));
            mState = DISMISS;
        } else {
            Alarms.saveSnoozeAlert(AlarmAlert.this, mAlarmId, snoozeTime,
                    mLabel);
            Alarms.setNextAlert(AlarmAlert.this);
            displayTime = getString(R.string.alarm_alert_snooze_set,
                    SNOOZE_MINUTES);
            mState = SNOOZE;
        }
        // Intentionally log the snooze time for debugging.
        Log.v(displayTime);
        // Display the snooze minutes in a toast.
        Toast.makeText(AlarmAlert.this, displayTime, Toast.LENGTH_LONG).show();
        mKlaxon.stop(this, mState == SNOOZE);
        releaseLocks();
    }

    // Dismiss the alarm.
    private void dismiss() {
        if (mState != UNKNOWN) {
            return;
        }
        mState = DISMISS;
        mKlaxon.stop(this, false);
        releaseLocks();
    }

    /**
     * this is called when a second alarm is triggered while a
     * previous alert window is still active.
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (Log.LOGV) Log.v("AlarmAlert.OnNewIntent()");
        mState = UNKNOWN;
        mSnoozeButton.setEnabled(true);
        disableKeyguard();

        mAlarmId = intent.getIntExtra(Alarms.ID, -1);
        // Play the new alarm sound.
        mKlaxon.postPlay(this, mAlarmId);

        setTitleFromIntent(intent);

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
        // As a last resort, try to snooze if this activity is stopped.
        snooze();
        // We might have been killed by the KillerCallback so always release
        // the lock and keyguard.
        releaseLocks();
    }

    @Override
    public void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);
        updateLayout();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // Do this on key down to handle a few of the system keys. Only handle
        // the snooze and dismiss this alert if the state is unknown.
        boolean up = event.getAction() == KeyEvent.ACTION_UP;
        boolean dismiss = false;
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_DPAD_CENTER:
            // Ignore ENDCALL because we do not receive the event if the screen
            // is on. However, we do receive the key up for ENDCALL if the
            // screen was off.
            case KeyEvent.KEYCODE_ENDCALL:
                break;
            // Volume keys dismiss the alarm
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                dismiss = true;
            // All other keys will snooze the alarm
            default:
                // Check for UNKNOWN here so that we intercept both key events
                // and prevent the volume keys from triggering their default
                // behavior.
                if (mState == UNKNOWN && up) {
                    if (dismiss) {
                        dismiss();
                    } else {
                        snooze();
                    }
                    finish();
                }
                return true;
        }
        return super.dispatchKeyEvent(event);
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
