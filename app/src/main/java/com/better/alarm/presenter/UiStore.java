package com.better.alarm.presenter;

import com.better.alarm.configuration.EditedAlarm;

import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

/**
 * Created by Yuriy on 11.08.2017.
 */

public interface UiStore {
    BehaviorSubject<EditedAlarm> editing();

    PublishSubject<String> onBackPressed();

    void createNewAlarm();

    /**
     * createNewAlarm was called -> list updates should be ignored
     */
    Subject<Boolean> transitioningToNewAlarmDetails();

    void edit(int id);

    void edit(int id, RowHolder holder);

    void hideDetails();

    void hideDetails(RowHolder holder);
}
