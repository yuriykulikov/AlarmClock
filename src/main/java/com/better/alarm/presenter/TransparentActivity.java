package com.better.alarm.presenter;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.better.alarm.configuration.AlarmApplication;
import com.better.alarm.interfaces.Alarm;
import com.better.alarm.interfaces.IAlarmsManager;
import com.better.alarm.interfaces.Intents;
import com.better.alarm.presenter.TimePickerDialogFragment.AlarmTimePickerDialogHandler;
import com.better.alarm.presenter.TimePickerDialogFragment.OnAlarmTimePickerCanceledListener;

import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;

public class TransparentActivity extends Activity implements AlarmTimePickerDialogHandler,
        OnAlarmTimePickerCanceledListener {

    private Alarm alarm;
    Disposable dialog = Disposables.disposed();

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        IAlarmsManager alarmsManager = AlarmApplication.alarms();
        Intent intent = getIntent();
        int id = intent.getIntExtra(Intents.EXTRA_ID, -1);
        alarm = alarmsManager.getAlarm(id);
    }

    @Override
    protected void onResume() {
        super.onResume();
        dialog = TimePickerDialogFragment.showTimePicker(getFragmentManager());
    }

    @Override
    protected void onPause() {
        super.onPause();
        dialog.dispose();
    }

    @Override
    public void onTimePickerCanceled() {
        finish();
    }

    @Override
    public void onDialogTimeSet(int hourOfDay, int minute) {
        alarm.snooze(hourOfDay, minute);
        finish();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
