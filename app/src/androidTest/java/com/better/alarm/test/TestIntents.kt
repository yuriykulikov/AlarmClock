package com.better.alarm.test

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.test.platform.app.InstrumentationRegistry
import com.better.alarm.receivers.TestReceiver
import java.util.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking

suspend fun sentTestIntent(action: String) {
  sentTestIntent { context ->
    setAction(action)
    setClass(context, TestReceiver::class.java)
  }
}

fun dropDatabase() = runBlocking { sentTestIntent(TestReceiver.ACTION_DROP_AND_INSERT_DEFAULTS) }

suspend fun sentTestIntent(builder: Intent.(Context) -> Unit) {
  val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
  val latch = Channel<Unit>(1)
  val cbAction = UUID.randomUUID().toString()
  targetContext.registerReceiver(
      object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
          targetContext.unregisterReceiver(this)
          latch.trySend(Unit).getOrThrow()
        }
      },
      IntentFilter(cbAction))

  targetContext.sendBroadcast(
      Intent().apply {
        builder(targetContext)
        putExtra("CB", cbAction)
      })
  latch.receive()
}
