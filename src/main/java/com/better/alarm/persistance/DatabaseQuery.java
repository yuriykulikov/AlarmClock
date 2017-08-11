package com.better.alarm.persistance;

import android.content.ContentResolver;
import android.database.Cursor;

import com.better.alarm.model.AlarmContainer;
import com.better.alarm.model.ContainerFactory;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import org.reactivestreams.Publisher;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Function;

/**
 * Created by Yuriy on 10.06.2017.
 */

public class DatabaseQuery {
    private final ContentResolver contentResolver;
    private final ContainerFactory factory;

    public DatabaseQuery(ContentResolver contentResolver, ContainerFactory factory) {
        this.contentResolver = contentResolver;
        this.factory = factory;
    }

    public Single<List<AlarmContainer>> query() {

        return Single
                .create(new SingleOnSubscribe<Cursor>() {
                    @Override
                    public void subscribe(@NonNull SingleEmitter<Cursor> e) throws Exception {
                        final Cursor cursor = contentResolver
                                .query(PersistingContainerFactory.Columns.CONTENT_URI, PersistingContainerFactory.Columns.ALARM_QUERY_COLUMNS, null, null, PersistingContainerFactory.Columns.DEFAULT_SORT_ORDER);
                        e.onSuccess(Preconditions.checkNotNull(cursor));
                    }
                })
                .retryWhen(new Function<Flowable<Throwable>, Publisher<?>>() {
                    @Override
                    public Publisher<?> apply(@NonNull Flowable<Throwable> errors) throws Exception {
                        return errors
                                //retry 120 times
                                .take(120)
                                //every half a second
                                .delay(500, TimeUnit.MILLISECONDS);
                    }
                })
                .map(new Function<Cursor, List<AlarmContainer>>() {
                    @Override
                    public List<AlarmContainer> apply(@NonNull Cursor cursor) throws Exception {
                        List<AlarmContainer> alarms = new ArrayList<AlarmContainer>();
                        try {
                            if (cursor.moveToFirst()) {
                                do {
                                    alarms.add(factory.create(cursor));
                                } while (cursor.moveToNext());
                            }
                        } finally {
                            cursor.close();
                        }
                        return alarms;
                    }
                })
                //emit an empty list if it all fails
                .onErrorResumeNext(Single.just(Lists.<AlarmContainer>newArrayList()));
    }
}
