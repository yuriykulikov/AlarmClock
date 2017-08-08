package com.better.alarm.presenter;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.format.DateFormat;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.better.alarm.R;
import com.better.alarm.configuration.Prefs;
import com.better.alarm.configuration.Store;
import com.better.alarm.interfaces.IAlarmsManager;
import com.better.alarm.logger.Logger;
import com.better.alarm.model.AlarmValue;
import com.google.common.base.Optional;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import io.reactivex.functions.Consumer;

import static com.better.alarm.configuration.AlarmApplication.container;


/**
 * Shows a list of alarms. To react on user interaction, requires a strategy. An
 * activity hosting this fragment should provide a proper strategy for single
 * and multi-pane modes.
 *
 * @author Yuriy
 */
public class AlarmsListFragment extends Fragment {
    public final static String M12 = "h:mm aa";
    public final static String M24 = "kk:mm";
    public static final boolean MATERIAL_DESIGN = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;

    private final IAlarmsManager alarms = container().alarms();
    private final Store store = container().store();
    private UiStore uiStore;
    private final Prefs prefs = container().prefs();
    private final Logger logger = container().logger();

    private AlarmListAdapter mAdapter;
    private Disposable alarmsSub;
    private Disposable backSub;
    private Disposable timePickerDialogDisposable = Disposables.disposed();

    public class AlarmListAdapter extends ArrayAdapter<AlarmValue> {
        private final Context context;
        private final List<AlarmValue> values;

        public AlarmListAdapter(Context context, int alarmTime, int label, List<AlarmValue> values) {
            super(context, alarmTime, label, values);
            this.context = context;
            this.values = values;
        }

        private RowHolder recycleView(View convertView, ViewGroup parent, int id) {
            if (convertView != null) return new RowHolder(convertView, id);

            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView = inflater.inflate(R.layout.list_row, parent, false);
            RowHolder rowHolder = new RowHolder(rowView, id);
            rowHolder.getDigitalClock().setLive(false);
            return rowHolder;
        }

