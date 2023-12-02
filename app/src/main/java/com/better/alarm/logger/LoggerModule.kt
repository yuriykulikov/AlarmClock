package com.better.alarm.logger

import android.content.Context
import ch.qos.logback.classic.AsyncAppender
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.android.LogcatAppender
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.Appender
import ch.qos.logback.core.rolling.RollingFileAppender
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy
import ch.qos.logback.core.util.FileSize
import org.koin.dsl.module
import org.slf4j.ILoggerFactory

/**
 * Creates a module which exports a [LoggerFactory] to create loggers. These [Logger]s are backed by
 * a [RollingFileAppender] and a [LogcatAppender].
 */
fun loggerModule() = module {
  single<LoggerFactory> {
    object : LoggerFactory {
      val logback: ILoggerFactory = configureLogback(get())

      override fun createLogger(tag: String): Logger {
        return Logger(logback.getLogger(tag))
      }
    }
  }
}

private fun configureLogback(context: Context): ILoggerFactory {
  return logback {
    val logDir = context.filesDir.absolutePath

    addAppender(LogcatAppender()) { //
      encoder = patternLayoutEncoder("[%thread] - %msg%n")
    }

    addAppender(RollingFileAppender(), async = true) {
      isAppend = true
      rollingPolicy = timeBasedRollingPolicy {
        fileNamePattern = "$logDir/rolling-%d{yyyy-MM-dd}.log"
        maxHistory = 3
        isCleanHistoryOnStart = true
        setTotalSizeCap(FileSize.valueOf("800KB"))
      }

      encoder =
          patternLayoutEncoder(
              "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n")
    }
  }
}

/** Logback config DSL entry point. */
fun logback(config: LoggerContext.() -> Unit): ILoggerFactory {
  // reset the default context (which may already have been initialized)
  // since we want to reconfigure it
  val context = org.slf4j.LoggerFactory.getILoggerFactory() as LoggerContext
  context.stop()
  config(context)
  return context
}

/**
 * Configures and adds an appender to the [LoggerContext]. Can be wrapped in [AsyncAppender] if
 * [async] is set to `true`.
 */
fun <T : Appender<ILoggingEvent>> LoggerContext.addAppender(
    appender: T,
    async: Boolean = false,
    config: T.() -> Unit
) {
  appender.context = this
  config(appender)
  appender.start()

  val root =
      org.slf4j.LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)
          as ch.qos.logback.classic.Logger

  root.addAppender(
      when {
        async -> {
          AsyncAppender().apply {
            context = this@addAppender
            addAppender(appender)
            start()
          }
        }
        else -> appender
      })
}

/**
 * Creates and configures a [TimeBasedRollingPolicy].
 *
 * ## Example
 *
 * ```
 * rollingPolicy = timeBasedRollingPolicy {
 *   fileNamePattern = "$logDir/rolling-%d{yyyy-MM-dd}.log"
 *   maxHistory = 7
 *   isCleanHistoryOnStart = true
 * }
 * ```
 */
fun RollingFileAppender<ILoggingEvent>.timeBasedRollingPolicy(
    config: TimeBasedRollingPolicy<ILoggingEvent>.() -> Unit
): TimeBasedRollingPolicy<ILoggingEvent> {
  val parent = this
  return TimeBasedRollingPolicy<ILoggingEvent>().apply {
    context = parent.context
    setParent(parent)
    config(this)
    start()
  }
}

/**
 * Creates a [PatternLayoutEncoder].
 *
 * See
 * [Logback ClassicPatternLayout](http://logback.qos.ch/manual/layouts.html#ClassicPatternLayout)
 * for details.
 */
fun Appender<ILoggingEvent>.patternLayoutEncoder(template: String): PatternLayoutEncoder {
  return PatternLayoutEncoder().apply {
    context = this@patternLayoutEncoder.context
    pattern = template
    start()
  }
}
