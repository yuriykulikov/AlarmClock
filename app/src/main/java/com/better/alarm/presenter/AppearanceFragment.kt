package com.better.alarm.presenter

import android.annotation.TargetApi
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Resources
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import com.better.alarm.R
import com.better.alarm.configuration.Prefs
import com.better.alarm.configuration.globalInject


/**
 * Dialog to set alarm time.
 */
class AppearanceFragment : Fragment() {
    private val dynamicThemeHandler: DynamicThemeHandler by globalInject()
    private val prefs: Prefs by globalInject()
    private val store: UiStore by globalInject()

    @TargetApi(21)
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.appearance_fragment, container, false).apply {
            val themes = appearances(resources, dynamicThemeHandler)

            findViewById<LinearLayout>(R.id.appearance_row_0).apply {
                appendDivider(inflater)
                themes.forEach { appearance ->
                    appendThemeButton(inflater, appearance)
                    appendDivider(inflater)
                }
            }

            val arrowUp = findViewById<ImageButton>(R.id.appearance_arrow_up)
            val arrowDown = findViewById<ImageButton>(R.id.appearance_arrow_down)

            arrowUp.setOnClickListener {
                when (prefs.listRowLayout.value) {
                    "classic" -> prefs.listRowLayout.value = "compact"
                    "compact" -> prefs.listRowLayout.value = "bold"
                }
            }

            arrowDown.setOnClickListener {
                when (prefs.listRowLayout.value) {
                    "bold" -> prefs.listRowLayout.value = "compact"
                    "compact" -> prefs.listRowLayout.value = "classic"
                }
            }
        }
    }

    private fun LinearLayout.appendDivider(inflater: LayoutInflater) {
        addView(inflater.inflate(R.layout.appearance_fragment_divider, this, false))
    }

    @TargetApi(21)
    private fun LinearLayout.appendThemeButton(inflater: LayoutInflater, appearance: AppearanceModel) {
        val button = inflater.inflate(R.layout.appearance_fragment_theme_button, this, false).apply {
            findViewById<View>(R.id.appearance_fragment_theme_button_container).apply {
                backgroundTintList = ColorStateList.valueOf(appearance.accent)
            }
            findViewById<ImageButton>(R.id.appearance_fragment_theme_button)
                .apply {
                    backgroundTintList = ColorStateList.valueOf(appearance.background)
                    val imageTintColor = if (prefs.theme.value != appearance.preferenceName) 0 else appearance.textColor
                    imageTintList = ColorStateList.valueOf(imageTintColor)
                    setOnClickListener {
                        prefs.theme.value = appearance.preferenceName
                        applySelectedTheme()
                    }
                }
        }
        addView(button)
    }

    private fun applySelectedTheme() {
        Handler().post {
            val intent = requireActivity()
                .packageManager
                .getLaunchIntentForPackage(requireActivity().packageName)
                ?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    putExtra("reason", SettingsFragment.themeChangeReason)
                }
            startActivity(intent)
        }
    }
}

data class AppearanceModel(
    val name: String,
    val preferenceName: String,
    val background: Int,
    val accent: Int,
    val textColor: Int,
)

fun appearances(resources: Resources, themeHandler: DynamicThemeHandler): List<AppearanceModel> {
    val values = resources.getStringArray(R.array.themes_values)

    return resources.getStringArray(R.array.themes_entries)
        .mapIndexed { index, entry ->
            val theme = resources.newTheme().apply {
                applyStyle(themeHandler.defaultTheme(values[index]), true)
            }
            AppearanceModel(
                name = entry,
                preferenceName = values[index],
                background = theme.resolveColor(android.R.attr.windowBackground),
                accent = theme.resolveColor(R.attr.listFabColor),
                textColor = theme.resolveColor(android.R.attr.colorForeground)
            )
        }
}
