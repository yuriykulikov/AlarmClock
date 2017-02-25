package com.github.androidutils.statemachine;

import android.os.Message;

import com.google.common.eventbus.DeadEvent;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

public class EventState extends State {
    private final EventBus bus;
    private boolean handled;

    public EventState() {
        bus = new EventBus();
        bus.register(this);
        bus.register(new Object() {
            @Subscribe
            public void handle(DeadEvent deadEvent) {
                handled = false;
            }
        });
    }

    @Override
    public boolean processMessage(Message msg) {
        handled = true;
        if (msg.obj != null) {
            bus.post(msg.obj);
            return handled;
        } else return false;
    }

    protected final void markNotHandled() {
        handled = false;
    }
}
