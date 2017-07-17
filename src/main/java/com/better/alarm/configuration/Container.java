package com.better.alarm.configuration;

import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.PowerManager;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.telephony.TelephonyManager;

import com.better.alarm.interfaces.IAlarmsManager;
import com.better.alarm.logger.Logger;
import com.better.alarm.model.Alarms;
import com.better.alarm.wakelock.WakeLockManager;
import com.f2prateek.rx.preferences2.RxSharedPreferences;

import org.immutables.value.Value;

import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;

/**
 * Created by Yuriy on 09.08.2017.
 */
@Value.Immutable
@Value.Style(stagedBuilder = true)
public abstract class Container {
    @NonNull
    public abstract Context context();

    @NonNull
    public abstract Logger logger();

    @NonNull
    public abstract SharedPreferences sharedPreferences();

    @NonNull
    public abstract RxSharedPreferences rxPrefs();

    @NonNull
    public abstract Prefs prefs();

    @NonNull
    public abstract Store store();

    @NonNull
    public abstract Alarms rawAlarms();

    /**
     * Scheduler which can be used to schedule timed events.
     * Typically {@link AndroidSchedulers#finalize()} in production
     * and some kind of {@link io.reactivex.schedulers.TestScheduler} in tests.
     */
    @NonNull
    public abstract Scheduler scheduler();

    @NonNull
    @Value.Derived
    public IAlarmsManager alarms() {
        return rawAlarms();
    }

    @NonNull
    @Value.Derived
    public WakeLockManager wakeLocks() {
        return new WakeLockManager(logger(), powerManager());
    }

    @NonNull
    @Value.Derived
    public Vibrator vibrator() {
        return (Vibrator) context().getSystemService(Context.VIBRATOR_SERVICE);
    }

    @NonNull
    @Value.Derived
    public PowerManager powerManager() {
        return (PowerManager) context().getSystemService(Context.POWER_SERVICE);
    }

    @NonNull
    @Value.Derived
    public TelephonyManager telephonyManager() {
        return (TelephonyManager) context().getSystemService(Context.TELEPHONY_SERVICE);
    }

    @NonNull
    @Value.Derived
    public NotificationManager notificationManager() {
        return (NotificationManager) context().getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @NonNull
    @Value.Derived
    public AudioManager audioManager() {
        return (AudioManager) context().getSystemService(Context.AUDIO_SERVICE);
    }
}
