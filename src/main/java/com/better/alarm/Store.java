package com.better.alarm;

import com.better.alarm.model.AlarmValue;
import com.google.common.base.Optional;

import org.immutables.value.Value;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;

/**
 * Created by Yuriy on 10.06.2017.
 */

@Value.Immutable
@Value.Style(stagedBuilder = true)
public abstract class Store {
    @Value.Immutable
    @Value.Style(stagedBuilder = true)
    public interface Next {

        AlarmValue alarm();

        boolean isPrealarm();

        long nextNonPrealarmTime();
    }

    @Value.Immutable
    public interface AlarmSet {
        @Value.Parameter
        AlarmValue alarm();

        @Value.Parameter
        long millis();
    }

    public Observable<List<AlarmValue>> alarms(){
        return alarmsSubject().distinctUntilChanged();
    }

    public abstract BehaviorSubject<List<AlarmValue>> alarmsSubject();

    public abstract BehaviorSubject<Optional<Next>> next();

    public abstract Subject<AlarmSet> sets();
}
