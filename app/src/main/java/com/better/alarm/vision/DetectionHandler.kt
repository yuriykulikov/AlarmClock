package com.better.alarm.vision

import android.content.Context
import com.better.alarm.bootstrap.globalLogger
import com.better.alarm.ui.alert.AlarmAlertVisionFullScreen

enum class DetectionState {
  PEEKING, WAKING
}
class DetectionHandler : Detector.DetectorListener {
  private var state = DetectionState.PEEKING
  private var initialCoutDown = 30
  private val logger by globalLogger("DetectionHandler")

  private fun peekStateOnDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
    if (boundingBoxes.any { it.clsName == "person" }) {
      state = DetectionState.WAKING
    } else {
      initialCoutDown--
      if (initialCoutDown == 0) {
        logger.debug { "nothing detected after initial detection, switch to normal activity" }
      }
    }
  }

  private fun wakeStateOnDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {

  }

  override fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
    when (state) {
      DetectionState.PEEKING -> peekStateOnDetect(boundingBoxes, inferenceTime)
      DetectionState.WAKING -> wakeStateOnDetect(boundingBoxes, inferenceTime)
    }
  }
