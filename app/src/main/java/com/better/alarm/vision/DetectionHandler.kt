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
  fun onPeekFinish(isPersonDetected: Boolean)
  fun onPersonLeave()
  fun onDetectUiUpdate(boundingBoxes: List<BoundingBox>, inferenceTime: Long)
  fun onGestureDetected(gesture: String)
  fun destroy()

  val analyzer: ImageAnalysis.Analyzer
}

abstract class DualModelDetectionHandler (
  private val context: Context
) : DetectionHandler {
  private val HEAD_MODEL_PATH = "head_n_float16.tflite"
  private val GESTURE_MODEL_PATH = "YOLOv10n_gestures_float16.tflite"
  private val MAX_NOPERSON_TIME = 5000
  private val GESTURE_CONFRIM_TIME = 1000
  private val INITIAL_MAX_TIME = 6000
  private var state = DetectionState.PEEKING
  private var peekStartTime = 0L
  private var lastPersonDetectionTime = 0L
  private val logger by globalLogger("DetectionHandler")
  private var headDetectorV10: DetectorV10? = null
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
    boundingBoxesThisFrame.clear()
    if (state == DetectionState.WAKING) {
      gestureDetector?.detect(rotatedBitmap)
    }
    headDetectorV10?.detect(rotatedBitmap)
  }

  init {
    headDetectorV10 = DetectorV10(context, HEAD_MODEL_PATH, HeadDetectorListener(), {
      logger.debug { it }
    })
    gestureDetector = DetectorV10(context, GESTURE_MODEL_PATH, GestureDetectorListener(), {
      logger.debug { it }
    })
  }

  inner class HeadDetectorListener : DetectorV10.DetectorListener {
    private fun peekStateOnDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
      if (peekStartTime == 0L) {
        peekStartTime = System.currentTimeMillis()
      }
      if (boundingBoxes.any { it.clsName == Labels.HEAD }) {
        state = DetectionState.WAKING
        lastPersonDetectionTime = System.currentTimeMillis()
        onPeekFinish(true)
      } else {
        if (System.currentTimeMillis() - peekStartTime > INITIAL_MAX_TIME) {
          onPeekFinish(false)
        }
      }
    }

    private fun wakeStateOnDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
      if (boundingBoxes.any { it.clsName == Labels.HEAD }) {
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
    private val firstGestureTime = buildMap<String, Long> {
      for (gesture in Labels.GestureLabelList)
        put(gesture, 0)
    }.toMutableMap()

    override fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
      for (gesture in Labels.GestureLabelList) {
        if (!boundingBoxes.any { it.clsName == gesture }) {
          firstGestureTime[gesture] = 0
        } else if (firstGestureTime[gesture] == 0L) {
          firstGestureTime[gesture] = System.currentTimeMillis()
        } else if (System.currentTimeMillis() - firstGestureTime[gesture]!! > GESTURE_CONFRIM_TIME) {
          onGestureDetected(gesture)
        }
      }
      boundingBoxesThisFrame.addAll(boundingBoxes)
    }
  }

  override fun destroy() {
    headDetectorV10?.close()
    gestureDetector?.close()
    headDetectorV10 = null
    gestureDetector = null
  }
}

abstract class SingleModelDetectionHandler (
  private val context: Context,
) : DetectorV9.DetectorListener, DetectionHandler {
  private val MODEL_PATH = "alarm_v1.16.tflite"
  private val MAX_NOPERSON_TIME = 5000
  private val GESTURE_CONFRIM_TIME = 1000
  private var initialCountDown = 30000
  private var lastPersonDetectionTime = 0L
  private var state = DetectionState.PEEKING
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

  private val firstGestureTime = buildMap<String, Long> {
    for (gesture in Labels.GestureLabelList)
      put(gesture, 0)
  }.toMutableMap()

  init {
    detectorV9 = DetectorV9(context, MODEL_PATH, this) {
      logger.debug { it }
    }
  }

  private fun peekStateOnDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
    if (boundingBoxes.any { it.clsName == Labels.HEAD }) {
      state = DetectionState.WAKING
      lastPersonDetectionTime = System.currentTimeMillis()
      onPeekFinish(true)
    } else {
      initialCountDown--
      if (initialCountDown == 0) {
        onPeekFinish(false)
      }
    }
  }

  private fun wakeStateOnDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
    if (boundingBoxes.any { it.clsName == Labels.HEAD }) {
      lastPersonDetectionTime = System.currentTimeMillis()
    }
    if (System.currentTimeMillis() - lastPersonDetectionTime > MAX_NOPERSON_TIME) {
      onPersonLeave()
    }
    for (gesture in Labels.GestureLabelList) {
      if (!boundingBoxes.any { it.clsName == gesture }) {
        firstGestureTime[gesture] = 0
      } else if (firstGestureTime[gesture] == 0L) {
        firstGestureTime[gesture] = System.currentTimeMillis()
      } else if (System.currentTimeMillis() - firstGestureTime[gesture]!! > GESTURE_CONFRIM_TIME) {
        onGestureDetected(gesture)
      }
    }
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
