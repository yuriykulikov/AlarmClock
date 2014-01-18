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

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.better.alarm.R;
import com.better.alarm.model.interfaces.Alarm;
import com.better.alarm.model.interfaces.Intents;
import com.better.alarm.presenter.AlarmsListFragment.ShowDetailsStrategy;
import com.better.alarm.presenter.TimePickerDialogFragment.AlarmTimePickerDialogHandler;

/**
 * This activity displays a list of alarms and optionally a details fragment.
 */
public class AlarmsListActivity extends Activity implements AlarmTimePickerDialogHandler {

    private ActionBarHandler mActionBarHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(DynamicThemeHandler.getInstance().getIdForName(AlarmsListActivity.class.getName()));
        super.onCreate(savedInstanceState);

        mActionBarHandler = new ActionBarHandler(this);

        boolean isTablet = !getResources().getBoolean(R.bool.isTablet);
        if (isTablet) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        setContentView(R.layout.list_activity);
        alarmsListFragment = (AlarmsListFragment) getFragmentManager().findFragmentById(
                R.id.list_activity_list_fragment);

        if (isTablet) {
            // TODO
            // alarmsListFragment.setShowDetailsStrategy(showDetailsInFragmentStrategy);
            alarmsListFragment.setShowDetailsStrategy(showDetailsInActivityFragment);
        } else {
            alarmsListFragment.setShowDetailsStrategy(showDetailsInActivityFragment);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        View nextAlarmFragment = findViewById(R.id.list_activity_info_fragment);
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("show_info_fragment", false)) {
            nextAlarmFragment.setVisibility(View.VISIBLE);
        } else {
            nextAlarmFragment.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return mActionBarHandler.onCreateOptionsMenu(menu, getMenuInflater(), getActionBar());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_item_add_alarm) {
            showDetailsInActivityFragment.showDetails(null);
            return true;
        } else return mActionBarHandler.onOptionsItemSelected(item);
    }

    // private final ShowDetailsStrategy showDetailsInFragmentStrategy = new
    // ShowDetailsStrategy() {
    //
    // @Override
    // public void showDetails(Alarm alarm) {
    // Intent intent = new Intent();
    // intent.putExtra(Intents.EXTRA_ID, alarm.getId());
    //
    // // Check what fragment is currently shown, replace if needed.
    // AlarmDetailsFragment details = (AlarmDetailsFragment)
    // getFragmentManager().findFragmentById(
    // R.id.alarmsDetailsFragmentFrame);
    // if (details == null || details.getIntent() != intent) {
    // // Make new fragment to show this selection.
    // details = AlarmDetailsFragment.newInstance(intent);
    //
    // // Execute a transaction, replacing any existing fragment
    // // with this one inside the frame.
    // FragmentTransaction ft = getFragmentManager().beginTransaction();
    // ft.replace(R.id.alarmsDetailsFragmentFrame, details);
    // ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
    // ft.commit();
    // }
    // }
    // };

    private final ShowDetailsStrategy showDetailsInActivityFragment = new ShowDetailsStrategy() {
        @Override
        public void showDetails(Alarm alarm) {
            Intent intent = new Intent(AlarmsListActivity.this, AlarmDetailsActivity.class);
            if (alarm != null) {
                intent.putExtra(Intents.EXTRA_ID, alarm.getId());
            }
            startActivity(intent);
        }
    };
    private AlarmsListFragment alarmsListFragment;

    public void showTimePicker(Alarm alarm) {
        TimePickerDialogFragment.showTimePicker(alarm, getFragmentManager());
    }

    @Override
    public void onDialogTimeSet(Alarm alarm, int hourOfDay, int minute) {
        alarm.edit().setEnabled(true).setHour(hourOfDay).setMinutes(minute).commit();
        // this must be invoked synchronously on the Pickers's OK button onClick
        // otherwise fragment is closed too soon and the time is not updated
        alarmsListFragment.updateAlarmsList();
    }
}
