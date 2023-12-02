/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.better.alarm.ui.settings;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.better.alarm.R;
import com.better.alarm.bootstrap.AlarmApplication;
import com.better.alarm.bootstrap.InjectKt;
import com.better.alarm.ui.main.AlarmsListActivity;
import com.better.alarm.ui.themes.DynamicThemeHandler;

/** Settings for the Alarm Clock. */
public class SettingsActivity extends AppCompatActivity {
  private final DynamicThemeHandler dynamicThemeHandler =
      InjectKt.javaInject(DynamicThemeHandler.class);

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    AlarmApplication.startOnce(getApplication());
    setTheme(dynamicThemeHandler.defaultTheme());
    super.onCreate(savedInstanceState);
    setContentView(R.layout.settings_activity);
    if (!getResources().getBoolean(R.bool.isTablet)) {
      setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == android.R.id.home) {
      goBack();
      return true;
    } else return false;
  }

  private void goBack() {
    // This is called when the Home (Up) button is pressed
    // in the Action Bar.
    Intent parentActivityIntent = new Intent(this, AlarmsListActivity.class);
    // parentActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
    // Intent.FLAG_ACTIVITY_NEW_TASK);
    startActivity(parentActivityIntent);
    finish();
  }
}
