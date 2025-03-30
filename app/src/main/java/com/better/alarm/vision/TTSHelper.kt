package com.better.alarm.vision

import android.content.Context
import android.media.AudioManager.STREAM_ALARM
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.better.alarm.bootstrap.globalLogger
import java.util.UUID


class TTSHelper(
  private val context: Context,
) {
  private var tts: TextToSpeech? = null
  private val logger by globalLogger("TTSHelper")
  private var speakingParams = Bundle().apply{ putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, STREAM_ALARM) }
  private val callbackMap = mutableMapOf<String, () -> Unit>()

  init {
    tts = TextToSpeech(context) { status ->
      if (status == TextToSpeech.SUCCESS) {
        val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
          context.resources.configuration.locales[0]
        } else {
          context.resources.configuration.locale
        }
        val result = tts!!.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
          // Language data is missing or the language is not supported.
          logger.error { "Language data is missing or the language is not supported." }
          // close tts
          tts?.shutdown()
          tts = null
        } else {
          logger.info { "Language data is available." }
        }
      }
    }
    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
      override fun onStart(utteranceId: String?) { }

      override fun onDone(utteranceId: String?) {
        if (utteranceId == null) return
        if (!callbackMap.containsKey(utteranceId)) return
        callbackMap[utteranceId]?.invoke()
        callbackMap.remove(utteranceId)
      }

      override fun onError(utteranceId: String?) { }
    })


  }

  fun say(word: String, queue: Int = TextToSpeech.QUEUE_ADD, callback: (() -> Unit)? = null) {
    val uuid: String = UUID.randomUUID().toString()
    tts?.speak(word, queue, speakingParams, uuid)
    if (callback != null) {
      callbackMap[uuid] = callback
    }
  }

  fun destroy() {
    tts?.shutdown()
    tts = null
  }
}
