package com.better.alarm.ui.list

import android.animation.ArgbEvaluator
import android.app.AlertDialog
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
import android.widget.ListView
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import com.better.alarm.R
import com.better.alarm.bootstrap.globalLogger
import com.better.alarm.data.Prefs
import com.better.alarm.domain.IAlarmsManager
import com.better.alarm.domain.Store
import com.better.alarm.logger.Logger
import com.better.alarm.ui.row.ListRowHighlighter
import com.better.alarm.ui.row.RowHolder
import com.better.alarm.ui.themes.resolveColor
import com.better.alarm.ui.timepicker.TimePickerDialogFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.melnykov.fab.FloatingActionButton
import io.reactivex.disposables.Disposable
import io.reactivex.disposables.Disposables
import kotlinx.coroutines.channels.Channel
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Shows a list of alarms. To react on user interaction, requires a strategy. An activity hosting
 * this fragment should provide a proper strategy for single and multi-pane modes.
 *
 * @author Yuriy
 */
class AlarmsListFragment : Fragment() {
  @Deprecated("Use viewModel instead") private val alarms: IAlarmsManager by inject()
  @Deprecated("Use viewModel instead") private val store: Store by inject()
  private val listViewModel: ListViewModel by viewModel()
  @Deprecated("Use viewModel instead") private val prefs: Prefs by inject()
  private val logger: Logger by globalLogger("AlarmsListFragment")
  var transitionRowHolder: RowHolder? = null
    private set

  private val mAdapter: AlarmListAdapter by lazy {
    AlarmListAdapter(
        requireContext(),
        highlighter = ListRowHighlighter.createFor(requireContext().theme),
        logger = logger,
        changeAlarm = { id, enable ->
          alarms.getAlarm(alarmId = id)?.edit { copy(isEnabled = enable) }
        },
        showPicker = { alarm ->
          timePickerDialogDisposable =
              TimePickerDialogFragment.showTimePicker(parentFragmentManager).subscribe { picked ->
                if (picked.isPresent()) {
                  alarms.getAlarm(alarm.id)?.also { alarm ->
                    alarm.edit {
                      copy(
                          isEnabled = true, hour = picked.get().hour, minutes = picked.get().minute)
                    }
                  }
                }
              }
        })
  }

  private var alarmsSub: Disposable = Disposables.disposed()
  private var layoutSub: Disposable = Disposables.disposed()
  private var timePickerDialogDisposable = Disposables.disposed()

  companion object {
    var fabSync: Channel<Unit>? = null
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
          transitionRowHolder = listRow.tag as RowHolder
          mAdapter.getItem(position)?.let { listViewModel.edit(it) }
        }

    registerForContextMenu(listView)

    setHasOptionsMenu(true)

    val fab: View = view.findViewById(R.id.fab)
    fab.setOnClickListener {
      listViewModel.createNewAlarm()
      fabSync?.trySend(Unit)?.getOrThrow()
    }

    (fab as FloatingActionButton).attachToListView(listView)

    alarmsSub =
        store.alarms().subscribe { alarms ->
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
          mAdapter.dataset = sorted
        }

    configureBottomDrawer(view)

    logger.trace { "onCreateView() { postponeEnterTransition() }" }
    postponeEnterTransition()
    view.doOnPreDraw {
      logger.trace { "onCreateView() { doOnPreDraw { startPostponedEnterTransition() } }" }
      startPostponedEnterTransition()
    }

    return view
  }

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
      if (listViewModel.openDrawerOnCreate) {
        state = BottomSheetBehavior.STATE_EXPANDED
        // reset the flag after opening the drawer
        listViewModel.openDrawerOnCreate = false
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
                      ) as Int
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

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    layoutSub = prefs.listRowLayout().subscribe { mAdapter.listRowLayout = it }
  }

  override fun onPause() {
    super.onPause()
    // dismiss the time picker if it was showing. Otherwise we will have to uiStore the state and it
    // is not nice for the user
    timePickerDialogDisposable.dispose()
  }

  override fun onDestroy() {
    super.onDestroy()
    alarmsSub.dispose()
    layoutSub.dispose()
  }

  override fun onCreateContextMenu(menu: ContextMenu, view: View, menuInfo: ContextMenuInfo?) {
    // Inflate the menu from xml.
    requireActivity().menuInflater.inflate(R.menu.list_context_menu, menu)

    // Use the current item to create a custom view for the header.
    val info = menuInfo as AdapterContextMenuInfo
    val alarm = mAdapter.getItem(info.position) ?: return

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
}
