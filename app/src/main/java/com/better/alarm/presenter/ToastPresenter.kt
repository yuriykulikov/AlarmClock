/*
 * Copyright (C) 2008 The Android Open Source Project
 * Copyright (C) 2013 Yuriy Kulikov
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

package com.better.alarm.presenter

import android.content.Context
import android.widget.Toast
import com.better.alarm.R
import com.better.alarm.configuration.Store
import com.better.alarm.model.AlarmValue

class ToastPresenter(private val store: Store, private val context: Context) {
    private var sToast: Toast? = null

    fun start() {
        store.sets().subscribe { (alarm: AlarmValue, millis: Long) ->
            if (alarm.isEnabled) {
                popAlarmSetToast(context, millis)
            }
        }
    }

    fun setToast(toast: Toast) {
        sToast?.cancel()
        sToast = toast
    }

    private fun popAlarmSetToast(context: Context, timeInMillis: Long) {
        val toastText = formatToast(context, timeInMillis)
        val toast = Toast.makeText(context, toastText, Toast.LENGTH_LONG)
        setToast(toast)
        toast.show()
    }

    /**
     * format "Alarm set for 2 days 7 hours and 53 minutes from now"
     *
     *
     * If prealarm is on it will be
     *
     *
     * "Alarm set for 2 days 7 hours and 53 minutes from now. Prealarm will
     * start 30 minutes before the main alarm".
     */
    private fun formatToast(context: Context, timeInMillis: Long): String {
        val delta = timeInMillis - System.currentTimeMillis()
        val _hours = delta / (1000 * 60 * 60)
        val minutes = delta / (1000 * 60) % 60
        val days = _hours / 24
        val hours = _hours % 24

        val daySeq = when (days) {
            0L -> ""
            1L -> context.getString(R.string.day)
            else -> context.getString(R.string.days, days.toString())
        }

        val minSeq = when (minutes) {
            0L -> ""
            1L -> context.getString(R.string.minute)
            else -> context.getString(R.string.minutes, minutes.toString())
        }

        val hourSeq = when (hours) {
            0L -> ""
            1L -> context.getString(R.string.hour)
            else -> context.getString(R.string.hours, hours.toString())
        }

        val dispDays = days > 0
        val dispHour = hours > 0
        val dispMinute = minutes > 0

        val index = when {
            dispDays && !dispHour && !dispMinute -> 1 // days
            !dispDays && dispHour && !dispMinute -> 2 // hours
            dispDays && dispHour && !dispMinute -> 3 // days, hours
            !dispDays && !dispHour && dispMinute -> 4 // minutes
            dispDays && !dispHour && dispMinute -> 5 // days, minutes
            !dispDays && dispHour && dispMinute -> 6 // hours, minutes
            dispDays && dispHour && dispMinute -> 7 // days, hours, minutes
            else -> 0 // Alarm set for less than 1 minute from now.
        }

        val formats = context.resources.getStringArray(R.array.alarm_set)
        return String.format(formats[index], daySeq, hourSeq, minSeq)
    }
}
