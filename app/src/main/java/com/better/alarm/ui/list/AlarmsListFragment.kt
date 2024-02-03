package com.better.alarm.ui.list

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.better.alarm.R
import com.better.alarm.bootstrap.globalLogger
import com.better.alarm.data.Prefs
import com.better.alarm.domain.IAlarmsManager
import com.better.alarm.domain.Store
import com.better.alarm.logger.Logger
import com.better.alarm.ui.row.ListRowHighlighter
import com.better.alarm.ui.row.RowHolder
import com.better.alarm.ui.timepicker.TimePickerDialogFragment
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
        listRowLayout = prefs.layout(),
        changeAlarm = { id, enable ->
          alarms.getAlarm(alarmId = id)?.edit { copy(isEnabled = enable) }
        },
        showDetails = { alarm, row ->
          transitionRowHolder = row
          listViewModel.edit(alarm)
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
  private var timePickerDialogDisposable = Disposables.disposed()

  companion object {
    var fabSync: Channel<Unit>? = null
  }

  override fun onContextItemSelected(item: MenuItem): Boolean {
    val alarm = mAdapter.contextMenuAlarm ?: return false
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

    val recyclerView: RecyclerView = view.findViewById(R.id.list_fragment_list)

    recyclerView.adapter = mAdapter
    recyclerView.isVerticalScrollBarEnabled = false
    recyclerView.layoutManager = LinearLayoutManager(requireContext())
    registerForContextMenu(recyclerView)
    recyclerView.itemAnimator = DefaultItemAnimator().apply { supportsChangeAnimations = false }

    setHasOptionsMenu(true)

    val fab: View = view.findViewById(R.id.fab)
    fab.setOnClickListener {
      listViewModel.createNewAlarm()
      fabSync?.trySend(Unit)?.getOrThrow()
    }

    (fab as FloatingActionButton).attachToRecyclerView(recyclerView)

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

    logger.trace { "onCreateView() { postponeEnterTransition() }" }
    postponeEnterTransition()
    view.doOnPreDraw {
      logger.trace { "onCreateView() { doOnPreDraw { startPostponedEnterTransition() } }" }
      startPostponedEnterTransition()
    }

    return view
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
  }
}
