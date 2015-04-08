package com.better.alarm.presenter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import android.app.AlertDialog;
import android.app.ListFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
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

import com.better.alarm.R;
import com.better.alarm.model.AlarmsManager;
import com.better.alarm.model.DaysOfWeek;
import com.better.alarm.model.interfaces.Alarm;
import com.better.alarm.model.interfaces.IAlarmsManager;
import com.better.alarm.model.interfaces.Intents;
import com.better.alarm.view.DigitalClock;
import com.github.androidutils.logger.Logger;

/**
 * Shows a list of alarms. To react on user interaction, requires a strategy. An
 * activity hosting this fragment should provide a proper strategy for single
 * and multi-pane modes.
 * 
 * @author Yuriy
 * 
 */
public class AlarmsListFragment extends ListFragment {
    public interface ShowDetailsStrategy {
        public void showDetails(Alarm alarm);
    }

    private ShowDetailsStrategy showDetailsStrategy;

    private final int mCurCheckPosition = 0;

    private final Logger log = Logger.getDefaultLogger();

    public class AlarmListAdapter extends ArrayAdapter<Alarm> {
        private final Context context;
        private final List<Alarm> values;
        private final boolean isMaterial;

        public AlarmListAdapter(Context context, int alarmTime, int label, List<Alarm> values) {
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
            final Alarm alarm = values.get(position);

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

    public static final String PREFERENCES = "AlarmClock";
    /**
     * This must be false for production. If true, turns on logging, test code,
     * etc.
     */
    public static final boolean DEBUG = false;
    public final static String M12 = "h:mm aa";
    public final static String M24 = "kk:mm";
    private IAlarmsManager alarms;
    private AlarmListAdapter mAdapter;
    private BroadcastReceiver mAlarmsChangedReceiver;

    private class AlarmChangedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            log.d(intent.toString());
            updateAlarmsList();
        }
    }

    @Override
    public boolean onContextItemSelected(final MenuItem item) {
        final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        final Alarm alarm = mAdapter.getItem(info.position);
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
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        alarms = AlarmsManager.getAlarmsManager();

        setListAdapter(new AlarmListAdapter(getActivity(), R.layout.list_row, R.string.alarm_list_title,
                new ArrayList<Alarm>()));

        mAdapter = (AlarmListAdapter) getListAdapter();

        getListView().setVerticalScrollBarEnabled(true);
        getListView().setOnCreateContextMenuListener(this);

        setHasOptionsMenu(true);

        getListView().setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);

        mAlarmsChangedReceiver = new AlarmChangedReceiver();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        // We can display everything in-place with fragments, so update
        // the list to highlight the selected item and show the data.
        getListView().setSelection(position);
        ((AlarmsListActivity) getActivity()).showTimePicker(mAdapter.getItem(position));
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(mAlarmsChangedReceiver, new IntentFilter(Intents.ACTION_ALARM_CHANGED));
        updateAlarmsList();
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(mAlarmsChangedReceiver);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
        // Inflate the menu from xml.
        getActivity().getMenuInflater().inflate(R.menu.list_context_menu, menu);

        // Use the current item to create a custom view for the header.
        final AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        final Alarm alarm = mAdapter.getItem(info.position);

        // Construct the Calendar to compute the time.
        final Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, alarm.getHour());
        cal.set(Calendar.MINUTE, alarm.getMinutes());
        String format = android.text.format.DateFormat.is24HourFormat(getActivity()) ? M24 : M12;
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

    public void updateAlarmsList() {
        AlarmListAdapter adapter = mAdapter;
        adapter.clear();
        // TODO fixme when we have Parcelable
        adapter.addAll(alarms.getAlarmsList());
    }

}