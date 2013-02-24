/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.better.alarm.presenter.alert;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import com.better.alarm.R;
import com.better.alarm.model.AlarmsManager;
import com.better.alarm.model.interfaces.Alarm;
import com.better.alarm.model.interfaces.IAlarmsManager;
import com.better.alarm.model.interfaces.Intents;
import com.better.alarm.presenter.SettingsActivity;
import com.github.androidutils.logger.Logger;

/**
 * Alarm Clock alarm alert: pops visible indicator and plays alarm tone. This
 * activity is the full screen version which shows over the lock screen with the
 * wallpaper as the background.
 */
public class AlarmAlertFullScreen extends Activity {
    private static final boolean LONGCLICK_DISMISS_DEFAULT = false;
    private static final String LONGCLICK_DISMISS_KEY = "longclick_dismiss_key";
    private static final String DEFAULT_VOLUME_BEHAVIOR = "2";
    protected static final String SCREEN_OFF = "screen_off";

    protected Alarm mAlarm;
    private int mVolumeBehavior;
    boolean mFullscreenStyle;

    private IAlarmsManager alarmsManager;

    private boolean longClickToDismiss;
    /**
     * Receives Intents from the model
     */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            int id = intent.getIntExtra(Intents.EXTRA_ID, -1);
            if (action.equals(Intents.ALARM_SNOOZE_ACTION)) {
                if (mAlarm.getId() == id) {
                    finish();
                }
            } else if (action.equals(Intents.ALARM_DISMISS_ACTION)) {
                if (mAlarm.getId() == id) {
                    finish();
                }
            } else if (action.equals(Intents.ACTION_SOUND_EXPIRED)) {
                if (mAlarm.getId() == id) {
                    // if sound has expired there is no need to keep the screen
                    // on
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        if (getResources().getBoolean(R.bool.isTablet)) {
            // preserve initial rotation and disable rotation change
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                setRequestedOrientation(getRequestedOrientation());
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        alarmsManager = AlarmsManager.getAlarmsManager();

        int id = getIntent().getIntExtra(Intents.EXTRA_ID, -1);
        mAlarm = alarmsManager.getAlarm(id);

        // Get the volume/camera button behavior setting
        final String vol = PreferenceManager.getDefaultSharedPreferences(this).getString(
                SettingsActivity.KEY_VOLUME_BEHAVIOR, DEFAULT_VOLUME_BEHAVIOR);
        mVolumeBehavior = Integer.parseInt(vol);

        final Window win = getWindow();
        win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        // Turn on the screen unless we are being launched from the AlarmAlert
        // subclass as a result of the screen turning off.
        if (!getIntent().getBooleanExtra(SCREEN_OFF, false)) {
            win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
        }

        updateLayout();

        // Register to get the alarm killed/snooze/dismiss intent.
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intents.ALARM_SNOOZE_ACTION);
        filter.addAction(Intents.ALARM_DISMISS_ACTION);
        filter.addAction(Intents.ACTION_CANCEL_SNOOZE);
        filter.addAction(Intents.ACTION_SOUND_EXPIRED);
        registerReceiver(mReceiver, filter);
    }

    private void setTitle() {
        final String titleText = mAlarm.getLabelOrDefault(this);

        setTitle(titleText);
    }

    protected int getLayoutResId() {
        return R.layout.alarm_alert_fullscreen;
    }

    private void updateLayout() {
        LayoutInflater inflater = LayoutInflater.from(this);

        setContentView(inflater.inflate(getLayoutResId(), null));

        /*
         * snooze behavior: pop a snooze confirmation view, kick alarm manager.
         */
        Button snooze = (Button) findViewById(R.id.snooze);
        snooze.requestFocus();
        snooze.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                snooze();
            }
        });

        /* dismiss button: close notification */
        final Button dismissButton = (Button) findViewById(R.id.dismiss);
        dismissButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (longClickToDismiss) {
                    dismissButton.setText("Hold to dismiss");
                } else {
                    dismiss();
                }
            }
        });

        dismissButton.setOnLongClickListener(new Button.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                dismiss();
                return true;
            }
        });

        /* Set the title from the passed in alarm */
        setTitle();
    }

    // Attempt to snooze this alert.
    private void snooze() {
        alarmsManager.snooze(mAlarm);
        // Do not snooze if the snooze button is disabled.
        if (!findViewById(R.id.snooze).isEnabled()) {
            dismiss();
            return;
        }
    }

    // Dismiss the alarm.
    private void dismiss() {
        alarmsManager.dismiss(mAlarm);
    }

    /**
     * this is called when a second alarm is triggered while a previous alert
     * window is still active.
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        Logger.getDefaultLogger().d("AlarmAlert.OnNewIntent()");

        int id = intent.getIntExtra(Intents.EXTRA_ID, -1);
        mAlarm = alarmsManager.getAlarm(id);

        setTitle();
    }

    @Override
    protected void onResume() {
        super.onResume();
        longClickToDismiss = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(LONGCLICK_DISMISS_KEY,
                LONGCLICK_DISMISS_DEFAULT);
        // XXX this is some wierd logic and should not be here
        // If the alarm was deleted at some point, disable snooze.
        // if (AlarmsManager.getAlarm(getContentResolver(), mAlarm.id) == null)
        // {
        // Button snooze = (Button) findViewById(R.id.snooze);
        // snooze.setEnabled(false);
        // }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Logger.getDefaultLogger().d("AlarmAlert.onDestroy()");
        // No longer care about the alarm being killed.
        unregisterReceiver(mReceiver);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // Do this on key down to handle a few of the system keys.
        boolean up = event.getAction() == KeyEvent.ACTION_UP;
        switch (event.getKeyCode()) {
        // Volume keys and camera keys dismiss the alarm
        case KeyEvent.KEYCODE_VOLUME_UP:
        case KeyEvent.KEYCODE_VOLUME_DOWN:
        case KeyEvent.KEYCODE_VOLUME_MUTE:
        case KeyEvent.KEYCODE_CAMERA:
        case KeyEvent.KEYCODE_FOCUS:
            if (up) {
                switch (mVolumeBehavior) {
                case 1:
                    snooze();
                    break;

                case 2:
                    dismiss();
                    break;

                default:
                    break;
                }
            }
            return true;
        default:
            break;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onBackPressed() {
        // Don't allow back to dismiss. This method is overriden by AlarmAlert
        // so that the dialog is dismissed.
        return;
    }
}
