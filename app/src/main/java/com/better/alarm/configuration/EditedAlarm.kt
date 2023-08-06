package com.better.alarm.configuration

import com.better.alarm.model.AlarmValue

/** Created by Yuriy on 09.08.2017. */
data class EditedAlarm(
    val isNew: Boolean = false,
    val value: AlarmValue,
)
