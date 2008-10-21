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
import android.app.Notification;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.Toast;

import java.util.Calendar;

/**
 * Alarm Clock alarm alert: pops visible indicator and plays alarm
 * tone
 */
public class AlarmAlert extends Activity implements Alarms.AlarmSettings {

    private static long[] sVibratePattern = new long[] { 500, 500 };

    private NotificationManager mNotificationManager;

    private final static int AUDIO_ALERT_NOTIFICATION_ID = 0;
    /** Play alarm up to 5 minutes before silencing */
    private final static int ALARM_TIMEOUT_SECONDS = 300;
    private final static int SNOOZE_MINUTES = 10;

    private KeyguardManager mKeyguardManager;
    private KeyguardManager.KeyguardLock mKeyguardLock = null;
    private Handler mTimeout;
    private Button mSnoozeButton;

    private int mAlarmId;
    private Alarms.DaysOfWeek mDaysOfWeek;
    private String mAlert;
    private boolean mVibrate;
    private boolean mSnoozed;
    private boolean mCleanupCalled = false;

    public void reportAlarm(
            int idx, boolean enabled, int hour, int minutes,
            Alarms.DaysOfWeek daysOfWeek, boolean vibrate, String message,
            String alert) {
        if (Log.LOGV) Log.v("AlarmAlert.reportAlarm: " + idx + " " + hour +
                            " " + minutes + " dow " + daysOfWeek);
        mAlert = alert;
        mDaysOfWeek = daysOfWeek;
        mVibrate = vibrate;
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.alarm_alert);

        mKeyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

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

        playAlert(getIntent());

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
                disableKiller();
                cleanupAlarm();
                releaseLocks();
                finish();
            }
        });

        /* dismiss button: close notification */
        findViewById(R.id.dismiss).setOnClickListener(new Button.OnClickListener() {
                public void onClick(View v) {
                    disableKiller();
                    cleanupAlarm();
                    releaseLocks();
                    finish();
                }
            });
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
        cleanupAlarm();
        mCleanupCalled = false;
        disableKiller();
        playAlert(intent);
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
        disableKiller();
        cleanupAlarm();
        releaseLocks();
    }

    /**
     * kicks off audio/vibe alert
     */
    private void playAlert(Intent intent) {
        mAlarmId = intent.getIntExtra(Alarms.ID, 0);
        if (Log.LOGV) Log.v("playAlert() " + mAlarmId);

        /* load audio alert */
        ContentResolver cr = getContentResolver();
        /* this will call reportAlarm() callback */
        Alarms.getAlarm(cr, this, mAlarmId);

        /* play audio alert */
        if (mAlert == null) {
            Log.e("Unable to play alarm: no audio file available");
        } else {
            Notification audio = new Notification();
            audio.sound = Uri.parse(mAlert);
            audio.audioStreamType = AudioManager.STREAM_ALARM;
            audio.flags |= Notification.FLAG_INSISTENT;
            if (mVibrate) audio.vibrate = sVibratePattern;
            mNotificationManager.notify(AUDIO_ALERT_NOTIFICATION_ID, audio);
        }
        enableKiller();
    }

    /**
     * Kills alarm audio after ALARM_TIMEOUT_SECONDS, so the alarm
     * won't run all day.
     *
     * This just cancels the audio, but leaves the notification
     * popped, so the user will know that the alarm tripped.
     */
    private void enableKiller() {
        mTimeout = new Handler();
        mTimeout.postDelayed(new Runnable() {
                public void run() {
                    if (Log.LOGV) Log.v("*********** Alarm killer triggered *************");
                    /* don't allow snooze */
                    mSnoozeButton.setEnabled(false);
                    cleanupAlarm();
                    releaseLocks();
                }}, 1000 * ALARM_TIMEOUT_SECONDS);
    }

    private void disableKiller() {
        if (mTimeout != null) {
            mTimeout.removeCallbacksAndMessages(null);
            mTimeout = null;
        }
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

    /**
     * Stops alarm audio and disables alarm if it not snoozed and not
     * repeating
     */
    private synchronized void cleanupAlarm() {
        if (Log.LOGV) Log.v("cleanupAlarm " + mAlarmId);
        if (!mCleanupCalled) {
            mCleanupCalled = true;

            // Stop audio playing
            mNotificationManager.cancel(AUDIO_ALERT_NOTIFICATION_ID);

            /* disable alarm only if it is not set to repeat */
            if (!mSnoozed && ((mDaysOfWeek == null || !mDaysOfWeek.isRepeatSet()))) {
                Alarms.enableAlarm(AlarmAlert.this, mAlarmId, false);
            }
        }
    }
}
