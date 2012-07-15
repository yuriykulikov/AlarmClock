package com.better.alarm.model;

import android.app.Application;

public class AlarmApplication extends Application {

    @Override
    public void onCreate() {
        AlarmsManager.init(getApplicationContext());
        super.onCreate();
    }

}
