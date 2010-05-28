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
import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Bundle;

import java.util.Calendar;

public class TestAddAlarm extends Activity {

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        ContentResolver cr = getContentResolver();

        ContentValues values = new ContentValues();

        // 1:23 pm
        values.put("hour", 13);
        values.put("minutes", 23);
        values.put("message", "Auto-added alarm");

        cr.insert(Uri.parse("content://com.android.deskclock.alarmprovider/add"),
                  values);

        finish();
    }
}
