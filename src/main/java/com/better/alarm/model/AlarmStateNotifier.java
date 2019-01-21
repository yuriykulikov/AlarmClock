package com.better.alarm.model;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.better.alarm.Broadcasts;
import com.better.alarm.model.AlarmCore.IStateNotifier;
import com.better.alarm.model.interfaces.Intents;
import com.better.alarm.presenter.alert.AlarmAlertFullScreen;
import com.better.alarm.presenter.alert.AlarmAlertReceiver;
import com.better.alarm.presenter.background.KlaxonService;
import com.better.alarm.presenter.background.ScheduledReceiver;
import com.better.alarm.presenter.background.VibrationService;

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

    @Override
    public void broadcastAlarmState(int id, String action, long millis) {
        Intent intent = new Intent();
        intent.setAction(action);
        intent.putExtra(Intents.EXTRA_ID, id);
        intent.putExtra(Intents.EXTRA_NEXT_NORMAL_TIME_IN_MILLIS, millis);
        Broadcasts.sendExplicit(mContext, intent);
    }
}
