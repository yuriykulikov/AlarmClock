/*
 * Copyright (C) 2007 The Android Open Source Project
 * Copyright (C) 2012 Yuriy Kulikov yuriy.kulikov.87@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.better.alarm.presenter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import com.better.alarm.R;
import com.better.alarm.model.AlarmsManager;
import com.better.alarm.model.interfaces.Alarm;
import com.better.alarm.model.interfaces.IAlarmsManager;
import com.better.alarm.model.interfaces.Intents;
import com.better.alarm.view.DigitalClock;

/**
 * AlarmClock application.
 */
public class AlarmsListActivity extends Activity {

    public static final String PREFERENCES = "AlarmClock";

    /**
     * This must be false for production. If true, turns on logging, test code,
     * etc.
     */
    public static final boolean DEBUG = false;

    public final static String M12 = "h:mm aa";
    public final static String M24 = "kk:mm";

    IAlarmsManager alarms;

    private AlarmListAdapter mAdapter;
    private ListView mListView;
    private BroadcastReceiver mAlarmsChangedReceiver;

    public class AlarmListAdapter extends ArrayAdapter<Alarm> {
        private final Context context;
        private final List<Alarm> values;

        public AlarmListAdapter(Context context, int alarmTime, int label, List<Alarm> values) {
            super(context, alarmTime, label, values);
            this.context = context;
            this.values = values;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView = inflater.inflate(R.layout.alarm_time, parent, false);

            DigitalClock digitalClock = (DigitalClock) rowView.findViewById(R.id.digitalClock);
            digitalClock.setLive(false);

            // get the alarm which we have to display
            final Alarm alarm = values.get(position);

            // now populate rows views
            View indicator = rowView.findViewById(R.id.indicator);

            // Set the initial state of the clock "checkbox"
            final CheckBox clockOnOff = (CheckBox) indicator.findViewById(R.id.clock_onoff);
            clockOnOff.setChecked(alarm.isEnabled());

            // Clicking outside the "checkbox" should also change the state.
            indicator.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    clockOnOff.toggle();
                    alarms.enable(alarm.getId(), clockOnOff.isChecked());
                }
            });

            // set the alarm text
            final Calendar c = Calendar.getInstance();
            c.set(Calendar.HOUR_OF_DAY, alarm.getHour());
            c.set(Calendar.MINUTE, alarm.getMinutes());
            digitalClock.updateTime(c);

            // Set the repeat text or leave it blank if it does not repeat.
            TextView daysOfWeekView = (TextView) digitalClock.findViewById(R.id.daysOfWeek);
            final String daysOfWeekStr = alarm.getDaysOfWeek().toString(AlarmsListActivity.this, false);
            if (daysOfWeekStr != null && daysOfWeekStr.length() != 0) {
                daysOfWeekView.setText(daysOfWeekStr);
                daysOfWeekView.setVisibility(View.VISIBLE);
            } else {
                daysOfWeekView.setVisibility(View.GONE);
            }

            // Display the label
            TextView labelView = (TextView) rowView.findViewById(R.id.label);
            if (alarm.getLabel() != null && alarm.getLabel().length() != 0) {
                labelView.setText(alarm.getLabel());
                labelView.setVisibility(View.VISIBLE);
            } else {
                labelView.setVisibility(View.GONE);
            }

            return rowView;
        }
    }

    @Override
    public boolean onContextItemSelected(final MenuItem item) {
        final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        final Alarm alarm = mAdapter.getItem(info.position);
        switch (item.getItemId()) {
        case R.id.delete_alarm: {
            // Confirm that the alarm will be deleted.
            new AlertDialog.Builder(this).setTitle(getString(R.string.delete_alarm))
                    .setMessage(getString(R.string.delete_alarm_confirm))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface d, int w) {
                            alarms.delete(alarm.getId());
                        }
                    }).setNegativeButton(android.R.string.cancel, null).show();
            return true;
        }

        case R.id.enable_alarm: {
            alarms.enable(alarm.getId(), !alarm.isEnabled());
            return true;
        }

        case R.id.edit_alarm: {
            Intent intent = new Intent(this, AlarmDetailsActivity.class);
            intent.putExtra(Intents.EXTRA_ID, alarm.getId());
            startActivity(intent);
            return true;
        }

        default:
            break;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        if (!getResources().getBoolean(R.bool.isTablet)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        alarms = AlarmsManager.getAlarmsManager();

        setContentView(R.layout.alarm_clock);
        mListView = (ListView) findViewById(R.id.alarms_list);

        mAdapter = new AlarmListAdapter(this, R.layout.alarm_time, R.id.label, new ArrayList<Alarm>());
        mListView.setAdapter(mAdapter);
        mListView.setVerticalScrollBarEnabled(true);
        mListView.setOnCreateContextMenuListener(this);
        mListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Alarm alarm = mAdapter.getItem(position);
                Intent intent = new Intent(AlarmsListActivity.this, AlarmDetailsActivity.class);
                intent.putExtra(Intents.EXTRA_ID, alarm.getId());
                startActivity(intent);
            }
        });

        mAlarmsChangedReceiver = new AlarmChangedReceiver();
    }

    @Override
    protected void onResume() {
        registerReceiver(mAlarmsChangedReceiver, new IntentFilter(Intents.ACTION_ALARM_CHANGED));
        updateAlarmsList();
        super.onResume();
    }

    @Override
    protected void onPause() {
        unregisterReceiver(mAlarmsChangedReceiver);
        super.onPause();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
        // Inflate the menu from xml.
        getMenuInflater().inflate(R.menu.list_context_menu, menu);

        // Use the current item to create a custom view for the header.
        final AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        final Alarm alarm = mAdapter.getItem(info.position);

        // Construct the Calendar to compute the time.
        final Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, alarm.getHour());
        cal.set(Calendar.MINUTE, alarm.getMinutes());
        String format = android.text.format.DateFormat.is24HourFormat(this) ? M24 : M12;
        final String time = (cal == null) ? "" : (String) DateFormat.format(format, cal);

        // Inflate the custom view and set each TextView's text.
        final View v = getLayoutInflater().inflate(R.layout.context_menu_header, null);
        TextView textView = (TextView) v.findViewById(R.id.header_time);
        textView.setText(time);
        textView = (TextView) v.findViewById(R.id.header_label);
        textView.setText(alarm.getLabel());

        // Set the custom view on the menu.
        menu.setHeaderView(v);
        // Change the text based on the state of the alarm.
        if (alarm.isEnabled()) {
            menu.findItem(R.id.enable_alarm).setTitle(R.string.disable_alarm);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_item_settings:
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        case R.id.menu_item_add_alarm:
            startActivity(new Intent(this, AlarmDetailsActivity.class));
            return true;
        default:
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.list_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private void updateAlarmsList() {
        AlarmListAdapter adapter = mAdapter;
        adapter.clear();
        // TODO fixme when we have Parcelable
        adapter.addAll(alarms.getAlarmsList());
    }

    private class AlarmChangedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateAlarmsList();
        }
    }
}
