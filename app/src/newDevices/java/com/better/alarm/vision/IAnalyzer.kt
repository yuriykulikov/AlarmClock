package com.better.alarm.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageAnalysis
import com.better.alarm.bootstrap.globalLogger
import com.better.alarm.vision.BoundingBox
import com.better.alarm.vision.DetectionAnalyzer
import com.better.alarm.vision.DetectionHandler
import com.better.alarm.vision.DetectionState
import com.better.alarm.vision.DetectorV10
import com.better.alarm.vision.Gesture
import com.better.alarm.vision.Labels
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.components.containers.Landmark
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.OutputHandler.ResultListener
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizer
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizerResult
import kotlin.math.sqrt

data class YMGesture (
  override val name: String,
  val fingers: FingersState?
): Gesture {
  data class FingersState (val thumb: Int, val index: Int, val middle: Int, val ring: Int, val little: Int)
}

fun List<Int>.toFingersState(): YMGesture.FingersState {
  return YMGesture.FingersState(this[0], this[1], this[2], this[3], this[4])
}

fun getCos(v1: FloatArray, v2: FloatArray): Float {
  return (v1[0]*v2[0] + v1[1]*v2[1] + v1[2]*v2[2]) / sqrt(v1[0]*v1[0] + v1[1]*v1[1] + v1[2]*v1[2]) / sqrt(v2[0]*v2[0] + v2[1]*v2[1] + v2[2]*v2[2])
}

fun List<NormalizedLandmark>.toBoundingBox(name: String, w: Int, h: Int): BoundingBox {
  val minX = minOf { it.x() }
  val maxX = maxOf{ it.x() }
  val minY = minOf { it.y() }
  val maxY = maxOf{ it.y() }
  return BoundingBox(
    minX, minY, maxX, maxY, (minX+maxX)/2F, (minY+maxY)/2F, maxX-minX, maxY-minY, 1F, 0, name
  )
}

