package com.github.androidutils.eventbus;

import android.app.PendingIntent;
import android.content.Context;

public interface IEventBus<T> {
    public static final String ACTION_EVENT = "com.github.androidutils.eventbus.IntentEventReceiver.ACTION_EVENT";
    public static final String EXTRA_EVENT = "EXTRA_EVENT";

    public void post(T event, Context context);

    public void postSticky(T event, Context context);

    public PendingIntent createPendingIntent(T event, Context context);

    public void register(Object object);

    public void unregister(Object object);

}