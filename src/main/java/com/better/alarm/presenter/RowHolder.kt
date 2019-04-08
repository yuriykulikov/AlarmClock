package com.better.alarm.presenter

import android.view.View
import android.widget.CompoundButton
import android.widget.TextView
import com.better.alarm.R
import com.better.alarm.view.DigitalClock

/**
 * Created by Yuriy on 05.08.2017.
 */
class RowHolder {
    val digitalClock: DigitalClock
    private val rowView: View
    private val onOff: CompoundButton
    private val container: View
    private val alarmId: Int
    private val daysOfWeek: TextView
    private val label: TextView
    private val detailsButton: View
    private val idHasChanged: Boolean

    constructor(view: View, id: Int) {
        rowView = view
        alarmId = id
        digitalClock = find(R.id.list_row_digital_clock) as DigitalClock
        onOff = find(R.id.list_row_on_off_switch) as CompoundButton
        container = find(R.id.list_row_on_off_checkbox_container)
        daysOfWeek = find(R.id.list_row_daysOfWeek) as TextView
        label = find(R.id.list_row_label) as TextView
        detailsButton = find(R.id.details_button_container)

        val prev: RowHolder? = rowView.tag as RowHolder?;
        idHasChanged = prev?.alarmId != id

        rowView.setTag(this)
    }

    fun find(id: Int): View = rowView.findViewById(id)

    fun digitalClock(): DigitalClock = digitalClock
    fun rowView(): View = rowView
    fun onOff(): CompoundButton = onOff
    fun alarmId(): Int = alarmId
    fun container() = container
    fun daysOfWeek(): TextView = daysOfWeek
    fun label(): TextView = label
    fun detailsButton(): View = detailsButton
    fun idHasChanged(): Boolean = idHasChanged
}