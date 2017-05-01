package com.github.androidutils.statemachine;

/**
 * Created by Yuriy on 07.03.2017.
 */
public interface HandlerFactory {
    IHandler create(MessageHandler messageHandler);
}
