package com.better.alarm.presenter

import com.better.alarm.configuration.globalInject
import com.better.alarm.interfaces.IAlarmsManager
import com.better.alarm.interfaces.Intents
import io.reactivex.disposables.Disposables

class TransparentActivity : androidx.fragment.app.FragmentActivity() {
  private var dialog = Disposables.disposed()
  private val alarms: IAlarmsManager by globalInject()

  override fun onResume() {
    super.onResume()
    val id = intent.getIntExtra(Intents.EXTRA_ID, -1)
    val alarm = alarms.getAlarm(id)
    if (alarm != null) {
      dialog =
          TimePickerDialogFragment.showTimePicker(supportFragmentManager).subscribe { picked ->
            if (picked.isPresent()) {
              // get alarm again in case it was deleted while popup is showing
              alarms.getAlarm(id)?.snooze(picked.get().hour, picked.get().minute)
            }
            finish()
          }
    }
  }

  override fun onPause() {
    super.onPause()
    dialog.dispose()
  }

  override fun finish() {
    super.finish()
    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
  }

  override fun onBackPressed() {
    super.onBackPressed()
    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
  }
}
