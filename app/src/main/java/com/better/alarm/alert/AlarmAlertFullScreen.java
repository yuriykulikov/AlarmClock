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

package com.better.alarm.alert;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;

import com.better.alarm.R;
import com.better.alarm.background.Event;
import com.better.alarm.configuration.InjectKt;
import com.better.alarm.configuration.Prefs;
import com.better.alarm.configuration.Store;
import com.better.alarm.interfaces.Alarm;
import com.better.alarm.interfaces.IAlarmsManager;
import com.better.alarm.interfaces.Intents;
import com.better.alarm.logger.Logger;
import com.better.alarm.presenter.DynamicThemeHandler;
import com.better.alarm.presenter.PickedTime;
import com.better.alarm.presenter.TimePickerDialogFragment;
import com.better.alarm.util.Optional;

import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Predicate;

import static com.better.alarm.configuration.Prefs.LONGCLICK_DISMISS_DEFAULT;
import static com.better.alarm.configuration.Prefs.LONGCLICK_DISMISS_KEY;

/**
 * Alarm Clock alarm alert: pops visible indicator and plays alarm tone. This
 * activity is the full screen version which shows over the lock screen with the
 * wallpaper as the background.
 */
public class AlarmAlertFullScreen extends FragmentActivity {
    protected static final String SCREEN_OFF = "screen_off";
    private final Store store = InjectKt.globalInject(Store.class).getValue();
    private final IAlarmsManager alarmsManager = InjectKt.globalInject(IAlarmsManager.class).getValue();
    private final Prefs sp = InjectKt.globalInject(Prefs.class).getValue();
    private final Logger logger = InjectKt.globalLogger("AlarmAlertFullScreen").getValue();
    private final DynamicThemeHandler dynamicThemeHandler = InjectKt.globalInject(DynamicThemeHandler.class).getValue();

    protected Alarm mAlarm;

    private boolean longClickToDismiss;

    private Disposable disposableDialog = Disposables.disposed();
    private Disposable subscription;

    @Override
    protected void onCreate(Bundle icicle) {
        setTheme(dynamicThemeHandler.getIdForName(getClassName()));
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

        final int id = getIntent().getIntExtra(Intents.EXTRA_ID, -1);
        try {
            mAlarm = alarmsManager.getAlarm(id);

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
            subscription = store.getEvents()
                    .filter(new Predicate<Event>() {
                        @Override
                        public boolean test(Event event) throws Exception {
                            return (event instanceof Event.SnoozedEvent && ((Event.SnoozedEvent) event).getId() == id)
                                    || (event instanceof Event.DismissEvent && ((Event.DismissEvent) event).getId() == id)
                                    || (event instanceof Event.Autosilenced && ((Event.Autosilenced) event).getId() == id);
                        }
                    }).subscribe(new Consumer<Event>() {
                        @Override
                        public void accept(Event event) throws Exception {
                            finish();
                        }
                    });
        } catch (Exception e) {
            logger.e("Alarm not found", e);
        }
    }

    private void setTitle() {
        final String titleText = mAlarm.getLabelOrDefault();
        setTitle(titleText);
        TextView textView = findViewById(R.id.alarm_alert_label);
        textView.setText(titleText);
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
                    disposableDialog = TimePickerDialogFragment.showTimePicker(getSupportFragmentManager())
                            .subscribe(new Consumer<Optional<PickedTime>>() {
                                @Override
                                public void accept(@NonNull Optional<PickedTime> picked) {
                                    if (picked.isPresent()) {
                                        mAlarm.snooze(picked.get().getHour(), picked.get().getMinute());
                                    } else {
                                        AlarmAlertFullScreen.this.sendBroadcast(new Intent(Intents.ACTION_DEMUTE));
                                    }
                                }
                            });
                    store.getEvents().onNext(new Event.MuteEvent());
                    new android.os.Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            store.getEvents().onNext(new Event.DemuteEvent());
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
        return sp.getSnoozeDuration().getValue() != -1;
    }

    /**
     * this is called when a second alarm is triggered while a previous alert
     * window is still active.
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        logger.d("AlarmAlert.OnNewIntent()");

        int id = intent.getIntExtra(Intents.EXTRA_ID, -1);
        try {
            mAlarm = alarmsManager.getAlarm(id);
            setTitle();
        } catch (Exception e) {
            logger.d("Alarm not found");
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        longClickToDismiss = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(LONGCLICK_DISMISS_KEY,
                LONGCLICK_DISMISS_DEFAULT);

        Button snooze = findViewById(R.id.alert_button_snooze);
        View snoozeText = findViewById(R.id.alert_text_snooze);
        if (snooze != null) snooze.setEnabled(isSnoozeEnabled());
        if (snoozeText != null) snoozeText.setEnabled(isSnoozeEnabled());
    }

    @Override
    protected void onPause() {
        super.onPause();
        disposableDialog.dispose();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // No longer care about the alarm being killed.
        subscription.dispose();
    }

    @Override
    public void onBackPressed() {
        // Don't allow back to dismiss
    }
}
