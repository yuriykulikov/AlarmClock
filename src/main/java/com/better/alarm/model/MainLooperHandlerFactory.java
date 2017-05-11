package com.better.alarm.model;

import android.os.Handler;

import com.github.androidutils.statemachine.HandlerFactory;
import com.github.androidutils.statemachine.IHandler;
import com.github.androidutils.statemachine.ImmutableMessage;
import com.github.androidutils.statemachine.Message;
import com.github.androidutils.statemachine.MessageHandler;
import com.github.androidutils.statemachine.StateMachine;
import com.google.inject.Inject;

/**
 * Created by Yuriy on 01.05.2017.
 */
public class MainLooperHandlerFactory implements HandlerFactory {
    @Inject
    public MainLooperHandlerFactory(){};

    @Override
    public IHandler create(final MessageHandler handler) {
        final Handler realHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(android.os.Message msg) {
                handler.handleMessage((Message) msg.obj);
                return true;
            }
        });

        return new IHandler() {
            @Override
            public void sendMessageAtFrontOfQueue(Message message) {
                realHandler.sendMessageAtFrontOfQueue(realHandler.obtainMessage(1, message));
            }

            @Override
            public void sendMessage(Message message) {
                realHandler.sendMessage(realHandler.obtainMessage(1, message));
            }

            @Override
            public ImmutableMessage obtainMessage(int what, Object obj) {
                return obtainMessage(what).withObj(obj);
            }

            @Override
            public ImmutableMessage obtainMessage(int what) {
                return ImmutableMessage.builder().what(what).handler(this).build();
            }
        };
    }
}
