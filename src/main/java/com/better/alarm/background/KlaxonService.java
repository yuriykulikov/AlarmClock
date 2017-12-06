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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import com.better.alarm.configuration.Prefs;
import com.better.alarm.util.Service;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Cancellable;
import io.reactivex.functions.Function;

import static com.better.alarm.configuration.AlarmApplication.container;
import static com.better.alarm.configuration.Prefs.DEFAULT_PREALARM_VOLUME;
import static com.better.alarm.configuration.Prefs.KEY_PREALARM_VOLUME;

/**
 * Delegate everything to a {@link KlaxonDelegate} which will play some awesome music.
 */
public class KlaxonService extends Service implements KlaxonServiceCallback {
    private KlaxonDelegate delegate;

    @Override
    public Uri getDefaultUri(int type) {
        return RingtoneManager.getDefaultUri(type);
    }

    @Override
    public MediaPlayer createMediaPlayer() {
        return new MediaPlayer();
    }

    @Override
    public void onCreate() {
        final TelephonyManager tm = container().telephonyManager();

        Observable<Integer> callState = Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(@NonNull final ObservableEmitter<Integer> emitter) throws Exception {
                emitter.onNext(tm.getCallState());

                final PhoneStateListener listener = new PhoneStateListener() {
                    @Override
                    public void onCallStateChanged(int state, String ignored) {
                        emitter.onNext(state);
                    }
                };

                emitter.setCancellable(new Cancellable() {
                    @Override
                    public void cancel() throws Exception {
                        // Stop listening for incoming calls.
                        tm.listen(listener, PhoneStateListener.LISTEN_NONE);
                    }
                });

                tm.listen(listener, PhoneStateListener.LISTEN_CALL_STATE);
            }
        });

        Observable<Integer> fadeInTimeInSeconds = container().rxPrefs()
                .getString(Prefs.KEY_FADE_IN_TIME_SEC, "30")
                .asObservable()
                .map(new Function<String, Integer>() {
                    @Override
                    public Integer apply(@NonNull String s) throws Exception {
                        return Integer.parseInt(s) * 1000;
                    }
                });


        delegate = new KlaxonServiceDelegate(
                container().logger(),
                container().powerManager(),
                container().wakeLocks(),
                container().alarms(),
                this,
                getResources(),
                callState,
                container().rxPrefs().getInteger(KEY_PREALARM_VOLUME, DEFAULT_PREALARM_VOLUME).asObservable(),
                fadeInTimeInSeconds,
                this,
                AndroidSchedulers.mainThread()
        );
    }

    @Override
    public void onDestroy() {
        delegate.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return START_NOT_STICKY;
        } else {
            container().wakeLocks().releasePartialWakeLock(intent);
            return delegate.onStartCommand(intent) ? START_STICKY : START_NOT_STICKY;
        }
    }

    public interface KlaxonDelegate {
        void onDestroy();

        boolean onStartCommand(@android.support.annotation.NonNull Intent intent);
    }

    /**
     * android.media.AudioManagerDispatches intents to the KlaxonService
     */
    public static class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            intent.setClass(context, KlaxonService.class);
            container().wakeLocks().acquirePartialWakeLock(intent, "ForKlaxonService");
            context.startService(intent);
        }
    }
}
