package com.better.alarm.ui.details

import androidx.lifecycle.ViewModel
import com.better.alarm.data.AlarmValue
import com.better.alarm.ui.state.EditedAlarm
import com.better.alarm.ui.state.UiStore
import kotlinx.coroutines.flow.StateFlow

class AlarmDetailsViewModel(private val uiStore: UiStore) : ViewModel() {
  fun editor(): StateFlow<EditedAlarm?> {
    return uiStore.editing()
  }

  fun hideDetails() {
    uiStore.hideDetails()
  }

  fun modify(reason: String, function: (AlarmValue) -> AlarmValue) {
    uiStore.editing().value?.let { prev -> uiStore.edit(function(prev.value), prev.isNew) }
  }

  var newAlarmPopupSeen: Boolean = false
}
