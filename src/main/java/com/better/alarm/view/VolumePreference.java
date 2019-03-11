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

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.better.alarm.R;
import com.better.alarm.interfaces.Intents;
import com.better.alarm.logger.Logger;
import com.f2prateek.rx.preferences2.RxSharedPreferences;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;
import io.reactivex.subjects.PublishSubject;

import static com.better.alarm.configuration.AlarmApplication.container;
import static com.better.alarm.configuration.Prefs.DEFAULT_PREALARM_VOLUME;
import static com.better.alarm.configuration.Prefs.KEY_PREALARM_VOLUME;
import static com.better.alarm.configuration.Prefs.MAX_PREALARM_VOLUME;

public class VolumePreference extends Preference {
    private final Ringtone ringtone;
    private final Context context;

    public VolumePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.seekbar_dialog);

        //TODO consider using selected uri
        this.ringtone = RingtoneManager.getRingtone(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM));
        ringtone.setStreamType(AudioManager.STREAM_ALARM);
        this.context = context;
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        bindPrealarmSeekBar((SeekBar) view.findViewById(R.id.seekbar_dialog_seekbar_prealarm_volume));
        bindAudioManagerVolume((SeekBar) view.findViewById(R.id.seekbar_dialog_seekbar_master_volume));
    }

    @Override
    protected void onPrepareForRemoval() {
        super.onPrepareForRemoval();
        ringtone.stop();
        // TODO Broadcasts.sendExplicit(context, new Intent(Intents.ACTION_STOP_PREALARM_SAMPLE));
    }

    /**
     * This class is controls playback using AudioManager
     */
    private void bindAudioManagerVolume(final SeekBar seekBar) {
        final AudioManager am = container().audioManager();
        SeekBarListener masterListener = new SeekBarListener();
        seekBar.setOnSeekBarChangeListener(masterListener);

        seekBar.setProgress(am.getStreamVolume(AudioManager.STREAM_ALARM));
        seekBar.setMax(am.getStreamMaxVolume(AudioManager.STREAM_ALARM));

        masterListener
                .progressChanged$()
                .subscribe(new Consumer<Integer>() {
                    @Override
                    public void accept(@NonNull Integer progress) throws Exception {
                        //stop prealarm sample if there is one
                        context.sendBroadcast(new Intent(Intents.ACTION_STOP_PREALARM_SAMPLE));
                        am.setStreamVolume(AudioManager.STREAM_ALARM, progress, 0);
                        if (!ringtone.isPlaying()) {
                            ringtone.play();
                        }
                    }
                });

        masterListener
                .progressChanged$()
                .debounce(3, TimeUnit.SECONDS, AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Integer>() {
                    @Override
                    public void accept(@NonNull Integer integer) throws Exception {
                        ringtone.stop();
                    }
                });
    }

    private void bindPrealarmSeekBar(final SeekBar preAlarmSeekBar) {
        SeekBarListener prealarmListener = new SeekBarListener();
        preAlarmSeekBar.setOnSeekBarChangeListener(prealarmListener);
        RxSharedPreferences rxPrefs = container().rxPrefs();
        final Logger log = container().logger();
        final com.f2prateek.rx.preferences2.Preference<Integer> prealarmPreference = rxPrefs.getInteger(KEY_PREALARM_VOLUME, DEFAULT_PREALARM_VOLUME);
        preAlarmSeekBar.setMax(MAX_PREALARM_VOLUME);

        prealarmPreference.asObservable().subscribe(new Consumer<Integer>() {
            @Override
            public void accept(@NonNull Integer integer) throws Exception {
                preAlarmSeekBar.setProgress(integer);
            }
        });

        prealarmListener
                .progressChanged$()
                .subscribe(new Consumer<Integer>() {
                    @Override
                    public void accept(@NonNull Integer integer) throws Exception {
                        log.d("Pre-alarm " + integer);
                        prealarmPreference.set(integer);
                        ringtone.stop();
                        context.sendBroadcast(new Intent(Intents.ACTION_START_PREALARM_SAMPLE));
                    }
                });

        prealarmListener
                .progressChanged$()
                .debounce(3, TimeUnit.SECONDS, AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Integer>() {
                    @Override
                    public void accept(@NonNull Integer integer) throws Exception {
                        context.sendBroadcast(new Intent(Intents.ACTION_STOP_PREALARM_SAMPLE));
                    }
                });
    }

    /**
     * Turns a {@link SeekBar} into a volume control.
     */
    private static class SeekBarListener implements OnSeekBarChangeListener {
        private PublishSubject<Integer> progressChanged = PublishSubject.create();

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
            if (!fromTouch) return;
            progressChanged.onNext(progress);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            //empty
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            //empty startSample();
        }

        public Observable<Integer> progressChanged$() {
            return progressChanged;
        }
    }
}