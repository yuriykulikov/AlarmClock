package com.better.alarm.model;

import android.content.Context;
import android.content.Intent;

import com.better.alarm.model.AlarmCore.IStateNotifier;
import com.better.alarm.interfaces.Intents;
import com.google.inject.Inject;

/**
 * Broadcasts alarm state with an intent
 * 
 * @author Yuriy
 * 
 */
public class AlarmStateNotifier implements IStateNotifier {

    private final Context mContext;

    @Inject
    public AlarmStateNotifier(Context context) {
        mContext = context;
    }

    @Override
    public void broadcastAlarmState(int id, String action) {
        Intent intent = new Intent(action);
        intent.putExtra(Intents.EXTRA_ID, id);
        mContext.sendBroadcast(intent);
    }
}
