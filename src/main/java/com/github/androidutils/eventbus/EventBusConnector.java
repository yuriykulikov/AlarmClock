package com.github.androidutils.eventbus;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Parcelable;

import com.github.androidutils.logger.Logger;
import com.google.common.base.Preconditions;
import com.google.common.eventbus.DeadEvent;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

public class EventBusConnector extends BroadcastReceiver {
    private final EventBus eventBus;
    private final Logger logger;

    public EventBusConnector(Context context, Object listener, Logger pLogger) {
        this.eventBus = new EventBus();
        this.logger = Preconditions.checkNotNull(pLogger);

        eventBus.register(listener);
        eventBus.register(new Object() {
            @Subscribe
            public void handle(DeadEvent deadEvent) {
                logger.e("Event was not handled" + deadEvent.toString());
            }
        });
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        handleIntent(intent);
    }

    public boolean handleIntent(Intent intent) {
        if (intent != null && intent.hasExtra(IEventBus.EXTRA_EVENT)) {
            Parcelable event = intent.getParcelableExtra(IEventBus.EXTRA_EVENT);
            eventBus.post(event);
            return true;
        } else return false;
    }
}
