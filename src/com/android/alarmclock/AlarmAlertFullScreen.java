/*
 * Copyright (C) 2009 The Android Open Source Project
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

import android.graphics.drawable.BitmapDrawable;
import android.view.View;
import android.view.Gravity;
import android.view.LayoutInflater;

/**
 * Full screen alarm alert: pops visible indicator and plays alarm tone. This
 * activity displays the alert in full screen in order to be secure. The
 * background is the current wallpaper.
 */
public class AlarmAlertFullScreen extends AlarmAlert {

    @Override
    final protected View inflateView(LayoutInflater inflater) {
        View v = inflater.inflate(R.layout.alarm_alert, null);

        // Display the wallpaper as the background.
        BitmapDrawable wallpaper = (BitmapDrawable) getWallpaper();
        wallpaper.setGravity(Gravity.CENTER);
        v.setBackgroundDrawable(wallpaper);

        return v;
    }

}
