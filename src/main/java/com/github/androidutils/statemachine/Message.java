package com.github.androidutils.statemachine;

import com.google.common.base.Optional;

import org.immutables.value.Value;

/**
 * Created by Yuriy on 07.03.2017.
 */
@Value.Immutable
@Value.Style(stagedBuilder = true)
public abstract class Message {
    public abstract int what();

    @Value.Auxiliary
    public abstract IHandler handler();

    public abstract Optional<Integer> arg1();

    public abstract Optional<Integer> arg2();

    public abstract Optional<Object> obj();

    public void send() {
        handler().sendMessage(this);
    }

    public void sendAtFront() {
        handler().sendMessageAtFrontOfQueue(this);
    }
}
