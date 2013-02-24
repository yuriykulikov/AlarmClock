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
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import com.better.alarm.R;
import com.better.alarm.model.AlarmsManager;
import com.better.alarm.model.interfaces.Alarm;
import com.better.alarm.model.interfaces.Intents;
import com.github.androidutils.logger.Logger;
import com.github.androidutils.wakelock.WakeLockManager;

/**
 * Manages alarms and vibe. Runs as a service so that it can continue to play if
 * another activity overrides the AlarmAlert dialog.
 */
public class KlaxonService extends Service {
    private boolean mPlaying = false;
    private MediaPlayer mMediaPlayer;
    private TelephonyManager mTelephonyManager;
    private Logger log;
    private Intent mIntent;
    private Volume volume;

    /**
     * Dispatches intents to the KlaxonService
     */
    public static class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            intent.setClass(context, KlaxonService.class);
            WakeLockManager.getWakeLockManager().acquirePartialWakeLock(intent, "KlaxonService");
            context.startService(intent);
        }
    }

    private static class Volume extends PhoneStateListener implements OnSharedPreferenceChangeListener,
            Handler.Callback {
        private static final int MESSAGE_CHANGE_VOLUME = 0;
        private static final int FADE_IN_TIME = 5000;

        // Volume suggested by media team for in-call alarms.
        private static final float IN_CALL_VOLUME = 0.125f;

        // TODO XML
        // i^2/100
        private static final float[] ALARM_VOLUMES = { 0f, 0.01f, 0.04f, 0.09f, 0.16f, 0.25f, 0.36f, 0.49f, 0.64f,
                0.81f, 1.0f };

        public enum Type {
            NORMAL, PREALARM
        };

        private Type type = Type.NORMAL;
        private MediaPlayer player;
        private final Logger log;
        private final TelephonyManager mTelephonyManager;
        private int preAlarmVolume = 0;
        private int alarmVolume = 4;
        private final Handler handler;

        public Volume(Logger log, TelephonyManager telephonyManager) {
            this.log = log;
            mTelephonyManager = telephonyManager;
            handler = new Handler(this);
        }

        public void setMode(Type type) {
            this.type = type;
        }

        public void setPlayer(MediaPlayer player) {
            this.player = player;
        }

        /**
         * Instantly apply the volume. To fade in use {@link #fadeIn()}
         */
        public void apply() {
            float fvolume;
            try {
                // Check if we are in a call. If we are, use the in-call alarm
                // resource at a low volume to not disrupt the call.
                if (mTelephonyManager.getCallState() != TelephonyManager.CALL_STATE_IDLE) {
                    log.d("Using the in-call alarm");
                    fvolume = IN_CALL_VOLUME;
                } else if (type == Type.PREALARM) {
                    fvolume = ALARM_VOLUMES[preAlarmVolume];
                } else {
                    fvolume = ALARM_VOLUMES[alarmVolume];
                }
            } catch (IndexOutOfBoundsException e) {
                fvolume = 1f;
            }
            player.setVolume(fvolume, fvolume);
        }

        @Override
        public void onCallStateChanged(int state, String ignored) {
            // The user might already be in a call when the alarm fires. When
            // we register onCallStateChanged, we get the initial in-call state
            // which kills the alarm. Check against the initial call state so
            // we don't kill the alarm during a call.
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals(Intents.KEY_PREALARM_VOLUME)) {
                preAlarmVolume = sharedPreferences.getInt(key, Intents.DEFAULT_PREALARM_VOLUME);
                if (preAlarmVolume > ALARM_VOLUMES.length) {
                    preAlarmVolume = ALARM_VOLUMES.length;
                    log.w("Truncated volume!");
                }
                if (player != null && player.isPlaying() && type == Type.PREALARM) {
                    player.setVolume(ALARM_VOLUMES[preAlarmVolume], ALARM_VOLUMES[preAlarmVolume]);
                }

            } else if (key.equals(Intents.KEY_ALARM_VOLUME)) {
                alarmVolume = sharedPreferences.getInt(key, Intents.DEFAULT_ALARM_VOLUME);
                if (alarmVolume > ALARM_VOLUMES.length) {
                    alarmVolume = ALARM_VOLUMES.length;
                    log.w("Truncated volume!");
                }
                if (player != null && player.isPlaying() && type == Type.NORMAL) {
                    player.setVolume(ALARM_VOLUMES[alarmVolume], ALARM_VOLUMES[alarmVolume]);
                }
            }
        }

        /**
         * Fade in to set volume
         */
        public void fadeIn() {
            // set initial
            player.setVolume(ALARM_VOLUMES[0], ALARM_VOLUMES[0]);

            // in the end we have to reach target volume
            int targetVolumeIndex = type == Type.NORMAL ? alarmVolume : preAlarmVolume;
            // calculate fade steps, starting from 1
            int delayStep = FADE_IN_TIME / targetVolumeIndex;
            for (int i = 1; i <= targetVolumeIndex; i++) {
                Message msg = handler.obtainMessage(MESSAGE_CHANGE_VOLUME);
                msg.arg1 = i;
                handler.sendMessageDelayed(msg, delayStep * i);
            }
        }

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_CHANGE_VOLUME:
                int step = msg.arg1;
                player.setVolume(ALARM_VOLUMES[step], ALARM_VOLUMES[step]);
                break;

            default:
                break;
            }
            return false;
        }
    }

    @Override
    public void onCreate() {
        log = Logger.getDefaultLogger();
        // Listen for incoming calls to kill the alarm.
        mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        mTelephonyManager.listen(volume, PhoneStateListener.LISTEN_CALL_STATE);
        volume = new Volume(log, mTelephonyManager);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        sp.registerOnSharedPreferenceChangeListener(volume);
        volume.onSharedPreferenceChanged(sp, Intents.KEY_PREALARM_VOLUME);
        volume.onSharedPreferenceChanged(sp, Intents.KEY_ALARM_VOLUME);
    }

    @Override
    public void onDestroy() {
        stop();
        // Stop listening for incoming calls.
        mTelephonyManager.listen(volume, 0);
        WakeLockManager.getWakeLockManager().releasePartialWakeLock(mIntent);
        PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                .unregisterOnSharedPreferenceChangeListener(volume);
        log.d("Service destroyed");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mIntent = intent;
        int id = intent.getIntExtra(Intents.EXTRA_ID, -1);
        String action = intent.getAction();
        try {
            if (action.equals(Intents.ALARM_ALERT_ACTION)) {
                Alarm alarm = AlarmsManager.getAlarmsManager().getAlarm(intent.getIntExtra(Intents.EXTRA_ID, -1));
                onAlarm(alarm);
                return START_STICKY;

            } else if (action.equals(Intents.ALARM_PREALARM_ACTION)) {
                Alarm alarm = AlarmsManager.getAlarmsManager().getAlarm(intent.getIntExtra(Intents.EXTRA_ID, -1));
                onPreAlarm(alarm);
                return START_STICKY;

            } else if (action.equals(Intents.ALARM_DISMISS_ACTION)) {
                stopSelf();
                return START_NOT_STICKY;

            } else if (action.equals(Intents.ALARM_SNOOZE_ACTION)) {
                stopSelf();
                return START_NOT_STICKY;

            } else if (action.equals(Intents.ACTION_SOUND_EXPIRED)) {
                stopSelf();
                return START_NOT_STICKY;

            } else if (action.equals(Intents.ACTION_START_PREALARM_SAMPLE)) {
                onStartAlarmSample(Volume.Type.PREALARM);
                return START_STICKY;

            } else if (action.equals(Intents.ACTION_STOP_PREALARM_SAMPLE)) {
                stopSelf();
                return START_NOT_STICKY;

            } else if (action.equals(Intents.ACTION_START_ALARM_SAMPLE)) {
                onStartAlarmSample(Volume.Type.NORMAL);
                return START_STICKY;

            } else if (action.equals(Intents.ACTION_STOP_ALARM_SAMPLE)) {
                stopSelf();
                return START_NOT_STICKY;

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

    private void onAlarm(Alarm alarm) throws Exception {
        volume.setMode(Volume.Type.NORMAL);
        if (!alarm.isSilent()) {
            play(getAlertOrDefault(alarm));
        }
        mPlaying = true;
    }

    private void onPreAlarm(Alarm alarm) throws Exception {
        volume.setMode(Volume.Type.PREALARM);
        if (!alarm.isSilent()) {
            play(getAlertOrDefault(alarm));
        }
        mPlaying = true;
    }

    private void onStartAlarmSample(Volume.Type type) {
        volume.setMode(type);
        // if already playing do nothing. In this case signal continues.
        if (!mPlaying) {
            play(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM));
        }
        volume.apply();
        mPlaying = true;
    }

    private void play(Uri alert) {
        // stop() checks to see if we are already playing.
        stop();

        // TODO: Reuse mMediaPlayer instead of creating a new one and/or use
        // RingtoneManager.
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setOnErrorListener(new OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                log.e("Error occurred while playing audio.");
                mp.stop();
                mp.release();
                mMediaPlayer = null;
                return true;
            }
        });

        volume.setPlayer(mMediaPlayer);
        volume.fadeIn();
        try {
            // Check if we are in a call. If we are, use the in-call alarm
            // resource at a low volume to not disrupt the call.
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
    private void startAlarm(MediaPlayer player) throws java.io.IOException, IllegalArgumentException,
            IllegalStateException {
        final AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        // do not play alarms if stream volume is 0
        // (typically because ringer mode is silent).
        if (audioManager.getStreamVolume(AudioManager.STREAM_ALARM) != 0) {
            player.setAudioStreamType(AudioManager.STREAM_ALARM);
            player.setLooping(true);
            player.prepare();
            player.start();
        }
    }

    private void setDataSourceFromResource(Resources resources, MediaPlayer player, int res) throws java.io.IOException {
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
        log.d("stop()");
        if (mPlaying) {
            mPlaying = false;

            // Stop audio playing
            if (mMediaPlayer != null) {
                mMediaPlayer.stop();
                mMediaPlayer.release();
                mMediaPlayer = null;
            }
        }
    }
}
