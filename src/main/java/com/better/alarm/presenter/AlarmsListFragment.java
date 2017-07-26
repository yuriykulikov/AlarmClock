package com.better.alarm.presenter;

import android.app.AlertDialog;
import android.app.ListFragment;
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
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;

import com.better.alarm.R;
import com.better.alarm.configuration.AlarmApplication;
import com.better.alarm.configuration.Prefs;
import com.better.alarm.configuration.Store;
import com.better.alarm.interfaces.Alarm;
import com.better.alarm.interfaces.IAlarmsManager;
import com.better.alarm.logger.Logger;
import com.better.alarm.model.AlarmValue;
import com.better.alarm.view.DigitalClock;
import com.google.inject.Inject;

import org.immutables.value.Value;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;


/**
 * Shows a list of alarms. To react on user interaction, requires a strategy. An
 * activity hosting this fragment should provide a proper strategy for single
 * and multi-pane modes.
 *
 * @author Yuriy
 */
public class AlarmsListFragment extends ListFragment {
    public final static String M12 = "h:mm aa";
    public final static String M24 = "kk:mm";
    public static final boolean MATERIAL_DESIGN = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;

    private ShowDetailsStrategy details;
    @Inject
    private IAlarmsManager alarms;
    @Inject
    private Store store;
    @Inject
    private Prefs prefs;
    @Inject
    private Logger logger;

    private AlarmListAdapter mAdapter;
    private Disposable alarmsSub;

    @Value.Immutable
    @Value.Style(stagedBuilder = true)
    interface RowHolder {
        DigitalClock digitalClock();

        View rowView();

        CompoundButton onOff();

        View container();

        int alarmId();

        TextView daysOfWeek();

        TextView label();

        View detailsButton();
    }

    public class AlarmListAdapter extends ArrayAdapter<AlarmValue> {
        private final Context context;
        private final List<AlarmValue> values;

        public AlarmListAdapter(Context context, int alarmTime, int label, List<AlarmValue> values) {
            super(context, alarmTime, label, values);
            this.context = context;
            this.values = values;
        }

        private RowHolder recycleView(View convertView, ViewGroup parent) {
            if (convertView != null) return (RowHolder) convertView.getTag();

            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView = inflater.inflate(R.layout.list_row, parent, false);

            ImmutableRowHolder holder = ImmutableRowHolder.builder()
                    .digitalClock((DigitalClock) rowView.findViewById(R.id.list_row_digital_clock))
                    .rowView(rowView)
                    .onOff((CompoundButton) rowView.findViewById(R.id.list_row_on_off_switch))
                    .container(rowView.findViewById(R.id.list_row_on_off_checkbox_container))
                    .alarmId(-1)
                    .daysOfWeek((TextView) rowView.findViewById(R.id.list_row_daysOfWeek))
                    .label((TextView) rowView.findViewById(R.id.list_row_label))
                    .detailsButton(rowView.findViewById(R.id.details_button_container))
                    .build();

            holder.digitalClock().setLive(false);
            rowView.setTag(holder);
            return holder;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            // get the alarm which we have to display
            final AlarmValue alarm = values.get(position);

            RowHolder row = recycleView(convertView, parent);
            row.onOff().setOnCheckedChangeListener(null);
            row.container().setOnClickListener(null);
            row.onOff().setChecked(alarm.isEnabled());

            //Delete add, skip animation
            if (row.alarmId() != alarm.getId()) {
                row.onOff().jumpDrawablesToCurrentState();
                row.rowView().setTag(ImmutableRowHolder.copyOf(row).withAlarmId(alarm.getId()));
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

            row.onOff().setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean enable) {
                    logger.d("onCheckedChanged: " + (enable ? "enabled" : "disabled"));
                    alarms.enable(alarm, enable);
                }
            });

            row.detailsButton().setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    details.showDetails(mAdapter.getItem(position));
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
                details.showDetails(alarm);
                return true;
            }

            default:
                break;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        AlarmApplication.guice().injectMembers(this);
        super.onCreate(savedInstanceState);
        this.details = new ShowDetailsInActivity(getActivity());
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        alarms = AlarmApplication.alarms();

        setListAdapter(new AlarmListAdapter(getActivity(), R.layout.list_row, R.string.alarm_list_title,
                new ArrayList<AlarmValue>()));

        mAdapter = (AlarmListAdapter) getListAdapter();

        getListView().setVerticalScrollBarEnabled(false);
        getListView().setOnCreateContextMenuListener(this);

        setHasOptionsMenu(true);

        getListView().setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();

        alarmsSub = store.alarms().subscribe(new Consumer<List<AlarmValue>>() {
            @Override
            public void accept(@NonNull List<AlarmValue> alarms) throws Exception {
                mAdapter.clear();
                List<AlarmValue> sorted = new ArrayList<AlarmValue>(alarms);
                Collections.sort(sorted, new MinuteComparator());
                Collections.sort(sorted, new HourComparator());
                Collections.sort(sorted, new RepeatComparator());
                mAdapter.addAll(sorted);
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        alarmsSub.dispose();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        // We can display everything in-place with fragments, so update
        // the list to highlight the selected item and show the data.
        getListView().setSelection(position);
        ((AlarmsListActivity) getActivity()).showTimePicker(mAdapter.getItem(position));
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

    public void performOptimisticTimeUpdate(Alarm timePickerAlarm, int hourOfDay, int minute) {
        RowHolder row = (RowHolder) getListView()
                .getChildAt(indexById(timePickerAlarm.getId()))
                .getTag();

        final Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, hourOfDay);
        c.set(Calendar.MINUTE, minute);
        row.digitalClock().updateTime(c);
    }

    private int indexById(int id) {
        for (int i = 0; i < mAdapter.values.size(); i++) {
            if (mAdapter.values.get(i).getId() == id) {
                return i;
            }
        }
        throw new RuntimeException("Id " + id + " was not found!");
    }

    private final class RepeatComparator implements Comparator<AlarmValue> {
        @Override
        public int compare(AlarmValue lhs, AlarmValue rhs) {
            return Integer.valueOf(getPrio(lhs)).compareTo(Integer.valueOf(getPrio(rhs)));
        }

        /**
         * First comes on Weekdays, than on weekends and then the rest
         *
         * @param alarm
         * @return
         */
        private int getPrio(AlarmValue alarm) {
            switch (alarm.getDaysOfWeek().getCoded()) {
                case 0x7F:
                    return 1;
                case 0x1F:
                    return 2;
                case 0x60:
                    return 3;
                default:
                    return 0;
            }
        }
    }

    private final class HourComparator implements Comparator<AlarmValue> {
        @Override
        public int compare(AlarmValue lhs, AlarmValue rhs) {
            return Integer.valueOf(lhs.getHour()).compareTo(Integer.valueOf(rhs.getHour()));
        }
    }

    private final class MinuteComparator implements Comparator<AlarmValue> {
        @Override
        public int compare(AlarmValue lhs, AlarmValue rhs) {
            return Integer.valueOf(lhs.getMinutes()).compareTo(Integer.valueOf(rhs.getMinutes()));
        }
    }

    public interface ShowDetailsStrategy {
        void showDetails(AlarmValue alarm);

        void createNewAlarm();
    }
}