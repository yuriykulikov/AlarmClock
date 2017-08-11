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

package com.better.alarm.presenter;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.AlarmClock;

import com.better.alarm.configuration.AlarmApplication;
import com.better.alarm.configuration.Store;
import com.better.alarm.interfaces.Alarm;
import com.better.alarm.interfaces.Intents;
import com.better.alarm.logger.Logger;
import com.better.alarm.model.AlarmValue;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;

import java.util.Collection;
import java.util.List;

import static com.better.alarm.configuration.AlarmApplication.container;


public class HandleSetAlarm extends Activity {
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Intent intent = getIntent();
        Intent startDetailsIntent = new Intent(this, AlarmDetailsActivity.class);
        if (intent == null || !AlarmClock.ACTION_SET_ALARM.equals(intent.getAction())) {
            finish();
            return;
        } else if (!intent.hasExtra(AlarmClock.EXTRA_HOUR)) {
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

        List<AlarmValue> alarms = container().store().alarms().blockingFirst();
        Collection<AlarmValue> sameAlarms = Collections2.filter(alarms, new Predicate<AlarmValue>() {
            @Override
            public boolean apply(AlarmValue candidate) {
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
            alarm = container().alarms().createNewAlarm();
            //@formatter:off
            alarm.edit()
                    .withHour(hours)
                    .withMinutes(minutes)
                    .withLabel(label)
                    .withIsEnabled(true)
                    .commit();
            //@formatter:on
        } else {
            Logger.getDefaultLogger().d("Enable existing alarm");
            alarm = container().alarms().getAlarm(sameAlarms.iterator().next().getId());
            alarm.enable(true);
        }
        return alarm;
    }
}