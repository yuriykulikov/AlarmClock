package com.github.androidutils.logger;

import java.lang.Thread.UncaughtExceptionHandler;

public class LoggingExceptionHandler implements UncaughtExceptionHandler {

    private final Logger logger;

    UncaughtExceptionHandler previousExceptionHandler;

    public static void addLoggingExceptionHandlerToAllThreads(Logger logger) {
        Thread.setDefaultUncaughtExceptionHandler(new LoggingExceptionHandler(logger, Thread
                .getDefaultUncaughtExceptionHandler()));
    }

    public LoggingExceptionHandler(Logger logger, UncaughtExceptionHandler previousExceptionHandler) {
        this.logger = logger;
        this.previousExceptionHandler = previousExceptionHandler;
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        logger.e("on " + thread.getName() + ": " + ex.getMessage());
        previousExceptionHandler.uncaughtException(thread, ex);
    }
}
