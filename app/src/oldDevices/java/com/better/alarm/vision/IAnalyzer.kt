package com.better.alarm.vision

import android.content.Context
import androidx.camera.core.ImageAnalysis


class IAnalyzer(
  val context: Context,
  private val handler: DetectionHandler
) : DetectionAnalyzer {
  override fun destroy() {
    TODO("Not yet implemented")
  }

  override val analyzer: ImageAnalysis.Analyzer
    get() = TODO("Not yet implemented")
}
