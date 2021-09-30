package com.better.alarm.presenter

import android.animation.ArgbEvaluator
import android.annotation.TargetApi
import android.app.AlertDialog
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.ContextMenu
import android.view.ContextMenu.ContextMenuInfo
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.AdapterView
import android.widget.AdapterView.AdapterContextMenuInfo
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.fragment.app.Fragment
import com.better.alarm.R
import com.better.alarm.configuration.Layout
import com.better.alarm.configuration.Prefs
import com.better.alarm.configuration.Store
import com.better.alarm.configuration.globalInject
import com.better.alarm.configuration.globalLogger
import com.better.alarm.interfaces.IAlarmsManager
import com.better.alarm.logger.Logger
import com.better.alarm.lollipop
import com.better.alarm.model.AlarmValue
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.melnykov.fab.FloatingActionButton
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.disposables.Disposables
import java.util.ArrayList
import java.util.Calendar

/**
 * Shows a list of alarms. To react on user interaction, requires a strategy. An activity hosting
 * this fragment should provide a proper strategy for single and multi-pane modes.
 *
 * @author Yuriy
 */
class AlarmsListFragment : Fragment() {
  private val alarms: IAlarmsManager by globalInject()
  private val store: Store by globalInject()
  private val uiStore: UiStore by globalInject()
  private val prefs: Prefs by globalInject()
  private val logger: Logger by globalLogger("AlarmsListFragment")

  private val mAdapter: AlarmListAdapter by lazy {
    AlarmListAdapter(R.layout.list_row_classic, R.string.alarm_list_title, ArrayList())
  }
  private val inflater: LayoutInflater by lazy {
    requireActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
  }

  private var alarmsSub: Disposable = Disposables.disposed()
  private var layoutSub: Disposable = Disposables.disposed()
  private var backSub: Disposable = Disposables.disposed()
  private var timePickerDialogDisposable = Disposables.disposed()

  /** changed by [Prefs.listRowLayout] in [onResume] */
  private var listRowLayoutId = R.layout.list_row_classic

  /** changed by [Prefs.listRowLayout] in [onResume] */
  private var listRowLayout = prefs.layout()

  inner class AlarmListAdapter(alarmTime: Int, label: Int, private val values: List<AlarmValue>) :
      ArrayAdapter<AlarmValue>(requireContext(), alarmTime, label, values) {
    private val highlighter: ListRowHighlighter? by lazy {
      ListRowHighlighter.createFor(requireActivity().theme)
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
      val alarm = values[position]

      val row = recycleView(convertView, parent, alarm.id)

      row.onOff.isChecked = alarm.isEnabled

      lollipop {
        row.digitalClock.transitionName = "clock" + alarm.id
        row.container.transitionName = "onOff" + alarm.id
        row.detailsButton.transitionName = "detailsButton" + alarm.id
      }

      // Delete add, skip animation
      if (row.idHasChanged) {
        row.onOff.jumpDrawablesToCurrentState()
      }

      row.container
          // onOff
          .setOnClickListener {
        val enable = !alarm.isEnabled
        logger.debug { "onClick: ${if (enable) "enable" else "disable"}" }
        alarms.getAlarm(alarm.id)?.enable(enable)
      }

      val pickerClickTarget =
          with(row) { if (layout == Layout.CLASSIC) digitalClockContainer else digitalClock }
      pickerClickTarget.setOnClickListener {
        timePickerDialogDisposable =
            TimePickerDialogFragment.showTimePicker(parentFragmentManager).subscribe { picked ->
              if (picked.isPresent()) {
                alarms.getAlarm(alarm.id)?.also { alarm ->
                  alarm.edit {
                    copy(isEnabled = true, hour = picked.get().hour, minutes = picked.get().minute)
                  }
                }
              }
            }
      }

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

      highlighter?.applyTo(row, alarm.isEnabled)

      // row.labelsContainer.visibility = when {
      //     row.label().visibility == View.GONE && row.daysOfWeek().visibility == View.GONE -> GONE
      //     else -> View.VISIBLE
      // }

      return row.rowView
    }

    private fun daysOfWeekStringWithSkip(alarm: AlarmValue): String {
      val daysOfWeekStr = alarm.daysOfWeek.toString(context, false)
      return if (alarm.skipping) "$daysOfWeekStr (skipping)" else daysOfWeekStr
    }
  }

