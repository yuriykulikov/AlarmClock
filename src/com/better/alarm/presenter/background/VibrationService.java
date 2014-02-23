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
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import com.better.alarm.model.interfaces.Intents;
import com.better.alarm.presenter.SettingsActivity;
import com.better.alarm.presenter.background.VibrationService.AlertConditionHelper.AlertStrategy;
import com.github.androidutils.logger.Logger;
import com.github.androidutils.wakelock.WakeLockManager;

public class VibrationService extends Service {
    private static final long[] sVibratePattern = new long[] { 500, 500 };
    private Vibrator mVibrator;
    private Logger log;
    private PowerManager pm;
    private WakeLock wakeLock;
    private SharedPreferences sp;
    private AlertConditionHelper alertConditionHelper;

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
        ((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE)).listen(new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                alertConditionHelper.setInCall(state != TelephonyManager.CALL_STATE_IDLE);
            }
        }, PhoneStateListener.LISTEN_CALL_STATE);
        alertConditionHelper = new AlertConditionHelper(new AlertStrategy() {
            @Override
            public void start() {
                log.d("Starting vibration");
                mVibrator.vibrate(sVibratePattern, 0);
            }

            @Override
            public void stop() {
                log.d("Canceling vibration");
                mVibrator.cancel();
            }
        });
        alertConditionHelper.setEnabled(sp.getBoolean("vibrate", true));
    }

    @Override
    public void onDestroy() {
        alertConditionHelper.setStarted(false);
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
                alertConditionHelper.setMuted(false);
                String asString = sp.getString(SettingsActivity.KEY_FADE_IN_TIME_SEC, "30");
                int time = Integer.parseInt(asString) * 1000;
                new CountDownTimer(time, time) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                    }

                    @Override
                    public void onFinish() {
                        alertConditionHelper.setStarted(true);
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
                alertConditionHelper.setMuted(true);
                return START_STICKY;

            } else if (action.equals(Intents.ACTION_DEMUTE)) {
                alertConditionHelper.setMuted(false);
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

    public static final class AlertConditionHelper {
        public interface AlertStrategy {
            public void start();

            public void stop();
        }

        private final AlertStrategy alertConditionHelperListener;

        private boolean inCall;
        private boolean isStarted;
        private boolean isMuted;
        private boolean isEnabled;

        private void update() {
            if (isEnabled && !inCall && !isMuted && isStarted) {
                alertConditionHelperListener.start();
            } else {
                alertConditionHelperListener.stop();
            }
        }

        public AlertConditionHelper(AlertStrategy alertConditionHelperListener) {
            this.alertConditionHelperListener = alertConditionHelperListener;
        }

        public void setStarted(boolean isStarted) {
            this.isStarted = isStarted;
            update();
        }

        public void setMuted(boolean isMuted) {
            this.isMuted = isMuted;
            update();
        }

        public void setInCall(boolean inCall) {
            this.inCall = inCall;
            update();
        }

        public void setEnabled(boolean isEnabled) {
            this.isEnabled = isEnabled;
            update();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
