/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.deskclock.tests;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.AlarmClock;

public class TestAddAlarm extends Activity {

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        Intent i = new Intent(AlarmClock.ACTION_SET_ALARM);
        i.putExtra(AlarmClock.EXTRA_MESSAGE, "New Alarm!");
        i.putExtra(AlarmClock.EXTRA_HOUR, 12);
        i.putExtra(AlarmClock.EXTRA_MINUTES, 27);

        startActivity(i);

        // Should not see a duplicate
        startActivity(i);

        i.removeExtra(AlarmClock.EXTRA_MESSAGE);
        startActivity(i);
        // No dup of null message.
        startActivity(i);

        // No extras, launches the alarm list.
        startActivity(new Intent(AlarmClock.ACTION_SET_ALARM));

        finish();
    }
}
