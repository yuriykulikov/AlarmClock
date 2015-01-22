/*
 * Copyright (C) 2008 The Android Open Source Project
 * Copyright (C) 2012 Yuriy Kulikov yuriy.kulikov.87@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.better.alarm.presenter.background;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import com.better.alarm.R;
import com.better.alarm.model.AlarmsManager;
import com.better.alarm.model.interfaces.Alarm;
import com.better.alarm.model.interfaces.Intents;
import com.better.alarm.presenter.SettingsActivity;
import com.github.androidutils.logger.Logger;
import com.github.androidutils.wakelock.WakeLockManager;

/**
 * Manages alarms and vibe. Runs as a service so that it can continue to play if
 * another activity overrides the AlarmAlert dialog.
 */
public class KlaxonService extends Service {
    private volatile IMediaPlayer mMediaPlayer;
    private TelephonyManager mTelephonyManager;
    private Logger log;
    private Volume volume;
    private PowerManager pm;
    private WakeLock wakeLock;
    private SharedPreferences sp;

    private Alarm alarm;
    private boolean lastInCallState;

    /**
     * Dispatches intents to the KlaxonService
     */
    public static class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            intent.setClass(context, KlaxonService.class);
            WakeLockManager.getWakeLockManager().acquirePartialWakeLock(intent, "ForKlaxonService");
            context.startService(intent);
        }
    }

    private final PhoneStateListener phoneStateListenerImpl = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String ignored) {
            boolean newState = state != TelephonyManager.CALL_STATE_IDLE;
            if (lastInCallState != newState) {
                lastInCallState = newState;
                if (lastInCallState) {
                    log.d("Call has started. Mute.");
                    volume.mute();
                } else {
                    log.d("Call has ended. fadeInFast.");
                    if (alarm != null && !alarm.isSilent()) {
                        initializePlayer(getAlertOrDefault(alarm));
                        volume.fadeInFast();
                    }
                }
            }
        }
    };

    private static class Volume implements OnSharedPreferenceChangeListener {
        private static final int FAST_FADE_IN_TIME = 5000;

        private static final int FADE_IN_STEPS = 100;

        // Volume suggested by media team for in-call alarms.
        private static final float IN_CALL_VOLUME = 0.125f;

        private static final float SILENT = 0f;

        private static final int MAX_VOLUME = 10;

        private final SharedPreferences sp;

        private final class FadeInTimer extends CountDownTimer {
            private final long fadeInTime;
            private final long fadeInStep;
            private final float targetVolume;
            private final double multiplier;

            private FadeInTimer(long millisInFuture, long countDownInterval) {
                super(millisInFuture, countDownInterval);
                fadeInTime = millisInFuture;
                fadeInStep = countDownInterval;
                targetVolume = getVolumeFor(type);
                multiplier = targetVolume / Math.pow(fadeInTime / fadeInStep, 2);
            }

            @Override
            public void onTick(long millisUntilFinished) {
                long elapsed = fadeInTime - millisUntilFinished;
                float i = elapsed / fadeInStep;
                float adjustedVolume = (float) (multiplier * (Math.pow(i, 2)));
                player.setVolume(adjustedVolume);
            }

            @Override
            public void onFinish() {
                log.d("Fade in completed");
            }
        }

        public enum Type {
            NORMAL, PREALARM
        };

        private Type type = Type.NORMAL;
        private IMediaPlayer player;
        private final Logger log;
        private int preAlarmVolume = 0;
        private int alarmVolume = 4;

        private CountDownTimer timer;

        public Volume(final Logger log, SharedPreferences sp) {
            this.log = log;
            this.sp = sp;
        }

        public void setMode(Type type) {
            this.type = type;
        }

        public void setPlayer(IMediaPlayer player) {
            this.player = player;
        }

        /**
         * Instantly apply the targetVolume. To fade in use
         * {@link #fadeInAsSetInSettings()}
         */
        public void apply() {
            player.setVolume(getVolumeFor(type));
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals(Intents.KEY_PREALARM_VOLUME)) {
                preAlarmVolume = sharedPreferences.getInt(key, Intents.DEFAULT_PREALARM_VOLUME);
                if (player.isPlaying() && type == Type.PREALARM) {
                    player.setVolume(getVolumeFor(Type.PREALARM));
                }

            } else if (key.equals(Intents.KEY_ALARM_VOLUME)) {
                alarmVolume = sharedPreferences.getInt(key, Intents.DEFAULT_ALARM_VOLUME);
                if (player.isPlaying() && type == Type.NORMAL) {
                    player.setVolume(getVolumeFor(Type.NORMAL));
                }
            }
        }

        /**
         * Fade in to set targetVolume
         */
        public void fadeInAsSetInSettings() {
            String asString = sp.getString(SettingsActivity.KEY_FADE_IN_TIME_SEC, "30");
            int time = Integer.parseInt(asString) * 1000;
            fadeIn(time);
        }

        public void fadeInFast() {
            fadeIn(FAST_FADE_IN_TIME);
        }

        public void cancelFadeIn() {
            if (timer != null) {
                timer.cancel();
            }
        }

        public void mute() {
            cancelFadeIn();
            player.setVolume(SILENT);
        }

        private void fadeIn(int time) {
            cancelFadeIn();
            player.setVolume(SILENT);
            timer = new FadeInTimer(time, time / FADE_IN_STEPS);
            timer.start();
        }

        private float getVolumeFor(Type type) {
            int volume = Math.min(type.equals(Type.PREALARM) ? preAlarmVolume : alarmVolume, MAX_VOLUME);
            float fVolume = (float) (Math.pow(volume + 1, 2) / Math.pow(MAX_VOLUME + 1, 2));
            if (type.equals(Type.PREALARM)) return fVolume / 4;
            else return fVolume;
        }
    }

    @Override
    public void onCreate() {
        mMediaPlayer = new NullMediaPlayer();
        log = Logger.getDefaultLogger();
        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "KlaxonService");
        wakeLock.acquire();
        // Listen for incoming calls to kill the alarm.
        mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        volume = new Volume(log, sp);
        volume.setPlayer(mMediaPlayer);
        lastInCallState = mTelephonyManager.getCallState() != TelephonyManager.CALL_STATE_IDLE;
        mTelephonyManager.listen(phoneStateListenerImpl, PhoneStateListener.LISTEN_CALL_STATE);
        sp.registerOnSharedPreferenceChangeListener(volume);
        volume.onSharedPreferenceChanged(sp, Intents.KEY_PREALARM_VOLUME);
        volume.onSharedPreferenceChanged(sp, Intents.KEY_ALARM_VOLUME);
    }

    @Override
    public void onDestroy() {
        stop();
        // Stop listening for incoming calls.
        mTelephonyManager.listen(phoneStateListenerImpl, PhoneStateListener.LISTEN_NONE);
        PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                .unregisterOnSharedPreferenceChangeListener(volume);
        log.d("Service destroyed");
        wakeLock.release();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            WakeLockManager.getWakeLockManager().releasePartialWakeLock(intent);
        }
        try {
            String action = intent.getAction();
            if (action.equals(Intents.ALARM_ALERT_ACTION)) {
                alarm = AlarmsManager.getAlarmsManager().getAlarm(intent.getIntExtra(Intents.EXTRA_ID, -1));
                onAlarm(alarm);
                return START_STICKY;

            } else if (action.equals(Intents.ALARM_PREALARM_ACTION)) {
                alarm = AlarmsManager.getAlarmsManager().getAlarm(intent.getIntExtra(Intents.EXTRA_ID, -1));
                onPreAlarm(alarm);
                return START_STICKY;

            } else if (action.equals(Intents.ALARM_DISMISS_ACTION)) {
                stopAndCleanup();
                return START_NOT_STICKY;

            } else if (action.equals(Intents.ALARM_SNOOZE_ACTION)) {
                stopAndCleanup();
                return START_NOT_STICKY;

            } else if (action.equals(Intents.ACTION_SOUND_EXPIRED)) {
                stopAndCleanup();
                return START_NOT_STICKY;

            } else if (action.equals(Intents.ACTION_START_PREALARM_SAMPLE)) {
                onStartAlarmSample(Volume.Type.PREALARM);
                return START_STICKY;

            } else if (action.equals(Intents.ACTION_STOP_PREALARM_SAMPLE)) {
                stopAndCleanup();
                return START_NOT_STICKY;

            } else if (action.equals(Intents.ACTION_START_ALARM_SAMPLE)) {
                onStartAlarmSample(Volume.Type.NORMAL);
                return START_STICKY;

            } else if (action.equals(Intents.ACTION_STOP_ALARM_SAMPLE)) {
                stopAndCleanup();
                return START_NOT_STICKY;

            } else if (action.equals(Intents.ACTION_MUTE)) {
                volume.mute();
                return START_STICKY;

            } else if (action.equals(Intents.ACTION_DEMUTE)) {
                volume.fadeInFast();
                return START_STICKY;

            } else if (action.equals(Intents.ACTION_STOP_ALARM_SAMPLE)) {
                stopAndCleanup();
                return START_NOT_STICKY;

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

    private void onAlarm(Alarm alarm) throws Exception {
        volume.cancelFadeIn();
        volume.setMode(Volume.Type.NORMAL);
        if (!alarm.isSilent()) {
            initializePlayer(getAlertOrDefault(alarm));
            volume.fadeInAsSetInSettings();
        }
    }

    private void onPreAlarm(Alarm alarm) throws Exception {
        volume.cancelFadeIn();
        volume.setMode(Volume.Type.PREALARM);
        if (!alarm.isSilent()) {
            initializePlayer(getAlertOrDefault(alarm));
            volume.fadeInAsSetInSettings();
        }
    }

    private void onStartAlarmSample(Volume.Type type) {
        volume.cancelFadeIn();
        volume.setMode(type);
        // if already playing do nothing. In this case signal continues.
        if (!mMediaPlayer.isPlaying()) {
            initializePlayer(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM));
        }
        volume.apply();
    }

    /**
     * Inits player and sets volume to 0
     * 
     * @param alert
     */
    private void initializePlayer(Uri alert) {
        // stop() checks to see if we are already playing.
        stop();

        // TODO: Reuse mMediaPlayer instead of creating a new one and/or use
        // RingtoneManager.
        mMediaPlayer = new MediaPlayerWrapper(new MediaPlayer());
        mMediaPlayer.setOnErrorListener(new OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                log.e("Error occurred while playing audio.");
                volume.cancelFadeIn();
                mMediaPlayer.stop();
                mMediaPlayer.release();
                nullifyMediaPlayer();
                return true;
            }
        });

        volume.setPlayer(mMediaPlayer);
        volume.mute();
        try {
            // Check if we are in a call. If we are, use the in-call alarm
            // resource at a low targetVolume to not disrupt the call.
            if (mTelephonyManager.getCallState() != TelephonyManager.CALL_STATE_IDLE) {
                log.d("Using the in-call alarm");
                setDataSourceFromResource(getResources(), mMediaPlayer, R.raw.in_call_alarm);
            } else {
                mMediaPlayer.setDataSource(this, alert);
            }
            startAlarm(mMediaPlayer);
        } catch (Exception ex) {
            log.w("Using the fallback ringtone");
            // The alert may be on the sd card which could be busy right
            // now. Use the fallback ringtone.
            try {
                // Must reset the media player to clear the error state.
                mMediaPlayer.reset();
                setDataSourceFromResource(getResources(), mMediaPlayer, R.raw.fallbackring);
                startAlarm(mMediaPlayer);
            } catch (Exception ex2) {
                // At this point we just don't play anything.
                log.e("Failed to play fallback ringtone", ex2);
            }
        }
    }

    private Uri getAlertOrDefault(Alarm alarm) {
        Uri alert = alarm.getAlert();
        // Fall back on the default alarm if the database does not have an
        // alarm stored.
        if (alert == null) {
            alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            log.d("Using default alarm: " + alert.toString());
        }
        return alert;
    }

    // Do the common stuff when starting the alarm.
    private void startAlarm(IMediaPlayer player) throws java.io.IOException, IllegalArgumentException,
            IllegalStateException {
        final AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        // do not play alarms if stream targetVolume is 0
        // (typically because ringer mode is silent).
        if (audioManager.getStreamVolume(AudioManager.STREAM_ALARM) != 0) {
            player.setAudioStreamType(AudioManager.STREAM_ALARM);
            player.setLooping(true);
            player.prepare();
            player.start();
        }
    }

    private void setDataSourceFromResource(Resources resources, IMediaPlayer player, int res)
            throws java.io.IOException {
        AssetFileDescriptor afd = resources.openRawResourceFd(res);
        if (afd != null) {
            player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
        }
    }

    /**
     * Stops alarm audio
     */
    private void stop() {
        log.d("stopping media player");
        // Stop audio playing
        try {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.stop();
            }
            mMediaPlayer.release();
        } catch (IllegalStateException e) {
            log.e("stop failed with ", e);
        } finally {
            nullifyMediaPlayer();
        }
    }

    private void stopAndCleanup() {
        volume.cancelFadeIn();
        stopSelf();
    }

    private void nullifyMediaPlayer() {
        mMediaPlayer = new NullMediaPlayer();
        volume.setPlayer(mMediaPlayer);
    }
}
