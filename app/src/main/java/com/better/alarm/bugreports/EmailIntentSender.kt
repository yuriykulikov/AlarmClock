/*
 * Copyright (c) 2017 acra authors
 * Copyright (c) 2023 Yuriy Kulikov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.better.alarm.bugreports

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import com.better.alarm.configuration.globalInject
import com.better.alarm.logger.Logger
import com.better.alarm.logger.LoggerFactory
import com.google.auto.service.AutoService
import java.io.File
import org.acra.attachment.AcraContentProvider
import org.acra.config.Configuration
import org.acra.config.CoreConfiguration
import org.acra.config.CoreConfigurationBuilder
import org.acra.config.getPluginConfiguration
import org.acra.data.CrashReportData
import org.acra.ktx.plus
import org.acra.sender.ReportSender
import org.acra.sender.ReportSenderFactory

@AutoService(ReportSenderFactory::class)
class EmailIntentSenderFactory : ReportSenderFactory {
  private val loggerFactory: LoggerFactory by globalInject()
  override fun create(context: Context, config: CoreConfiguration): ReportSender {
    return EmailIntentSender(config, loggerFactory.createLogger("EmailIntentSender"))
  }
}

data class MailSenderConfiguration(
    val mailTo: String,
    val subject: String,
    val body: String,
) : Configuration {
  override fun enabled(): Boolean = true
}

fun CoreConfigurationBuilder.customSender(mailTo: String, subject: String, body: String) {
  pluginConfigurations += MailSenderConfiguration(mailTo, subject, body)
}

/**
 * Send reports through an email intent.
 *
 * This is slimmed down version of [org.acra.sender.EmailIntentSender] that doesn't use
 * [Intent.setSelector] and instead relies on [PackageManager.queryIntentActivities].
 *
 * Here are relevant issues:
 * * [acra#1146](https://github.com/ACRA/acra/issues/1146)
 * * [acra#1179](https://github.com/ACRA/acra/issues/1179)
 */
class EmailIntentSender(private val config: CoreConfiguration, private val logger: Logger) :
    ReportSender {
  private val mailConfig: MailSenderConfiguration =
      config.getPluginConfiguration(MailSenderConfiguration::class.java)

  override fun send(context: Context, errorContent: CrashReportData) {
    runCatching {
          val attachment = writeReportData(errorContent, context)
          sendWithChooser(mailConfig.subject, mailConfig.body, attachment, context)
        }
        .recoverCatching {
          logger.error { "Failed to send the report: $it, trying a fallback" }
          context.startActivity(buildFallbackIntent(mailConfig.subject, mailConfig.body))
        }
        .onFailure { logger.error { "Fallback also failed: $it" } }
  }

  private fun writeReportData(errorContent: CrashReportData, context: Context): Uri {
    val reportText: String =
        config.reportFormat.toFormattedString(
            data = errorContent,
            order = config.reportContent,
            mainJoiner = "\n",
            subJoiner = "\n\t",
            urlEncode = false,
        )
    val file = File(context.cacheDir, "bugreport.txt")
    file.writeText(reportText, Charsets.UTF_8)
    return AcraContentProvider.getUriForFile(context, file)
  }

  private fun queryActivities(context: Context): MutableList<ResolveInfo> {
    val pm = context.packageManager
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      pm.queryIntentActivities(
          buildSendToIntent(),
          PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()))
    } else {
      @Suppress("DEPRECATION")
      pm.queryIntentActivities(buildSendToIntent(), PackageManager.MATCH_DEFAULT_ONLY)
    }
  }

  private fun sendWithChooser(subject: String, body: String, attachment: Uri, context: Context) {
    // grant permission for android to read the attachment
    context.grantUriPermission(
        context.packageName, attachment, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    val activityIntents: List<Intent> =
        queryActivities(context)
            .onEach { info ->
              logger.warning { "Found $info" }
              context.grantUriPermission(
                  info.activityInfo.packageName, attachment, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            .flatMap { info ->
              listOf(
                  buildSendIntent(subject, body, attachment).apply {
                    setPackage(info.activityInfo.packageName)
                  },
                  buildSendIntent(subject, body, attachment).apply {
                    setPackage(info.activityInfo.packageName)
                    type = "*/*" // gmail and outlook will only match with type set
                  },
              )
            }
            .filter { intent -> intent.resolveActivity(context.packageManager) != null }

    check(activityIntents.isNotEmpty()) { "No activities found to handle email!" }

    context.grantUriPermission("android", attachment, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    val chooser =
        Intent(Intent.ACTION_CHOOSER).apply {
          putExtra(Intent.EXTRA_INTENT, activityIntents.first())
          putExtra(Intent.EXTRA_INITIAL_INTENTS, activityIntents.drop(1).toTypedArray())
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    context.startActivity(chooser)
  }

  @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.Q)
  override fun requiresForeground(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
  }

  /** Builds an intent used to resolve email clients */
  private fun buildSendToIntent(): Intent =
      Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }

  /** Builds a list of intents to be used when no default email client is set */
  private fun buildSendIntent(subject: String, body: String?, attachment: Uri) =
      Intent(Intent.ACTION_SEND).apply {
        putExtra(Intent.EXTRA_EMAIL, arrayOf(mailConfig.mailTo))
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_STREAM, attachment)
        putExtra(Intent.EXTRA_TEXT, body)
      }

  /** Builds an email intent without attachment */
  private fun buildFallbackIntent(subject: String, body: String): Intent =
      Intent(Intent.ACTION_SENDTO).apply {
        val encodedSubject = Uri.encode(subject)
        val encodedBody = Uri.encode(body)
        data = Uri.parse("mailto:${mailConfig.mailTo}?subject=$encodedSubject&body=$encodedBody")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, body)
      }
}
