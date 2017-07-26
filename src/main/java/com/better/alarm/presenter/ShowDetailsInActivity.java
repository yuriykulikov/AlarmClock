package com.better.alarm.presenter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.better.alarm.interfaces.Intents;
import com.better.alarm.model.AlarmValue;
import com.better.alarm.presenter.AlarmDetailsActivity;
import com.better.alarm.presenter.AlarmsListFragment;
import com.google.common.base.Preconditions;

/**
 * Created by Yuriy on 25.07.2017.
 */
public class ShowDetailsInActivity implements AlarmsListFragment.ShowDetailsStrategy {
    private final Context context;

    //not injected - requires activity
    public ShowDetailsInActivity(Activity context) {
        this.context = context;
    }

    @Override
    public void showDetails(AlarmValue alarm) {
        Preconditions.checkNotNull(alarm);
        Intent intent = new Intent(context, AlarmDetailsActivity.class);
        intent.putExtra(Intents.EXTRA_ID, alarm.getId());
        context.startActivity(intent);
    }

    @Override
    public void createNewAlarm() {
        Intent intent = new Intent(context, AlarmDetailsActivity.class);
        context.startActivity(intent);
    }
}
