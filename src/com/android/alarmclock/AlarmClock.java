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

package com.android.alarmclock;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.CheckBox;

import java.util.Calendar;
import java.text.DateFormatSymbols;

/**
 * AlarmClock application.
 */
public class AlarmClock extends Activity implements OnItemClickListener {

    final static String PREFERENCES = "AlarmClock";
    final static String PREF_CLOCK_FACE = "face";
    final static String PREF_SHOW_CLOCK = "show_clock";

    /** Cap alarm count at this number */
    final static int MAX_ALARM_COUNT = 12;

    /** This must be false for production.  If true, turns on logging,
        test code, etc. */
    final static boolean DEBUG = false;

    private SharedPreferences mPrefs;
    private LayoutInflater mFactory;
    private ViewGroup mClockLayout;
    private View mClock = null;
    private ListView mAlarmsList;
    private Cursor mCursor;

    private String mAm, mPm;

    /**
     * Which clock face to show
     */
    private int mFace = -1;

    /*
     * FIXME: it would be nice for this to live in an xml config file.
     */
    final static int[] CLOCKS = {
        R.layout.clock_basic_bw,
        R.layout.clock_googly,
        R.layout.clock_droid2,
        R.layout.clock_droids,
        R.layout.digital_clock
    };

    private class AlarmTimeAdapter extends CursorAdapter {
        public AlarmTimeAdapter(Context context, Cursor cursor) {
            super(context, cursor);
        }

        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View ret = mFactory.inflate(R.layout.alarm_time, parent, false);

            ((TextView) ret.findViewById(R.id.am)).setText(mAm);
            ((TextView) ret.findViewById(R.id.pm)).setText(mPm);

            DigitalClock digitalClock = (DigitalClock)ret.findViewById(R.id.digitalClock);
            digitalClock.setLive(false);
            if (Log.LOGV) Log.v("newView " + cursor.getPosition());
            return ret;
        }

