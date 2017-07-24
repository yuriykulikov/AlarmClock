/*
 * Copyright (C) 2008 The Android Open Source Project
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

import java.text.DateFormatSymbols;
import java.util.Calendar;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.ListPreference;
import android.util.AttributeSet;

import com.better.alarm.model.DaysOfWeek;
import com.better.alarm.model.ImmutableDaysOfWeek;

public class RepeatPreference extends ListPreference {

    // Initial value that can be set with the values saved in the database.
    private DaysOfWeek mDaysOfWeek;

    public RepeatPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        String[] weekdays = new DateFormatSymbols().getWeekdays();
        String[] values = new String[]{weekdays[Calendar.MONDAY], weekdays[Calendar.TUESDAY],
                weekdays[Calendar.WEDNESDAY], weekdays[Calendar.THURSDAY], weekdays[Calendar.FRIDAY],
                weekdays[Calendar.SATURDAY], weekdays[Calendar.SUNDAY],};
        setEntries(values);
        setEntryValues(values);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            setSummary(mDaysOfWeek.toString(getContext(), true));
            callChangeListener(mDaysOfWeek);
        }
    }

    @Override
    protected void onPrepareDialogBuilder(Builder builder) {
        CharSequence[] entries = getEntries();
        builder.setMultiChoiceItems(entries, mDaysOfWeek.getBooleanArray(),
                new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        int mutableDays = mDaysOfWeek.getCoded();
                        if (isChecked) {
                            mutableDays |= 1 << which;
                        } else {
                            mutableDays &= ~(1 << which);
                        }
                        mDaysOfWeek = ImmutableDaysOfWeek.of(mutableDays);
                    }
                });
    }

    public void setDaysOfWeek(DaysOfWeek dow) {
        mDaysOfWeek = dow;
        setSummary(dow.toString(getContext(), true));
    }

    public DaysOfWeek getDaysOfWeek() {
        return mDaysOfWeek;
    }
}
