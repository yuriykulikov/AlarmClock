package com.better.alarm;

import com.github.androidutils.statemachine.HandlerFactory;
import com.github.androidutils.statemachine.IHandler;
import com.github.androidutils.statemachine.ImmutableMessage;
import com.github.androidutils.statemachine.Message;
import com.github.androidutils.statemachine.MessageHandler;
import com.google.inject.Inject;

import io.reactivex.Scheduler;
import io.reactivex.schedulers.TestScheduler;

/**
 * Created by Yuriy on 25.06.2017.
 */
class TestHandlerFactory implements HandlerFactory {
    private Scheduler testScheduler;

    @Inject
    public TestHandlerFactory(Scheduler scheduler) {
        this.testScheduler = scheduler;
    }

    @Override
    public IHandler create(final MessageHandler messageHandler) {
        return new IHandler() {
            @Override
            public void sendMessageAtFrontOfQueue(Message message) {
                sendMessage(message);
            }

            @Override
            public void sendMessage(final Message message) {
                testScheduler.scheduleDirect(new Runnable() {
                    @Override
                    public void run() {
                        messageHandler.handleMessage(message);
                    }
                });
            }

            @Override
            public ImmutableMessage obtainMessage(int what, Object obj) {
                return ImmutableMessage.builder().what(what).handler(this).obj(obj).build();
            }

            @Override
            public ImmutableMessage obtainMessage(int what) {
                return ImmutableMessage.builder().what(what).handler(this).build();
            }
        };
    }
}
