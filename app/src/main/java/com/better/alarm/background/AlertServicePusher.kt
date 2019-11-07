package com.better.alarm.background

import android.content.Context
import android.content.Intent
import com.better.alarm.configuration.AlarmApplication.container
import com.better.alarm.configuration.Store
import com.better.alarm.interfaces.Intents
import com.better.alarm.oreo
import com.better.alarm.preOreo
import com.better.alarm.util.mapNotNull

class AlertServicePusher(store: Store, context: Context) {
    init {
        val disposable = store.events
                .mapNotNull {
                    when (it) {
                        is Event.AlarmEvent -> Intent(Intents.ALARM_ALERT_ACTION).apply { putExtra(Intents.EXTRA_ID, it.id) }
                        is Event.PrealarmEvent -> Intent(Intents.ALARM_PREALARM_ACTION).apply { putExtra(Intents.EXTRA_ID, it.id) }
                        is Event.DismissEvent -> Intent(Intents.ALARM_DISMISS_ACTION).apply { putExtra(Intents.EXTRA_ID, it.id) }
                        is Event.SnoozedEvent -> Intent(Intents.ALARM_SNOOZE_ACTION).apply { putExtra(Intents.EXTRA_ID, it.id) }
                        is Event.Autosilenced -> Intent(Intents.ACTION_SOUND_EXPIRED).apply { putExtra(Intents.EXTRA_ID, it.id) }
                        is Event.MuteEvent -> Intent(Intents.ACTION_MUTE)
                        is Event.DemuteEvent -> Intent(Intents.ACTION_DEMUTE)
                        is Event.CancelSnoozedEvent -> null
                        is Event.ShowSkip -> null
                        is Event.HideSkip -> null
                        is Event.NullEvent -> throw RuntimeException("NullEvent")
                    }?.apply {
                        setClass(context, AlertServiceWrapper::class.java)
                    }
                }
                .subscribe { intent ->
                    container().wakeLocks().acquireTransitionWakeLock(intent)
                    oreo { context.startForegroundService(intent) }
                    preOreo { context.startService(intent) }
                    container().logger.d("pushed intent ${intent.action} to AlertServiceWrapper")
                }
    }
}