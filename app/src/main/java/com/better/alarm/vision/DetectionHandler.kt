package com.better.alarm.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageAnalysis
import com.better.alarm.bootstrap.globalLogger

enum class DetectionState {
  PEEKING, WAKING
}

interface DetectionHandler{
  fun onPeekNoPerson()
  fun onPersonLeave()
  fun onDetectUiUpdate(boundingBoxes: List<BoundingBox>, inferenceTime: Long)
  fun destroy()

  val analyzer: ImageAnalysis.Analyzer
}

abstract class DualModelDetectionHandler (
  private val actions: MutableList<DetectionAction>,
  private val context: Context
) : DetectionHandler {
  private val HEAD_MODEL_PATH = "head_26_float16.tflite"
  private val GESTURE_MODEL_PATH = "YOLOv10n_gestures_float16.tflite"
  private val MAX_NOPERSON_TIME = 5000
  private var state = DetectionState.PEEKING
  private var initialCountDown = 30000
  private var lastPersonDetectionTime = 0L
  private val logger by globalLogger("DetectionHandler")
  private var headDetectorV9: DetectorV9? = null
  private var gestureDetector: DetectorV10? = null
  private val boundingBoxesThisFrame = mutableListOf<BoundingBox>()

  override val analyzer: ImageAnalysis.Analyzer = ImageAnalysis.Analyzer {
    imageProxy ->
    val bitmapBuffer =
      Bitmap.createBitmap(
        imageProxy.width,
        imageProxy.height,
        Bitmap.Config.ARGB_8888
      )
    imageProxy.use { bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer) }
    imageProxy.close()

    val matrix = Matrix().apply {
      postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
    }

    val rotatedBitmap = Bitmap.createBitmap(
      bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height,
      matrix, true
    )
    logger.debug { "analyze ${rotatedBitmap.width}x${rotatedBitmap.height}" }
    boundingBoxesThisFrame.clear()
    if (state == DetectionState.WAKING) {
      gestureDetector?.detect(rotatedBitmap)
    }
    headDetectorV9?.detect(rotatedBitmap)
  }

  init {
    headDetectorV9 = DetectorV9(context, HEAD_MODEL_PATH, HeadDetectorListener(), {
      logger.debug { it }
    })
    gestureDetector = DetectorV10(context, GESTURE_MODEL_PATH, GestureDetectorListener(), {
      logger.debug { it }
    })
  }

  inner class HeadDetectorListener : DetectorV9.DetectorListener {
    private fun peekStateOnDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
      if (boundingBoxes.any { it.clsName == "person" }) {
        state = DetectionState.WAKING
        lastPersonDetectionTime = System.currentTimeMillis()
      } else {
        if (--initialCountDown== 0) {
          onPeekNoPerson()
        }
      }
    }

    private fun wakeStateOnDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
      if (boundingBoxes.any { it.clsName == "person" }) {
        lastPersonDetectionTime = System.currentTimeMillis()
      }
      if (System.currentTimeMillis() - lastPersonDetectionTime > MAX_NOPERSON_TIME) {
        onPersonLeave()
      }
    }
    override fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
      when (state) {
        DetectionState.PEEKING -> peekStateOnDetect(boundingBoxes, inferenceTime)
        DetectionState.WAKING -> wakeStateOnDetect(boundingBoxes, inferenceTime)
      }
      boundingBoxesThisFrame.addAll(boundingBoxes)
      onDetectUiUpdate(boundingBoxesThisFrame, inferenceTime)
    }
  }

  inner class GestureDetectorListener : DetectorV10.DetectorListener {
    override fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
      actions.forEach { it.checkDetectionState(boundingBoxes, inferenceTime) }
      boundingBoxesThisFrame.addAll(boundingBoxes)
    }
  }

  override fun destroy() {
    headDetectorV9?.close()
    gestureDetector?.close()
  }
}

abstract class SingleModelDetectionHandler (
  private val context: Context,
  private val actions: MutableList<DetectionAction>,
) : DetectorV9.DetectorListener, DetectionHandler {
  private val MODEL_PATH = "alarm_v1.16.tflite"
  private val MAX_NOPERSON_TIME = 5000
  private var state = DetectionState.PEEKING
  private var initialCountDown = 30000
  private var lastPersonDetectionTime = 0L
  private val logger by globalLogger("DetectionHandler")
  private var detectorV9: DetectorV9? = null
  override val analyzer: ImageAnalysis.Analyzer = ImageAnalysis.Analyzer {
    imageProxy ->
    val bitmapBuffer =
      Bitmap.createBitmap(
        imageProxy.width,
        imageProxy.height,
        Bitmap.Config.ARGB_8888
      )
    imageProxy.use { bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer) }
    imageProxy.close()

    val matrix = Matrix().apply {
      postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
    }

    val rotatedBitmap = Bitmap.createBitmap(
      bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height,
      matrix, true
    )
    logger.debug { "analyze ${rotatedBitmap.width}x${rotatedBitmap.height}" }
    detectorV9?.detect(rotatedBitmap)
  }

  init {
    detectorV9 = DetectorV9(context, MODEL_PATH, this) {
      logger.debug { it }
    }
  }

  private fun peekStateOnDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
    if (boundingBoxes.any { it.clsName == Constants.LabelList[Constants.HEAD] }) {
      state = DetectionState.WAKING
      lastPersonDetectionTime = System.currentTimeMillis()
    } else {
      initialCountDown--
      if (initialCountDown == 0) {
        onPeekNoPerson()
      }
    }
  }

  private fun wakeStateOnDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
    if (boundingBoxes.any { it.clsName == Constants.LabelList[Constants.HEAD] }) {
      lastPersonDetectionTime = System.currentTimeMillis()
    }
    if (System.currentTimeMillis() - lastPersonDetectionTime > MAX_NOPERSON_TIME) {
      onPersonLeave()
    }
    actions.forEach { it.checkDetectionState(boundingBoxes, inferenceTime) }
  }

  override fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
    logger.debug { "detected ${boundingBoxes.size} objects in $inferenceTime ms" }
    onDetectUiUpdate(boundingBoxes, inferenceTime)

    when (state) {
      DetectionState.PEEKING -> peekStateOnDetect(boundingBoxes, inferenceTime)
      DetectionState.WAKING -> wakeStateOnDetect(boundingBoxes, inferenceTime)
    }
  }

  override fun destroy() {
    detectorV9?.close()
  }
}


interface DetectionAction {
  fun checkDetectionState(boundingBoxes: List<BoundingBox>, inferenceTime: Long)

  fun startAction()
}

abstract class DetectedAction(val triggerGesture: String) : DetectionAction {
  private val ACTION_CONFIRMING_TIME = 2000
  private var firstSignalTime: Long = 0
  override fun checkDetectionState(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
    if (boundingBoxes.any { it.clsName == triggerGesture }) {
      if (firstSignalTime == 0L) {
        firstSignalTime = System.currentTimeMillis()
      } else if (System.currentTimeMillis() - firstSignalTime > ACTION_CONFIRMING_TIME) {
        startAction()
      }
    } else {
      firstSignalTime = 0L
    }
  }
  abstract override fun startAction()
}

