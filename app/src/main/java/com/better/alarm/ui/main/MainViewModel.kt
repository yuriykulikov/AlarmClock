package com.better.alarm.ui.main

import androidx.lifecycle.ViewModel
import com.better.alarm.domain.IAlarmsManager
import com.better.alarm.logger.BugReporter
import com.better.alarm.ui.state.EditedAlarm
import com.better.alarm.ui.state.UiStore
import kotlinx.coroutines.flow.StateFlow

class MainViewModel(
    private val uiStore: UiStore,
    private val alarms: IAlarmsManager,
    private val bugReporter: BugReporter,
) : ViewModel() {
  var openDrawerOnCreate: Boolean = false

  fun editing(): StateFlow<EditedAlarm?> {
    return uiStore.editing()
  }

  fun hideDetails() {
    uiStore.hideDetails()
  }

  fun deleteEdited() {
    uiStore.editing().value?.value?.id?.let { alarms.getAlarm(it)?.delete() }
    uiStore.hideDetails()
  }

  fun sendUserReport() {
    bugReporter.sendUserReport()
  }

  fun edit(restored: EditedAlarm) {
    uiStore.edit(restored.value, restored.isNew)
  }

  fun awaitStored() {}
}
