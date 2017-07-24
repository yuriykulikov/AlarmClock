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
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.better.alarm.AlarmApplication;
import com.better.alarm.R;
import com.better.alarm.ShowDetailsInActivity;
import com.better.alarm.interfaces.Alarm;
import com.better.alarm.logger.Logger;
import com.better.alarm.model.AlarmValue;
import com.better.alarm.presenter.AlarmsListFragment.ShowDetailsStrategy;
import com.better.alarm.presenter.TimePickerDialogFragment.AlarmTimePickerDialogHandler;
import com.melnykov.fab.FloatingActionButton;


/**
 * This activity displays a list of alarms and optionally a details fragment.
 */
public class AlarmsListActivity extends Activity implements AlarmTimePickerDialogHandler {
    private ActionBarHandler mActionBarHandler;
    private ShowDetailsStrategy details;

    private Alarm timePickerAlarm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(DynamicThemeHandler.getInstance().getIdForName(AlarmsListActivity.class.getName()));
        super.onCreate(savedInstanceState);
        AlarmApplication.guice().injectMembers(this);
        this.details = new ShowDetailsInActivity(this);
        this.mActionBarHandler = new ActionBarHandler(this, details);

        boolean isTablet = !getResources().getBoolean(R.bool.isTablet);
        if (isTablet) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        setContentView(R.layout.list_activity);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AlarmsListFragment alarmsListFragment = (AlarmsListFragment) getFragmentManager()
                    .findFragmentById(R.id.list_activity_list_fragment);

            FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
            fab.attachToListView(alarmsListFragment.getListView());
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    details.createNewAlarm();
                }
            });
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
        timePickerAlarm = AlarmApplication.alarms().getAlarm(alarm.getId());
        TimePickerDialogFragment.showTimePicker(getFragmentManager());
    }

    @Override
    public void onDialogTimeSet(int hourOfDay, int minute) {
        timePickerAlarm.edit().withIsEnabled(true).withHour(hourOfDay).withMinutes(minute).commit();
        // this must be invoked synchronously on the Pickers's OK button onClick
        // otherwise fragment is closed too soon and the time is not updated
        //alarmsListFragment.updateAlarmsList();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (timePickerAlarm != null) {
            outState.putInt("timePickerAlarm", timePickerAlarm.getId());
        }
    }

    /**
     * I do not know why but sometimes we get funny exceptions like this:
     * <p>
     * <pre>
     * STACK_TRACE=java.lang.NullPointerException
     *         at com.better.alarm.presenter.AlarmsListActivity.onDialogTimeSet(AlarmsListActivity.java:139)
     *         at com.better.alarm.presenter.TimePickerDialogFragment$2.onClick(TimePickerDialogFragment.java:90)
     *         at android.view.View.performClick(View.java:4204)
     *         at android.view.View$PerformClick.run(View.java:17355)
     *         at android.os.Handler.handleCallback(Handler.java:725)
     *         at android.os.Handler.dispatchMessage(Handler.java:92)
     *         at android.os.Looper.loop(Looper.java:137)
     *         at android.app.ActivityThread.main(ActivityThread.java:5041)
     *         at java.lang.reflect.Method.invokeNative(Native Method)
     *         at java.lang.reflect.Method.invoke(Method.java:511)
     *         at com.android.internal.os.ZygoteInit$MethodAndArgsCaller.run(ZygoteInit.java:793)
     *         at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:560)
     *         at dalvik.system.NativeStart.main(Native Method)
     * </pre>
     * <p>
     * And this happens on application start. So I suppose the fragment is
     * showing even though the activity is not there. So we can use this method
     * to make sure the alarm is there.
     */
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        try {
            timePickerAlarm = AlarmApplication.alarms().getAlarm(
                    savedInstanceState.getInt("timePickerAlarm", -1));
            Logger.getDefaultLogger().d("restored " + timePickerAlarm.toString());
        } catch (Exception e) {
            Logger.getDefaultLogger().d("no timePickerAlarm was restored");
        }
    }
}
