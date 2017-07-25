package com.better.alarm.view;

import android.app.Activity;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.preference.Preference;

import com.f2prateek.rx.preferences2.RxSharedPreferences;

import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

/**
 * Created by Yuriy on 27.07.2017.
 */

public class RingtonePreferenceExtension {
    public static Disposable updatePreferenceSummary(RxSharedPreferences rxSharedPreferences, final Preference ringtonePreference, final Activity activity) {
        return rxSharedPreferences.getString(ringtonePreference.getKey(), "")
                .asObservable()
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(@NonNull String uriString) throws Exception {
                        Uri uri = Uri.parse(uriString);
                        Ringtone ringtone = RingtoneManager.getRingtone(activity, uri);
                        ringtonePreference.setSummary(ringtone.getTitle(activity));
                    }
                });
    }
}
