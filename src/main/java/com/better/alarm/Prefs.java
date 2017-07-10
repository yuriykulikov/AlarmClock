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
    Observable<Integer> preAlarmDuration();
    Observable<Integer> snoozeDuration();
    Observable<Integer> autoSilence();

    Single<Boolean> is24HoutFormat();
}
