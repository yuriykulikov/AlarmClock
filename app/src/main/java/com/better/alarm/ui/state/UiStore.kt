package com.better.alarm.ui.state

import com.better.alarm.data.AlarmValue
import com.better.alarm.domain.Alarms
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.flow.MutableStateFlow

class UiStore(private val alarms: Alarms) {

  private var onBackPressed = PublishSubject.create<String>()
  private val editing: MutableStateFlow<EditedAlarm?> = MutableStateFlow(null)

  fun editing(): MutableStateFlow<EditedAlarm?> {
    return editing
  }

  fun onBackPressed(): PublishSubject<String> {
    return onBackPressed
  }

  fun createNewAlarm() {
    editing.value = EditedAlarm(isNew = true, value = AlarmValue(isDeleteAfterDismiss = true))
  }

  fun edit(id: Int) {
    alarms.getAlarm(id)?.let { alarm ->
      editing.value = EditedAlarm(isNew = false, value = alarm.data)
    }
  }

  fun hideDetails() {
    editing.value = null
  }

  var openDrawerOnCreate: Boolean = false
}
