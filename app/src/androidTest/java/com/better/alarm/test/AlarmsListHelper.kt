package com.better.alarm.test

import android.widget.ListView
import androidx.test.espresso.Espresso
import androidx.test.espresso.matcher.ViewMatchers
import com.better.alarm.R
import com.better.alarm.model.AlarmValue
import java.lang.RuntimeException

/** Returns [AlarmValue] currently in the alarms list. */
fun alarmsList(): List<AlarmValue> {
  return mutableListOf<AlarmValue>().apply {
    Espresso.onView(ViewMatchers.withId(R.id.list_fragment_list)).check { view, noViewFoundException
      ->
      if (noViewFoundException != null) {
        throw RuntimeException(noViewFoundException)
      } else {
        val adapter = (view as ListView).adapter
        for (i in 0 until adapter.count) {
          add(adapter.getItem(i) as AlarmValue)
        }
      }
    }
  }
}
