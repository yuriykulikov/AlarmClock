/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.better.alarm.view;

import java.util.ArrayList;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.better.alarm.R;
import com.better.alarm.model.interfaces.Intents;
import com.better.alarm.view.VolumePreference.SeekBarVolumizer;
import com.github.androidutils.logger.Logger;

interface IVolumizerMaster {
    void onSampleStarting(SeekBarVolumizer volumizer);
}

interface IVolumizerStrategy {

    int getMaxVolume();

    int getVolume();

    void setVolume(int progress);

    void stopSample();

    void startSample();
}

/**
 * This class represents the dialog
 */
public class VolumePreference extends DialogPreference implements View.OnKeyListener, IVolumizerMaster {
    private final Drawable mMyIcon;

    private final ArrayList<SeekBarVolumizer> volumizers;
    private SeekBarVolumizer activeVolumizer;

    public VolumePreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        setDialogLayoutResource(R.layout.seekbar_dialog);

        // Steal the XML dialogIcon attribute's value
        mMyIcon = getDialogIcon();
        setDialogIcon(null);
        volumizers = new ArrayList<VolumePreference.SeekBarVolumizer>();
        createActionButtons(context, attrs);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        final ImageView iconView = (ImageView) view.findViewById(R.id.seekbar_dialog_icon);
        if (mMyIcon != null) {
            iconView.setImageDrawable(mMyIcon);
        } else {
            iconView.setVisibility(View.GONE);
        }

        final SeekBar seekBar = (SeekBar) view.findViewById(R.id.seekbar_dialog_seekbar_master_volume);
        volumizers.add(new SeekBarVolumizer(seekBar, new AudioManagerVolumizerStrategy(getContext(),
                AudioManager.STREAM_ALARM, null), this));

        final SeekBar alarmSeekBar = (SeekBar) view.findViewById(R.id.seekbar_dialog_seekbar_alarm_volume);
        volumizers.add(new SeekBarVolumizer(alarmSeekBar, new AlarmVolumizerStrategy(getContext()), this));

        final SeekBar preAlarmSeekBar = (SeekBar) view.findViewById(R.id.seekbar_dialog_seekbar_prealarm_volume);
        volumizers.add(new SeekBarVolumizer(preAlarmSeekBar, new PreAlarmVolumizerStrategy(getContext()), this));

        activeVolumizer = volumizers.get(0);

        // getPreferenceManager().registerOnActivityStopListener(this);

