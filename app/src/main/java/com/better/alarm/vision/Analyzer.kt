package com.better.alarm.vision

import androidx.camera.core.ImageAnalysis
import kotlinx.serialization.Serializable

enum class DetectionState {
  PEEKING, WAKING
}

interface DetectionHandler{
  fun onPeekFinish(isPersonDetected: Boolean)
  fun onPersonLeave()
  fun onDetectUiUpdate(boundingBoxes: List<BoundingBox>, inferenceTime: Long)
  fun onBehaviorAction(operation: String)
}


@Serializable
data class Gesture (
  val fingers: FingersState?
) {
  @Serializable
  data class FingersState (val thumb: Int, val index: Int, val middle: Int, val ring: Int, val little: Int)
}

fun List<Int>.toFingersState(): Gesture.FingersState {
  return Gesture.FingersState(this[0], this[1], this[2], this[3], this[4])
}

fun Gesture.FingersState.toFingersList(): List<Int> {
  return listOf(thumb, index, middle, ring, little)
}


interface DetectionAnalyzer {
  val analyzer: ImageAnalysis.Analyzer
  fun skipPeek()
  fun destroy()
}
