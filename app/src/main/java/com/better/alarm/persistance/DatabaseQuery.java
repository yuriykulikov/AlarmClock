package com.better.alarm.persistance;

import android.content.ContentResolver;
import android.database.Cursor;

import com.better.alarm.model.AlarmActiveRecord;
import com.better.alarm.model.AlarmStore;
import com.better.alarm.model.ContainerFactory;

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
import kotlin.jvm.internal.Intrinsics;

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

    public Single<List<AlarmStore>> query() {

        return Single
                .create(new SingleOnSubscribe<Cursor>() {
                    @Override
                    public void subscribe(@NonNull SingleEmitter<Cursor> e) throws Exception {
                        final Cursor cursor = contentResolver
                                .query(Columns.contentUri(), Columns.ALARM_QUERY_COLUMNS, null, null, Columns.DEFAULT_SORT_ORDER);
                        Intrinsics.checkNotNull(cursor, "cursor");
                        e.onSuccess(cursor);
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
                .map(new Function<Cursor, List<AlarmStore>>() {
                    @Override
                    public List<AlarmStore> apply(@NonNull Cursor cursor) throws Exception {
                        List<AlarmStore> alarms = new ArrayList<AlarmStore>();
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
                .onErrorResumeNext(Single.just(new ArrayList<AlarmStore>()));
    }
}
