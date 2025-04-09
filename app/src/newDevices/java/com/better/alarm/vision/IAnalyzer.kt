package com.better.alarm.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageAnalysis
import com.better.alarm.bootstrap.globalLogger
import com.better.alarm.ui.settings.VisionBehaviorItemView
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.components.containers.Landmark
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.OutputHandler.ResultListener
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizer
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizerResult
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import kotlin.math.sqrt

fun getCos(v1: FloatArray, v2: FloatArray): Float {
  return (v1[0]*v2[0] + v1[1]*v2[1] + v1[2]*v2[2]) /
    (sqrt(v1[0]*v1[0] + v1[1]*v1[1] + v1[2]*v1[2]) * sqrt(v2[0]*v2[0] + v2[1]*v2[1] + v2[2]*v2[2]))
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
  private val handler: DetectionHandler,
  private val behaviors: VisionBehaviorItemView.Companion.BehaviorsStoreValue
): DetectionAnalyzer {
  private val MAX_NOPERSON_TIME = 20000
  private val INITIAL_MAX_TIME = 6000
  private var peekStartTime = 0L
  private var lastPersonDetectionTime = 0L
  private val logger by globalLogger("DetectionHandler")
  private val HEAD_MODEL_PATH = "head_2_float32.tflite"
  private val GESTURE_MODEL_PATH = "gesture_recognizer.task"
  private var headDetectorV10: DetectorV10? = null
  private var gestureRecognizer: GestureRecognizer? = null
  private val gestureBoundingBox: Subject<List<BoundingBox>> = BehaviorSubject.create()
  private val headBoundingBox: Subject<List<BoundingBox>> = BehaviorSubject.create()
  private val compositeDisposable = CompositeDisposable()
  private var state = DetectionState.PEEKING
  private var stopped = false

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
    bitmapBuffer.recycle()
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

    val boundingBoxesObservable = Observable.zip(headBoundingBox, gestureBoundingBox) { head, gesture -> head + gesture }
    val disposable = boundingBoxesObservable.subscribe {
      if (!stopped)
        handler.onDetectUiUpdate(it, 0)
    }
    compositeDisposable.add(disposable)
  }

  override fun stop() {
    stopped = true
  }

  override fun skipPeek() {
    state = DetectionState.WAKING
  }

  inner class HeadDetectorListener : DetectorV10.DetectorListener {
    private fun peekStateOnDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
      if (peekStartTime == 0L) {
        peekStartTime = System.currentTimeMillis()
      }
      if (boundingBoxes.any { it.clsName == Labels.HEAD }) {
        state = DetectionState.WAKING
        lastPersonDetectionTime = System.currentTimeMillis()
        if (!stopped)
          handler.onPeekFinish(true)
      } else {
        if (System.currentTimeMillis() - peekStartTime > INITIAL_MAX_TIME) {
          state = DetectionState.WAKING
          if (!stopped)
            handler.onPeekFinish(false)
        }
      }
    }

    private fun wakeStateOnDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
      if (boundingBoxes.any { it.clsName == Labels.HEAD }) {
        lastPersonDetectionTime = System.currentTimeMillis()
      }
      if (System.currentTimeMillis() - lastPersonDetectionTime > MAX_NOPERSON_TIME && lastPersonDetectionTime != -1L) {
        if (!stopped)
          handler.onPersonLeave()
        lastPersonDetectionTime = -1L
      }
    }
    override fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
      when (state) {
        DetectionState.PEEKING -> peekStateOnDetect(boundingBoxes, inferenceTime)
        DetectionState.WAKING -> wakeStateOnDetect(boundingBoxes, inferenceTime)
      }
      headBoundingBox.onNext(boundingBoxes)
    }
  }

  inner class GestureDetectorListener : ResultListener<GestureRecognizerResult, MPImage>{
    private val ACCEPT_CONFIDENCE = 3
    private val behaviorConfidenceMap = buildMap {
      behaviors.items.forEach {
        put(it, 0)
      }
    }.toMutableMap()

    private fun onDetect(gestures: List<Gesture>) {
      behaviorConfidenceMap.forEach { (behavior, confidence) ->
        if (gestures.any { it == behavior.gesture})
          behaviorConfidenceMap[behavior] = confidence + 1
        else if (confidence > 0)
          behaviorConfidenceMap[behavior] = confidence - 1
        if (behaviorConfidenceMap[behavior]!! > ACCEPT_CONFIDENCE){
          behaviorConfidenceMap[behavior] = 0
          if (!stopped)
            handler.onBehaviorAction(behavior.operation)
        }
      }
    }

    private fun transform2YMGesture(worldLandmarks: List<Landmark>): Gesture {
      val fingersState = buildList {
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
          if (cosValue > 0.1) add(1)
          else if (cosValue < -0.1) add(-1)
          else add(0)
        }
      }.toFingersState()
      return Gesture(fingersState)
    }

    override fun run(result: GestureRecognizerResult?, input: MPImage?) {
      if (result == null || input == null) return
      val boundingBoxes = mutableListOf<BoundingBox>()
      val gestures = mutableListOf<Gesture>()
      for (i in 0 until result.worldLandmarks().size) {
        val resultGesture = transform2YMGesture(result.worldLandmarks()[i])
        val name = behaviors.items.find {
          it.gesture == resultGesture
        }?.operation?: resultGesture.fingers?.toFingersList().toString()
        boundingBoxes.add(result.landmarks()[i].toBoundingBox(name, input.width, input.height))
        gestures.add(resultGesture)
      }
      onDetect(gestures)
      gestureBoundingBox.onNext(boundingBoxes)
    }
  }

  override fun destroy() {
    headDetectorV10?.close()
    headDetectorV10 = null
    gestureRecognizer?.close()
    gestureRecognizer = null
    compositeDisposable.dispose()
  }
}
