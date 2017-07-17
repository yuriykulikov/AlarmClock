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

package com.better.alarm.alert

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView

import com.better.alarm.R
import com.better.alarm.interfaces.Alarm
import com.better.alarm.interfaces.Intents
import com.better.alarm.presenter.TimePickerDialogFragment

import java.util.concurrent.TimeUnit

import io.reactivex.disposables.Disposables
import io.reactivex.subjects.PublishSubject

import com.better.alarm.configuration.AlarmApplication.container
import com.better.alarm.configuration.AlarmApplication.themeHandler
import com.better.alarm.configuration.Prefs.LONGCLICK_DISMISS_DEFAULT
import com.better.alarm.configuration.Prefs.LONGCLICK_DISMISS_KEY

/**
 * Alarm Clock alarm alert: pops visible indicator and plays alarm tone. This
 * activity is the full screen version which shows over the lock screen with the
 * wallpaper as the background.
 */
class AlarmAlertFullScreen : Activity() {

    private lateinit var mAlarm: Alarm

    private val alarmsManager = container().alarms()
    private val sp = container().sharedPreferences()
    private val logger = container().logger()
    private val scheduler = container().scheduler()

    private var longClickToDismiss: Boolean = false

    private var disposableDialog = Disposables.disposed()
    /**
     * Receives Intents from the model
     * Intents.ALARM_SNOOZE_ACTION
     * Intents.ALARM_DISMISS_ACTION
     * Intents.ACTION_SOUND_EXPIRED
     */
    private val mReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getIntExtra(Intents.EXTRA_ID, -1)
            if (mAlarm.id == id) {
                finish()
            }
        }
    }

    private val layoutResId = R.layout.alert_fullscreen

    private val className = AlarmAlertFullScreen::class.java.name

    private val isSnoozeEnabled: Boolean
        get() = Integer.parseInt(sp.getString("snooze_duration", "-1")) != -1

    override fun onCreate(icicle: Bundle?) {
        setTheme(themeHandler().getIdForName(className))
        super.onCreate(icicle)

        requestedOrientation = when {
            !resources.getBoolean(R.bool.isTablet) -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        // preserve initial rotation and disable rotation change
            resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT -> requestedOrientation
            else -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }

        val id = intent.getIntExtra(Intents.EXTRA_ID, -1)
        try {
            // if alarm is not found we at least wake the person up
            mAlarm = alarmsManager.getAlarm(id)

            val win = window
            // Turn on the screen
            win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    or WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON)

            updateLayout()

            // Register to get the alarm killed/snooze/dismiss intent.
            val filter = IntentFilter()
            filter.addAction(Intents.ALARM_SNOOZE_ACTION)
            filter.addAction(Intents.ALARM_DISMISS_ACTION)
            filter.addAction(Intents.ACTION_SOUND_EXPIRED)
            registerReceiver(mReceiver, filter)
        } catch (e: Exception) {
            logger.d("Alarm not found")
            finish()
        }
    }

    private fun setTitle() {
        val titleText = mAlarm.labelOrDefault
        title = titleText
        val textView = findViewById(R.id.alarm_alert_label) as TextView
        textView.text = titleText
    }

    private fun updateLayout() {
        val inflater = LayoutInflater.from(this)

        setContentView(inflater.inflate(layoutResId, null))

        /*
         * snooze behavior: pop a snooze confirmation view, kick alarm manager.
         */
        val snooze: Button = findViewById(R.id.alert_button_snooze) as Button
        snooze.requestFocus()

        snooze
                .let {
                    val clicks = PublishSubject.create<View>()
                    it.setOnClickListener { view -> clicks.onNext(view) }
                    clicks
                }
                .timeInterval(scheduler)
                .skip(1)
                .filter { interval -> interval.time(TimeUnit.MILLISECONDS) < 750 }
                .subscribe {
                    logger.d("Double click")
                    snoozeIfEnabledInSettings()
                }

        snooze.setOnLongClickListener { _ ->
            if (isSnoozeEnabled) {
                disposableDialog = TimePickerDialogFragment.showTimePicker(fragmentManager)
                        .subscribe { picked ->
                            when {
                                picked.isPresent -> mAlarm.snooze(picked.get().hour(), picked.get().minute())
                                else -> broadcast(Intents.ACTION_DEMUTE)
                            }
                        }
                broadcast(Intents.ACTION_MUTE)

                //TODO think about removing this or whatevar
                scheduler.scheduleDirect({ broadcast(Intents.ACTION_DEMUTE) }, 10L, TimeUnit.SECONDS)
            }
            true
        }

        /* dismiss button: close notification */
        val dismissButton = findViewById(R.id.alert_button_dismiss) as Button
        dismissButton.setOnClickListener { _ ->
            when {
                longClickToDismiss -> dismissButton.text = getString(R.string.alarm_alert_hold_the_button_text)
                else -> dismiss()
            }
        }

        dismissButton.setOnLongClickListener {
            dismiss()
            true
        }

        /* Set the title from the passed in alarm */
        setTitle()
    }

    private fun broadcast(action: String) {
        this@AlarmAlertFullScreen.sendBroadcast(android.content.Intent(action))
    }

    // Attempt to snooze this alert.
    private fun snoozeIfEnabledInSettings() {
        if (isSnoozeEnabled) {
            alarmsManager.snooze(mAlarm)
        }
    }

    // Dismiss the alarm.
    private fun dismiss() {
        alarmsManager.dismiss(mAlarm)
        window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
    }

    /**
     * this is called when a second alarm is triggered while a previous alert
     * window is still active.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val id = intent.getIntExtra(Intents.EXTRA_ID, -1)
        mAlarm = alarmsManager.getAlarm(id)
        setTitle()
    }

    override fun onResume() {
        super.onResume()
        longClickToDismiss = sp.getBoolean(LONGCLICK_DISMISS_KEY, LONGCLICK_DISMISS_DEFAULT)

        val snooze = findViewById(R.id.alert_button_snooze) as Button
        val snoozeText = findViewById(R.id.alert_text_snooze)
        snooze.isEnabled = isSnoozeEnabled
        snoozeText.isEnabled = isSnoozeEnabled
    }

    override fun onPause() {
        super.onPause()
        disposableDialog.dispose()
    }

    public override fun onDestroy() {
        super.onDestroy()
        // No longer care about the alarm being killed.
        unregisterReceiver(mReceiver)
    }

    override fun onBackPressed() {
        // Don't allow back to dismiss
    }
}
