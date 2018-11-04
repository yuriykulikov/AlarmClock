package com.better.alarm.model

/**
 * Created by Yuriy on 11.06.2017.
 */
interface AlarmValue {
    val id: Int

    val isEnabled: Boolean

    val hour: Int

    val minutes: Int

    val daysOfWeek: DaysOfWeek

    val label: String

    val isPrealarm: Boolean

    val isVibrate: Boolean

    val alertString: String
}
