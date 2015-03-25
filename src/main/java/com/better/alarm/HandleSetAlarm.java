/*
 * Copyright (C) 2010 The Android Open Source Project
 * Copyright (C) 2013 Yuriy Kulikov yuriy.kulikov.87@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.better.alarm;

import static android.provider.AlarmClock.ACTION_SET_ALARM;
import static android.provider.AlarmClock.EXTRA_HOUR;

import java.util.Collection;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.AlarmClock;

import com.better.alarm.model.AlarmsManager;
import com.better.alarm.model.interfaces.Alarm;
import com.better.alarm.model.interfaces.IAlarmsManager;
import com.better.alarm.model.interfaces.Intents;
import com.better.alarm.presenter.AlarmDetailsActivity;
import com.better.alarm.presenter.AlarmsListActivity;
import com.github.androidutils.logger.Logger;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;

public class HandleSetAlarm extends Activity {

    private IAlarmsManager alarms;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        alarms = AlarmsManager.getAlarmsManager();
        Intent intent = getIntent();
        Intent startDetailsIntent = new Intent(this, AlarmDetailsActivity.class);
        if (intent == null || !ACTION_SET_ALARM.equals(intent.getAction())) {
            finish();
            return;
        } else if (!intent.hasExtra(EXTRA_HOUR)) {
            // no extras - start list activity
            startActivity(new Intent(this, AlarmsListActivity.class));
            finish();
            return;
        }

        Alarm alarm = createNewAlarmFromIntent(intent);

        boolean skipUi = intent.getBooleanExtra(AlarmClock.EXTRA_SKIP_UI, false);

        if (!skipUi) {
            startDetailsIntent.putExtra(Intents.EXTRA_ID, alarm.getId());
            startActivity(startDetailsIntent);
        }
        finish();
    }

    /**
     * A new alarm has to be created or an existing one edited based on the
     * intent extras.
     */
    private Alarm createNewAlarmFromIntent(Intent intent) {
        final int hours = intent.getIntExtra(AlarmClock.EXTRA_HOUR, 0);
        final int minutes = intent.getIntExtra(AlarmClock.EXTRA_MINUTES, 0);
        final String msg = intent.getStringExtra(AlarmClock.EXTRA_MESSAGE);
        final String label = msg == null ? "" : msg;

        Collection<Alarm> sameAlarms = Collections2.filter(alarms.getAlarmsList(), new Predicate<Alarm>() {
            @Override
            public boolean apply(Alarm candidate) {
                boolean hoursMatch = candidate.getHour() == hours;
                boolean minutesMatch = candidate.getMinutes() == minutes;
                boolean labelsMatch = candidate.getLabel() != null && candidate.getLabel().equals(label);
                boolean noRepeating = !candidate.getDaysOfWeek().isRepeatSet();
                return hoursMatch && minutesMatch && labelsMatch && noRepeating;
            }
        });

        Alarm alarm;
        if (sameAlarms.isEmpty()) {
            Logger.getDefaultLogger().d("No alarm found, creating a new one");
            alarm = AlarmsManager.getAlarmsManager().createNewAlarm();
            //@formatter:off
            alarm.edit()
                .setHour(hours)
                .setMinutes(minutes)
                .setLabel(label)
                .setEnabled(true)
                .commit();
        //@formatter:on
        } else {
            Logger.getDefaultLogger().d("Enable existing alarm");
            alarm = sameAlarms.iterator().next();
            alarm.enable(true);
        }
        return alarm;
    }
}