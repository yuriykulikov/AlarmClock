package com.better.alarm.ui.themes

import com.better.alarm.R
import com.better.alarm.data.Prefs
import com.better.alarm.data.stores.modify

class DynamicThemeHandler(
    private val prefs: Prefs,
) {
  private val light = "light"
  private val dark = "dark"
  private val synthwave = "synthwave"
  private val deusex = "deusex"
  private val deepblue = "deepblue"

  private val themes: Map<String, List<Int>> =
      mapOf(
          light to
              listOf(
                  R.style.DefaultLightTheme,
                  R.style.AlarmAlertFullScreenLightTheme,
                  R.style.TimePickerDialogFragmentLight,
              ),
          dark to
              listOf(
                  R.style.DefaultDarkTheme,
                  R.style.AlarmAlertFullScreenDarkTheme,
                  R.style.TimePickerDialogFragmentDark,
              ),
          synthwave to
              listOf(
                  R.style.DefaultSynthwaveTheme,
                  R.style.AlarmAlertFullScreenSynthwaveTheme,
                  R.style.TimePickerDialogFragmentSynthwave,
              ),
          deusex to
              listOf(
                  R.style.DefaultDeusExTheme,
                  R.style.AlarmAlertFullScreenDeusExTheme,
                  R.style.TimePickerDialogFragmentDeusEx,
              ),
          deepblue to
              listOf(
                  R.style.DefaultDeepBlueTheme,
                  R.style.AlarmAlertFullScreenDeepBlueTheme,
                  R.style.TimePickerDialogFragmentDeepBlue,
              ),
      )

  init {
    prefs.theme.modify { currentTheme ->
      when (currentTheme) {
        in themes.keys -> currentTheme
        else -> dark
      }
    }
  }

  @JvmOverloads
  fun defaultTheme(theme: String? = null): Int = themes.getValue(theme ?: prefs.theme.value)[0]

  fun alertTheme(): Int = themes.getValue(prefs.theme.value)[1]

  fun pickerTheme(): Int = themes.getValue(prefs.theme.value)[2]
}
