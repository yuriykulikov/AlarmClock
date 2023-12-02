package com.better.alarm.ui.row

import android.view.View
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.TextView
import com.better.alarm.R
import com.better.alarm.data.Layout
import com.better.alarm.ui.view.DigitalClock

/** Created by Yuriy on 05.08.2017. */
class RowHolder(view: View, id: Int, val layout: Layout) {
  val digitalClock: DigitalClock
  val digitalClockContainer: View
  val rowView: View = view
  val onOff: CompoundButton
  val container: View
  val alarmId: Int = id
  val daysOfWeek: TextView
  val label: TextView
  val detailsButton: View
  val detailsImageView: ImageView
  val detailsCheckImageView: ImageView
  val idHasChanged: Boolean

  init {
    digitalClock = find(R.id.list_row_digital_clock) as DigitalClock
    digitalClockContainer = find(R.id.list_row_digital_clock_container)
    onOff = find(R.id.list_row_on_off_switch) as CompoundButton
    container = find(R.id.list_row_on_off_checkbox_container)
    daysOfWeek = find(R.id.list_row_daysOfWeek) as TextView
    label = find(R.id.list_row_label) as TextView
    detailsButton = find(R.id.details_button_container)
    detailsImageView = detailsButton.findViewById(R.id.details_button_textview)
    detailsCheckImageView = detailsButton.findViewById(R.id.details_button_check)
    val prev: RowHolder? = rowView.tag as RowHolder?
    idHasChanged = prev?.alarmId != id
    rowView.tag = this
  }

  private fun find(id: Int): View = rowView.findViewById(id)
}
