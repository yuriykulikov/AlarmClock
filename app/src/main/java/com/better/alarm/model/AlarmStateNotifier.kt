package com.better.alarm.model

import com.better.alarm.background.Event
import com.better.alarm.configuration.Store
import com.better.alarm.interfaces.Intents
import java.util.Calendar

/**
 * Broadcasts alarm state with an intent
 *
 * @author Yuriy
 */
class AlarmStateNotifier(private val store: Store) : IStateNotifier {
    override fun broadcastAlarmState(id: Int, action: String, calendar: Calendar?) {
        val event = when (action) {
            Intents.ALARM_ALERT_ACTION -> Event.AlarmEvent(id)
            Intents.ALARM_PREALARM_ACTION -> Event.PrealarmEvent(id)
            Intents.ACTION_MUTE -> Event.MuteEvent()
            Intents.ACTION_DEMUTE -> Event.DemuteEvent()
            Intents.ACTION_SOUND_EXPIRED -> Event.Autosilenced(id)
            Intents.ALARM_SNOOZE_ACTION -> Event.SnoozedEvent(id, calendar = calendar!!)
            Intents.ACTION_CANCEL_SNOOZE -> Event.CancelSnoozedEvent(id)
            Intents.ALARM_DISMISS_ACTION -> Event.DismissEvent(id)
            else -> throw RuntimeException("Unknown action $action")
        }
        store.events.onNext(event)
    }
}