        // grab focus and key events so that pressing the volume buttons in the
        // dialog doesn't also show the normal volume adjust toast.
        view.setOnKeyListener(this);
        view.setFocusableInTouchMode(true);
        view.requestFocus();
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        cleanup();
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        // If key arrives immediately after the activity has been cleaned up.
        if (volumizers.isEmpty()) return true;
        boolean isdown = event.getAction() == KeyEvent.ACTION_DOWN;
        switch (keyCode) {
        case KeyEvent.KEYCODE_VOLUME_DOWN:
            if (isdown) {
                activeVolumizer.changeVolumeBy(-1);
            }
            return true;
        case KeyEvent.KEYCODE_VOLUME_UP:
            if (isdown) {
                activeVolumizer.changeVolumeBy(1);
            }
            return true;
        case KeyEvent.KEYCODE_VOLUME_MUTE:
            if (isdown) {
                activeVolumizer.muteVolume();
            }
            return true;
        default:
            return false;
        }
    }

    @Override
    public void onSampleStarting(SeekBarVolumizer requestedVolumizer) {
        // We stop all volumizers other than the given one
        for (SeekBarVolumizer volumizer : volumizers) {
            if (!volumizer.equals(requestedVolumizer)) {
                volumizer.stopSample();
            }
        }
        // key events will be handled by the volumizer that is currently active
        activeVolumizer = requestedVolumizer;
    }

    /**
     * Do clean up. This can be called multiple times!
     */
    private void cleanup() {
        // getPreferenceManager().unregisterOnActivityStopListener(this);

        Dialog dialog = getDialog();
        if (dialog != null && dialog.isShowing()) {
            View view = dialog.getWindow().getDecorView().findViewById(R.id.seekbar_dialog_seekbar_master_volume);
            if (view != null) {
                view.setOnKeyListener(null);
            }
            // TODO ? Stopped while dialog was showing, revert changes
        }
        for (SeekBarVolumizer volumizer : volumizers) {
            volumizer.stopSample();
            volumizer.mSeekBar.setOnSeekBarChangeListener(null);
        }
        volumizers.clear();
        activeVolumizer = null;
    }

    // Allow subclasses to override the action buttons
    private void createActionButtons(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.com_better_alarm_view_VolumePreference, 0, 0);
        boolean cancelButtonUsed = a.getBoolean(R.styleable.com_better_alarm_view_VolumePreference_cancelButton, true);
        setPositiveButtonText(android.R.string.ok);
        if (cancelButtonUsed) {
            setNegativeButtonText(android.R.string.cancel);
        } else {
            setNegativeButtonText("");
        }
        a.recycle();
    }

    /**
     * This class is controls playback using AudioManager
     */
    private static class AudioManagerVolumizerStrategy implements IVolumizerStrategy, Handler.Callback {
        private static final int MSG_SET_VOLUME = 1;
        private final AudioManager mAudioManager;
        private final int mStreamType;
        private final Context mContext;
        /**
         * Can be <code>null</code>, e.g. when SD is busy. We should use
         * fallback in this case
         */
        private final Ringtone mRingtone;
        private final Handler mHandler = new Handler(this);

        public AudioManagerVolumizerStrategy(Context context, int streamType, Uri defaultUri) {
            mContext = context;
            mStreamType = streamType;
            mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

            if (defaultUri == null) {
                if (mStreamType == AudioManager.STREAM_RING) {
                    defaultUri = Settings.System.DEFAULT_RINGTONE_URI;
                } else if (mStreamType == AudioManager.STREAM_NOTIFICATION) {
                    defaultUri = Settings.System.DEFAULT_NOTIFICATION_URI;
                } else {
                    defaultUri = Settings.System.DEFAULT_ALARM_ALERT_URI;
                }
            }

            mRingtone = RingtoneManager.getRingtone(mContext, defaultUri);

            if (mRingtone != null) {
                mRingtone.setStreamType(mStreamType);
            }
        }

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
            case MSG_SET_VOLUME:
                mAudioManager.setStreamVolume(mStreamType, msg.arg1, 0);
                return true;

            default:
                return false;
            }

        }

        @Override
        public int getVolume() {
            return mAudioManager.getStreamVolume(mStreamType);
        }

        @Override
        public int getMaxVolume() {
            return mAudioManager.getStreamMaxVolume(mStreamType);
        }

        @Override
        public void startSample() {
            if (mRingtone != null && !isSamplePlaying()) {
                mRingtone.play();
            }
        }

        @Override
        public void stopSample() {
            if (mRingtone != null) {
                mRingtone.stop();
            }
        }

        @Override
        public void setVolume(int progress) {
            mHandler.removeMessages(MSG_SET_VOLUME);
            Message message = mHandler.obtainMessage(MSG_SET_VOLUME, progress, -1);
            message.sendToTarget();
        }

        private boolean isSamplePlaying() {
            return mRingtone != null && mRingtone.isPlaying();
        }
    }

    /**
     * This volumizer strategy uses a dedicated service to play sound and change
     * volume. Service is started/stopped using intents. Service observes volume
     * preference, which is changed by the volumizer strategy.
     */
    public static class PreAlarmVolumizerStrategy implements IVolumizerStrategy {
        private final Context mContext;
        Logger log = Logger.getDefaultLogger();
        private final SharedPreferences sp;
        private boolean isPlaying = false;

        public PreAlarmVolumizerStrategy(Context context) {
            mContext = context;
            sp = PreferenceManager.getDefaultSharedPreferences(mContext);
        }

        @Override
        public int getMaxVolume() {
            return Intents.MAX_PREALARM_VOLUME;
        }

        @Override
        public int getVolume() {
            return sp.getInt(Intents.KEY_PREALARM_VOLUME, Intents.DEFAULT_PREALARM_VOLUME);
        }

        @Override
        public void setVolume(int progress) {
            Editor editor = sp.edit();
            editor.putInt(Intents.KEY_PREALARM_VOLUME, progress);
            editor.commit();
        };

        @Override
        public void stopSample() {
            if (isPlaying) {
                isPlaying = false;
                mContext.sendBroadcast(new Intent(Intents.ACTION_STOP_PREALARM_SAMPLE));
            }
        }

        @Override
        public void startSample() {
            if (!isPlaying) {
                isPlaying = true;
                mContext.sendBroadcast(new Intent(Intents.ACTION_START_PREALARM_SAMPLE));
            }
        }
    }

    /**
     * This volumizer strategy uses a dedicated service to play sound and change
     * volume. Service is started/stopped using intents. Service observes volume
     * preference, which is changed by the volumizer strategy.
     */
    public static class AlarmVolumizerStrategy implements IVolumizerStrategy {
        private final Context mContext;
        Logger log = Logger.getDefaultLogger();
        private final SharedPreferences sp;
        private boolean isPlaying = false;

        public AlarmVolumizerStrategy(Context context) {
            mContext = context;
            sp = PreferenceManager.getDefaultSharedPreferences(mContext);
        }

        @Override
        public int getMaxVolume() {
            return Intents.MAX_ALARM_VOLUME;
        }

        @Override
        public int getVolume() {
            return sp.getInt(Intents.KEY_ALARM_VOLUME, Intents.DEFAULT_ALARM_VOLUME);
        }

        @Override
        public void setVolume(int progress) {
            Editor editor = sp.edit();
            editor.putInt(Intents.KEY_ALARM_VOLUME, progress);
            editor.commit();
        };

        @Override
        public void stopSample() {
            if (isPlaying) {
                isPlaying = false;
                mContext.sendBroadcast(new Intent(Intents.ACTION_STOP_ALARM_SAMPLE));
            }
        }

        @Override
        public void startSample() {
            if (!isPlaying) {
                isPlaying = true;
                mContext.sendBroadcast(new Intent(Intents.ACTION_START_ALARM_SAMPLE));
            }
        }
    }

    /**
     * Turns a {@link SeekBar} into a volume control.
     */
    public static class SeekBarVolumizer implements OnSeekBarChangeListener {
        private final SeekBar mSeekBar;
        private int mVolumeBeforeMute = -1;

        private final IVolumizerStrategy volumizerStrategy;
        private final IVolumizerMaster volumizerMaster;

        public SeekBarVolumizer(SeekBar seekBar, IVolumizerStrategy volumizerStrategy, IVolumizerMaster volumizerMaster) {
            mSeekBar = seekBar;
            this.volumizerStrategy = volumizerStrategy;
            this.volumizerMaster = volumizerMaster;
            seekBar.setMax(volumizerStrategy.getMaxVolume());
            seekBar.setProgress(volumizerStrategy.getVolume());
            seekBar.setOnSeekBarChangeListener(this);
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
            if (!fromTouch) return;

            volumizerStrategy.setVolume(progress);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            startSample();
        }

        public void changeVolumeBy(int amount) {
            mSeekBar.incrementProgressBy(amount);
            startSample();
            volumizerStrategy.setVolume(mSeekBar.getProgress());
            mVolumeBeforeMute = -1;
        }

        public void muteVolume() {
            if (mVolumeBeforeMute != -1) {
                mSeekBar.setProgress(mVolumeBeforeMute);
                startSample();
                volumizerStrategy.setVolume(mVolumeBeforeMute);
                mVolumeBeforeMute = -1;
            } else {
                mVolumeBeforeMute = mSeekBar.getProgress();
                mSeekBar.setProgress(0);
                volumizerStrategy.stopSample();
                volumizerStrategy.setVolume(0);
            }
        }

        // public void onSaveInstanceState(VolumeStore volumeStore) {
        // if (mLastProgress >= 0) {
        // volumeStore.volume = mLastProgress;
        // volumeStore.originalVolume = mOriginalStreamVolume;
        // }
        // }
        //
        // public void onRestoreInstanceState(VolumeStore volumeStore) {
        // if (volumeStore.volume != -1) {
        // mOriginalStreamVolume = volumeStore.originalVolume;
        // mLastProgress = volumeStore.volume;
        // postSetVolume(mLastProgress);
        // }
        // }

        public void stopSample() {
            volumizerStrategy.stopSample();
        }

        private void startSample() {
            volumizerMaster.onSampleStarting(this);
            volumizerStrategy.startSample();
        }
    }
}