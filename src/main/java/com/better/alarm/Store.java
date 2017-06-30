package com.better.alarm;

import com.better.alarm.model.AlarmValue;
import com.better.alarm.model.interfaces.Alarm;
import com.google.common.base.Optional;

import org.immutables.value.Value;

import java.util.List;

import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;

/**
 * Created by Yuriy on 10.06.2017.
 */

@Value.Immutable
@Value.Style(stagedBuilder = true)
public interface Store {
    @Value.Immutable
    @Value.Style(stagedBuilder = true)
    interface Next {

        AlarmValue alarm();

        boolean isPrealarm();

        long nextNonPrealarmTime();
    }

    @Value.Immutable
    interface AlarmSet {
        @Value.Parameter
        AlarmValue alarm();

        @Value.Parameter
        long millis();
    }

    BehaviorSubject<List<AlarmValue>> alarms();

    BehaviorSubject<Optional<Next>> next();

    Subject<AlarmSet> sets();
}
