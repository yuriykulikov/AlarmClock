package com.better.alarm.ui.themes

import android.annotation.TargetApi
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Resources
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.better.alarm.R
import com.better.alarm.data.Prefs
import com.better.alarm.ui.settings.SettingsFragment
import io.reactivex.disposables.CompositeDisposable
import org.koin.android.ext.android.inject

/** Dialog to set alarm time. */
class AppearanceFragment : Fragment() {
  private val dynamicThemeHandler: DynamicThemeHandler by inject()
  private val prefs: Prefs by inject()
  private val disposable: CompositeDisposable = CompositeDisposable()

  @TargetApi(21)
  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.appearance_fragment, container, false).apply {
      findViewById<LinearLayout>(R.id.appearance_row_0).apply {
        removeAllViews()
        // appendDivider(inflater)
        val themes = appearances(resources, dynamicThemeHandler)
        themes.forEachIndexed { index, appearance ->
          appendThemeButton(inflater, appearance)
          if (index != themes.lastIndex) appendDivider(inflater)
        }
      }

      findViewById<LinearLayout>(R.id.appearance_row_layout).apply {
        removeAllViews()
        val layouts = layouts(resources)
        layouts
            .sortedBy { it.perceivedSize }
            .forEachIndexed { index, layout ->
              appendLayoutButton(inflater, layout)
              if (index != layouts.lastIndex) appendDivider(inflater)
            }
      }
    }
  }

  private fun LinearLayout.appendDivider(inflater: LayoutInflater) {
    addView(inflater.inflate(R.layout.appearance_fragment_divider, this, false))
  }

  @TargetApi(21)
  private fun LinearLayout.appendThemeButton(
      inflater: LayoutInflater,
      appearance: AppearanceModel
  ) {
    val button =
        inflater.inflate(R.layout.appearance_fragment_theme_button, this, false).apply {
          setOnClickListener {
            prefs.theme.value = appearance.preferenceName
            applySelectedTheme()
          }

          findViewById<View>(R.id.appearance_fragment_theme_button_container).backgroundTintList =
              ColorStateList.valueOf(appearance.background)
          findViewById<ImageView>(R.id.appearance_fragment_fab_image).backgroundTintList =
              ColorStateList.valueOf(appearance.accent)
          findViewById<TextView>(R.id.appearance_fragment_theme_text).text = appearance.name

          findViewById<ImageButton>(R.id.appearance_fragment_theme_button).apply {
            backgroundTintList = ColorStateList.valueOf(appearance.background)
            val imageTintColor =
                if (prefs.theme.value != appearance.preferenceName) 0 else appearance.textColor
            imageTintList = ColorStateList.valueOf(imageTintColor)
            isClickable = false
          }
        }
    addView(button)
  }

  @TargetApi(21)
  private fun LinearLayout.appendLayoutButton(inflater: LayoutInflater, layout: LayoutModel) {
    val button =
        inflater.inflate(R.layout.appearance_fragment_layout_button, this, false).apply {
          val size =
              when (layout.preferenceName) {
                "classic" -> 13f
                "compact" -> 17f
                else -> 20f
              }
          findViewById<TextView>(R.id.appearance_fragment_layout_text)
              .setTextSize(TypedValue.COMPLEX_UNIT_DIP, size)

          val button = findViewById<ImageView>(R.id.appearance_fragment_layout_check_button)

          disposable.add(
              prefs.listRowLayout.observe().subscribe { value ->
                val selected = value == layout.preferenceName
                button.imageAlpha = (if (selected) 0xFF else 0)
              })

          button.isClickable = false
          setOnClickListener {
            button.callOnClick()
            prefs.listRowLayout.value = layout.preferenceName
          }
        }
    addView(button)
  }

  private fun applySelectedTheme() {
    Handler(Looper.getMainLooper()).post {
      requireActivity()
          .packageManager
          .getLaunchIntentForPackage(requireActivity().packageName)
          ?.apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("reason", SettingsFragment.themeChangeReason)
          }
          ?.let { startActivity(it) }
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    disposable.dispose()
  }
}

data class AppearanceModel(
    val name: String,
    val preferenceName: String,
    val background: Int,
    val accent: Int,
    val surface: Int,
    val textColor: Int,
)

fun appearances(resources: Resources, themeHandler: DynamicThemeHandler): List<AppearanceModel> {
  val values = resources.getStringArray(R.array.themes_values)

  return resources.getStringArray(R.array.themes_entries).mapIndexed { index, entry ->
    val theme =
        resources.newTheme().apply { applyStyle(themeHandler.defaultTheme(values[index]), true) }
    AppearanceModel(
        name = entry,
        preferenceName = values[index],
        background = theme.resolveColor(android.R.attr.windowBackground),
        accent = theme.resolveColor(R.attr.listFabColor),
        textColor = theme.resolveColor(android.R.attr.colorForeground),
        surface = theme.resolveColor(R.attr.drawerBackgroundColor),
    )
  }
}

data class LayoutModel(
    val name: String,
    val preferenceName: String,
) {
  val perceivedSize: Int =
      when (preferenceName) {
        "classic" -> 1
        "compact" -> 2
        "bold" -> 3
        else -> 4
      }
}

fun layouts(resources: Resources): List<LayoutModel> {
  return resources
      .getStringArray(R.array.preference_list_row_layout_entries)
      .zip(
          other = resources.getStringArray(R.array.preference_list_row_layout_values),
          transform = { name, preferenceName -> LayoutModel(name, preferenceName) })
}
