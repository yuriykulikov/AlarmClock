package com.better.alarm.ui.row

import android.R
import android.content.res.Resources
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import com.better.alarm.ui.themes.resolveColor

/**
 * Applies colors from themes to certain views of a [RowHolder]. If an alarm is disabled, colors
 * will be greyed out.
 */
class ListRowHighlighter(
    val accentColor: Int,
    val primaryTextColor: Int,
    val disabledColor: Int,
) {
  fun applyTo(row: RowHolder, enabled: Boolean) {
    if (enabled) {
      row.label.setTextColor(accentColor)
      row.daysOfWeek.setTextColor(accentColor)
      row.digitalClock.setColor(primaryTextColor)
      row.detailsImageView.colorFilter =
          PorterDuffColorFilter(primaryTextColor, PorterDuff.Mode.MULTIPLY)
      row.detailsCheckImageView.colorFilter =
          PorterDuffColorFilter(primaryTextColor, PorterDuff.Mode.MULTIPLY)
    } else {
      row.label.setTextColor(disabledColor)
      row.daysOfWeek.setTextColor(disabledColor)
      row.digitalClock.setColor(disabledColor)
      row.detailsImageView.colorFilter =
          PorterDuffColorFilter(disabledColor, PorterDuff.Mode.MULTIPLY)
      row.detailsCheckImageView.colorFilter =
          PorterDuffColorFilter(disabledColor, PorterDuff.Mode.MULTIPLY)
    }
  }

  companion object {
    fun createFor(theme: Resources.Theme): ListRowHighlighter {
      return ListRowHighlighter(
          accentColor = theme.resolveColor(R.attr.colorAccent),
          primaryTextColor = theme.resolveColor(R.attr.colorForeground),
          disabledColor = theme.resolveColor(com.better.alarm.R.attr.listRowDisabledColor))
    }
  }
}
