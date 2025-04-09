package com.better.alarm.ui.settings

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.better.alarm.R
import com.better.alarm.bootstrap.AlarmApplication
import com.better.alarm.ui.themes.DynamicThemeHandler
import org.koin.android.ext.android.inject

class VisionWakingHelpActivity : AppCompatActivity() {
  private val dynamicThemeHandler: DynamicThemeHandler by inject()
    override fun onCreate(savedInstanceState: Bundle?) {
      AlarmApplication.startOnce(application)
      setTheme(dynamicThemeHandler.defaultTheme())
      super.onCreate(savedInstanceState)
      setContentView(R.layout.activity_vision_waking_help)

      val actionBar = supportActionBar
      actionBar?.setDisplayHomeAsUpEnabled(true)
    }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    if (item.itemId == android.R.id.home) {
      finish()
      return true
    }

    return super.onOptionsItemSelected(item)
  }
}