        @TargetApi(21)
        private void setTranstionNames(RowHolder row, AlarmValue alarm) {
            if (MATERIAL_DESIGN) {
                row.digitalClock().setTransitionName("clock" + alarm.getId());
                row.container().setTransitionName("onOff" + alarm.getId());
            }
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            // get the alarm which we have to display
            final AlarmValue alarm = values.get(position);

            final RowHolder row = recycleView(convertView, parent, alarm.getId());

            row.onOff().setChecked(alarm.isEnabled());

            setTranstionNames(row, alarm);

            //Delete add, skip animation
            if (row.idHasChanged()) {
                logger.d("Jump to current state");
                //row.onOff().jumpDrawablesToCurrentState();
            }

            row.container()
                    //onOff
                    .setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            boolean enable = !alarm.isEnabled();
                            logger.d("onClick: " + (enable ? "enable" : "disable"));
                            alarms.enable(alarm, enable);
                        }
                    });

            row.digitalClock().setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    timePickerDialogDisposable = TimePickerDialogFragment.showTimePicker(getFragmentManager())
                            .subscribe(new Consumer<Optional<TimePickerDialogFragment.PickedTime>>() {
                                @Override
                                public void accept(@NonNull Optional<TimePickerDialogFragment.PickedTime> picked) {
                                    if (picked.isPresent()) {
                                        alarms.getAlarm(alarm.getId())
                                                .edit()
                                                .withIsEnabled(true)
                                                .withHour(picked.get().hour())
                                                .withMinutes(picked.get().minute())
                                                .commit();
                                    }
                                }
                            });
                }
            });

            // set the alarm text
            final Calendar c = Calendar.getInstance();
            c.set(Calendar.HOUR_OF_DAY, alarm.getHour());
            c.set(Calendar.MINUTE, alarm.getMinutes());
            row.digitalClock().updateTime(c);

            // Set the repeat text or leave it blank if it does not repeat.
            final String daysOfWeekStr = alarm.getDaysOfWeek().toString(getContext(), false);
            if (daysOfWeekStr.length() != 0) {
                row.daysOfWeek().setText(daysOfWeekStr);
                row.daysOfWeek().setVisibility(View.VISIBLE);
            } else {
                row.daysOfWeek().setVisibility(MATERIAL_DESIGN ? View.INVISIBLE : View.GONE);
            }

            // Set the repeat text or leave it blank if it does not repeat.
            if (alarm.getLabel() != null && !alarm.getLabel().isEmpty()) {
                row.label().setText(alarm.getLabel());
                row.label().setVisibility(View.VISIBLE);
            } else {
                row.label().setVisibility(MATERIAL_DESIGN ? View.INVISIBLE : View.GONE);
            }

            return row.rowView();
        }
    }

    @Override
    public boolean onContextItemSelected(final MenuItem item) {
        final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        final AlarmValue alarm = mAdapter.getItem(info.position);
        switch (item.getItemId()) {
            case R.id.delete_alarm: {
                // Confirm that the alarm will be deleted.
                new AlertDialog.Builder(getActivity()).setTitle(getString(R.string.delete_alarm))
                        .setMessage(getString(R.string.delete_alarm_confirm))
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int w) {
                                alarms.delete(alarm);
                            }
                        }).setNegativeButton(android.R.string.cancel, null).show();
                return true;
            }

            case R.id.enable_alarm: {
                alarms.enable(alarm, !alarm.isEnabled());
                return true;
            }

            case R.id.edit_alarm: {
                uiStore.edit(alarm.getId());
                return true;
            }

            default:
                break;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        uiStore = AlarmsListActivity.uiStore(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.list_fragment, container, false);

        ListView listView = (ListView) view.findViewById(R.id.list_fragment_list);

        listView.setAdapter(new AlarmListAdapter(getActivity(), R.layout.list_row, R.string.alarm_list_title,
                new ArrayList<AlarmValue>()));

        mAdapter = (AlarmListAdapter) listView.getAdapter();

        listView.setVerticalScrollBarEnabled(false);
        listView.setOnCreateContextMenuListener(this);
        listView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                // We can display everything in-place with fragments, so update
                // the list to highlight the selected item and show the data.
                //TODO what does this do? listView.setSelection(position);

                int id = mAdapter.getItem(position).getId();
                uiStore.edit(id, (RowHolder) view.getTag());
            }
        });

        registerForContextMenu(listView);

        setHasOptionsMenu(true);

        View fab = view.findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uiStore.createNewAlarm();
            }
        });

        //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            //((FloatingActionButton) fab).attachToListView(listView);
        //}

        alarmsSub = store.alarms().subscribe(new Consumer<List<AlarmValue>>() {
            @Override
            public void accept(@NonNull List<AlarmValue> alarms) throws Exception {
                mAdapter.clear();
                List<AlarmValue> sorted = new ArrayList<AlarmValue>(alarms);
                Collections.sort(sorted, new Comparators.MinuteComparator());
                Collections.sort(sorted, new Comparators.HourComparator());
                Collections.sort(sorted, new Comparators.RepeatComparator());
                mAdapter.addAll(sorted);
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        backSub = uiStore.onBackPressed().subscribe(new Consumer<String>() {
            @Override
            public void accept(@NonNull String s) throws Exception {
                getActivity().finish();
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        backSub.dispose();
        //dismiss the time picker if it was showing. Otherwise we will have to uiStore the state and it is not nice for the user
        timePickerDialogDisposable.dispose();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        alarmsSub.dispose();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
        // Inflate the menu from xml.
        getActivity().getMenuInflater().inflate(R.menu.list_context_menu, menu);

        // Use the current item to create a custom view for the header.
        final AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        final AlarmValue alarm = mAdapter.getItem(info.position);

        // Construct the Calendar to compute the time.
        final Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, alarm.getHour());
        cal.set(Calendar.MINUTE, alarm.getMinutes());
        String format = prefs.is24HoutFormat().blockingGet() ? M24 : M12;
        final String time = cal == null ? "" : (String) DateFormat.format(format, cal);

        // Change the text based on the state of the alarm.
        if (alarm.isEnabled()) {
            menu.findItem(R.id.enable_alarm).setTitle(R.string.disable_alarm);
        }
    }
}