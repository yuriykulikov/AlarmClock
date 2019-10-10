package com.better.alarm.model

import java.util.Calendar

/**
 * Strategy used to notify other components about alarm state.
 */
interface IStateNotifier {
    fun broadcastAlarmState(id: Int, action: String, calendar: Calendar? = null)
}