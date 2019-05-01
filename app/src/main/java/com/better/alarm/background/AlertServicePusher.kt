package com.better.alarm.background

import android.content.Context
import android.content.Intent
import com.better.alarm.configuration.Store
import com.better.alarm.interfaces.Intents
import com.better.alarm.oreo
import com.better.alarm.preOreo
import java.lang.RuntimeException

class AlertServicePusher(store: Store, context: Context) {
    init {
        val disposable = store.events
                .map {
                    when (it) {
                        is Event.AlarmEvent -> Intent(Intents.ALARM_ALERT_ACTION).apply { putExtra(Intents.EXTRA_ID, it.id) }
                        is Event.PrealarmEvent -> Intent(Intents.ALARM_PREALARM_ACTION).apply { putExtra(Intents.EXTRA_ID, it.id) }
                        is Event.DismissEvent -> Intent(Intents.ALARM_DISMISS_ACTION).apply { putExtra(Intents.EXTRA_ID, it.id) }
                        is Event.SnoozedEvent -> Intent(Intents.ALARM_SNOOZE_ACTION).apply { putExtra(Intents.EXTRA_ID, it.id) }
                        is Event.Autosilenced -> Intent(Intents.ACTION_SOUND_EXPIRED).apply { putExtra(Intents.EXTRA_ID, it.id) }
                        is Event.MuteEvent -> Intent(Intents.ACTION_MUTE)
                        is Event.DemuteEvent -> Intent(Intents.ACTION_DEMUTE)
                        is Event.CancelSnoozedEvent -> Intent(Intents.ACTION_CANCEL_SNOOZE)
                        is Event.NullEvent -> throw RuntimeException("NullEvent")
                    }.apply {
                        setClass(context, AlertServiceWrapper::class.java)
                    }
                }
                .filter { it.action != Intents.ACTION_CANCEL_SNOOZE }
                .subscribe { intent ->
                    oreo { context.startForegroundService(intent) }
                    preOreo { context.startService(intent) }
                }
    }
}