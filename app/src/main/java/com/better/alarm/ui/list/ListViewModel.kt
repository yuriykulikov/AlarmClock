package com.better.alarm.ui.list

import androidx.lifecycle.ViewModel
import com.better.alarm.data.AlarmValue
import com.better.alarm.ui.state.UiStore

class ListViewModel(
    private val uiStore: UiStore,
) : ViewModel() {

  fun edit(alarmValue: AlarmValue) {
    uiStore.edit(alarmValue)
  }

  fun createNewAlarm() {
    uiStore.createNewAlarm()
  }
}
