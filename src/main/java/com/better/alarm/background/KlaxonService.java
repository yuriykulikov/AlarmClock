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

package com.better.alarm.background;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
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

import com.better.alarm.AlarmApplication;
import com.better.alarm.R;
import com.better.alarm.logger.Logger;
import com.better.alarm.interfaces.Alarm;
import com.better.alarm.interfaces.Intents;
import com.better.alarm.presenter.SettingsActivity;
import com.f2prateek.rx.preferences2.RxSharedPreferences;
import com.google.common.base.Optional;

import io.reactivex.Observable;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;

/**
 * Manages alarms and vibe. Runs as a service so that it can continue to play if
 * another activity overrides the AlarmAlert dialog.
 */
public class KlaxonService extends Service {
    private Optional<MediaPlayer> mMediaPlayer = Optional.absent();

    private TelephonyManager mTelephonyManager;
    private Logger log;
    private Volume volume;
    private PowerManager pm;
    private WakeLock wakeLock;

    private Alarm alarm;
    private boolean lastInCallState;
    private Observable<Integer> fadeInTimeInSeconds;
    private RxSharedPreferences rxPreferences;

    CompositeDisposable disposables = new CompositeDisposable();

    /**
     * Dispatches intents to the KlaxonService
     */
    public static class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            intent.setClass(context, KlaxonService.class);
            AlarmApplication.wakeLocks().acquirePartialWakeLock(intent, "ForKlaxonService");
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

    public enum Type {
        NORMAL, PREALARM
    }

    private class Volume {
        private static final int FAST_FADE_IN_TIME = 5000;

        private static final int FADE_IN_STEPS = 100;

        // Volume suggested by media team for in-call alarms.
        private static final float IN_CALL_VOLUME = 0.125f;

        private static final float SILENT = 0f;

        private static final int MAX_VOLUME = 10;

        Volume() {
            disposables.add(
                    rxPreferences.getInteger(Intents.KEY_PREALARM_VOLUME, Intents.DEFAULT_PREALARM_VOLUME)
                            .asObservable()
                            .subscribe(new VolumePrefConsumer(Type.PREALARM)));
            disposables.add(
                    rxPreferences.getInteger(Intents.KEY_ALARM_VOLUME, Intents.DEFAULT_ALARM_VOLUME)
                            .asObservable()
                            .subscribe(new VolumePrefConsumer(Type.NORMAL)));
        }

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
            public void onTick(final long millisUntilFinished) {
                long elapsed = fadeInTime - millisUntilFinished;
                float i = elapsed / fadeInStep;
                float adjustedVolume = (float) (multiplier * (Math.pow(i, 2)));
                if (mMediaPlayer.isPresent()) {
                    mMediaPlayer.get().setVolume(adjustedVolume, adjustedVolume);
                }
            }

            @Override
            public void onFinish() {
                log.d("Fade in completed");
            }
        }

        private Type type = Type.NORMAL;
        private int preAlarmVolume = 0;
        private int alarmVolume = 4;

        private CountDownTimer timer;

        public void setMode(Type type) {
            this.type = type;
        }

        /**
         * Instantly apply the targetVolume. To fade in use
         * {@link #fadeInAsSetInSettings()}
         */
        public void apply() {
            float volume = getVolumeFor(type);
            if (mMediaPlayer.isPresent()) {
                mMediaPlayer.get().setVolume(volume, volume);
            }
        }

        /**
         * Fade in to set targetVolume
         */
        public void fadeInAsSetInSettings() {
            fadeIn(fadeInTimeInSeconds.blockingFirst());
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
            if (mMediaPlayer.isPresent()) {
                mMediaPlayer.get().setVolume(SILENT, SILENT);
            }
        }

        private void fadeIn(int time) {
            cancelFadeIn();
            if (mMediaPlayer.isPresent()) {
                mMediaPlayer.get().setVolume(SILENT, SILENT);
            }
            timer = new FadeInTimer(time, time / FADE_IN_STEPS);
            timer.start();
        }

        private float getVolumeFor(Type type) {
            int volume = Math.min(type.equals(Type.PREALARM) ? preAlarmVolume : alarmVolume, MAX_VOLUME);
            float fVolume = (float) (Math.pow(volume + 1, 2) / Math.pow(MAX_VOLUME + 1, 2));
            if (type.equals(Type.PREALARM)) return fVolume / 4;
            else return fVolume;
        }

        private class VolumePrefConsumer implements Consumer<Integer> {
            private final Type consumerType;

            VolumePrefConsumer(Type type) {
                this.consumerType = type;
            }

