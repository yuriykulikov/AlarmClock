package com.better.alarm.ui.alert

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.view.View
import android.view.WindowManager
import android.widget.Button
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.better.alarm.R
import com.better.alarm.bootstrap.AlarmApplication
import com.better.alarm.bootstrap.globalLogger
import com.better.alarm.data.Prefs
import com.better.alarm.domain.Alarm
import com.better.alarm.domain.Calendars
import com.better.alarm.domain.IAlarmsManager
import com.better.alarm.domain.Store
import com.better.alarm.receivers.Intents
import com.better.alarm.services.Event
import com.better.alarm.services.Event.Autosilenced
import com.better.alarm.services.Event.DemuteEvent
import com.better.alarm.services.Event.DismissEvent
import com.better.alarm.services.Event.MuteEvent
import com.better.alarm.services.Event.SnoozedEvent
import com.better.alarm.ui.settings.VisionBehaviorItemView
import com.better.alarm.ui.themes.DynamicThemeHandler
import com.better.alarm.ui.timepicker.TimePickerDialogFragment
import com.better.alarm.vision.BoundingBox
import com.better.alarm.vision.CameraXHelper
import com.better.alarm.vision.DetectionAnalyzer
import com.better.alarm.vision.DetectionHandler
import com.better.alarm.vision.EmptyTTSHelper
import com.better.alarm.vision.Gesture
import com.better.alarm.vision.ITTSHelper
import com.better.alarm.vision.OverlayView
import com.better.alarm.vision.TTSHelper
import com.better.alarm.vision.IAnalyzer
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.disposables.Disposables
import kotlinx.serialization.json.Json
import org.koin.android.ext.android.inject
import java.util.Calendar
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class AlarmAlertVisionFullScreen : FragmentActivity() {
  private val store: Store by inject()
  private val alarmsManager: IAlarmsManager by inject()
  private val sp: Prefs by inject()
  private val logger by globalLogger("AlarmAlertVisionFullScreen")
  private val dynamicThemeHandler: DynamicThemeHandler by inject()
  private val calendars: Calendars by inject()
  private var mAlarm: Alarm? = null
  private var disposableDialog = Disposables.empty()
  private var subscription: Disposable? = null
  private var cameraXHelper: CameraXHelper? = null
  private var cameraExecutor: ExecutorService? = null
  private var analyzer: DetectionAnalyzer? = null
  private var ttsHelper: TTSHelper? = null
  private var isFirstAlarm = false
  private lateinit var overlayView: OverlayView

  companion object {
    val behaviorList = listOf(
      "Snooze for 5 minutes",
      "Snooze for 10 minutes",
      "Snooze for 15 minutes",
      "Snooze for 20 minutes",
      "Snooze for 30 minutes",
      "Snooze for 45 minutes",
      "Snooze for 1 hour",
      "Report time"
    )
  }

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
    isFirstAlarm = intent.getStringExtra(Intents.EXTRA_TYPE) == Intents.TYPE_NORMAL_ALARM

    mAlarm = alarmsManager.getAlarm(id)

    turnScreenOn()
    updateLayout()

    overlayView = findViewById(R.id.alert_vision_overlay)

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
    ttsHelper = if (sp.visionTTS.value) ITTSHelper(this) else EmptyTTSHelper()

    if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
      logger.debug { "camera permission is not granted, switch to normal activity" }
      switchToNormalActivity()
    } else {
      cameraExecutor = Executors.newSingleThreadExecutor()

      cameraExecutor?.execute {
        analyzer = IAnalyzer(this, detectionHandler, Json.decodeFromString(sp.visionBehavior.value))
        Handler(Looper.getMainLooper()).post {
          cameraXHelper = CameraXHelper(
            this,
            this,
            findViewById(R.id.alert_vision_preview),
            cameraExecutor!!,
            imageAnalyzer = analyzer!!.analyzer
          ) { succeeded ->
            if (succeeded) {
              if (sp.visionFlashlight.value) {
                cameraXHelper?.setOrToggleFlash(true)
              }
            } else {
              logger.debug { "camera is not opened after 5 seconds, switch to normal activity" }
              switchToNormalActivity()
            }
          }
        }
      }

      // avoid auto silence in vision mode
      mAlarm?.deleteAutoSilence()
    }
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


  private fun updateLayout() {
    setContentView(R.layout.alert_vision_fullscreen)

    findViewById<Button>(R.id.alert_vision_snooze).run {
      requestFocus()
      setOnClickListener {
        if (isSnoozeEnabled) {
          analyzer?.stop()
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
    findViewById<Button>(R.id.alert_vision_dismiss).run {
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
          analyzer?.stop()
          mAlarm?.snooze(picked.get().hour, picked.get().minute)
        } else {
          store.events.onNext(DemuteEvent())
        }
      }

    disposableDialog = CompositeDisposable(dialog, timer)
  }

  private fun dismiss() {
    analyzer?.stop()
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
  }

  override fun onResume() {
    super.onResume()
    findViewById<Button>(R.id.alert_button_snooze)?.isEnabled = isSnoozeEnabled
    findViewById<View>(R.id.alert_text_snooze)?.isEnabled = isSnoozeEnabled
  }

  override fun onPause() {
    super.onPause()
  }

  override fun onBackPressed() {
    // Don't allow back to dismiss
  }

  private val detectionHandler: DetectionHandler = object: DetectionHandler {
    override fun onPeekFinish(isPersonDetected: Boolean) {
      logger.debug { "onPeekFinish: $isPersonDetected" }
      if (isPersonDetected)
        store.events.onNext(Event.StartWakingEvent())
      else {
        if (isFirstAlarm)
          switchToNormalActivity()
        else {
          runOnUiThread { dismiss() }
          logger.debug { "nothing detected after initial detection, dismiss" }
        }
      }
    }

    override fun onPersonLeave() {
      logger.debug { "onPersonLeave" }
      ttsHelper?.speak("person left") {
        runOnUiThread{
          val t = calendars.now()
          t.add(Calendar.MINUTE, 1)
          analyzer?.stop()
          mAlarm?.snooze(t.get(Calendar.HOUR_OF_DAY), t.get(Calendar.MINUTE))
        }
      }
    }


    override fun onBehaviorAction(operation: String) {
      when (operation) {
        "Snooze for 5 minutes" -> snoozeAction(5)
        "Snooze for 10 minutes" -> snoozeAction(10)
        "Snooze for 15 minutes" -> snoozeAction(15)
        "Snooze for 20 minutes" -> snoozeAction(20)
        "Snooze for 30 minutes" -> snoozeAction(30)
        "Snooze for 45 minutes" -> snoozeAction(45)
        "Snooze for 1 hour" -> snoozeAction(60)
        "Report time" -> {

        }
      }
    }

    fun snoozeAction(minutes: Int) {
      logger.debug { "snooze gesture detected, snooze!!!" }
      ttsHelper?.speak("snooze") {
        runOnUiThread{
          val t = calendars.now()
          t.add(Calendar.MINUTE, minutes)
          analyzer?.stop()
          mAlarm?.snooze(t.get(Calendar.HOUR_OF_DAY), t.get(Calendar.MINUTE))
        }
      }
    }

    override fun onDetectUiUpdate(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
      runOnUiThread {
        overlayView.setResults(boundingBoxes)
        overlayView.invalidate()
      }
    }
  }

  /**
   * Starts the normal alarm alert activity and finishes this one.
   *
   * This is used when the camera is not available or the detector do not detect a person.
   */
  private fun switchToNormalActivity() {
    val id = intent.getIntExtra(Intents.EXTRA_ID, -1)
    val alarmType = intent.getStringExtra(Intents.EXTRA_TYPE)
    logger.debug { "switchToNormalActivity: $id $alarmType" }

    finish()
    val normalActivityIntent = Intent(this, AlarmAlertFullScreen::class.java)
    normalActivityIntent.putExtra(Intents.EXTRA_ID, id)
    normalActivityIntent.putExtra(Intents.EXTRA_TYPE, alarmType)
    startActivity(normalActivityIntent)
    store.events.onNext(Event.StartWakingEvent())
  }

  public override fun onDestroy() {
    // No longer care about the alarm being killed.
    subscription?.dispose()
    disposableDialog.dispose()
    cameraXHelper?.destroy()
    analyzer?.destroy()
    cameraExecutor?.shutdown()
    ttsHelper?.destroy()
    cameraXHelper = null
    ttsHelper = null
    super.onDestroy()
  }
}
