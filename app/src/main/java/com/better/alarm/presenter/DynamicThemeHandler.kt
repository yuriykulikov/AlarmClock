package com.better.alarm.presenter

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.better.alarm.R
import com.better.alarm.alert.AlarmAlertFullScreen

class DynamicThemeHandler(context: Context) {
    private val themeKey = "theme"
    private val sp: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    init {
        when (sp.getString(themeKey, "dark")) {
            "light", "dark" -> {
            }
            else -> {
                sp.edit().putString(themeKey, "dark").apply()
            }
        }
    }

    fun defaultTheme(): Int = when (preference()) {
        "light" -> R.style.DefaultLightTheme
        "dark" -> R.style.DefaultDarkTheme
        else -> R.style.DefaultDarkTheme
    }

    private fun preference(): String = sp.getString(themeKey, "dark")

    fun getIdForName(name: String): Int = when {
        preference() == "light" && name.equals(AlarmAlertFullScreen::class.java.name) -> R.style.AlarmAlertFullScreenLightTheme
        preference() == "light" && name.equals(TimePickerDialogFragment::class.java.name) -> R.style.TimePickerDialogFragmentLight
        preference() == "dark" && name.equals(AlarmAlertFullScreen::class.java.name) -> R.style.AlarmAlertFullScreenDarkTheme
        preference() == "dark" && name.equals(TimePickerDialogFragment::class.java.name) -> R.style.TimePickerDialogFragmentDark
        else -> defaultTheme()
    }
}
