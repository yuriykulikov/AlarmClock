package com.better.alarm.presenter;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.better.alarm.interfaces.Alarm;
import com.better.alarm.interfaces.Intents;
import com.better.alarm.util.Optional;

import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import io.reactivex.functions.Consumer;

import static com.better.alarm.configuration.AlarmApplication.container;

public class TransparentActivity extends FragmentActivity {

    private Alarm alarm;
    private Disposable dialog = Disposables.disposed();

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Intent intent = getIntent();
        int id = intent.getIntExtra(Intents.EXTRA_ID, -1);
        alarm = container().alarms().getAlarm(id);
    }

    @Override
    protected void onResume() {
        super.onResume();
        dialog = TimePickerDialogFragment.showTimePicker(getSupportFragmentManager()).subscribe(new Consumer<Optional<PickedTime>>() {
            @Override
            public void accept(@NonNull Optional<PickedTime> picked) {
                if (picked.isPresent()) {
                    alarm.snooze(picked.get().getHour(), picked.get().getMinute());
                }
                finish();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        dialog.dispose();
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