            @Override
            public void accept(@NonNull Integer preAlarmVolume) throws Exception {
                if (mMediaPlayer.isPresent()) {
                    MediaPlayer player = mMediaPlayer.get();
                    if (player.isPlaying() && type == consumerType) {
                        float volumeFor = getVolumeFor(consumerType);
                        player.setVolume(volumeFor, volumeFor);
                    }
                }
            }
        }
    }

    @Override
    public void onCreate() {
        rxPreferences = RxSharedPreferences.create(PreferenceManager.getDefaultSharedPreferences(getApplication()));
        mMediaPlayer = Optional.absent();
        log = Logger.getDefaultLogger();
        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "KlaxonService");
        wakeLock.acquire();
        // Listen for incoming calls to kill the alarm.
        mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        volume = new Volume();
        lastInCallState = mTelephonyManager.getCallState() != TelephonyManager.CALL_STATE_IDLE;
        mTelephonyManager.listen(phoneStateListenerImpl, PhoneStateListener.LISTEN_CALL_STATE);

        fadeInTimeInSeconds = rxPreferences
                .getString(SettingsActivity.KEY_FADE_IN_TIME_SEC, "30")
                .asObservable()
                .map(new Function<String, Integer>() {
                    @Override
                    public Integer apply(@NonNull String s) throws Exception {
                        return Integer.parseInt(s) * 1000;
                    }
                });
    }

    @Override
    public void onDestroy() {
        if (mMediaPlayer.isPresent()) {
            stop(mMediaPlayer.get());
        }
        // Stop listening for incoming calls.
        mTelephonyManager.listen(phoneStateListenerImpl, PhoneStateListener.LISTEN_NONE);
        disposables.dispose();
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
            AlarmApplication.wakeLocks().releasePartialWakeLock(intent);
        }
        try {
            String action = intent.getAction();
            if (action.equals(Intents.ALARM_ALERT_ACTION)) {
                alarm = AlarmApplication.alarms().getAlarm(intent.getIntExtra(Intents.EXTRA_ID, -1));
                onAlarm(alarm);
                return START_STICKY;

            } else if (action.equals(Intents.ALARM_PREALARM_ACTION)) {
                alarm = AlarmApplication.alarms().getAlarm(intent.getIntExtra(Intents.EXTRA_ID, -1));
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
                onStartAlarmSample(Type.PREALARM);
                return START_STICKY;

            } else if (action.equals(Intents.ACTION_STOP_PREALARM_SAMPLE)) {
                stopAndCleanup();
                return START_NOT_STICKY;

            } else if (action.equals(Intents.ACTION_START_ALARM_SAMPLE)) {
                onStartAlarmSample(Type.NORMAL);
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
        volume.setMode(Type.NORMAL);
        if (!alarm.isSilent()) {
            initializePlayer(getAlertOrDefault(alarm));
            volume.fadeInAsSetInSettings();
        }
    }

    private void onPreAlarm(Alarm alarm) throws Exception {
        volume.cancelFadeIn();
        volume.setMode(Type.PREALARM);
        if (!alarm.isSilent()) {
            initializePlayer(getAlertOrDefault(alarm));
            volume.fadeInAsSetInSettings();
        }
    }

    private void onStartAlarmSample(Type type) {
        volume.cancelFadeIn();
        volume.setMode(type);
        // if already playing do nothing. In this case signal continues.
        if (mMediaPlayer.isPresent() && !mMediaPlayer.get().isPlaying()) {
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
        if (mMediaPlayer.isPresent()) {
            stop(mMediaPlayer.get());
        }

        // TODO: Reuse mMediaPlayer instead of creating a new one and/or use
        // RingtoneManager.
        MediaPlayer created = new MediaPlayer();
        mMediaPlayer = Optional.of(created);
        created.setOnErrorListener(new OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                log.e("Error occurred while playing audio.");
                volume.cancelFadeIn();
                mp.stop();
                mp.release();
                nullifyMediaPlayer();
                return true;
            }
        });

        volume.mute();
        try {
            // Check if we are in a call. If we are, use the in-call alarm
            // resource at a low targetVolume to not disrupt the call.
            if (mTelephonyManager.getCallState() != TelephonyManager.CALL_STATE_IDLE) {
                log.d("Using the in-call alarm");
                setDataSourceFromResource(getResources(), created, R.raw.in_call_alarm);
            } else {
                created.setDataSource(this, alert);
            }
            startAlarm(created);
        } catch (Exception ex) {
            log.w("Using the fallback ringtone");
            // The alert may be on the sd card which could be busy right
            // now. Use the fallback ringtone.
            try {
                // Must reset the media player to clear the error state.
                created.reset();
                setDataSourceFromResource(getResources(), created, R.raw.fallbackring);
                startAlarm(created);
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
    private void startAlarm(MediaPlayer player) throws java.io.IOException, IllegalArgumentException,
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

    private void setDataSourceFromResource(Resources resources, MediaPlayer player, int res)
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
    private void stop(MediaPlayer player) {
        log.d("stopping media player");
        // Stop audio playing
        try {
            if (player.isPlaying()) {
                player.stop();
            }
            player.release();
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
        mMediaPlayer = Optional.absent();
    }
}
