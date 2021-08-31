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

package com.better.alarm.view

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.better.alarm.model.DaysOfWeek
import io.reactivex.Single
import java.text.DateFormatSymbols
import java.util.Calendar

fun DaysOfWeek.summary(context: Context): String {
  return toString(context, true)
}

fun DaysOfWeek.showDialog(context: Context): Single<DaysOfWeek> {
  return Single.create { emitter ->
    val weekdays = DateFormatSymbols().weekdays
    val entries =
        arrayOf(
            weekdays[Calendar.MONDAY],
            weekdays[Calendar.TUESDAY],
            weekdays[Calendar.WEDNESDAY],
            weekdays[Calendar.THURSDAY],
            weekdays[Calendar.FRIDAY],
            weekdays[Calendar.SATURDAY],
            weekdays[Calendar.SUNDAY])
    var mutableDays = coded
    AlertDialog.Builder(context)
        .setMultiChoiceItems(entries, booleanArray) { _, which, isChecked ->
          mutableDays =
              when {
                isChecked -> mutableDays or (1 shl which)
                else -> mutableDays and (1 shl which).inv()
              }
        }
        .setPositiveButton(android.R.string.ok) { _, _ ->
          emitter.onSuccess(DaysOfWeek(mutableDays))
        }
        .setOnCancelListener { emitter.onSuccess(DaysOfWeek(mutableDays)) }
        .create()
        .show()
  }
}
