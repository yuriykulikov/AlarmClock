package com.better.alarm.compose

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.setContent
import com.better.alarm.configuration.Prefs
import com.better.alarm.configuration.Store
import com.better.alarm.configuration.globalInject
import com.better.alarm.interfaces.IAlarmsManager
import com.better.alarm.model.AlarmValue

class AlarmsListComposeActivity : AppCompatActivity() {
  private val alarms: IAlarmsManager by globalInject()
  private val store: Store by globalInject()
  private val backs: Backs by globalInject()
  private val prefs: Prefs by globalInject()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      AlarmsApp(
        editor = AlarmEditor(),
        alarms = store.alarms(),
        backs = backs,
        themeStore = prefs.theme,
      )
    }
  }

  inner class AlarmEditor : Editor {
    override fun change(
      alarm: AlarmValue,
    ) {
      (alarms.getAlarm(alarm.id) ?: alarms.createNewAlarm())
        .edit {
          copy(
            isEnabled = alarm.isEnabled,
            label = alarm.label,
            hour = alarm.hour,
            minutes = alarm.minutes,
            daysOfWeek = alarm.daysOfWeek,
            alarmtone = alarm.alarmtone,
          )
        }
    }
  }

  override fun onBackPressed() {
    backs.backPressed.onNext("MainActivity")
  }
}