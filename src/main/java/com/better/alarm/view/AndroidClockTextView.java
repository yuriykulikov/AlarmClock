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

package com.better.alarm.view;

import org.acra.ACRA;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

import com.better.alarm.logger.Logger;

/**
 * Displays text using the special AndroidClock font.
 */
public class AndroidClockTextView extends TextView {

    private static final String SYSTEM = "/system/fonts/";
    private static final String SYSTEM_FONT_TIME_BACKGROUND = SYSTEM + "AndroidClock.ttf";

    private static final String ATTR_USE_CLOCK_TYPEFACE = "useClockTypeface";

    private static Typeface sClockTypeface;
    private static Typeface sStandardTypeface;

    public AndroidClockTextView(Context context) {
        super(context);
    }

    public AndroidClockTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public AndroidClockTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        boolean useClockTypeface = attrs.getAttributeBooleanValue(null, ATTR_USE_CLOCK_TYPEFACE, true) && !isInEditMode();

        sStandardTypeface = Typeface.DEFAULT;
        Paint paint = getPaint();
        try {
            if (sClockTypeface == null && useClockTypeface) {
                sClockTypeface = Typeface.createFromFile(SYSTEM_FONT_TIME_BACKGROUND);
            }
            paint.setTypeface(useClockTypeface ? sClockTypeface : sStandardTypeface);
        } catch (RuntimeException e) {
            Logger.getDefaultLogger().e(e.getMessage());
            ACRA.getErrorReporter().handleSilentException(e);
            paint.setTypeface(sStandardTypeface);
        }
    }
}
