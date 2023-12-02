package com.better.alarm.ui.list

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.better.alarm.R
import com.better.alarm.data.AlarmValue
import com.better.alarm.data.Layout
import com.better.alarm.logger.Logger
import com.better.alarm.ui.row.ListRowHighlighter
import com.better.alarm.ui.row.RowHolder
import java.text.SimpleDateFormat
import java.util.*

class AlarmListAdapter(
    context: Context,
    private val highlighter: ListRowHighlighter,
    private val logger: Logger,
    private val changeAlarm: (id: Int, enable: Boolean) -> Unit,
    private val showPicker: (alarm: AlarmValue) -> Unit,
) : ArrayAdapter<AlarmValue>(context, android.R.layout.simple_list_item_1) {
  private val inflater: LayoutInflater = LayoutInflater.from(context)
  var listRowLayout: Layout = Layout.CLASSIC
    set(value) {
      field = value
      notifyDataSetChanged()
    }

  var dataset: List<AlarmValue> = emptyList()
    set(value) {
      field = value
      clear()
      addAll(value)
      notifyDataSetChanged()
    }

  private val listRowLayoutId: Int
    get() =
        when (listRowLayout) {
          Layout.CLASSIC -> R.layout.list_row_classic
          Layout.COMPACT -> R.layout.list_row_compact
          Layout.BOLD -> R.layout.list_row_bold
        }

  private fun recycleView(convertView: View?, parent: ViewGroup, id: Int): RowHolder {
    val tag = convertView?.tag
    return when {
      tag is RowHolder && tag.layout == listRowLayout -> RowHolder(convertView, id, listRowLayout)
      else -> {
        val rowView = inflater.inflate(listRowLayoutId, parent, false)
        RowHolder(rowView, id, listRowLayout).apply { digitalClock.setLive(false) }
      }
    }
  }

  override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
    // get the alarm which we have to display
    val alarm = dataset[position]

    logger.trace { "getView($position) $alarm" }

    val row = recycleView(convertView, parent, alarm.id)

    row.onOff.isChecked = alarm.isEnabled

    row.digitalClock.transitionName = "clock" + alarm.id
    row.container.transitionName = "onOff" + alarm.id
    row.detailsButton.transitionName = "detailsButton" + alarm.id

    // Delete add, skip animation
    if (row.idHasChanged) {
      row.onOff.jumpDrawablesToCurrentState()
    }

    row.container
        // onOff
        .setOnClickListener {
          val enable = !alarm.isEnabled
          logger.debug { "onClick: ${if (enable) "enable" else "disable"}" }
          changeAlarm(alarm.id, enable)
        }

    val pickerClickTarget =
        with(row) { if (layout == Layout.CLASSIC) digitalClockContainer else digitalClock }
    pickerClickTarget.setOnClickListener { showPicker(alarm) }

    pickerClickTarget.setOnLongClickListener { false }

    // set the alarm text
    val c = Calendar.getInstance()
    c.set(Calendar.HOUR_OF_DAY, alarm.hour)
    c.set(Calendar.MINUTE, alarm.minutes)
    row.digitalClock.updateTime(c)

    val removeEmptyView = listRowLayout == Layout.CLASSIC || listRowLayout == Layout.COMPACT
    // Set the repeat text or leave it blank if it does not repeat.

    row.daysOfWeek.run {
      text = daysOfWeekStringWithSkip(alarm)
      visibility =
          when {
            text.isNotEmpty() -> View.VISIBLE
            removeEmptyView -> View.GONE
            else -> View.INVISIBLE
          }
    }

    // Set the repeat text or leave it blank if it does not repeat.
    row.label.text = alarm.label

    row.label.visibility =
        when {
          alarm.label.isNotBlank() -> View.VISIBLE
          removeEmptyView -> View.GONE
          else -> View.INVISIBLE
        }

    highlighter.applyTo(row, alarm.isEnabled)

    return row.rowView
  }

  private fun daysOfWeekStringWithSkip(alarm: AlarmValue): String {
    val daysOfWeekStr = alarm.daysOfWeek.toString(context, false)
    return when {
      alarm.date != null -> SimpleDateFormat.getDateInstance().format(alarm.date.time)
      alarm.skipping -> "$daysOfWeekStr (skipping)"
      else -> daysOfWeekStr
    }
  }
}
