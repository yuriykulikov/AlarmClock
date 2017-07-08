package com.better.alarm.model;

import android.net.Uri;

/**
 * Created by Yuriy on 08.07.2017.
 */

public interface AlarmChangeData extends  AlarmValue {
    boolean isPrealarm();

    Uri getAlert();

    boolean isVibrate();
}
