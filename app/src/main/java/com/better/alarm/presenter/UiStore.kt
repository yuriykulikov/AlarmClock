package com.better.alarm.presenter

import com.better.alarm.configuration.EditedAlarm
import com.better.alarm.model.AlarmValue
import com.better.alarm.model.Alarms
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.flow.MutableStateFlow

/** Created by Yuriy on 11.08.2017. */
interface UiStore {
  fun editing(): MutableStateFlow<EditedAlarm?>
  fun onBackPressed(): PublishSubject<String>
  fun createNewAlarm()
  fun edit(id: Int)
  fun hideDetails()

  /** Open the drawer when the activity is created. True after theme changes. */
  var openDrawerOnCreate: Boolean
}

class UiStoreImpl(private val alarms: Alarms) : UiStore {
  private var onBackPressed = PublishSubject.create<String>()
  private val editing: MutableStateFlow<EditedAlarm?> = MutableStateFlow(null)

  override fun editing(): MutableStateFlow<EditedAlarm?> {
    return editing
  }

  override fun onBackPressed(): PublishSubject<String> {
    return onBackPressed
  }

  override fun createNewAlarm() {
    editing.value = EditedAlarm(isNew = true, value = AlarmValue(isDeleteAfterDismiss = true))
  }

  override fun edit(id: Int) {
    alarms.getAlarm(id)?.let { alarm ->
      editing.value = EditedAlarm(isNew = false, value = alarm.data)
    }
  }

  override fun hideDetails() {
    editing.value = null
  }

  override var openDrawerOnCreate: Boolean = false
}
