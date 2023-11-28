package com.better.alarm.configuration

import com.better.alarm.model.AlarmValue
import com.better.alarm.presenter.RowHolder
import com.better.alarm.util.Optional

/** Created by Yuriy on 09.08.2017. */
data class EditedAlarm(
    val isNew: Boolean = false,
    val id: Int = -1,
    val value: Optional<AlarmValue> = Optional.absent(),
    val holder: Optional<RowHolder> = Optional.absent()
) {
  fun id() = id
  val isEdited: Boolean = value.isPresent()
}
