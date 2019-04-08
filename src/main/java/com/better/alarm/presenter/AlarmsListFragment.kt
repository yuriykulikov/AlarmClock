package com.better.alarm.presenter

import android.app.AlertDialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.*
import android.view.ContextMenu.ContextMenuInfo
import android.widget.AbsListView
import android.widget.AdapterView
import android.widget.AdapterView.AdapterContextMenuInfo
import android.widget.ArrayAdapter
import android.widget.ListView
import com.better.alarm.R
import com.better.alarm.configuration.AlarmApplication.container
import com.better.alarm.lollipop
import com.better.alarm.model.AlarmValue
import com.melnykov.fab.FloatingActionButton
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.disposables.Disposables
import java.util.*


/**
 * Shows a list of alarms. To react on user interaction, requires a strategy. An
 * activity hosting this fragment should provide a proper strategy for single
 * and multi-pane modes.
 *
 * @author Yuriy
 */
class AlarmsListFragment : Fragment() {
    private val alarms = container().alarms()
    private val store = container().store()
    private val uiStore: UiStore by lazy { AlarmsListActivity.uiStore(activity as AlarmsListActivity) }
    private val prefs = container().prefs()
    private val logger = container().logger()

    private val mAdapter: AlarmListAdapter by lazy { AlarmListAdapter(R.layout.list_row, R.string.alarm_list_title, ArrayList()) }

    private var alarmsSub: Disposable = Disposables.disposed()
    private var backSub: Disposable = Disposables.disposed()
    private var timePickerDialogDisposable = Disposables.disposed()

