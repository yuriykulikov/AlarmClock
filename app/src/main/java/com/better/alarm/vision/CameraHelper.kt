package com.better.alarm.vision

import android.content.ContentValues
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Size
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.core.ImageCapture.FLASH_MODE_ON
import androidx.camera.core.ImageCapture.FlashMode
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.better.alarm.bootstrap.globalLogger
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.text.format

class CameraXHelper(
  private val context: Context,
  private val lifecycleOwner: LifecycleOwner,
  private val previewView: androidx.camera.view.PreviewView,
  private val cameraExecutor: ExecutorService,
  private var lensFacing: Int = CameraSelector.LENS_FACING_BACK,
  private val imageAnalyzer: ImageAnalysis.Analyzer? = null,
  private val cameraOpenedCB: (Boolean) -> Unit
) {
  private val logger by globalLogger("CameraXHelper")
  private var cameraProvider: ProcessCameraProvider? = null
  private var imageCapture: ImageCapture? = null
  private var imageAnalysis: ImageAnalysis? = null
  private var camera: Camera? = null
  private var isTorchOn = false
  private var cameraResolution: Size = Size(640, 480)
  private var _cameraIsOpened: Boolean = false
  private var cameraOpenedCBCalled = false
  val cameraIsOpened: Boolean
    get() = _cameraIsOpened

  init {

    startCamera()
  }

  private fun startCamera() {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

    cameraProviderFuture.addListener({
      try {
        cameraProvider = cameraProviderFuture.get()

        val cameraSelector = CameraSelector.Builder()
          .requireLensFacing(lensFacing)
          .build()

        val preview = Preview.Builder()
          .setTargetResolution(Size(640, 640))
          .build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
          }

        if (imageAnalyzer != null)
          imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(640, 640))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build().also{
              it.setAnalyzer(cameraExecutor, imageAnalyzer)
            }
        cameraProvider?.unbindAll()
        camera =
          if (imageAnalysis != null) cameraProvider?.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis)
          else cameraProvider?.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
        if (camera == null) {
          logger.error { "Use case binding failed: (camera == null)" }
          _cameraIsOpened = false
        } else {
          _cameraIsOpened = true
        }
      } catch (e: Exception) {
        logger.error { "Use case binding failed: $e" }
        _cameraIsOpened = false
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
        val mainCameraId = cameraManager?.cameraIdList?.maxByOrNull { cameraId ->
          val characteristics = cameraManager.getCameraCharacteristics(cameraId)
          val sensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)!!
          (sensorSize.width * sensorSize.height).toFloat()
        }?.takeIf { characteristics ->
          cameraManager.getCameraCharacteristics(characteristics).get(CameraCharacteristics.LENS_FACING) == lensFacing
        }?: cameraManager?.cameraIdList?.firstOrNull()
        cameraManager?.registerAvailabilityCallback(
          object : CameraManager.AvailabilityCallback() {
            override fun onCameraAvailable(cameraId: String) {
              super.onCameraAvailable(cameraId)
              logger.debug{"onCameraAvailable: $cameraId"}
              if (cameraId == mainCameraId)
                startCamera()
            }
          }, Handler(Looper.getMainLooper())
        )
      }

      camera?.cameraInfo?.torchState?.observe(lifecycleOwner) {
        state -> isTorchOn = state == TorchState.ON
      }
      if (camera!=null) {
        _cameraIsOpened = true
        cameraOpenedCB(_cameraIsOpened)
        cameraOpenedCBCalled = true
      } else {
        Handler(Looper.getMainLooper()).postDelayed({
          if (!cameraOpenedCBCalled) {
            cameraOpenedCB(_cameraIsOpened)
            cameraOpenedCBCalled = true
          }
        },5000)
      }
    }, ContextCompat.getMainExecutor(context))
  }

  fun takePhoto(flashed: Boolean=false, saveToGallery: Boolean=false) {

    val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
      .format(System.currentTimeMillis()) + ".jpg"

    val contentValues = ContentValues().apply {
      put(MediaStore.MediaColumns.DISPLAY_NAME, name)
      put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
      if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/AlarmClock")
      }
    }

    val outputOptions = ImageCapture.OutputFileOptions
      .Builder(
        context.contentResolver,
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        contentValues
      )
      .build()

    if (flashed) {
      imageCapture?.flashMode = FLASH_MODE_ON
    }

    imageCapture?.takePicture(
      outputOptions,
      cameraExecutor,
      object : ImageCapture.OnImageSavedCallback {
        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
        }

        override fun onError(exception: ImageCaptureException) {
          logger.error {  "拍照失敗: ${exception.message}" }
        }
      })
  }

  fun setOrToggleFlash(targetState: Boolean? = null) {
    if (targetState != null && targetState != isTorchOn) {
      camera?.cameraControl?.enableTorch(targetState)
    }
    else {
      camera?.cameraControl?.enableTorch(!isTorchOn)
    }
  }

  fun switchCamera() {
    lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK)
      CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK

    startCamera() // 重新啟動相機
  }

  fun adjustExposure(evValue: Int) {
    val exposureState = camera?.cameraInfo?.exposureState
    val evRange = exposureState?.exposureCompensationRange?:android.util.Range(0, 0)
    val newEV = evValue.coerceIn(evRange.lower, evRange.upper)

    camera?.cameraControl?.setExposureCompensationIndex(newEV)
      ?.addListener({
        showToast("曝光值設定: $newEV")
      }, ContextCompat.getMainExecutor(context))
  }

  private fun showToast(message: String) {
    //Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
  }

  fun destroy() {
    cameraProvider?.unbindAll()
  }
}
