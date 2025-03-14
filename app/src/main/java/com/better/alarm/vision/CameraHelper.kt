package com.better.alarm.vision

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.core.ImageCapture.FLASH_MODE_ON
import androidx.camera.core.ImageCapture.FlashMode
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.text.format

class CameraXHelper(
  private val context: Context,
  private val lifecycleOwner: LifecycleOwner,
  private val previewView: androidx.camera.view.PreviewView
) {
  private var cameraProvider: ProcessCameraProvider? = null
  private var imageCapture: ImageCapture? = null
  private var camera: Camera? = null
  private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
  private var lensFacing = CameraSelector.LENS_FACING_BACK

  init {
    startCamera()
  }

  private fun startCamera() {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

    cameraProviderFuture.addListener({
      cameraProvider = cameraProviderFuture.get()

      val preview = Preview.Builder().build().also {
        it.setSurfaceProvider(previewView.surfaceProvider)
      }

      imageCapture = ImageCapture.Builder().build()

      val cameraSelector = CameraSelector.Builder()
        .requireLensFacing(lensFacing)
        .build()

      cameraProvider?.unbindAll()
      camera = cameraProvider?.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture)

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
          Log.e("CameraXHelper", "拍照失敗: ${exception.message}", exception)
        }
      })
  }

  fun toggleFlash() {
    imageCapture?.flashMode = if (imageCapture?.flashMode == ImageCapture.FLASH_MODE_ON)
      ImageCapture.FLASH_MODE_OFF else ImageCapture.FLASH_MODE_ON
    showToast("閃光燈: ${if (imageCapture?.flashMode == ImageCapture.FLASH_MODE_ON) "開啟" else "關閉"}")
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

  fun shutdown() {
    cameraExecutor.shutdown()
  }
}
