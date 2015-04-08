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
import android.content.SharedPreferences;
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
import android.widget.TextView;

import com.better.alarm.R;
import com.better.alarm.model.AlarmsManager;
import com.better.alarm.model.interfaces.Alarm;
import com.better.alarm.model.interfaces.AlarmNotFoundException;
import com.better.alarm.model.interfaces.IAlarmsManager;
import com.better.alarm.model.interfaces.Intents;
import com.better.alarm.presenter.DynamicThemeHandler;
import com.better.alarm.presenter.SettingsActivity;
import com.better.alarm.presenter.TimePickerDialogFragment;
import com.better.alarm.presenter.TimePickerDialogFragment.AlarmTimePickerDialogHandler;
import com.better.alarm.presenter.TimePickerDialogFragment.OnAlarmTimePickerCanceledListener;
import com.github.androidutils.logger.Logger;

import java.util.logging.Handler;

/**
 * Alarm Clock alarm alert: pops visible indicator and plays alarm tone. This
 * activity is the full screen version which shows over the lock screen with the
 * wallpaper as the background.
 */
public class AlarmAlertFullScreen extends Activity implements AlarmTimePickerDialogHandler,
        OnAlarmTimePickerCanceledListener {
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
    private SharedPreferences sp;

    @Override
    protected void onCreate(Bundle icicle) {
        setTheme(DynamicThemeHandler.getInstance().getIdForName(getClassName()));
        super.onCreate(icicle);

        sp = PreferenceManager.getDefaultSharedPreferences(this);

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
        try {
            mAlarm = alarmsManager.getAlarm(id);

            final String vol = sp.getString(SettingsActivity.KEY_VOLUME_BEHAVIOR, DEFAULT_VOLUME_BEHAVIOR);
            mVolumeBehavior = Integer.parseInt(vol);

            final Window win = getWindow();
            win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
            // Turn on the screen unless we are being launched from the
            // AlarmAlert
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
        } catch (AlarmNotFoundException e) {
            Logger.getDefaultLogger().d("Alarm not found");
        }
    }

    private void setTitle() {
        final String titleText = mAlarm.getLabelOrDefault(this);
        setTitle(titleText);
        TextView textView = (TextView) findViewById(R.id.alarm_alert_label);
        textView.setText(titleText);

        if (getLayoutResId() == R.layout.alert || getString(R.string.default_label).equals(titleText)) {
            // in non-full screen mode we already see the label in the title.
            // Therefore we hade the views with an additional label
            // also, if the label is default, do not show it
            textView.setVisibility(View.GONE);
            findViewById(R.id.alert_label_divider).setVisibility(View.GONE);
        }
    }

    protected int getLayoutResId() {
        return R.layout.alert_fullscreen;
    }

    protected String getClassName() {
        return AlarmAlertFullScreen.class.getName();
    }

    private void updateLayout() {
        LayoutInflater inflater = LayoutInflater.from(this);

        setContentView(inflater.inflate(getLayoutResId(), null));

        /*
         * snooze behavior: pop a snooze confirmation view, kick alarm manager.
         */
        final Button snooze = (Button) findViewById(R.id.alert_button_snooze);
        snooze.requestFocus();
        snooze.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                snoozeIfEnabledInSettings();
            }
        });

        snooze.setOnLongClickListener(new Button.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (isSnoozeEnabled()) {
                    TimePickerDialogFragment.showTimePicker(getFragmentManager());
                    AlarmAlertFullScreen.this.sendBroadcast(new Intent(Intents.ACTION_MUTE));
                    new android.os.Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            //TODO think about removing this or whatevar
                            AlarmAlertFullScreen.this.sendBroadcast(new Intent(Intents.ACTION_DEMUTE));
                        }
                    }, 10000);
                }
                return true;
            }
        });

        /* dismiss button: close notification */
        final Button dismissButton = (Button) findViewById(R.id.alert_button_dismiss);
        dismissButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (longClickToDismiss) {
                    dismissButton.setText(getString(R.string.alarm_alert_hold_the_button_text));
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
    private void snoozeIfEnabledInSettings() {
        if (isSnoozeEnabled()) {
            alarmsManager.snooze(mAlarm);
        }
    }

    // Dismiss the alarm.
    private void dismiss() {
        alarmsManager.dismiss(mAlarm);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
    }

    private boolean isSnoozeEnabled() {
        return Integer.parseInt(sp.getString("snooze_duration", "-1")) != -1;
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
        try {
            mAlarm = alarmsManager.getAlarm(id);
            setTitle();
        } catch (AlarmNotFoundException e) {
            Logger.getDefaultLogger().d("Alarm not found");
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        longClickToDismiss = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(LONGCLICK_DISMISS_KEY,
                LONGCLICK_DISMISS_DEFAULT);

        Button snooze = (Button) findViewById(R.id.alert_button_snooze);
        snooze.setEnabled(isSnoozeEnabled());
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
                    snoozeIfEnabledInSettings();
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

    @Override
    public void onDialogTimeSet(int hourOfDay, int minute) {
        mAlarm.snooze(hourOfDay, minute);

    }

    @Override
    public void onTimePickerCanceled() {
        AlarmAlertFullScreen.this.sendBroadcast(new Intent(Intents.ACTION_DEMUTE));
    }
}
