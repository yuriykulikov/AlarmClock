package com.better.alarm.bugreports

import android.app.Application
import android.content.Context
import com.better.alarm.BuildConfig
import com.better.alarm.R
import com.better.alarm.logger.Logger
import org.acra.ACRA
import org.acra.ReportField
import org.acra.config.CoreConfigurationBuilder
import org.acra.config.MailSenderConfigurationBuilder
import org.acra.data.StringFormat
import org.acra.file.Directory

class BugReporter(
    private val logger: Logger,
    private val context: Context,
) {
  fun sendUserReport() {
    if (BuildConfig.ACRA_EMAIL.isNotEmpty()) {
      ACRA.getErrorReporter().putCustomData("LOGS", rollingLogs())
      ACRA.getErrorReporter().handleSilentException(Exception())
    }
  }

  private fun rollingLogs() =
      context
          .filesDir
          .walk()
          .filter { it.name.startsWith("rolling") }
          .sortedBy { it.name }
          .flatMap { listOf(it.name) + it.readLines() }
          .joinToString("\n")

  fun attachToMainThread(application: Application) {
    ACRA.init(
        application,
        CoreConfigurationBuilder(context).apply {
          setBuildConfigClass(BuildConfig::class.java)
          setReportFormat(StringFormat.KEY_VALUE_LIST)
          setEnabled(true)
          setReportContent(
              ReportField.IS_SILENT,
              ReportField.APP_VERSION_CODE,
              ReportField.PHONE_MODEL,
              ReportField.ANDROID_VERSION,
              ReportField.CUSTOM_DATA,
              ReportField.STACK_TRACE,
              ReportField.SHARED_PREFERENCES,
          )
          setApplicationLogFileDir(Directory.ROOT)
          setApplicationLogFile(context.getFileStreamPath("app.log").absolutePath)
          getPluginConfigurationBuilder(MailSenderConfigurationBuilder::class.java).apply {
            setMailTo(BuildConfig.ACRA_EMAIL)
            setReportAsFile(true)
            setReportFileName("application-logs.txt")
            setEnabled(true)
            setSubject(
                "${context.getString(R.string.simple_alarm_clock)} ${BuildConfig.FLAVOR} ${BuildConfig.VERSION_NAME} Bug Report")
            setBody(context.getString(R.string.dialog_bugreport_hint))
          }
        })

    val prev = Thread.currentThread().uncaughtExceptionHandler
    Thread.currentThread().setUncaughtExceptionHandler { thread, throwable ->
      logger.error(throwable) { "Uncaught exception $throwable" }
      ACRA.getErrorReporter().putCustomData("LOGS", rollingLogs())
      ACRA.getErrorReporter().handleException(throwable)
      prev?.uncaughtException(thread, throwable)
    }
  }
}
