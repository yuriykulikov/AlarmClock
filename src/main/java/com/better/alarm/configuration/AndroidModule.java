package com.better.alarm.configuration;

import android.app.AlarmManager;
import android.app.Application;
import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.PowerManager;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;

import com.f2prateek.rx.preferences2.RxSharedPreferences;
import com.google.inject.Binder;
import com.google.inject.Module;

/**
 * Created by Yuriy on 26.07.2017.
 */
public class AndroidModule implements Module {
    private final Application context;

    AndroidModule(Application context) {
        this.context = context;
    }

    @Override
    public void configure(Binder binder) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        RxSharedPreferences rxPrefs = RxSharedPreferences.create(sp);

        binder.bind(Context.class).toInstance(context);
        binder.bind(SharedPreferences.class).toInstance(sp);
        binder.bind(RxSharedPreferences.class).toInstance(rxPrefs);

        binder.bind(ContentResolver.class).toInstance(context.getContentResolver());

        binder.bind(AlarmManager.class).toInstance((AlarmManager) context.getSystemService(Context.ALARM_SERVICE));
        binder.bind(NotificationManager.class).toInstance((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE));
        binder.bind(AudioManager.class).toInstance((AudioManager) context.getSystemService(Context.AUDIO_SERVICE));
        binder.bind(KeyguardManager.class).toInstance((KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE));
        binder.bind(TelephonyManager.class).toInstance((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE));
        binder.bind(PowerManager.class).toInstance((PowerManager) context.getSystemService(Context.POWER_SERVICE));
        binder.bind(Vibrator.class).toInstance((Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE));
    }
}
