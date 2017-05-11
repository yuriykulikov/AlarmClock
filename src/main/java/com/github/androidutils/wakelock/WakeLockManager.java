/*
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
package com.github.androidutils.wakelock;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

import com.github.androidutils.logger.Logger;
import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * Utility class to pass {@link WakeLock} objects with intents. It contains a
 * factory, which has to be initialized in {@link Application#onCreate()} or
 * otherwise there will be an exception. Instance maintains a {@link Map} of
 * String tag to {@link WakeLock} wakelock
 */
public class WakeLockManager {
    public static final String EXTRA_WAKELOCK_TAG = "WakeLockManager.EXTRA_WAKELOCK_TAG";
    public static final String EXTRA_WAKELOCK_HASH = "WakeLockManager.EXTRA_WAKELOCK_HASH";

    private final Logger log;
    private final CopyOnWriteArrayList<WakeLock> wakeLocks;
    private final PowerManager pm;

    @Inject
    public WakeLockManager(Context context, @Named("debug") Logger logger) {
        pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        wakeLocks = new CopyOnWriteArrayList<PowerManager.WakeLock>();
        log = logger;
    }

    /**
     * Acquires a partial {@link WakeLock}, stores it internally and puts the
     * tag into the {@link Intent}. To be used with
     * {@link WakeLockManager#releasePartialWakeLock(Intent)}
     * 
     * @param intent
     * @param wlTag
     */
    public void acquirePartialWakeLock(Intent intent, String wlTag) {
        final WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, wlTag);
        wl.acquire();
        wakeLocks.add(wl);
        intent.putExtra(WakeLockManager.EXTRA_WAKELOCK_HASH, wl.hashCode());
        intent.putExtra(WakeLockManager.EXTRA_WAKELOCK_TAG, wlTag);
        log.d(wl.toString() + " " + wlTag + " was acquired");
    }

    /**
     * Releases a partial {@link WakeLock} with a tag contained in the given
     * {@link Intent}
     * 
     * @param intent
     */
    public void releasePartialWakeLock(Intent intent) {
        if (intent.hasExtra(WakeLockManager.EXTRA_WAKELOCK_TAG)) {
            final int hash = intent.getIntExtra(WakeLockManager.EXTRA_WAKELOCK_HASH, -1);
            final String tag = intent.getStringExtra(WakeLockManager.EXTRA_WAKELOCK_TAG);
            // We use copy on write list. Iterator won't cause
            // ConcurrentModificationException
            for (Iterator<WakeLock> iterator = wakeLocks.iterator(); iterator.hasNext();) {
                WakeLock wakeLock = iterator.next();
                if (hash == wakeLock.hashCode()) {
                    if (wakeLock.isHeld()) {
                        wakeLock.release();
                        log.d(wakeLock.toString() + " " + tag + " was released");
                    } else {
                        log.e(wakeLock.toString() + " " + tag + " was already released!");
                    }
                    wakeLocks.remove(wakeLock);
                    return;
                }
            }
            log.e("Hash " + hash + " was not found");
        }
    }
}