class IAnalyzer (
  private val context: Context,
  private val handler: DetectionHandler
): DetectionAnalyzer {
  private val MAX_NOPERSON_TIME = 10000
  private val INITIAL_MAX_TIME = 6000
  private var peekStartTime = 0L
  private var lastPersonDetectionTime = 0L
  private val logger by globalLogger("DetectionHandler")
  private val HEAD_MODEL_PATH = "head_n_float16.tflite"
  private val GESTURE_MODEL_PATH = "gesture_recognizer.task"
  private var headDetectorV10: DetectorV10? = null
  private var gestureRecognizer: GestureRecognizer? = null
  private val gestureBoundingBox = mutableListOf<BoundingBox>()
  private var state = DetectionState.PEEKING

  companion object {
    val GESTURE_NONE = YMGesture("NONE", null)
    val GESTURE_O = YMGesture("zero", YMGesture.FingersState(-1, -1, -1, -1, -1))
    val GESTURE_1 = YMGesture("one", YMGesture.FingersState(-1, 1, -1, -1, -1))
    val GESTURE_2 = YMGesture("two", YMGesture.FingersState(-1, 1, 1, -1, -1))
    val GESTURE_3 = YMGesture("three", YMGesture.FingersState(-1, 1, 1, 1, -1))
    val GESTURE_4 = YMGesture("four", YMGesture.FingersState(-1, 1, 1, 1, 1))
    val GESTURE_5 = YMGesture("five", YMGesture.FingersState(1, 1, 1, 1, 1))
    val GESTURE_MID = YMGesture("middle finger", YMGesture.FingersState(-1,-1,1,-1,-1))
    val GESTURE_LITTLE = YMGesture("little finger", YMGesture.FingersState(-1,-1,-1,-1,1))
    val GESTURE_CALL = YMGesture("call", YMGesture.FingersState(1,-1,-1,-1,1))
    val GESTURE_ROCK = YMGesture("rock", YMGesture.FingersState(-1,1,-1,-1,1))
    val GESTURE_THUMBS_UP = YMGesture("thumbs up", YMGesture.FingersState(1,-1,-1,-1,-1))
    val GESTURE_GUN = YMGesture("gun", YMGesture.FingersState(1,1,-1,-1,-1))
    val GESTURE_LIST = listOf(GESTURE_NONE, GESTURE_O, GESTURE_1, GESTURE_2, GESTURE_3, GESTURE_4, GESTURE_5, GESTURE_MID, GESTURE_LITTLE, GESTURE_CALL, GESTURE_ROCK, GESTURE_THUMBS_UP, GESTURE_GUN)
  }

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
    if (state == DetectionState.WAKING) {
      val mpImage = BitmapImageBuilder(rotatedBitmap).build()
      gestureRecognizer?.recognizeAsync(mpImage, System.currentTimeMillis())
    }
    headDetectorV10?.detect(rotatedBitmap)
  }

  init {
    headDetectorV10 = DetectorV10(context, HEAD_MODEL_PATH, HeadDetectorListener(), {
        logger.debug { it }
    })
    val gestureRecognizerOption = GestureRecognizer.GestureRecognizerOptions.builder().apply {
      setBaseOptions(BaseOptions.builder().setModelAssetPath(GESTURE_MODEL_PATH).build())
      setRunningMode(RunningMode.LIVE_STREAM)
      setResultListener(GestureDetectorListener())
    }.build()
    gestureRecognizer = GestureRecognizer.createFromOptions(context, gestureRecognizerOption)
    logger.debug { "init YOLOMediaPipeDetectionHandler" }
  }

  inner class HeadDetectorListener : DetectorV10.DetectorListener {
    private var hasPersonLeft = false
    private fun peekStateOnDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
      if (peekStartTime == 0L) {
        peekStartTime = System.currentTimeMillis()
      }
      if (boundingBoxes.any { it.clsName == Labels.HEAD }) {
        state = DetectionState.WAKING
        lastPersonDetectionTime = System.currentTimeMillis()
        handler.onPeekFinish(true)
      } else {
        if (System.currentTimeMillis() - peekStartTime > INITIAL_MAX_TIME) {
          state = DetectionState.FINISHED
          handler.onPeekFinish(false)
        }
      }
    }

    private fun wakeStateOnDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
      if (hasPersonLeft) return
      if (boundingBoxes.any { it.clsName == Labels.HEAD }) {
        lastPersonDetectionTime = System.currentTimeMillis()
      }
      if (System.currentTimeMillis() - lastPersonDetectionTime > MAX_NOPERSON_TIME) {
        hasPersonLeft = true
        handler.onPersonLeave()
      }
    }
    override fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
      when (state) {
        DetectionState.PEEKING -> peekStateOnDetect(boundingBoxes, inferenceTime)
        DetectionState.WAKING -> wakeStateOnDetect(boundingBoxes, inferenceTime)
        DetectionState.FINISHED -> return
      }
      val boundingBoxesThisFrame = mutableListOf<BoundingBox>()
      boundingBoxesThisFrame.addAll(boundingBoxes)
      boundingBoxesThisFrame.addAll(gestureBoundingBox)
      handler.onDetectUiUpdate(boundingBoxes, inferenceTime)
    }
  }

  inner class GestureDetectorListener : ResultListener<GestureRecognizerResult, MPImage>{
    private val ACCEPT_CONFIDENCE = 5
    private val gestureConfidenceMap = buildMap<String, Int> {
      for (gesture in GESTURE_LIST)
        put(gesture.name, 0)
    }.toMutableMap()

    private fun onDetect(boundingBoxes: List<BoundingBox>) {
      gestureConfidenceMap.forEach { (name, confidence) ->
        if (boundingBoxes.any { it.clsName == name })
          gestureConfidenceMap[name] = confidence + 1
        else if (confidence > 0)
          gestureConfidenceMap[name] = confidence - 1
        if (gestureConfidenceMap[name]!! > ACCEPT_CONFIDENCE){
          gestureConfidenceMap[name] = 0
          handler.onGestureDetected(name)
        }
      }
      gestureBoundingBox.clear()
      gestureBoundingBox.addAll(boundingBoxes)
    }

    private fun transform2YMGesture(worldLandmarks: List<Landmark>): YMGesture {
      val fingersState = buildList<Int> {
        // thumb
        var v1 = floatArrayOf(worldLandmarks[0].x() - worldLandmarks[1].x(), worldLandmarks[0].y() - worldLandmarks[1].y(), worldLandmarks[0].z() - worldLandmarks[1].z())
        var v2 = floatArrayOf(worldLandmarks[3].x() - worldLandmarks[4].x(), worldLandmarks[3].y() - worldLandmarks[4].y(), worldLandmarks[3].z() - worldLandmarks[4].z())
        var cosValue = getCos(v1, v2)
        if (cosValue > 0.5) add(1)
        else add(-1)
        // index, middle, ring, little
        for (i in listOf(5,9,13,17)) {
          v1 = floatArrayOf(worldLandmarks[0].x() - worldLandmarks[i+1].x(), worldLandmarks[0].y() - worldLandmarks[i+1].y(), worldLandmarks[0].z() - worldLandmarks[i+1].z())
          v2 = floatArrayOf(worldLandmarks[i+1].x() - worldLandmarks[i+3].x(), worldLandmarks[i+1].y() - worldLandmarks[i+3].y(), worldLandmarks[i+1].z() - worldLandmarks[i+3].z())
          cosValue = getCos(v1, v2)
          if (cosValue > 0.2) add(1)
          else if (cosValue < -0.2) add(-1)
          else add(0)
        }
      }.toFingersState()
      logger.debug { fingersState.toString() }
      return GESTURE_LIST.find { it.fingers == fingersState }?: GESTURE_NONE
    }

    override fun run(result: GestureRecognizerResult?, input: MPImage?) {
      if (result == null || input == null) return
      val boundingBoxes = mutableListOf<BoundingBox>()
      for (i in 0 until result.worldLandmarks().size) {
        val resultGesture = transform2YMGesture(result.worldLandmarks()[i])
        if (resultGesture != GESTURE_NONE) {
          boundingBoxes.add(result.landmarks()[i].toBoundingBox(resultGesture.name, input.width, input.height))
        }
      }
      onDetect(boundingBoxes)
    }
  }

  override fun destroy() {
    headDetectorV10?.close()
    headDetectorV10 = null
    gestureRecognizer?.close()
    gestureRecognizer = null
  }
}
