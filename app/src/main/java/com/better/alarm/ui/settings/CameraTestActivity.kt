package com.better.alarm.ui.settings

import android.Manifest
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.better.alarm.R
import com.better.alarm.bootstrap.AlarmApplication
import com.better.alarm.bootstrap.globalLogger
import com.better.alarm.data.Prefs
import com.better.alarm.domain.Alarm
import com.better.alarm.domain.Calendars
import com.better.alarm.domain.IAlarmsManager
import com.better.alarm.domain.Store
import com.better.alarm.ui.themes.DynamicThemeHandler
import com.better.alarm.vision.BoundingBox
import com.better.alarm.vision.CameraXHelper
import com.better.alarm.vision.DetectionAnalyzer
import com.better.alarm.vision.DetectionHandler
import com.better.alarm.vision.IAnalyzer
import com.better.alarm.vision.OverlayView
import com.better.alarm.vision.TTSHelper
import io.reactivex.disposables.Disposable
import io.reactivex.disposables.Disposables
import kotlinx.serialization.json.Json
import org.koin.android.ext.android.inject
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraTestActivity : AppCompatActivity() {
  private val store: Store by inject()
  private val sp: Prefs by inject()
  private val logger by globalLogger("CameraTestActivity")
  private val dynamicThemeHandler: DynamicThemeHandler by inject()
  private var cameraXHelper: CameraXHelper? = null
  private var cameraExecutor: ExecutorService? = null
  private var analyzer: DetectionAnalyzer? = null
  private var overlayView: OverlayView? = null
  override fun onCreate(savedInstanceState: Bundle?) {
    AlarmApplication.startOnce(application)
    setTheme(dynamicThemeHandler.alertTheme())
    super.onCreate(savedInstanceState)

    requestedOrientation =
      when {
        // portrait on smartphone
        !resources.getBoolean(R.bool.isTablet) -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        // preserve initial rotation and disable rotation change on tablets
        resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT ->
          requestedOrientation
        else -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
      }

    setContentView(R.layout.activity_camera_test)
    overlayView = findViewById(R.id.alert_vision_overlay)

    if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 1000)
    }
    cameraExecutor = Executors.newSingleThreadExecutor()

    cameraExecutor!!.execute {
      analyzer = IAnalyzer(this, detectionHandler, Json.decodeFromString(sp.visionBehavior.value))
      analyzer!!.skipPeek()
      cameraXHelper = CameraXHelper(this, this,findViewById(R.id.alert_vision_preview) , cameraExecutor!!, lensFacing = CameraSelector.LENS_FACING_BACK, imageAnalyzer =  analyzer!!.analyzer) { succeeded ->
        if (!succeeded) {
          logger.error {"CameraXHelper failed to start"}
        }
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

  private fun dismiss() {
    finish()
  }
  private val detectionHandler: DetectionHandler = object : DetectionHandler {
    override fun onPeekFinish(isPersonDetected: Boolean) {

    }

    override fun onPersonLeave() {
    }

    override fun onDetectUiUpdate(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
      runOnUiThread {
        overlayView?.setResults(boundingBoxes)
        overlayView?.invalidate()
      }
    }

    override fun onBehaviorAction(operation: String) {

    }
  }
  override fun onDestroy() {
    cameraXHelper?.destroy()
    analyzer?.destroy()
    cameraExecutor?.shutdown()
    super.onDestroy()
  }
}
