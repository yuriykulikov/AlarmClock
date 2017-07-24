package com.better.alarm;

import org.immutables.value.Value;

import io.reactivex.Observable;
import io.reactivex.Single;

/**
 * Created by Yuriy on 10.06.2017.
 */

@Value.Immutable
@Value.Style(stagedBuilder = true)
public interface Prefs {
    String KEY_ALARM_IN_SILENT_MODE = "alarm_in_silent_mode";
    String KEY_ALARM_SNOOZE = "snooze_duration";
    String KEY_DEFAULT_RINGTONE = "default_ringtone";
    String KEY_AUTO_SILENCE = "auto_silence";
    String KEY_PREALARM_DURATION = "prealarm_duration";
    String KEY_FADE_IN_TIME_SEC = "fade_in_time_sec";
    boolean LONGCLICK_DISMISS_DEFAULT = false;
    String LONGCLICK_DISMISS_KEY = "longclick_dismiss_key";

    int MAX_PREALARM_VOLUME = 10;
    int DEFAULT_PREALARM_VOLUME = 5;
    String KEY_PREALARM_VOLUME = "key_prealarm_volume";

    Observable<Integer> preAlarmDuration();
    Observable<Integer> snoozeDuration();
    Observable<Integer> autoSilence();

    Single<Boolean> is24HoutFormat();
}