  override fun onContextItemSelected(item: MenuItem): Boolean {
    val info = item.menuInfo as AdapterContextMenuInfo
    val alarm = mAdapter.getItem(info.position) ?: return false
    when (item.itemId) {
      R.id.delete_alarm -> {
        // Confirm that the alarm will be deleted.
        AlertDialog.Builder(activity)
            .setTitle(getString(R.string.delete_alarm))
            .setMessage(getString(R.string.delete_alarm_confirm))
            .setPositiveButton(android.R.string.ok) { _, _ -> alarms.getAlarm(alarm.id)?.delete() }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
      }
      R.id.list_context_enable -> {
        alarms.getAlarm(alarmId = alarm.id)?.run { edit { copy(isEnabled = true) } }
      }
      R.id.list_context_menu_disable -> {
        alarms.getAlarm(alarmId = alarm.id)?.run { edit { copy(isEnabled = false) } }
      }
      R.id.skip_alarm -> {
        alarms.getAlarm(alarmId = alarm.id)?.run {
          if (isSkipping()) {
            // removes the skip
            edit { this }
          } else {
            requestSkip()
          }
        }
      }
    }
    return true
  }

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    val view = inflater.inflate(R.layout.list_fragment, container, false)

    val listView = view.findViewById(R.id.list_fragment_list) as ListView

    listView.adapter = mAdapter

    listView.isVerticalScrollBarEnabled = false
    listView.setOnCreateContextMenuListener(this)
    listView.choiceMode = AbsListView.CHOICE_MODE_SINGLE

    listView.onItemClickListener =
        AdapterView.OnItemClickListener { _, listRow, position, _ ->
          mAdapter.getItem(position)?.id?.let { uiStore.edit(it, listRow.tag as RowHolder) }
        }

    registerForContextMenu(listView)

    setHasOptionsMenu(true)

    val fab: View = view.findViewById(R.id.fab)
    fab.setOnClickListener { uiStore.createNewAlarm() }

    lollipop { (fab as FloatingActionButton).attachToListView(listView) }

    alarmsSub =
        prefs
            .listRowLayout
            .observe()
            .switchMap { uiStore.transitioningToNewAlarmDetails() }
            .switchMap { transitioning ->
              if (transitioning) Observable.never() else store.alarms()
            }
            .subscribe { alarms ->
              val sorted =
                  alarms //
                      .sortedBy { it.minutes }
                      .sortedBy { it.hour }
                      .sortedBy {
                        when (it.daysOfWeek.coded) {
                          0x7F -> 1
                          0x1F -> 2
                          0x60 -> 3
                          else -> 0
                        }
                      }
              mAdapter.clear()
              mAdapter.addAll(sorted)
            }

    lollipop { configureBottomDrawer(view) }

