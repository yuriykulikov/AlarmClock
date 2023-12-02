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

package com.better.alarm.ui.toast

import android.content.Context
import android.widget.Toast
import com.better.alarm.domain.Store
import com.better.alarm.util.subscribeForever

class ToastPresenter(private val store: Store, private val context: Context) {
  private var sToast: Toast? = null

  fun start() {
    store
        .sets()
        .withLatestFrom(store.uiVisible, { set, uiVisible -> set to uiVisible })
        .subscribeForever { (set: Store.AlarmSet, uiVisible: Boolean) ->
          if (!uiVisible && set.alarm.isEnabled && set.fromUserInteraction) {
            popAlarmSetToast(context, set.millis)
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
}
