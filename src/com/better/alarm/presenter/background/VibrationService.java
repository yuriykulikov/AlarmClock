package com.better.alarm.presenter.background;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.Vibrator;
import android.preference.PreferenceManager;

import com.better.alarm.model.interfaces.Intents;
import com.better.alarm.presenter.SettingsActivity;
import com.github.androidutils.logger.Logger;
import com.github.androidutils.wakelock.WakeLockManager;

public class VibrationService extends Service {
    private static final long[] sVibratePattern = new long[] { 500, 500 };
    private Vibrator mVibrator;
    private Logger log;
    private PowerManager pm;
    private WakeLock wakeLock;
    private CountDownTimer timer;
    private SharedPreferences sp;

    /**
     * Dispatches intents to the KlaxonService
     */
    public static class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            intent.setClass(context, VibrationService.class);
            WakeLockManager.getWakeLockManager().acquirePartialWakeLock(intent, "ForVibrationService");
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
    }

    @Override
    public void onDestroy() {
        stopVibration();
        log.d("Service destroyed");
        wakeLock.release();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            WakeLockManager.getWakeLockManager().releasePartialWakeLock(intent);
        }
        try {
            String action = intent.getAction();
            if (action.equals(Intents.ALARM_ALERT_ACTION)) {
                String asString = sp.getString(SettingsActivity.KEY_FADE_IN_TIME_SEC, "30");
                int time = Integer.parseInt(asString) * 1000;
                timer = new CountDownTimer(time, time) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                    }

                    @Override
                    public void onFinish() {
                        startVibrationIfShould();
                    }
                }.start();

                return START_STICKY;

            } else if (action.equals(Intents.ALARM_SNOOZE_ACTION)) {
                stopSelf();
                return START_NOT_STICKY;

            } else if (action.equals(Intents.ALARM_DISMISS_ACTION)) {
                stopSelf();
                return START_NOT_STICKY;

            } else if (action.equals(Intents.ACTION_SOUND_EXPIRED)) {
                stopSelf();
                return START_NOT_STICKY;

            } else if (action.equals(Intents.ACTION_MUTE)) {
                stopVibration();
                return START_STICKY;

            } else if (action.equals(Intents.ACTION_DEMUTE)) {
                startVibrationIfShould();
                return START_STICKY;

            } else {
                log.e("unexpected intent " + intent.getAction());
                stopSelf();
                return START_NOT_STICKY;
            }
        } catch (Exception e) {
            log.e("Something went wrong" + e.getMessage());
            stopSelf();
            return START_NOT_STICKY;
        }
    }

    private void startVibrationIfShould() {
        boolean shouldVibrate = sp.getBoolean("vibrate", true);
        if (shouldVibrate) {
            mVibrator.vibrate(sVibratePattern, 0);
        }
    }

    private void stopVibration() {
        mVibrator.cancel();
        if (timer != null) {
            timer.cancel();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
