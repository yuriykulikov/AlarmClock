package com.better.alarm.presenter;

import android.app.AlertDialog;
import android.app.ListFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.better.alarm.AlarmApplication;
import com.better.alarm.Prefs;
import com.better.alarm.R;
import com.better.alarm.Store;
import com.better.alarm.model.AlarmValue;
import com.better.alarm.model.DaysOfWeek;
import com.better.alarm.interfaces.IAlarmsManager;
import com.better.alarm.view.DigitalClock;
import com.better.alarm.logger.Logger;
import com.google.inject.Inject;

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
    /**
     * This must be false for production. If true, turns on logging, test code,
     * etc.
     */
    public static final boolean DEBUG = false;
    public final static String M12 = "h:mm aa";
    public final static String M24 = "kk:mm";

    private ShowDetailsStrategy showDetailsStrategy;

    @Inject
    private Logger log;
    @Inject
    private IAlarmsManager alarms;
    @Inject
    private Store store;
    @Inject
    private Prefs prefs;

    private AlarmListAdapter mAdapter;
    private Disposable alarmsSub;

    public class AlarmListAdapter extends ArrayAdapter<AlarmValue> {
        private final Context context;
        private final List<AlarmValue> values;
        private final boolean isMaterial;

        public AlarmListAdapter(Context context, int alarmTime, int label, List<AlarmValue> values) {
            super(context, alarmTime, label, values);
            this.context = context;
            this.values = values;
            this.isMaterial = !PreferenceManager.getDefaultSharedPreferences(context).getString("theme", "dark")
                    .equals("green");
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View rowView;
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                rowView = inflater.inflate(R.layout.list_row, parent, false);
            } else {
                rowView = convertView;
            }
            DigitalClock digitalClock = (DigitalClock) rowView.findViewById(R.id.list_row_digital_clock);
            digitalClock.setLive(false);

            // get the alarm which we have to display
            final AlarmValue alarm = values.get(position);

            // now populate rows views
            View indicator = rowView.findViewById(R.id.list_row_on_off_checkbox_container);

            // Set the initial state of the clock "checkbox"
            final CompoundButton clockOnOff;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && isMaterial) {
                clockOnOff = (CompoundButton) indicator.findViewById(R.id.list_row_on_off_switch);
                indicator.findViewById(R.id.list_row_on_off_checkbox).setVisibility(View.GONE);
            } else {
                clockOnOff = (CompoundButton) indicator.findViewById(R.id.list_row_on_off_checkbox);
                indicator.findViewById(R.id.list_row_on_off_switch).setVisibility(View.GONE);
            }

            clockOnOff.setChecked(alarm.isEnabled());

            // Clicking outside the "checkbox" should also change the state.
            indicator.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    clockOnOff.toggle();
                    alarms.enable(alarm, clockOnOff.isChecked());
                }
            });

            View detailsWrapper = rowView.findViewById(R.id.details_button_container);
            // Set the initial state of the clock "checkbox"
            final ImageButton detailsButton = (ImageButton) detailsWrapper.findViewById(R.id.list_row_details_button);
            detailsButton.setFocusable(false);
            detailsWrapper.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (showDetailsStrategy != null) {
                        showDetailsStrategy.showDetails(mAdapter.getItem(position));
                    }
                }
            });

            // set the alarm text
            final Calendar c = Calendar.getInstance();
            c.set(Calendar.HOUR_OF_DAY, alarm.getHour());
            c.set(Calendar.MINUTE, alarm.getMinutes());
            digitalClock.updateTime(c);

            // Set the repeat text or leave it blank if it does not repeat.
            TextView subtitle = (TextView) rowView.findViewById(R.id.list_row_daysOfWeek);
            DaysOfWeek days = alarm.getDaysOfWeek();
            final String daysOfWeekStr = days.toString(getContext(), false);
            if (daysOfWeekStr != null && daysOfWeekStr.length() != 0) {
                subtitle.setText(daysOfWeekStr);
                subtitle.setVisibility(View.VISIBLE);
            } else {
                subtitle.setVisibility(View.GONE);
            }

            // Set the repeat text or leave it blank if it does not repeat.
            TextView label = (TextView) rowView.findViewById(R.id.list_row_label);
            boolean hasLabel = alarm.getLabel() != null && !alarm.getLabel().isEmpty();
            if (hasLabel) {
                label.setText(alarm.getLabel());
                label.setVisibility(View.VISIBLE);
            } else {
                label.setVisibility(View.GONE);
            }

            return rowView;
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
                if (showDetailsStrategy != null) {
                    showDetailsStrategy.showDetails(alarm);
                }
                return true;
            }

            default:
                break;
        }
        return super.onContextItemSelected(item);
    }

    public void setShowDetailsStrategy(ShowDetailsStrategy showDetailsStrategy) {
        this.showDetailsStrategy = showDetailsStrategy;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        AlarmApplication.guice().injectMembers(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        alarms = AlarmApplication.alarms();

        setListAdapter(new AlarmListAdapter(getActivity(), R.layout.list_row, R.string.alarm_list_title,
                new ArrayList<AlarmValue>()));

        mAdapter = (AlarmListAdapter) getListAdapter();

        getListView().setVerticalScrollBarEnabled(true);
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

        // Inflate the custom view and set each TextView's text.
        final View v = getActivity().getLayoutInflater().inflate(R.layout.list_context_menu, null);
        TextView textView = (TextView) v.findViewById(R.id.list_context_menu_header_time);
        textView.setText(time);
        textView = (TextView) v.findViewById(R.id.list_context_menu_header_label);
        textView.setText(alarm.getLabel());

        // Set the custom view on the menu.
        menu.setHeaderView(v);
        // Change the text based on the state of the alarm.
        if (alarm.isEnabled()) {
            menu.findItem(R.id.enable_alarm).setTitle(R.string.disable_alarm);
        }
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
    }
}