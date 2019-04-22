/*
 * Copyright (C) 2012 The Android Open Source Project
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
 * limitations under the License
 */

package com.better.alarm.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.better.alarm.R;

/**
 * @author Yuriy
 */
public class TimerView extends LinearLayout {

    private TextView mHoursOnes, mMinutesOnes;
    private TextView mHoursTens, mMinutesTens;
    private TextView mSeconds;

    public TimerView(Context context) {
        this(context, null);
    }

    public TimerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mHoursTens = (TextView) findViewById(R.id.hours_tens);
        mMinutesTens = (TextView) findViewById(R.id.minutes_tens);
        mHoursOnes = (TextView) findViewById(R.id.hours_ones);
        mMinutesOnes = (TextView) findViewById(R.id.minutes_ones);
    }

    public void setTime(int hoursTensDigit, int hoursOnesDigit, int minutesTensDigit, int minutesOnesDigit, int seconds) {
        if (mHoursTens != null) {
            // Hide digit
            if (hoursTensDigit == -2) {
                mHoursTens.setVisibility(View.INVISIBLE);
            } else if (hoursTensDigit == -1) {
                mHoursTens.setText("-");
                mHoursTens.setEnabled(false);
                mHoursTens.setVisibility(View.VISIBLE);
            } else {
                mHoursTens.setText(String.format("%d", hoursTensDigit));
                mHoursTens.setEnabled(true);
                mHoursTens.setVisibility(View.VISIBLE);
            }
        }
        if (mHoursOnes != null) {
            if (hoursOnesDigit == -1) {
                mHoursOnes.setText("-");
                mHoursOnes.setEnabled(false);
            } else {
                mHoursOnes.setText(String.format("%d", hoursOnesDigit));
                mHoursOnes.setEnabled(true);
            }
        }
        if (mMinutesTens != null) {
            if (minutesTensDigit == -1) {
                mMinutesTens.setText("-");
                mMinutesTens.setEnabled(false);
            } else {
                mMinutesTens.setEnabled(true);
                mMinutesTens.setText(String.format("%d", minutesTensDigit));
            }
        }
        if (mMinutesOnes != null) {
            if (minutesOnesDigit == -1) {
                mMinutesOnes.setText("-");
                mMinutesOnes.setEnabled(false);
            } else {
                mMinutesOnes.setText(String.format("%d", minutesOnesDigit));
                mMinutesOnes.setEnabled(true);
            }
        }

        if (mSeconds != null) {
            mSeconds.setText(String.format("%02d", seconds));
        }
    }
}