    return view
  }

  @TargetApi(21)
  private fun configureBottomDrawer(view: View) {
    val drawerContainer: View = view.findViewById<View>(R.id.bottom_drawer_container)
    val bottomDrawerToolbar = view.findViewById<View>(R.id.bottom_drawer_toolbar)
    val bottomDrawerContent = view.findViewById<View>(R.id.bottom_drawer_content)
    val fab = view.findViewById<FloatingActionButton>(R.id.fab)
    val infoFragment = view.findViewById<View>(R.id.list_activity_info_fragment)

    fun setDrawerBackgrounds(resolveColor: Int) {
      val colorDrawable = ColorDrawable(resolveColor)
      bottomDrawerToolbar.background = colorDrawable
      bottomDrawerContent.background = colorDrawable
      drawerContainer.background = colorDrawable
    }

    val openColor = requireActivity().theme.resolveColor(R.attr.drawerBackgroundColor)
    val closedColor = requireActivity().theme.resolveColor(R.attr.drawerClosedBackgroundColor)

    BottomSheetBehavior.from(drawerContainer).apply {
      val initialElevation = drawerContainer.elevation
      val initialFabElevation = fab.elevation
      val fabAtOverlap = 3f
      // offset of about 0.1 means overlap
      val overlap = 0.1f
      val fabK = -((initialFabElevation - fabAtOverlap) / overlap)

      peekHeight = bottomDrawerToolbar.minimumHeight
      if (uiStore.openDrawerOnCreate) {
        state = BottomSheetBehavior.STATE_EXPANDED
        // reset the flag after opening the drawer
        uiStore.openDrawerOnCreate = false
        setDrawerBackgrounds(openColor)
      } else {
        setDrawerBackgrounds(closedColor)
        drawerContainer.elevation = 0f
      }

      bottomDrawerToolbar.setOnClickListener {
        state =
            when (state) {
              BottomSheetBehavior.STATE_COLLAPSED -> BottomSheetBehavior.STATE_EXPANDED
              else -> BottomSheetBehavior.STATE_COLLAPSED
            }
      }

      addBottomSheetCallback(
          object : BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
              if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                setDrawerBackgrounds(openColor)
              } else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                setDrawerBackgrounds(closedColor)
              }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
              val color =
                  ArgbEvaluator()
                      .evaluate(
                          slideOffset,
                          closedColor,
                          openColor,
                      ) as
                      Int
              setDrawerBackgrounds(color)

              drawerContainer.elevation = initialElevation * slideOffset
              if (slideOffset > overlap) {
                fab.elevation = fabAtOverlap
              } else {
                fab.elevation = fabK * slideOffset + initialFabElevation
              }
              // hide the info when drawer is open
              infoFragment.alpha = 1.0f - slideOffset
            }
          })
    }
  }

  override fun onResume() {
    super.onResume()
    backSub = uiStore.onBackPressed().subscribe { requireActivity().finish() }
    layoutSub =
        prefs.listRowLayout.observe().subscribe {
          listRowLayout = prefs.layout()
          listRowLayoutId =
              when (listRowLayout) {
                Layout.COMPACT -> R.layout.list_row_compact
                Layout.CLASSIC -> R.layout.list_row_classic
                else -> R.layout.list_row_bold
              }
        }
  }

  override fun onPause() {
    super.onPause()
    backSub.dispose()
    layoutSub.dispose()
    // dismiss the time picker if it was showing. Otherwise we will have to uiStore the state and it
    // is not nice for the user
    timePickerDialogDisposable.dispose()
  }

  override fun onDestroy() {
    super.onDestroy()
    alarmsSub.dispose()
  }

  override fun onCreateContextMenu(menu: ContextMenu, view: View, menuInfo: ContextMenuInfo?) {
    // Inflate the menu from xml.
    requireActivity().menuInflater.inflate(R.menu.list_context_menu, menu)

    // Use the current item to create a custom view for the header.
    val info = menuInfo as AdapterContextMenuInfo
    val alarm = mAdapter.getItem(info.position)

    // Construct the Calendar to compute the time.
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, alarm!!.hour)
    cal.set(Calendar.MINUTE, alarm.minutes)

    val visible =
        when {
          alarm.isEnabled ->
              when {
                alarm.skipping -> listOf(R.id.list_context_enable)
                alarm.daysOfWeek.isRepeatSet -> listOf(R.id.skip_alarm)
                else -> listOf(R.id.list_context_menu_disable)
              }
          // disabled
          else -> listOf(R.id.list_context_enable)
        }

    listOf(R.id.list_context_enable, R.id.list_context_menu_disable, R.id.skip_alarm)
        .minus(visible)
        .forEach { menu.removeItem(it) }
  }
}
