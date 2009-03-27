/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.alarmclock;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.telephony.TelephonyManager;

/**
 * Manages alarms and vibe.  Singleton, so it can be initiated in
 * AlarmReceiver and shut down in the AlarmAlert activity
 */
class AlarmKlaxon implements Alarms.AlarmSettings {

    interface KillerCallback {
        public void onKilled();
    }

    /** Play alarm up to 10 minutes before silencing */
    final static int ALARM_TIMEOUT_SECONDS = 10 * 60;

    private static final long[] sVibratePattern = new long[] { 500, 500 };

    private int mAlarmId;
    private String mAlert;
    private Alarms.DaysOfWeek mDaysOfWeek;
    private boolean mVibrate;
    private boolean mPlaying = false;
    private Vibrator mVibrator;
    private MediaPlayer mMediaPlayer;
    private KillerCallback mKillerCallback;

    // Internal messages
    private static final int KILLER = 1000;
    private static final int PLAY   = 1001;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case KILLER:
                    if (Log.LOGV) {
                        Log.v("*********** Alarm killer triggered ***********");
                    }
                    if (mKillerCallback != null) {
                        mKillerCallback.onKilled();
                    }
                    break;
                case PLAY:
                    play((Context) msg.obj, msg.arg1);
                    break;
            }
        }
    };

    AlarmKlaxon() {
        mVibrator = new Vibrator();
    }

    public void reportAlarm(
            int idx, boolean enabled, int hour, int minutes,
            Alarms.DaysOfWeek daysOfWeek, boolean vibrate, String message,
            String alert) {
        if (Log.LOGV) Log.v("AlarmKlaxon.reportAlarm: " + idx + " " + hour +
                            " " + minutes + " dow " + daysOfWeek);
        mAlert = alert;
        mDaysOfWeek = daysOfWeek;
        mVibrate = vibrate;
    }

    public void postPlay(final Context context, final int alarmId) {
        mHandler.sendMessage(mHandler.obtainMessage(PLAY, alarmId, 0, context));
    }

    // Volume suggested by media team for in-call alarms.
    private static final float IN_CALL_VOLUME = 0.125f;

    private void play(Context context, int alarmId) {
        ContentResolver contentResolver = context.getContentResolver();

        if (mPlaying) stop(context, false);

        mAlarmId = alarmId;

        /* this will call reportAlarm() callback */
        Alarms.getAlarm(contentResolver, this, mAlarmId);

        if (Log.LOGV) Log.v("AlarmKlaxon.play() " + mAlarmId + " alert " + mAlert);

        // TODO: Reuse mMediaPlayer instead of creating a new one and/or use
        // RingtoneManager.
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setOnErrorListener(new OnErrorListener() {
            public boolean onError(MediaPlayer mp, int what, int extra) {
                Log.e("Error occurred while playing audio.");
                mp.stop();
                mp.release();
                mMediaPlayer = null;
                return true;
            }
        });

        try {
            TelephonyManager tm = (TelephonyManager) context.getSystemService(
                    Context.TELEPHONY_SERVICE);
            // Check if we are in a call. If we are, use the in-call alarm
            // resource at a low volume to not disrupt the call.
            if (tm.getCallState() != TelephonyManager.CALL_STATE_IDLE) {
                Log.v("Using the in-call alarm");
                mMediaPlayer.setVolume(IN_CALL_VOLUME, IN_CALL_VOLUME);
                setDataSourceFromResource(context.getResources(),
                        mMediaPlayer, R.raw.in_call_alarm);
            } else {
                mMediaPlayer.setDataSource(context, Uri.parse(mAlert));
            }
            startAlarm(mMediaPlayer);
        } catch (Exception ex) {
            Log.v("Using the fallback ringtone");
            // The alert may be on the sd card which could be busy right now.
            // Use the fallback ringtone.
            try {
                // Must reset the media player to clear the error state.
                mMediaPlayer.reset();
                setDataSourceFromResource(context.getResources(), mMediaPlayer,
                        com.android.internal.R.raw.fallbackring);
                startAlarm(mMediaPlayer);
            } catch (Exception ex2) {
                // At this point we just don't play anything.
                Log.e("Failed to play fallback ringtone", ex2);
            }
        }

        /* Start the vibrator after everything is ok with the media player */
        if (mVibrate) {
            mVibrator.vibrate(sVibratePattern, 0);
        } else {
            mVibrator.cancel();
        }

        enableKiller();
        mPlaying = true;
    }

    // Do the common stuff when starting the alarm.
    private void startAlarm(MediaPlayer player)
            throws java.io.IOException, IllegalArgumentException,
                   IllegalStateException {
        player.setAudioStreamType(AudioManager.STREAM_ALARM);
        player.setLooping(true);
        player.prepare();
        player.start();
    }

    private void setDataSourceFromResource(Resources resources,
            MediaPlayer player, int res) throws java.io.IOException {
        AssetFileDescriptor afd = resources.openRawResourceFd(res);
        if (afd != null) {
            player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(),
                    afd.getLength());
            afd.close();
        }
    }

    /**
     * Stops alarm audio and disables alarm if it not snoozed and not
     * repeating
     */
    public void stop(Context context, boolean snoozed) {
        if (Log.LOGV) Log.v("AlarmKlaxon.stop() " + mAlarmId);
        if (mPlaying) {
            mPlaying = false;

            // Stop audio playing
            if (mMediaPlayer != null) {
                mMediaPlayer.stop();
                mMediaPlayer.release();
                mMediaPlayer = null;
            }

            // Stop vibrator
            mVibrator.cancel();

            /* disable alarm only if it is not set to repeat */
            if (!snoozed && ((mDaysOfWeek == null || !mDaysOfWeek.isRepeatSet()))) {
                Alarms.enableAlarm(context, mAlarmId, false);
            }
        }
        disableKiller();
    }

    /**
     * This callback called when alarm killer times out unattended
     * alarm
     */
    public void setKillerCallback(KillerCallback killerCallback) {
        mKillerCallback = killerCallback;
    }

    /**
     * Kills alarm audio after ALARM_TIMEOUT_SECONDS, so the alarm
     * won't run all day.
     *
     * This just cancels the audio, but leaves the notification
     * popped, so the user will know that the alarm tripped.
     */
    private void enableKiller() {
        mHandler.sendMessageDelayed(mHandler.obtainMessage(KILLER),
                1000 * ALARM_TIMEOUT_SECONDS);
    }

    private void disableKiller() {
        mHandler.removeMessages(KILLER);
    }


}