    inner class AlarmListAdapter(alarmTime: Int, label: Int, private val values: List<AlarmValue>) : ArrayAdapter<AlarmValue>(activity, alarmTime, label, values) {

        private fun recycleView(convertView: View?, parent: ViewGroup, id: Int): RowHolder {
            if (convertView != null) return RowHolder(convertView, id)

            val inflater = activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val rowView = inflater.inflate(R.layout.list_row, parent, false)
            return RowHolder(rowView, id).apply {
                digitalClock.setLive(false)
            }
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            // get the alarm which we have to display
            val alarm = values[position]

            val row = recycleView(convertView, parent, alarm.id)

            row.onOff().isChecked = alarm.isEnabled

            lollipop {
                row.digitalClock().transitionName = "clock" + alarm.id
                row.container().transitionName = "onOff" + alarm.id
                row.detailsButton().transitionName = "detailsButton" + alarm.id
            }

            //Delete add, skip animation
            if (row.idHasChanged()) {
                logger.d("Jump to current state")
                row.onOff().jumpDrawablesToCurrentState()
            }

            row.container()
                    //onOff
                    .setOnClickListener {
                        val enable = !alarm.isEnabled
                        logger.d("onClick: " + if (enable) "enable" else "disable")
                        alarms.enable(alarm, enable)
                    }

            row.digitalClock().setOnClickListener {
                timePickerDialogDisposable = TimePickerDialogFragment.showTimePicker(fragmentManager)
                        .subscribe { picked ->
                            if (picked.isPresent()) {
                                alarms.getAlarm(alarm.id)
                                        .edit()
                                        .withIsEnabled(true)
                                        .withHour(picked.get().hour)
                                        .withMinutes(picked.get().minute)
                                        .commit()
                            }
                        }
            }

            // set the alarm text
            val c = Calendar.getInstance()
            c.set(Calendar.HOUR_OF_DAY, alarm.hour)
            c.set(Calendar.MINUTE, alarm.minutes)
            row.digitalClock().updateTime(c)

            // Set the repeat text or leave it blank if it does not repeat.
            val daysOfWeekStr = alarm.daysOfWeek.toString(getContext(), false)
            if (daysOfWeekStr.length != 0) {
                row.daysOfWeek().text = daysOfWeekStr
                row.daysOfWeek().visibility = View.VISIBLE
            } else {
                row.daysOfWeek().visibility = if (lollipop()) View.INVISIBLE else View.GONE
            }

            // Set the repeat text or leave it blank if it does not repeat.
            if (alarm.label != null && !alarm.label.isEmpty()) {
                row.label().text = alarm.label
                row.label().visibility = View.VISIBLE
            } else {
                row.label().visibility = if (lollipop()) View.INVISIBLE else View.GONE
            }

            return row.rowView()
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val info = item.menuInfo as AdapterContextMenuInfo
        val alarm = mAdapter.getItem(info.position)
        when (item.itemId) {
            R.id.delete_alarm -> {
                // Confirm that the alarm will be deleted.
                AlertDialog.Builder(activity).setTitle(getString(R.string.delete_alarm))
                        .setMessage(getString(R.string.delete_alarm_confirm))
                        .setPositiveButton(android.R.string.ok) { d, w -> alarms.delete(alarm) }.setNegativeButton(android.R.string.cancel, null).show()
                return true
            }

            R.id.enable_alarm -> {
                alarms.enable(alarm, !alarm.isEnabled)
                return true
            }

            R.id.edit_alarm -> {
                uiStore.edit(alarm.id)
                return true
            }

            else -> {
                return super.onContextItemSelected(item)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.list_fragment, container, false)

        val listView = view.findViewById(R.id.list_fragment_list) as ListView

        listView.adapter = mAdapter

        listView.isVerticalScrollBarEnabled = false
        listView.setOnCreateContextMenuListener(this)
        listView.choiceMode = AbsListView.CHOICE_MODE_SINGLE

        listView.onItemClickListener = AdapterView.OnItemClickListener { _, listRow, position, _ ->
            // We can display everything in-place with fragments, so update
            // the list to highlight the selected item and show the data.
            //TODO what does this do? listView.setSelection(position);

            val id = mAdapter.getItem(position).id
            uiStore.edit(id, listRow.tag as RowHolder)
        }

        registerForContextMenu(listView)

        setHasOptionsMenu(true)

        val fab: View = view.findViewById(R.id.fab)
        fab.setOnClickListener { uiStore.createNewAlarm() }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            (fab as FloatingActionButton).attachToListView(listView)
        }

        alarmsSub = uiStore.transitioningToNewAlarmDetails()
                .switchMap { transitioning -> if (transitioning) Observable.never() else store.alarms() }
                .subscribe { alarms ->
                    val sorted = alarms
                            .sortedWith(Comparators.MinuteComparator())
                            .sortedWith(Comparators.HourComparator())
                            .sortedWith(Comparators.RepeatComparator())
                    mAdapter.clear()
                    mAdapter.addAll(sorted)
                }

        return view
    }

    override fun onResume() {
        super.onResume()
        backSub = uiStore.onBackPressed().subscribe { activity.finish() }
    }

    override fun onPause() {
        super.onPause()
        backSub.dispose()
        //dismiss the time picker if it was showing. Otherwise we will have to uiStore the state and it is not nice for the user
        timePickerDialogDisposable.dispose()
    }

    override fun onDestroy() {
        super.onDestroy()
        alarmsSub.dispose()
    }

    override fun onCreateContextMenu(menu: ContextMenu, view: View, menuInfo: ContextMenuInfo) {
        // Inflate the menu from xml.
        activity.menuInflater.inflate(R.menu.list_context_menu, menu)

        // Use the current item to create a custom view for the header.
        val info = menuInfo as AdapterContextMenuInfo
        val alarm = mAdapter.getItem(info.position)

        // Construct the Calendar to compute the time.
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, alarm!!.hour)
        cal.set(Calendar.MINUTE, alarm.minutes)

        // Change the text based on the state of the alarm.
        if (alarm.isEnabled) {
            menu.findItem(R.id.enable_alarm).setTitle(R.string.disable_alarm)
        }
    }
}