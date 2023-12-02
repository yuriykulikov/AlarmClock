/*
 * Copyright (C) 2012 Yuriy Kulikov yuriy.kulikov.87@gmail.com
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
package com.better.alarm.receivers;

import android.app.AlarmManager;
import android.app.AlarmManager.AlarmClockInfo;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import com.better.alarm.data.Prefs;
import com.better.alarm.domain.Store;
import com.better.alarm.platform.OreoKt;
import com.better.alarm.ui.main.AlarmsListActivity;
import com.better.alarm.util.Optional;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;

/**
 * @author Yuriy
 */
public class ScheduledReceiver {
  private static final String DM12 = "E h:mm aa";
  private static final String DM24 = "E kk:mm";
  private static final Intent FAKE_INTENT_JUST_TO_DISPLAY_IN_ICON =
      new Intent("FAKE_ACTION_JUST_TO_DISPLAY_AN_ICON");
  private Store store;
  private Context context;
  private Prefs prefs;
  private AlarmManager am;

  public ScheduledReceiver(Store store, final Context context, Prefs prefs, AlarmManager am) {
    this.store = store;
    this.context = context;
    this.prefs = prefs;
    this.am = am;
  }

  public void start() {
    store
        .next()
        .subscribe(
            new Consumer<Optional<Store.Next>>() {
              @Override
              public void accept(@NonNull Optional<Store.Next> nextOptional) throws Exception {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                  // we use setAlarmClock for these anyway, so nothing to do here.
                } else {
                  doForLollipop(context, nextOptional);
                }
              }
            });
  }

  private void doForLollipop(Context context, Optional<Store.Next> nextOptional) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !am.canScheduleExactAlarms()) {
      return;
    }

    if (nextOptional.isPresent()) {
      int id = nextOptional.get().alarm().getId();

      Intent showList = new Intent(context, AlarmsListActivity.class);
      showList.putExtra(Intents.EXTRA_ID, id);
      PendingIntent showIntent =
          PendingIntent.getActivity(context, id, showList, OreoKt.pendingIntentUpdateCurrentFlag());

      long milliseconds = nextOptional.get().nextNonPrealarmTime();

      am.setAlarmClock(
          new AlarmClockInfo(milliseconds, showIntent),
          PendingIntent.getBroadcast(
              context,
              hashCode(),
              FAKE_INTENT_JUST_TO_DISPLAY_IN_ICON,
              OreoKt.pendingIntentUpdateCurrentFlag()));
    } else {
      am.cancel(
          PendingIntent.getBroadcast(
              context,
              hashCode(),
              FAKE_INTENT_JUST_TO_DISPLAY_IN_ICON,
              OreoKt.pendingIntentUpdateCurrentFlag()));
    }
  }
}
