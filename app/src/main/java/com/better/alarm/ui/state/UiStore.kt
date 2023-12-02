package com.better.alarm.ui.state

import com.better.alarm.data.AlarmValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class UiStore {

  private val editing: MutableStateFlow<EditedAlarm?> = MutableStateFlow(null)

  fun editing(): StateFlow<EditedAlarm?> {
    return editing
  }

  fun createNewAlarm() {
    editing.value = EditedAlarm(isNew = true, value = AlarmValue(isDeleteAfterDismiss = true))
  }

  fun edit(alarmValue: AlarmValue, isNew: Boolean = false) {
    editing.value = EditedAlarm(isNew = isNew, value = alarmValue)
  }

  fun hideDetails() {
    editing.value = null
  }

  var openDrawerOnCreate: Boolean = false
}
