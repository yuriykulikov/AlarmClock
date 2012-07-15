/*
 * Copyright (C) 2007 The Android Open Source Project
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

import java.util.Calendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
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
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.better.alarm.R;
import com.better.alarm.model.Alarm;
import com.better.alarm.model.AlarmsManager;
import com.better.alarm.model.IAlarmsManager;
import com.better.alarm.model.Intents;
import com.better.alarm.model.Alarm.Columns;
import com.better.alarm.view.DigitalClock;

/**
 * AlarmClock application.
 */
public class AlarmsListActivity extends Activity implements OnItemClickListener {

    public static final String PREFERENCES = "AlarmClock";

    /**
     * This must be false for production. If true, turns on logging, test code,
     * etc.
     */
    public static final boolean DEBUG = false;

    public final static String M12 = "h:mm aa";
    public final static String M24 = "kk:mm";

    private LayoutInflater mFactory;
    private ListView mAlarmsList;
    private Cursor mCursor;

    IAlarmsManager alarms;

    private void updateAlarm(boolean enabled, Alarm alarm) {
        alarms.enable(alarm.id, enabled);
    }

    private class AlarmTimeAdapter extends CursorAdapter {
        public AlarmTimeAdapter(Context context, Cursor cursor) {
            super(context, cursor);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View ret = mFactory.inflate(R.layout.alarm_time, parent, false);

            DigitalClock digitalClock = (DigitalClock) ret.findViewById(R.id.digitalClock);
            digitalClock.setLive(false);
            return ret;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            final Alarm alarm = alarms.getAlarm(cursor.getInt(Columns.ALARM_ID_INDEX));

            View indicator = view.findViewById(R.id.indicator);

            // Set the initial state of the clock "checkbox"
            final CheckBox clockOnOff = (CheckBox) indicator.findViewById(R.id.clock_onoff);
            clockOnOff.setChecked(alarm.enabled);

            // Clicking outside the "checkbox" should also change the state.
            indicator.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    clockOnOff.toggle();
                    updateAlarm(clockOnOff.isChecked(), alarm);
                }
            });

            DigitalClock digitalClock = (DigitalClock) view.findViewById(R.id.digitalClock);

            // set the alarm text
            final Calendar c = Calendar.getInstance();
            c.set(Calendar.HOUR_OF_DAY, alarm.hour);
            c.set(Calendar.MINUTE, alarm.minutes);
            digitalClock.updateTime(c);

            // Set the repeat text or leave it blank if it does not repeat.
            TextView daysOfWeekView = (TextView) digitalClock.findViewById(R.id.daysOfWeek);
            final String daysOfWeekStr = alarm.daysOfWeek.toString(AlarmsListActivity.this, false);
            if (daysOfWeekStr != null && daysOfWeekStr.length() != 0) {
                daysOfWeekView.setText(daysOfWeekStr);
                daysOfWeekView.setVisibility(View.VISIBLE);
            } else {
                daysOfWeekView.setVisibility(View.GONE);
            }

            // Display the label
            TextView labelView = (TextView) view.findViewById(R.id.label);
            if (alarm.label != null && alarm.label.length() != 0) {
                labelView.setText(alarm.label);
                labelView.setVisibility(View.VISIBLE);
            } else {
                labelView.setVisibility(View.GONE);
            }
        }
    };

    @Override
    public boolean onContextItemSelected(final MenuItem item) {
        final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        //XXX ? ah?
        final int id = (int) info.id;
        // Error check just in case.
        if (id == -1) {
            return super.onContextItemSelected(item);
        }
        switch (item.getItemId()) {
        case R.id.delete_alarm: {
            // Confirm that the alarm will be deleted.
            new AlertDialog.Builder(this).setTitle(getString(R.string.delete_alarm))
                    .setMessage(getString(R.string.delete_alarm_confirm))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface d, int w) {
                            alarms.delete(id);
                        }
                    }).setNegativeButton(android.R.string.cancel, null).show();
            return true;
        }

        case R.id.enable_alarm: {
            final Cursor c = (Cursor) mAlarmsList.getAdapter().getItem(info.position);
            int alarmId = c.getInt(Columns.ALARM_ID_INDEX);
            Alarm alarm = alarms.getAlarm(alarmId);
            alarms.enable(alarmId, !alarm.enabled);
            return true;
        }

        case R.id.edit_alarm: {
            // XXX i don't like this whole cursor thing, but for now just remove
            // the constructor
            final Cursor c = (Cursor) mAlarmsList.getAdapter().getItem(info.position);
            Intent intent = new Intent(this, SetAlarmActivity.class);
            intent.putExtra(Intents.EXTRA_ID, c.getInt(Columns.ALARM_ID_INDEX));
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

        alarms = AlarmsManager.getAlarmsManager();
        mFactory = LayoutInflater.from(this);
        mCursor = alarms.getCursor();

        updateLayout();
    }

    private void updateLayout() {
        setContentView(R.layout.alarm_clock);
        mAlarmsList = (ListView) findViewById(R.id.alarms_list);
        AlarmTimeAdapter adapter = new AlarmTimeAdapter(this, mCursor);
        mAlarmsList.setAdapter(adapter);
        mAlarmsList.setVerticalScrollBarEnabled(true);
        mAlarmsList.setOnItemClickListener(this);
        mAlarmsList.setOnCreateContextMenuListener(this);

        View doneButton = findViewById(R.id.done);
        if (doneButton != null) {
            doneButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    finish();
                }
            });
        }
    }

    private void addNewAlarm() {
        startActivity(new Intent(this, SetAlarmActivity.class));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCursor != null) {
            mCursor.close();
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
        // Inflate the menu from xml.
        getMenuInflater().inflate(R.menu.context_menu, menu);

        // Use the current item to create a custom view for the header.
        final AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        final Cursor c = (Cursor) mAlarmsList.getAdapter().getItem(info.position);

        final Alarm alarm = alarms.getAlarm(c.getInt(Columns.ALARM_ID_INDEX));

        // Construct the Calendar to compute the time.
        final Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, alarm.hour);
        cal.set(Calendar.MINUTE, alarm.minutes);
        String format = android.text.format.DateFormat.is24HourFormat(this) ? M24 : M12;
        final String time = (cal == null) ? "" : (String) DateFormat.format(format, cal);

        // Inflate the custom view and set each TextView's text.
        final View v = mFactory.inflate(R.layout.context_menu_header, null);
        TextView textView = (TextView) v.findViewById(R.id.header_time);
        textView.setText(time);
        textView = (TextView) v.findViewById(R.id.header_label);
        textView.setText(alarm.label);

        // Set the custom view on the menu.
        menu.setHeaderView(v);
        // Change the text based on the state of the alarm.
        if (alarm.enabled) {
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
            addNewAlarm();
            return true;
        default:
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.alarm_list_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onItemClick(@SuppressWarnings("rawtypes") AdapterView parent, View v, int pos, long id) {
        final Cursor c = (Cursor) mAlarmsList.getAdapter().getItem(pos);
        Intent intent = new Intent(this, SetAlarmActivity.class);
        intent.putExtra(Intents.EXTRA_ID, c.getInt(Columns.ALARM_ID_INDEX));
        startActivity(intent);
    }
}
