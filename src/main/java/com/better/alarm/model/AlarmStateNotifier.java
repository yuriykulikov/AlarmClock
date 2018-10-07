package com.better.alarm.model;

import android.content.Context;
import android.content.Intent;

import com.better.alarm.Broadcasts;
import com.better.alarm.interfaces.Intents;
import com.better.alarm.model.AlarmCore.IStateNotifier;

/**
 * Broadcasts alarm state with an intent
 *
 * @author Yuriy
 */
public class AlarmStateNotifier implements IStateNotifier {

    private final Context mContext;

    public AlarmStateNotifier(Context context) {
        mContext = context;
    }

    @Override
    public void broadcastAlarmState(int id, String action) {
        Intent intent = new Intent();
        intent.setAction(action);
        intent.putExtra(Intents.EXTRA_ID, id);
        Broadcasts.sendExplicit(mContext, intent);
    }
}
