package com.better.alarm.services;

import android.content.Intent;
import android.os.IBinder;
import androidx.annotation.Nullable;

/** Created by Yuriy on 15.08.2017. */
public class Service extends android.app.Service {
  @Nullable
  @Override
  public final IBinder onBind(Intent intent) {
    return null;
  }
}
