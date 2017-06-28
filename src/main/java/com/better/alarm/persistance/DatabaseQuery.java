package com.better.alarm.persistance;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;

import com.better.alarm.model.IAlarmContainer;
import com.github.androidutils.logger.Logger;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import org.reactivestreams.Publisher;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Flowable;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Function;

/**
 * Created by Yuriy on 10.06.2017.
 */

public class DatabaseQuery {
    /**
     * in millis
     */
    private static final long RETRY_TOTAL_TIME = 61 * 1000;
    /**
     * in millis
     */
    private static final long RETRY_INTERVAL = 500;
    private final ContentResolver contentResolver;
    private final Logger logger;
    private final Context context;

    @Inject
    public DatabaseQuery(ContentResolver contentResolver, Logger logger, Context context) {
        this.contentResolver = contentResolver;
        this.logger = logger;
        this.context = context;
    }

    public Single<List<IAlarmContainer>> query() {

        return Single
                .create(new SingleOnSubscribe<Cursor>() {
                    @Override
                    public void subscribe(@NonNull SingleEmitter<Cursor> e) throws Exception {
                        final Cursor cursor = contentResolver
                                .query(AlarmContainer.Columns.CONTENT_URI, AlarmContainer.Columns.ALARM_QUERY_COLUMNS, null, null, AlarmContainer.Columns.DEFAULT_SORT_ORDER);
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
                .map(new Function<Cursor, List<IAlarmContainer>>() {
                    @Override
                    public List<IAlarmContainer> apply(@NonNull Cursor cursor) throws Exception {
                        List<IAlarmContainer> alarms = new ArrayList<IAlarmContainer>();
                        try {
                            if (cursor.moveToFirst()) {
                                do {
                                    alarms.add(new AlarmContainer(cursor, logger, context));
                                } while (cursor.moveToNext());
                            }
                        } finally {
                            cursor.close();
                        }
                        return alarms;
                    }
                })
                //emit an empty list if it all fails
                .onErrorResumeNext(Single.just(Lists.<IAlarmContainer>newArrayList()));
    }
}
