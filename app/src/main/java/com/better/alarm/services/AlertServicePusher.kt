package com.better.alarm.services

import android.content.Context
import android.content.Intent
import com.better.alarm.domain.Store
import com.better.alarm.logger.Logger
import com.better.alarm.platform.WakeLockManager
import com.better.alarm.platform.oreo
import com.better.alarm.platform.preOreo
import com.better.alarm.receivers.Intents
import com.better.alarm.util.mapNotNull
import com.better.alarm.util.subscribeForever

class AlertServicePusher(store: Store, context: Context, wm: WakeLockManager, logger: Logger) {
  init {
    store.events
        .mapNotNull {
          when (it) {
            is Event.AlarmEvent ->
                Intent(Intents.ALARM_ALERT_ACTION).apply { putExtra(Intents.EXTRA_ID, it.id) }
            is Event.PrealarmEvent ->
                Intent(Intents.ALARM_PREALARM_ACTION).apply { putExtra(Intents.EXTRA_ID, it.id) }
            is Event.DismissEvent ->
                Intent(Intents.ALARM_DISMISS_ACTION).apply { putExtra(Intents.EXTRA_ID, it.id) }
            is Event.MuteEvent -> Intent(Intents.ACTION_MUTE)
            is Event.DemuteEvent -> Intent(Intents.ACTION_DEMUTE)
            is Event.SnoozedEvent -> null
            is Event.Autosilenced -> null
            is Event.CancelSnoozedEvent -> null
            is Event.ShowSkip -> null
            is Event.HideSkip -> null
            is Event.NullEvent -> throw RuntimeException("NullEvent")
          }?.apply { setClass(context, AlertServiceWrapper::class.java) }
        }
        .subscribeForever { intent ->
          wm.acquireTransitionWakeLock(intent)
          oreo { context.startForegroundService(intent) }
          preOreo { context.startService(intent) }
          logger.debug { "pushed intent ${intent.action} to AlertServiceWrapper" }
        }
  }
}
