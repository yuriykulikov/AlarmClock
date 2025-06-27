package com.better.alarm.ui.settings

import android.Manifest
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.CheckBoxPreference
import com.better.alarm.R
import com.better.alarm.bootstrap.AlarmApplication
import com.better.alarm.bootstrap.globalLogger
import com.better.alarm.data.Prefs
import com.better.alarm.ui.themes.DynamicThemeHandler
import com.better.alarm.vision.BoundingBox
import com.better.alarm.vision.CameraXHelper
import com.better.alarm.vision.DetectionAnalyzer
import com.better.alarm.vision.DetectionHandler
import com.better.alarm.vision.IAnalyzer
import com.better.alarm.vision.OverlayView
import kotlinx.serialization.json.Json
import org.koin.android.ext.android.inject
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraTestActivity : AppCompatActivity() {
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
      val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
      ) { granted ->
        if (!granted) {
          finish()
        }
      }
      requestPermissionLauncher.launch(Manifest.permission.CAMERA)
    }
    cameraExecutor = Executors.newSingleThreadExecutor()

    cameraExecutor!!.execute {
      analyzer = IAnalyzer(this, detectionHandler, Json.decodeFromString(sp.visionBehavior.value), sp.visionModelSelect.value)
      analyzer!!.skipPeek()
      cameraXHelper = CameraXHelper(this, this,findViewById(R.id.alert_vision_preview) , cameraExecutor!!, lensFacing = CameraSelector.LENS_FACING_BACK, imageAnalyzer =  analyzer!!.analyzer) { succeeded ->
        if (!succeeded) {
          logger.error {"CameraXHelper failed to start"}
        }
      }
      analyzer!!.lensFacingObservable = cameraXHelper!!.lensFacingObservable
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
    findViewById<Button>(R.id.switch_camera).run {
      setOnClickListener {
        cameraXHelper?.switchCamera()
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
