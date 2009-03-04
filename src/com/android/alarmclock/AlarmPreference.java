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

import android.content.Context;
import android.net.Uri;
import android.preference.RingtonePreference;
import android.util.AttributeSet;

public class AlarmPreference extends RingtonePreference {
    public Uri mAlert;
    private IRingtoneChangedListener mRingtoneChangedListener;

    public interface IRingtoneChangedListener {
        public void onRingtoneChanged(Uri ringtoneUri);
    };

    public AlarmPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setRingtoneChangedListener(IRingtoneChangedListener listener) {
        mRingtoneChangedListener = listener;
    }

    @Override
    protected void onSaveRingtone(Uri ringtoneUri) {
        if (ringtoneUri != null) {
            mAlert = ringtoneUri;
            if (mRingtoneChangedListener != null) {
                mRingtoneChangedListener.onRingtoneChanged(ringtoneUri);
            }
        }
    }

    @Override
    protected Uri onRestoreRingtone() {
        return mAlert;
    }
}
