package com.better.alarm.vision

import android.content.Context
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.better.alarm.ui.settings.VisionBehaviorItemView
import io.reactivex.Observable


class IAnalyzer(
  val context: Context,
  private val handler: DetectionHandler,
  private val behaviorList: Behavior.BehaviorsStoreValue,
  private val modelPath: String
) : DetectionAnalyzer {

  companion object {
    fun checkAvailability(context: Context): Int { return 1 }
  }

  override fun destroy() {}

  override val analyzer: ImageAnalysis.Analyzer
    get() = ImageAnalysis.Analyzer { }
  override var lensFacingObservable: Observable<Int>? = null

  override fun skipPeek() {}
}
