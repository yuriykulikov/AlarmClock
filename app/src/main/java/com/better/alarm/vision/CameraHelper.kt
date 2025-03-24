package com.better.alarm.vision

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
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
  private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor(),
  private val imageAnalyzer: ImageAnalysis.Analyzer? = null,
) {
  private val logger by globalLogger("CameraXHelper")
  private var cameraProvider: ProcessCameraProvider? = null
  private var imageCapture: ImageCapture? = null
  private var imageAnalysis: ImageAnalysis? = null
  private var camera: Camera? = null
  private var lensFacing = CameraSelector.LENS_FACING_BACK
  private var isTorchOn = false
  private var cameraResolution: Size = Size(640, 480)
  private var _cameraIsOpened: Boolean = false
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

        val preview = Preview.Builder()
          .setTargetResolution(cameraResolution)
          .build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
          }

        imageCapture = ImageCapture.Builder()
          .setTargetResolution(cameraResolution)
          .build()

        val cameraSelector = CameraSelector.Builder()
          .requireLensFacing(lensFacing)
          .build()

        if (imageAnalyzer != null)
          imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(cameraResolution)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build().also{
              it.setAnalyzer(cameraExecutor, imageAnalyzer)
            }
        cameraProvider?.unbindAll()
        camera = cameraProvider?.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture, imageAnalysis)
        if (camera == null) {
          logger.error { "Use case binding failed: (camera == null)" }
          _cameraIsOpened = false
        } else {
          _cameraIsOpened = true
        }
      } catch (e: Exception) {
        logger.error { "Use case binding failed: $e" }
        _cameraIsOpened = false
      }

      camera?.cameraInfo?.torchState?.observe(lifecycleOwner) {
        state -> isTorchOn = state == TorchState.ON
      }
      if (camera!=null) {
        _cameraIsOpened = true
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
    cameraExecutor.shutdown()
    cameraProvider?.unbindAll()
  }
}
