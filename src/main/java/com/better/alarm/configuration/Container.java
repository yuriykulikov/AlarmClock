package com.better.alarm.configuration;

import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.PowerManager;
import android.os.Vibrator;
import android.telephony.TelephonyManager;

import com.better.alarm.interfaces.IAlarmsManager;
import com.better.alarm.logger.Logger;
import com.better.alarm.model.Alarms;
import com.better.alarm.wakelock.WakeLockManager;
import com.f2prateek.rx.preferences2.RxSharedPreferences;

import org.immutables.value.Value;

/**
 * Created by Yuriy on 09.08.2017.
 */
@Value.Immutable
@Value.Style(stagedBuilder = true)
public abstract class Container {
    public abstract Context context();

    @android.support.annotation.NonNull
    @Value.Derived
    public WakeLockManager wakeLocks() {
        return new WakeLockManager(logger(), powerManager());
    }

    @android.support.annotation.NonNull
    public abstract Logger logger();

    @android.support.annotation.NonNull
    public abstract SharedPreferences sharedPreferences();

    @android.support.annotation.NonNull
    public abstract RxSharedPreferences rxPrefs();

    @android.support.annotation.NonNull
    public abstract Prefs prefs();

    @android.support.annotation.NonNull
    public abstract Store store();

    @android.support.annotation.NonNull
    public abstract Alarms rawAlarms();

    @android.support.annotation.NonNull
    @Value.Derived
    public IAlarmsManager alarms() {
        return rawAlarms();
    }

    @android.support.annotation.NonNull
    @Value.Derived
    public Vibrator vibrator() {
        return (Vibrator) context().getSystemService(Context.VIBRATOR_SERVICE);
    }

    @android.support.annotation.NonNull
    @Value.Derived
    public PowerManager powerManager() {
        return (PowerManager) context().getSystemService(Context.POWER_SERVICE);
    }

    @android.support.annotation.NonNull
    @Value.Derived
    public TelephonyManager telephonyManager() {
        return (TelephonyManager) context().getSystemService(Context.TELEPHONY_SERVICE);
    }

    @android.support.annotation.NonNull
    @Value.Derived
    public NotificationManager notificationManager() {
        return (NotificationManager) context().getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @android.support.annotation.NonNull
    @Value.Derived
    public AudioManager audioManager() {
        return (AudioManager) context().getSystemService(Context.AUDIO_SERVICE);
    }
}
