package com.better.alarm.background;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import com.better.alarm.AlarmApplication;
import com.better.alarm.interfaces.Intents;
import com.better.alarm.logger.Logger;
import com.better.alarm.presenter.SettingsActivity;
import com.f2prateek.rx.preferences2.RxSharedPreferences;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function4;
import io.reactivex.subjects.BehaviorSubject;

public class VibrationService extends Service {
    private static final long[] sVibratePattern = new long[]{500, 500};
    private Vibrator mVibrator;
    private Logger log;
    private PowerManager pm;
    private WakeLock wakeLock;
    private SharedPreferences sp;

    //isEnabled && !inCall && !isMuted && isStarted
    private final BehaviorSubject<Boolean> inCall = BehaviorSubject.createDefault(false);
    private final BehaviorSubject<Boolean> muted = BehaviorSubject.createDefault(false);
    private Disposable subscription = Disposables.disposed();

    /**
     * Dispatches intents to the KlaxonService
     */
    public static class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            intent.setClass(context, VibrationService.class);
            AlarmApplication.wakeLocks().acquirePartialWakeLock(intent, "ForVibrationService");
            context.startService(intent);
        }
    }

    @Override
    public void onCreate() {
        log = Logger.getDefaultLogger();
        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "VibrationService");
        wakeLock.acquire();
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        sp = PreferenceManager.getDefaultSharedPreferences(this);
        ((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE)).listen(new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                inCall.onNext(state != TelephonyManager.CALL_STATE_IDLE);
            }
        }, PhoneStateListener.LISTEN_CALL_STATE);
    }

    @Override
    public void onDestroy() {
        subscription.dispose();
        log.d("Service destroyed");
        wakeLock.release();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            AlarmApplication.wakeLocks().releasePartialWakeLock(intent);
        }
        try {
            String action = intent.getAction();
            if (action.equals(Intents.ALARM_ALERT_ACTION)) {
                onAlert();
                return START_STICKY;

            } else if (action.equals(Intents.ALARM_SNOOZE_ACTION)) {
                stopAndCleanup();
                return START_NOT_STICKY;

            } else if (action.equals(Intents.ALARM_DISMISS_ACTION)) {
                stopAndCleanup();
                return START_NOT_STICKY;

            } else if (action.equals(Intents.ACTION_SOUND_EXPIRED)) {
                stopAndCleanup();
                return START_NOT_STICKY;

            } else if (action.equals(Intents.ACTION_MUTE)) {
                muted.onNext(true);
                return START_STICKY;

            } else if (action.equals(Intents.ACTION_DEMUTE)) {
                muted.onNext(false);
                return START_STICKY;

            } else {
                log.e("unexpected intent " + intent.getAction());
                stopAndCleanup();
                return START_NOT_STICKY;
            }
        } catch (Exception e) {
            log.e("Something went wrong" + e.getMessage());
            stopAndCleanup();
            return START_NOT_STICKY;
        }
    }

    private void onAlert() {
        muted.onNext(false);
        String asString = sp.getString(SettingsActivity.KEY_FADE_IN_TIME_SEC, "30");
        int time = Integer.parseInt(asString);

        Observable<Boolean> preference = RxSharedPreferences.create(sp).getBoolean("vibrate").asObservable();
        Observable<Boolean> timer = Observable
                .just(true)
                .delay(time, TimeUnit.SECONDS, AndroidSchedulers.mainThread())
                .startWith(false);

        subscription = Observable
                .combineLatest(preference, inCall, muted, timer, new Function4<Boolean, Boolean, Boolean, Boolean, Boolean>() {
                    @Override
                    public Boolean apply(Boolean isEnabled, Boolean inCall, Boolean isMuted, Boolean timerStarted) {
                        return isEnabled && !inCall && !isMuted && timerStarted;
                    }
                })
                .distinctUntilChanged()
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(@NonNull Boolean vibrate) throws Exception {
                        if (vibrate) {
                            log.d("Starting vibration");
                            mVibrator.vibrate(sVibratePattern, 0);
                        } else {
                            log.d("Canceling vibration");
                            mVibrator.cancel();
                        }
                    }
                });
    }

    private void stopAndCleanup() {
        log.d("stopAndCleanup");
        mVibrator.cancel();
        subscription.dispose();
        stopSelf();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
