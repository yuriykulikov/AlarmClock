package com.better.alarm.logger

import android.util.Log
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.PatternLayout
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.UnsynchronizedAppenderBase

class LogcatAppender : UnsynchronizedAppenderBase<ILoggingEvent>() {
  var encoder: PatternLayoutEncoder? = null
  var tagEncoder: PatternLayoutEncoder? = null

  override fun start() {
    if (encoder?.layout == null) {
      addError("Encoder layout not set for $name")
      return
    }

    tagEncoder?.run {
      val tagEncoderLayout = layout
      if (tagEncoderLayout == null) {
        addError("Tag layout not set for $name")
        return
      }

      if (tagEncoderLayout is PatternLayout) {
        if (!pattern.contains("%nopex")) {
          stop()
          pattern = "$pattern%nopex"
          start()
        }
        tagEncoderLayout.setPostCompileProcessor(null)
      }
    }

    super.start()
  }

  public override fun append(event: ILoggingEvent) {
    if (!isStarted) {
      return
    }
    val tag = (tagEncoder?.layout?.doLayout(event) ?: event.loggerName).take(MAX_TAG_LENGTH)
    val priority: Int? =
        when (event.level.levelInt) {
          Level.ALL_INT,
          Level.TRACE_INT -> Log.VERBOSE
          Level.DEBUG_INT -> Log.DEBUG
          Level.INFO_INT -> Log.INFO
          Level.WARN_INT -> Log.WARN
          Level.ERROR_INT -> Log.ERROR
          Level.OFF_INT -> null
          else -> null
        }

    if (priority != null) {
      Log.println(priority, tag, checkNotNull(encoder?.layout).doLayout(event))
    }
  }

  companion object {
    private const val MAX_TAG_LENGTH = 23
  }
}
