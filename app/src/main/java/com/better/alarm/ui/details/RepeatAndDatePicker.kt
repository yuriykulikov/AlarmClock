package com.better.alarm.ui.details

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.DatePicker
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.better.alarm.R
import com.better.alarm.data.AlarmValue
import com.better.alarm.data.DaysOfWeek
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import io.reactivex.Single
import java.text.DateFormatSymbols
import java.util.*
import java.util.Calendar.DAY_OF_MONTH
import java.util.Calendar.FRIDAY
import java.util.Calendar.MONDAY
import java.util.Calendar.MONTH
import java.util.Calendar.SATURDAY
import java.util.Calendar.SUNDAY
import java.util.Calendar.THURSDAY
import java.util.Calendar.TUESDAY
import java.util.Calendar.WEDNESDAY
import java.util.Calendar.YEAR
import java.util.Calendar.getInstance
import java.util.concurrent.atomic.AtomicReference
import kotlin.properties.Delegates

private class Holder(val view: View) : RecyclerView.ViewHolder(view)

/** Shows a dialog with tabs "Repeat" and "Date" allowing the user to switch between these modes. */
fun Context.showRepeatAndDateDialog(repeatAndDate: AlarmValue): Single<AlarmValue> {
  return Single.create { emitter ->
    var resultSupplier: () -> AlarmValue by Delegates.notNull()
    resultSupplier =
        AlertDialog.Builder(this)
            .setView(R.layout.repeat_picker)
            .setPositiveButton(android.R.string.ok) { _, _ -> emitter.onSuccess(resultSupplier()) }
            .setOnCancelListener { emitter.onSuccess(repeatAndDate) }
            .create()
            .apply { show() }
            .bindView(this, repeatAndDate)
  }
}

/**
 * Attaches [ViewPager2] to [TabLayout] and binds child views. Returns a supplier of resulting
 * [DaysOfWeek]
 */
private fun AlertDialog.bindView(context: Context, initial: AlarmValue): () -> AlarmValue {
  val viewPager = requireNotNull(findViewById<ViewPager2>(R.id.repeat_and_date_picker_pager))
  val repeatAdapter = RepeatAdapter(layoutInflater, initial.daysOfWeek)
  val selectedDate = AtomicReference(initial.date ?: getInstance())

  viewPager.adapter =
      PickersAdapter(
          layoutInflater,
          bindRepeatPicker = { picker ->
            picker.adapter = repeatAdapter
            picker.layoutManager = LinearLayoutManager(context)
          },
          bindDatePicker = { picker -> picker.bindDatePicker(selectedDate) })

  val tabLayout = requireNotNull(findViewById<TabLayout>(R.id.repeat_and_date_picker_tabs))
  TabLayoutMediator(tabLayout, viewPager) { tab, position ->
        tab.text =
            when (position) {
              PickersAdapter.datePosition -> context.getText(R.string.date)
              else -> context.getText(R.string.alarm_repeat)
            }
      }
      .attach()

  tabLayout.selectTab(tabLayout.getTabAt(if (initial.date != null) 1 else 0))
  return {
    initial.copy(
        daysOfWeek = repeatAdapter.repeatDays,
        date = if (tabLayout.selectedTabPosition == 1) selectedDate.get() else null,
    )
  }
}

private fun DatePicker.bindDatePicker(selectedDate: AtomicReference<Calendar>) {
  val current = selectedDate.get()
  init(current.get(YEAR), current.get(MONTH), current.get(DAY_OF_MONTH)) { _, year, month, day ->
    selectedDate.set(
        Calendar.getInstance().apply {
          set(YEAR, year)
          set(MONTH, month)
          set(DAY_OF_MONTH, day)
        })
  }
}

private class PickersAdapter(
    val layoutInflater: LayoutInflater,
    val bindRepeatPicker: (RecyclerView) -> Unit,
    val bindDatePicker: (DatePicker) -> Unit,
) : RecyclerView.Adapter<Holder>() {
  companion object {
    const val datePosition = 1
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
    return Holder(layoutInflater.inflate(R.layout.repeat_picker_page, parent, false))
  }

  override fun onBindViewHolder(holder: Holder, position: Int) {
    val datePicker = holder.view.findViewById<DatePicker>(R.id.repeat_picker_date)
    val repeatPicker = holder.view.findViewById<View>(R.id.repeat_picker_repeat)
    if (position == datePosition) {
      repeatPicker.visibility = View.GONE
      datePicker.visibility = View.VISIBLE
      bindDatePicker(datePicker)
    } else {
      datePicker.visibility = View.GONE
      repeatPicker.visibility = View.VISIBLE
      bindRepeatPicker(repeatPicker.findViewById(R.id.repeat_picker_repeat_list))
    }
  }

  override fun getItemCount(): Int {
    return 2
  }
}

private class RepeatAdapter(
    private val layoutInflater: LayoutInflater,
    var repeatDays: DaysOfWeek,
) : RecyclerView.Adapter<Holder>() {
  private val dayNames = dayNames()

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
    return Holder(layoutInflater.inflate(R.layout.repeat_picker_repeat_list_row, parent, false))
  }

  override fun onBindViewHolder(holder: Holder, position: Int) {
    val cb = holder.view.findViewById<CheckBox>(R.id.repeat_picker_repeat_list_row_checkbox)
    cb.text = dayNames[position]
    cb.isChecked = repeatDays.isDaySet(position)
    cb.setOnCheckedChangeListener { _, isChecked ->
      repeatDays =
          when {
            isChecked -> repeatDays.withSet(position)
            else -> repeatDays.withCleared(position)
          }
    }
    holder.view.setOnClickListener { cb.callOnClick() }
  }

  override fun getItemCount(): Int {
    return 7
  }

  private fun dayNames(): Array<String> {
    val weekdays = DateFormatSymbols().weekdays
    return arrayOf(
        weekdays[MONDAY],
        weekdays[TUESDAY],
        weekdays[WEDNESDAY],
        weekdays[THURSDAY],
        weekdays[FRIDAY],
        weekdays[SATURDAY],
        weekdays[SUNDAY])
  }
}