        public void bindView(View view, Context context, Cursor cursor) {
            final Alarm alarm = new Alarm(cursor);

            CheckBox onButton = (CheckBox)view.findViewById(R.id.alarmButton);
            onButton.setChecked(alarm.enabled);
            onButton.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        boolean isChecked = ((CheckBox) v).isChecked();
                        Alarms.enableAlarm(AlarmClock.this, alarm.id,
                            isChecked);
                        if (isChecked) {
                            SetAlarm.popAlarmSetToast(AlarmClock.this,
                                alarm.hour, alarm.minutes, alarm.daysOfWeek);
                        }
                    }
            });

            DigitalClock digitalClock =
                    (DigitalClock) view.findViewById(R.id.digitalClock);

            // set the alarm text
            final Calendar c = Calendar.getInstance();
            c.set(Calendar.HOUR_OF_DAY, alarm.hour);
            c.set(Calendar.MINUTE, alarm.minutes);
            digitalClock.updateTime(c);

            // Set the repeat text or leave it blank if it does not repeat.
            TextView daysOfWeekView =
                    (TextView) digitalClock.findViewById(R.id.daysOfWeek);
            final String daysOfWeekStr =
                    alarm.daysOfWeek.toString(AlarmClock.this, false);
            if (daysOfWeekStr != null && daysOfWeekStr.length() != 0) {
                daysOfWeekView.setText(daysOfWeekStr);
                daysOfWeekView.setVisibility(View.VISIBLE);
            } else {
                daysOfWeekView.setVisibility(View.GONE);
            }

            // Display the label
            TextView labelView =
                    (TextView) digitalClock.findViewById(R.id.label);
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
        final AdapterContextMenuInfo info =
                (AdapterContextMenuInfo) item.getMenuInfo();
        final int id = (int) info.id;
        switch (item.getItemId()) {
            case R.id.delete_alarm:
                // Confirm that the alarm will be deleted.
                new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.delete_alarm))
                        .setMessage(getString(R.string.delete_alarm_confirm))
                        .setPositiveButton(android.R.string.ok,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface d,
                                            int w) {
                                        Alarms.deleteAlarm(AlarmClock.this, id);
                                    }
                                })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
                return true;

            case R.id.enable_alarm:
                final Cursor c = (Cursor) mAlarmsList.getAdapter()
                        .getItem(info.position);
                final Alarm alarm = new Alarm(c);
                Alarms.enableAlarm(this, alarm.id, !alarm.enabled);
                if (!alarm.enabled) {
                    SetAlarm.popAlarmSetToast(this, alarm.hour, alarm.minutes,
                            alarm.daysOfWeek);
                }
                return true;

            default:
                break;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        String[] ampm = new DateFormatSymbols().getAmPmStrings();
        mAm = ampm[0];
        mPm = ampm[1];

        mFactory = LayoutInflater.from(this);
        mPrefs = getSharedPreferences(PREFERENCES, 0);
        mCursor = Alarms.getAlarmsCursor(getContentResolver());

        updateLayout();
        setClockVisibility(mPrefs.getBoolean(PREF_SHOW_CLOCK, true));
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateLayout();
        inflateClock();
    }

    private void updateLayout() {
        setContentView(R.layout.alarm_clock);
        mAlarmsList = (ListView) findViewById(R.id.alarms_list);
        mAlarmsList.setAdapter(new AlarmTimeAdapter(this, mCursor));
        mAlarmsList.setVerticalScrollBarEnabled(true);
        mAlarmsList.setOnItemClickListener(this);
        mAlarmsList.setOnCreateContextMenuListener(this);

        mClockLayout = (ViewGroup) findViewById(R.id.clock_layout);
        mClockLayout.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    final Intent intent =
                            new Intent(AlarmClock.this, ClockPicker.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }
            });
    }

    @Override
    protected void onResume() {
        super.onResume();

        int face = mPrefs.getInt(PREF_CLOCK_FACE, 0);
        if (mFace != face) {
            if (face < 0 || face >= AlarmClock.CLOCKS.length) {
                mFace = 0;
            } else {
                mFace = face;
            }
            inflateClock();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ToastMaster.cancelToast();
        mCursor.deactivate();
    }

    protected void inflateClock() {
        if (mClock != null) {
            mClockLayout.removeView(mClock);
        }

        LayoutInflater.from(this).inflate(CLOCKS[mFace], mClockLayout);
        mClock = findViewById(R.id.clock);

        TextView am = (TextView) findViewById(R.id.am);
        TextView pm = (TextView) findViewById(R.id.pm);

        if (am != null) {
            am.setText(mAm);
        }
        if (pm != null) {
            pm.setText(mPm);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate our menu.
        getMenuInflater().inflate(R.menu.main_menu, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view,
            ContextMenuInfo menuInfo) {
        // Inflate the menu from xml.
        getMenuInflater().inflate(R.menu.context_menu, menu);

        // Use the current item to create a custom view for the header.
        final AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        final Cursor c =
                (Cursor) mAlarmsList.getAdapter().getItem((int) info.position);
        final Alarm alarm = new Alarm(c);

        // Construct the Calendar to compute the time.
        final Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, alarm.hour);
        cal.set(Calendar.MINUTE, alarm.minutes);
        final String time = Alarms.formatTime(this, cal);

        // Inflate the custom view and set each TextView's text.
        final View v = mFactory.inflate(R.layout.context_menu_header, null);
        TextView textView = (TextView) v.findViewById(R.id.header_time);
        textView.setText(time);
        textView = (TextView) v.findViewById(R.id.header_label);
        textView.setText(alarm.label);

        // Set the custom view on the menu.
        menu.setHeaderView(v);
        // Change the text to "disable" if the alarm is already enabled.
        if (alarm.enabled) {
            menu.findItem(R.id.enable_alarm).setTitle(R.string.disable_alarm);
        }
    }

    public void onItemClick(AdapterView parent, View v, int pos, long id) {
        Intent intent = new Intent(this, SetAlarm.class);
        intent.putExtra(Alarms.ALARM_ID, (int) id);
        startActivity(intent);
    }

    /**
     * Only allow user to add a new alarm if there are fewer than
     * MAX_ALARM_COUNT
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.menu_add_alarm).setVisible(
                mAlarmsList.getAdapter().getCount() < MAX_ALARM_COUNT);
        menu.findItem(R.id.menu_toggle_clock).setTitle(
                getClockVisibility() ? R.string.hide_clock
                    : R.string.show_clock);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_add_alarm:
                Uri uri = Alarms.addAlarm(getContentResolver());
                // FIXME: scroll to new item?
                String segment = uri.getPathSegments().get(1);
                int newId = Integer.parseInt(segment);
                if (Log.LOGV) {
                    Log.v("In AlarmClock, new alarm id = " + newId);
                }
                Intent intent = new Intent(this, SetAlarm.class);
                intent.putExtra(Alarms.ALARM_ID, newId);
                startActivity(intent);
                return true;

            case R.id.menu_toggle_clock:
                setClockVisibility(!getClockVisibility());
                saveClockVisibility();
                return true;

            case R.id.menu_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private boolean getClockVisibility() {
        return mClockLayout.getVisibility() == View.VISIBLE;
    }

    private void setClockVisibility(boolean visible) {
        mClockLayout.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void saveClockVisibility() {
        mPrefs.edit().putBoolean(PREF_SHOW_CLOCK, getClockVisibility()).commit();
    }
}
