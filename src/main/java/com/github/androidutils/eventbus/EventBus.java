package com.github.androidutils.eventbus;

import java.util.Map;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.inputmethodservice.ExtractEditText;
import android.os.Handler;
import android.os.Parcelable;

import com.github.androidutils.logger.Logger;
import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

public class EventBus<T extends Parcelable> implements IEventBus<T> {
    private final Multimap<Class<? extends T>, IEventSender<T>> listeners;
    private final com.google.common.eventbus.EventBus eventBus;
    private final com.google.common.eventbus.EventBus notifyOnAddBus;
    private final Map<Class<? extends T>, T> stickyEvents;
    private final Logger logger;
    private final Handler handler = new Handler();

    public EventBus(Logger logger) {
        this.listeners = ArrayListMultimap.create();
        this.eventBus = new com.google.common.eventbus.EventBus();
        this.notifyOnAddBus = new com.google.common.eventbus.EventBus();
        this.stickyEvents = Maps.newHashMap();
        this.logger = Preconditions.checkNotNull(logger);
    }

    public void registerActivity(Class<? extends Activity> activity, Class<? extends T> eventType) {
        listeners.put(eventType, new ActivityEventSender<T>(activity));
    }

    public void registerService(Class<? extends Service> service, Class<? extends T> eventType) {
        listeners.put(eventType, new IntentEventServiceSender<T>(service));
    }

    public void registerBroadcast(Class<? extends T> eventType) {
        listeners.put(eventType, new IntentEventBroadcastSender<T>());
    }

    @Override
    public void register(Object listener) {
        logger.d(listener.toString());
        eventBus.register(listener);
        logger.d("will now dispatch to a new listener: " + stickyEvents.values());
        notifyOnAddBus.register(listener);
        for (T event : stickyEvents.values()) {
            notifyOnAddBus.post(event);
        }
        notifyOnAddBus.unregister(listener);
    }

    @Override
    public void unregister(Object listener) {
        logger.d(listener.toString());
        eventBus.unregister(listener);
    }

    @Override
    public void post(T event, Context context) {
        logger.d(event.toString());
        for (IEventSender<T> sender : listeners.get((Class<? extends T>) event.getClass())) {
            sender.post(event, context);
        }
        postLocalEvent(event);
    }

    @Override
    public void postSticky(T event, Context context) {
        stickyEvents.put((Class<? extends T>) event.getClass(), event);
        post(event, context);
    }

    private void postLocalEvent(final T event) {
        // post event to local event bus
        handler.post(new Runnable() {
            @Override
            public void run() {
                eventBus.post(event);
            }
        });
    }

    @Override
    public PendingIntent createPendingIntent(T event, Context context) {
        if (listeners.containsKey(event.getClass())) {
            // special event
            for (IEventSender<T> sender : listeners.get((Class<T>) event.getClass()))
                return sender.createPendingIntent(event, context);
        }
        throw new UnsupportedOperationException();
    }

    private interface IEventSender<T> {
        public void post(T event, Context context);

        public PendingIntent createPendingIntent(T event, Context context);
    }

    /**
     * This one is to post events. Events will be packed into Intents and
     * broadcast. All registered {@link IntentEventReceiver} will get the
     * intent, {@link ExtractEditText} the event and pass it to the internal
     * bus.
     * 
     */
    static class IntentEventBroadcastSender<T extends Parcelable> implements IEventSender<T> {
        @Override
        public void post(T event, Context context) {
            Intent intent = createIntent(event);
            context.sendBroadcast(intent);
        }

        @Override
        public PendingIntent createPendingIntent(T event, Context context) {
            return PendingIntent.getBroadcast(context, event.hashCode(), createIntent(event), 0);
        }

        private Intent createIntent(T event) {
            Intent intent = new Intent(IEventBus.ACTION_EVENT + "_" + event.getClass().getSimpleName()
                    + event.getClass().getSimpleName());
            intent.putExtra(IEventBus.EXTRA_EVENT, event);
            return intent;
        }
    }

    /**
     * This one is to send events to a service. Events will be packed into
     * Intents and sent. All registered {@link IntentEventReceiver} will get the
     * intent, unparcel the event and pass it to the internal bus.
     * 
     */
    static class ActivityEventSender<T extends Parcelable> implements IEventSender<T> {
        private final Class<? extends Activity> clazz;

        ActivityEventSender(Class<? extends Activity> clazz) {
            this.clazz = Preconditions.checkNotNull(clazz);
        }

        @Override
        public void post(T event, Context context) {
            context.startActivity(createIntent(event, context));
        }

        @Override
        public PendingIntent createPendingIntent(T event, Context context) {
            return PendingIntent.getActivity(context, event.hashCode(), createIntent(event, context), 0);
        }

        private Intent createIntent(T event, Context context) {
            Intent intent = new Intent(IEventBus.ACTION_EVENT + "_" + event.getClass().getSimpleName());
            intent.setClass(context, clazz);
            intent.putExtra(IEventBus.EXTRA_EVENT, event);
            return intent;
        }
    }

    /**
     * This one is to send events to a service. Events will be packed into
     * Intents and sent. All registered {@link IntentEventReceiver} will get the
     * intent, unparcel the event and pass it to the internal bus.
     * 
     */
    static class IntentEventServiceSender<T extends Parcelable> implements IEventSender<T> {
        private final Class<? extends Service> clazz;

        IntentEventServiceSender(Class<? extends Service> clazz) {
            this.clazz = Preconditions.checkNotNull(clazz);
        }

        @Override
        public void post(T event, Context context) {
            context.startService(createIntent(event, context));
        }

        @Override
        public PendingIntent createPendingIntent(T event, Context context) {
            return PendingIntent.getService(context, event.hashCode(), createIntent(event, context), 0);
        }

        private Intent createIntent(T event, Context context) {
            Intent intent = new Intent(IEventBus.ACTION_EVENT + "_" + event.getClass().getSimpleName());
            intent.setClass(context, clazz);
            intent.putExtra(IEventBus.EXTRA_EVENT, event);
            return intent;
        }
    }
}
