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
package com.better.alarm.ui.alert

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import com.better.alarm.R
import com.better.alarm.bootstrap.AlarmApplication
import com.better.alarm.bootstrap.globalLogger
import com.better.alarm.data.Prefs
import com.better.alarm.domain.Alarm
import com.better.alarm.domain.IAlarmsManager
import com.better.alarm.domain.Store
import com.better.alarm.receivers.Intents
import com.better.alarm.services.Event.Autosilenced
import com.better.alarm.services.Event.DemuteEvent
import com.better.alarm.services.Event.DismissEvent
import com.better.alarm.services.Event.MuteEvent
import com.better.alarm.services.Event.SnoozedEvent
import com.better.alarm.ui.themes.DynamicThemeHandler
import com.better.alarm.ui.timepicker.TimePickerDialogFragment
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.disposables.Disposables
import java.util.concurrent.TimeUnit
import org.koin.android.ext.android.inject

/**
 * Alarm Clock alarm alert: pops visible indicator and plays alarm tone. This activity is the full
 * screen version which shows over the lock screen with the wallpaper as the background.
 */
class AlarmAlertFullScreen : FragmentActivity() {
  private val store: Store by inject()
  private val alarmsManager: IAlarmsManager by inject()
  private val sp: Prefs by inject()
  private val logger by globalLogger("AlarmAlertFullScreen")
  private val dynamicThemeHandler: DynamicThemeHandler by inject()
  private var mAlarm: Alarm? = null
  private var disposableDialog = Disposables.empty()
  private var subscription: Disposable? = null

  override fun onCreate(icicle: Bundle?) {
    AlarmApplication.startOnce(application)
    setTheme(dynamicThemeHandler.alertTheme())
    super.onCreate(icicle)
    requestedOrientation =
        when {
          // portrait on smartphone
          !resources.getBoolean(R.bool.isTablet) -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
          // preserve initial rotation and disable rotation change on tablets
          resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT ->
              requestedOrientation
          else -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
    val id = intent.getIntExtra(Intents.EXTRA_ID, -1)

    mAlarm = alarmsManager.getAlarm(id)

    turnScreenOn()
    updateLayout()

    // Register to get the alarm killed/snooze/dismiss intent.
    subscription =
        store.events
            .filter { event ->
              (event is SnoozedEvent && event.id == id ||
                  event is DismissEvent && event.id == id ||
                  event is Autosilenced && event.id == id)
            }
            .take(1)
            .subscribe { finish() }
  }

  /**
   * ## Turns the screen on
   *
   * See https://github.com/yuriykulikov/AlarmClock/issues/360 It seems that on some devices with
   * API>=27 calling `setTurnScreenOn(true)` is not enough, so we will just add all flags regardless
   * of the API level, and call `setTurnScreenOn(true)` if API level is 27+
   *
   * ### 3.07.01 reference In `3.07.01` we added these 4 flags:
   * ```
   * final Window win = getWindow();
   * win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
   * win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
   *         | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
   *         | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
   * ```
   */
  private fun turnScreenOn() {
    if (Build.VERSION.SDK_INT >= 27) {
      setShowWhenLocked(true)
      setTurnScreenOn(true)
    }
    // Deprecated flags are required on some devices, even with API>=27
    window.addFlags(
        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
  }

  private fun setTitle() {
    val titleText = mAlarm?.labelOrDefault ?: ""
    title = titleText
    findViewById<TextView>(R.id.alarm_alert_label).text = titleText
  }

  private fun updateLayout() {
    setContentView(R.layout.alert_fullscreen)

    findViewById<Button>(R.id.alert_button_snooze).run {
      requestFocus()
      setOnClickListener {
        if (isSnoozeEnabled) {
          mAlarm?.snooze()
        }
      }
      setOnLongClickListener {
        if (isSnoozeEnabled) {
          showSnoozePicker()
        }
        true
      }
    }
    findViewById<Button>(R.id.alert_button_dismiss).run {
      setOnClickListener {
        if (sp.longClickDismiss.value) {
          text = getString(R.string.alarm_alert_hold_the_button_text)
        } else {
          dismiss()
        }
      }
      setOnLongClickListener {
        dismiss()
        true
      }
    }

    setTitle()
  }

  /**
   * Shows a time picker to pick the next snooze time. Mutes the sound for the first 10 seconds to
   * let the user choose the time. Demutes after cancel or after 10 seconds to deal with
   * unintentional clicks.
   */
  private fun showSnoozePicker() {
    store.events.onNext(MuteEvent())
    val timer =
        Observable.timer(10, TimeUnit.SECONDS, AndroidSchedulers.mainThread()).subscribe {
          store.events.onNext(DemuteEvent())
        }

    val dialog =
        TimePickerDialogFragment.showTimePicker(supportFragmentManager).subscribe { picked ->
          timer.dispose()
          if (picked.isPresent()) {
            mAlarm?.snooze(picked.get().hour, picked.get().minute)
          } else {
            store.events.onNext(DemuteEvent())
          }
        }

    disposableDialog = CompositeDisposable(dialog, timer)
  }

  private fun dismiss() {
    mAlarm?.dismiss()
  }

  private val isSnoozeEnabled: Boolean
    get() = sp.snoozeDuration.value != -1

  /**
   * this is called when a second alarm is triggered while a previous alert window is still active.
   */
  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    logger.debug { "AlarmAlert.OnNewIntent()" }
    val id = intent.getIntExtra(Intents.EXTRA_ID, -1)
    mAlarm = alarmsManager.getAlarm(id)
    setTitle()
  }

  override fun onResume() {
    super.onResume()
    findViewById<Button>(R.id.alert_button_snooze)?.isEnabled = isSnoozeEnabled
    findViewById<View>(R.id.alert_text_snooze)?.isEnabled = isSnoozeEnabled
  }

  override fun onPause() {
    super.onPause()
    disposableDialog.dispose()
  }

  public override fun onDestroy() {
    super.onDestroy()
    // No longer care about the alarm being killed.
    subscription?.dispose()
    disposableDialog.dispose()
  }

  override fun onBackPressed() {
    // Don't allow back to dismiss
  }
}
