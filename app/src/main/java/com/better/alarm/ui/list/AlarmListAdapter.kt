package com.better.alarm.ui.list

import android.content.Context
import android.view.LayoutInflater
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.better.alarm.R
import com.better.alarm.data.AlarmValue
import com.better.alarm.data.Layout
import com.better.alarm.logger.Logger
import com.better.alarm.ui.row.ListRowHighlighter
import com.better.alarm.ui.row.RowHolder
import java.text.SimpleDateFormat
import java.util.Calendar

class AlarmListAdapter(
    private val context: Context,
    private val highlighter: ListRowHighlighter,
    private val logger: Logger,
    private val listRowLayout: Layout,
    private val showDetails: (alarm: AlarmValue, holder: RowHolder) -> Unit,
    private val changeAlarm: (id: Int, enable: Boolean) -> Unit,
    private val showPicker: (alarm: AlarmValue) -> Unit,
) :
    ListAdapter<AlarmValue, RowHolder>(
        object : DiffUtil.ItemCallback<AlarmValue>() {
          override fun areItemsTheSame(oldItem: AlarmValue, newItem: AlarmValue): Boolean {
            return oldItem.id == newItem.id
          }

          override fun areContentsTheSame(oldItem: AlarmValue, newItem: AlarmValue): Boolean {
            return oldItem == newItem
          }
        }) {
  private val holders = mutableMapOf<Int, RowHolder>()
  private val inflater: LayoutInflater = LayoutInflater.from(context)

  var dataset: List<AlarmValue> = emptyList()
    set(value) {
      field = value
      submitList(value)
    }

  private val listRowLayoutId: Int
    get() =
        when (listRowLayout) {
          Layout.CLASSIC -> R.layout.list_row_classic
          Layout.COMPACT -> R.layout.list_row_compact
          Layout.BOLD -> R.layout.list_row_bold
        }

  var contextMenuAlarm: AlarmValue? = null
    private set

  override fun getItemCount(): Int {
    return dataset.size
  }

  override fun onCreateViewHolder(p0: ViewGroup, p1: Int): RowHolder {
    return RowHolder(inflater.inflate(listRowLayoutId, p0, false), p1, listRowLayout)
  }

  override fun onBindViewHolder(row: RowHolder, position: Int) {
    // get the alarm which we have to display
    val alarm = dataset[position]

    logger.trace { "getView($position) $alarm" }

    row.onOff.isChecked = alarm.isEnabled

    row.digitalClock.transitionName = "clock" + alarm.id
    row.container.transitionName = "onOff" + alarm.id
    row.container.tag = "onOff" + alarm.id
    row.detailsButton.transitionName = "detailsButton" + alarm.id
    row.daysOfWeek.transitionName = "daysOfWeek" + alarm.id
    row.label.transitionName = "label" + alarm.id

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
    row.digitalClock.setLive(false)
    row.digitalClock.updateTime(c)

    // Set the repeat text or leave it blank if it does not repeat.

    row.daysOfWeek.run {
      text = daysOfWeekStringWithSkip(alarm)
      visibility = if (text.isNotEmpty()) View.VISIBLE else View.INVISIBLE
    }

    // Set the repeat text or leave it blank if it does not repeat.
    row.label.text = alarm.label

    row.label.visibility = if (alarm.label.isNotBlank()) View.VISIBLE else View.INVISIBLE

    highlighter.applyTo(row, alarm.isEnabled)
    row.rowView.setOnCreateContextMenuListener { menu, _, _ ->
      contextMenuAlarm = alarm

      // Inflate the menu from xml.
      MenuInflater(context).inflate(R.menu.list_context_menu, menu)

      val visible =
          when {
            alarm.isEnabled ->
                when {
                  alarm.skipping -> listOf(R.id.list_context_enable)
                  alarm.isRepeatSet -> listOf(R.id.skip_alarm)
                  else -> listOf(R.id.list_context_menu_disable)
                }
            // disabled
            else -> listOf(R.id.list_context_enable)
          }
      listOf(R.id.list_context_enable, R.id.list_context_menu_disable, R.id.skip_alarm)
          .minus(visible)
          .forEach { menu.removeItem(it) }
    }

    row.rowView.setOnClickListener { showDetails(alarm, row) }

    holders[alarm.id] = row
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
