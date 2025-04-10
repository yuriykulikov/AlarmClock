package com.better.alarm.vision

import android.content.Context
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.better.alarm.ui.settings.VisionBehaviorItemView


class IAnalyzer(
  val context: Context,
  private val handler: DetectionHandler,
  private val behaviorList: VisionBehaviorItemView.Companion.BehaviorsStoreValue
) : DetectionAnalyzer {

  companion object {
    fun checkAvailability(context: Context): Boolean { return false }
  }

  override fun destroy() {}

  override val analyzer: ImageAnalysis.Analyzer
    get() = ImageAnalysis.Analyzer { }

  override fun stop() {}

  override fun skipPeek() {}
}
