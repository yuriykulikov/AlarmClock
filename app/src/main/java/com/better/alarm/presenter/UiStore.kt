package com.better.alarm.presenter

import com.better.alarm.configuration.EditedAlarm
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
