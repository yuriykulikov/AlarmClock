package com.better.alarm.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import com.better.alarm.bootstrap.globalLogger
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.components.containers.Landmark
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.core.OutputHandler.ResultListener
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker.HandLandmarkerOptions
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.sqrt

fun getCos(v1: FloatArray, v2: FloatArray): Float {
  return (v1[0]*v2[0] + v1[1]*v2[1] + v1[2]*v2[2]) /
    (sqrt(v1[0]*v1[0] + v1[1]*v1[1] + v1[2]*v1[2]) * sqrt(v2[0]*v2[0] + v2[1]*v2[1] + v2[2]*v2[2]))
}

fun List<NormalizedLandmark>.toBoundingBox(name: String): BoundingBox {
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
  private val behaviors: Behavior.BehaviorsStoreValue,
  private val modelPath: String
): DetectionAnalyzer {
  private val logger by globalLogger("DetectionHandler")
  private var headDetectorV10: DetectorV10? = null
  private var gestureRecognizer: HandLandmarker? = null
  private val gestureRecognizerOperatingLock = Any()
  private val gestureBoundingBox: Subject<List<BoundingBox>> = BehaviorSubject.createDefault(listOf())
  private val headBoundingBox: Subject<List<BoundingBox>> = BehaviorSubject.createDefault(listOf())
  private val compositeDisposable = CompositeDisposable()
  private var state = DetectionState.PEEKING
  private var handlerOperationLock = Any()
  private val personWatcher = PersonWatcher()
  private val headDetectorExecutor = Executors.newSingleThreadExecutor()
  private val headDetectorExecutorIsBusy = AtomicBoolean(false)
  private var lensFacing = CameraSelector.LENS_FACING_BACK
  override var lensFacingObservable: Observable<Int>? = null
    set(value) {
      field = value
      compositeDisposable.add(value!!.subscribe {
        lensFacing = it
      })
    }

  companion object {
    const val GESTURE_MODEL_PATH = "hand_landmarker.task"
    const val MAX_NO_HEAD_TIME = 5000L
    const val MAX_NO_HAND_Time = 10000L

    const val STATUS_DETECTED = 1
    const val STATUS_NOT_DETECTED = 0
    const val STATUS_UNKNOWN = -1

    fun checkAvailability(context: Context): Boolean {
      try {
        val gestureRecognizerOption = HandLandmarkerOptions.builder().apply {
          setBaseOptions(BaseOptions.builder().setModelAssetPath(GESTURE_MODEL_PATH).build())
          setResultListener { _, _ -> }
          setRunningMode(RunningMode.LIVE_STREAM)
        }.build()
        val recognizer = HandLandmarker.createFromOptions(context, gestureRecognizerOption)
        recognizer.close()
        return true
      } catch (e: Exception) {
        return false
      }
    }
  }

  init {
    headDetectorV10 = DetectorV10(context, modelPath, HeadDetectorListener(), {
        logger.debug { it }
    })
    val gestureRecognizerOption = HandLandmarkerOptions.builder().apply {
      setBaseOptions(BaseOptions.builder().apply {
        setModelAssetPath(GESTURE_MODEL_PATH)
        setDelegate(Delegate.GPU)
      }.build())
      setRunningMode(RunningMode.LIVE_STREAM)
      setResultListener(GestureDetectorListener())
    }.build()
    gestureRecognizer = HandLandmarker.createFromOptions(context, gestureRecognizerOption)
    logger.debug { "init YOLOMediaPipeDetectionHandler" }

    val boundingBoxesObservable = Observable.combineLatest(headBoundingBox, gestureBoundingBox) { head, gesture -> head + gesture }
    val disposable = boundingBoxesObservable.subscribe {
      sendAction { handler.onDetectUiUpdate(it, 0) }
    }
    compositeDisposable.add(disposable)
  }

  override fun skipPeek() {
    state = DetectionState.WAKING
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
      if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
        postScale(-1F, 1F)
      }
    }

    val rotatedBitmap = Bitmap.createBitmap(
      bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height,
      matrix, true
    )
    val mpImage = BitmapImageBuilder(rotatedBitmap).build()
    synchronized(gestureRecognizerOperatingLock) {
      gestureRecognizer?.detectAsync(mpImage, System.currentTimeMillis())
    }

    if (headDetectorExecutorIsBusy.compareAndSet(false, true)) {
      val inputBitmap = Bitmap.createBitmap(rotatedBitmap)
      headDetectorExecutor.execute {
        headDetectorV10?.detect(inputBitmap)
        inputBitmap.recycle()
        headDetectorExecutorIsBusy.set(false)
      }
    }
    bitmapBuffer.recycle()
    rotatedBitmap.recycle()
  }

  private inner class PersonWatcher {
    private val compositeDisposable = CompositeDisposable()
    private val headResultSubject = BehaviorSubject.createDefault(STATUS_UNKNOWN) // because rxjava 2 cannot use boolean? so I have to use int
    private val handResultSubject = BehaviorSubject.createDefault(STATUS_UNKNOWN)
    private var lastNoHeadTime = 0L
    private var lastNoHandTime = 0L

    init {
      val disposable = Observable.combineLatest(headResultSubject, handResultSubject) { head, hand ->
        if (head == STATUS_DETECTED || hand == STATUS_DETECTED) {
          STATUS_DETECTED
        } else if (head == STATUS_NOT_DETECTED && hand == STATUS_NOT_DETECTED) {
          STATUS_NOT_DETECTED
        } else {
          STATUS_UNKNOWN
        }
      }
      .skip(1)
      .distinctUntilChanged()
      .subscribe {
        if (it == STATUS_UNKNOWN) return@subscribe
        if (it == STATUS_DETECTED) {
          logger.debug { "onDetectAction" }
          onDetectAction()
        } else {
          logger.debug { "onTimeoutAction" }
          onTimeoutAction()
        }
      }
      compositeDisposable.add(disposable)
    }

    fun updateHeadDetectionResult(result: Boolean, inferenceTime: Long) {
      if (result) {
        lastNoHeadTime = 0L
        headResultSubject.onNext(STATUS_DETECTED)
      } else {
        if (lastNoHeadTime == 0L) {
          lastNoHeadTime = System.currentTimeMillis()
        }
        if (System.currentTimeMillis() - inferenceTime - lastNoHeadTime > MAX_NO_HEAD_TIME) {
          headResultSubject.onNext(STATUS_NOT_DETECTED)
        }
      }
    }

    fun updateGestureDetectionResult(result: Boolean, inferenceTime: Long) {
      if (result) {
        lastNoHandTime = 0L
        handResultSubject.onNext(STATUS_DETECTED)
      } else {
        if (lastNoHandTime == 0L) {
          lastNoHandTime = System.currentTimeMillis()
        }
        if (System.currentTimeMillis() - inferenceTime - lastNoHandTime > MAX_NO_HAND_Time) {
          handResultSubject.onNext(STATUS_NOT_DETECTED)
        }
      }
    }

    private fun onDetectAction() {
      if (state == DetectionState.PEEKING) {
        state = DetectionState.WAKING
        sendAction { handler.onPeekFinish(true) }
      }
    }

    private fun onTimeoutAction() {
      when(state) {
        DetectionState.PEEKING -> {
          state = DetectionState.WAKING
          sendAction { handler.onPeekFinish(false) }
        }
        DetectionState.WAKING -> {
          sendAction { handler.onPersonLeave() }
        }
      }
    }

    fun stop() {
      compositeDisposable.dispose()
    }
  }

  private inner class HeadDetectorListener : DetectorV10.DetectorListener {
    override fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
      personWatcher.updateHeadDetectionResult(boundingBoxes.any { it.cls == 0 || it.cls == 1 }, inferenceTime)
      logger.debug { "person detection get ${boundingBoxes.size} boxes in $inferenceTime ms" }
      headBoundingBox.onNext(boundingBoxes)
    }
  }

  private inner class GestureDetectorListener : ResultListener<HandLandmarkerResult, MPImage>{
    private val ACCEPT_CONFIDENCE = 2
    private val behaviorConfidenceMap = buildMap {
      behaviors.items.forEach {
        put(it, 0)
      }
    }.toMutableMap()

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

    override fun run(result: HandLandmarkerResult?, input: MPImage?) {
      if (result == null || input == null) return
      val boundingBoxes = mutableListOf<BoundingBox>()
      val gestures = mutableListOf<Gesture>()
      for (i in 0 until result.worldLandmarks().size) {
        val resultGesture = transform2YMGesture(result.worldLandmarks()[i])
        val name = behaviors.items.find {
          it.gesture == resultGesture
        }?.operation?: resultGesture.fingers?.toFingersList().toString()
        boundingBoxes.add(result.landmarks()[i].toBoundingBox(name))
        gestures.add(resultGesture)
      }
      personWatcher.updateGestureDetectionResult(gestures.isNotEmpty(), System.currentTimeMillis() - result.timestampMs())
      if (gestures.isNotEmpty()) {
        if (state == DetectionState.WAKING)
          onGestureDetect(gestures)
      }
      gestureBoundingBox.onNext(boundingBoxes)
      input.close()
    }

    private fun onGestureDetect(gestures: List<Gesture>) {
      behaviorConfidenceMap.forEach { (behavior, confidence) ->
        if (gestures.any { it == behavior.gesture})
          behaviorConfidenceMap[behavior] = confidence + 1
        else if (confidence > 0)
          behaviorConfidenceMap[behavior] = confidence - 1
        if (behaviorConfidenceMap[behavior]!! > ACCEPT_CONFIDENCE){
          behaviorConfidenceMap[behavior] = 0
          sendAction { handler.onBehaviorAction(behavior.operation) }
        }
      }
    }
  }

  private fun sendAction(action: () -> Unit) {
    synchronized(handlerOperationLock) {
      action()
    }
  }

  override fun destroy() {
    personWatcher.stop()
    synchronized(gestureRecognizerOperatingLock) {
      gestureRecognizer?.close()
      gestureRecognizer = null
    }
    headDetectorV10?.close()
    headDetectorV10 = null
    headDetectorExecutor.shutdown()
    compositeDisposable.dispose()
  }
}