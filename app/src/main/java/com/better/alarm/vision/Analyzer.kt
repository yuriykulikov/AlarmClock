package com.better.alarm.vision

import androidx.camera.core.ImageAnalysis

enum class DetectionState {
  PEEKING, WAKING, FINISHED
}

interface DetectionHandler{
  fun onPeekFinish(isPersonDetected: Boolean)
  fun onPersonLeave()
  fun onDetectUiUpdate(boundingBoxes: List<BoundingBox>, inferenceTime: Long)
  fun onGestureDetected(gesture: String)
}

interface Gesture {
  val name: String
}

interface DetectionAnalyzer {
  val analyzer: ImageAnalysis.Analyzer
  fun destroy()
}
