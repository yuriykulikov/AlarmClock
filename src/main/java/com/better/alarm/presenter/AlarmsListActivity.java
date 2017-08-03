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

import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.better.alarm.R;
import com.better.alarm.configuration.AlarmApplication;
import com.better.alarm.interfaces.Alarm;
import com.better.alarm.model.AlarmValue;
import com.better.alarm.presenter.AlarmsListFragment.ShowDetailsStrategy;
import com.google.common.base.Optional;

import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import io.reactivex.functions.Consumer;

import static com.better.alarm.configuration.AlarmApplication.container;


/**
 * This activity displays a list of alarms and optionally a details fragment.
 */
public class AlarmsListActivity extends AppCompatActivity {
    private ActionBarHandler mActionBarHandler;
    private ShowDetailsStrategy details;

    private Alarm timePickerAlarm;
    private Disposable timePickerDialogDisposable = Disposables.disposed();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(DynamicThemeHandler.getInstance().getIdForName(AlarmsListActivity.class.getName()));
        super.onCreate(savedInstanceState);
        this.details = new ShowDetailsInActivity(this);
        this.mActionBarHandler = new ActionBarHandler(this, details);

        boolean isTablet = !getResources().getBoolean(R.bool.isTablet);
        if (isTablet) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        setContentView(R.layout.list_activity);

        View fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                details.createNewAlarm();
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AlarmsListFragment alarmsListFragment = (AlarmsListFragment) getFragmentManager()
                    .findFragmentById(R.id.list_activity_list_fragment);

            //TODO ((FloatingActionButton) fab).attachToListView(alarmsListFragment.getListView());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return mActionBarHandler.onCreateOptionsMenu(menu, getMenuInflater(), getActionBar());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return mActionBarHandler.onOptionsItemSelected(item);
    }

    public void showTimePicker(AlarmValue alarm) {
        timePickerAlarm = container().alarms().getAlarm(alarm.getId());
        timePickerDialogDisposable = TimePickerDialogFragment.showTimePicker(getFragmentManager())
                .subscribe(new Consumer<Optional<TimePickerDialogFragment.PickedTime>>() {
                    @Override
                    public void accept(@NonNull Optional<TimePickerDialogFragment.PickedTime> picked) {
                        if (picked.isPresent()) {
                            timePickerAlarm.edit()
                                    .withIsEnabled(true)
                                    .withHour(picked.get().hour())
                                    .withMinutes(picked.get().minute())
                                    .commit();
                            timePickerAlarm = null;
                        }
                    }
                });
    }

    @Override
    protected void onPause() {
        //dismiss the time picker if it was showing. Otherwise we will have to store the state and it is not nice for the user
        timePickerDialogDisposable.dispose();
        timePickerAlarm = null;
        super.onPause();
    }
}
