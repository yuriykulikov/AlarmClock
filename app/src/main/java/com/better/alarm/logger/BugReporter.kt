package com.better.alarm.logger

import android.app.Application
import android.content.Context
import com.better.alarm.BuildConfig
import com.better.alarm.R
import org.acra.ACRA
import org.acra.ReportField
import org.acra.config.mailSender
import org.acra.data.StringFormat
import org.acra.file.Directory
import org.acra.ktx.initAcra

class BugReporter(
    private val logger: Logger,
    private val context: Context,
) {
  fun sendUserReport() {
    if (BuildConfig.ACRA_EMAIL.isNotEmpty()) {
      ACRA.errorReporter.putCustomData("LOGS", rollingLogs())
      ACRA.errorReporter.handleSilentException(Exception())
    }
  }

  private fun rollingLogs() =
      context.filesDir
          .walk()
          .filter { it.name.startsWith("rolling") }
          .sortedBy { it.name }
          .flatMap { listOf(it.name) + it.readLines() }
          .joinToString("\n")

  fun attachToMainThread(application: Application) {
    application.initAcra {
      buildConfigClass = BuildConfig::class.java
      reportFormat = StringFormat.KEY_VALUE_LIST
      reportContent =
          listOf(
              ReportField.IS_SILENT,
              ReportField.APP_VERSION_CODE,
              ReportField.PHONE_MODEL,
              ReportField.ANDROID_VERSION,
              ReportField.CUSTOM_DATA,
              ReportField.STACK_TRACE,
              ReportField.SHARED_PREFERENCES,
          )
      applicationLogFileDir = Directory.ROOT
      // setApplicationLogFile(context.getFileStreamPath("app.log").absolutePath)
      mailSender {
        mailTo = BuildConfig.ACRA_EMAIL
        reportAsFile = true
        reportFileName = "application-logs.txt"
        enabled = true
        subject =
            "${context.getString(R.string.simple_alarm_clock)} ${BuildConfig.FLAVOR} ${BuildConfig.VERSION_NAME} Bug Report"
        body = context.getString(R.string.dialog_bugreport_hint)
      }
    }

    val prev = Thread.currentThread().uncaughtExceptionHandler
    Thread.currentThread().setUncaughtExceptionHandler { thread, throwable ->
      logger.error(throwable) { "Uncaught exception $throwable" }
      ACRA.errorReporter.putCustomData("LOGS", rollingLogs())
      ACRA.errorReporter.handleException(throwable)
      prev?.uncaughtException(thread, throwable)
    }
  }
}
