package com.github.androidutils.logger;

import android.util.Log;

import com.github.androidutils.logger.Logger.LogLevel;

public class LogcatLogWriter implements Logger.LogWriter {
    @Override
    public void write(LogLevel level, String tag, String message) {
        switch (level) {
        case INF:
            Log.i(tag, message);
            break;

        case DBG:
            Log.d(tag, message);
            break;

        case WRN:
            Log.w(tag, message);
            break;

        case ERR:
            Log.e(tag, message);
            break;

        default:
            break;
        }
    }

    @Override
    public void write(LogLevel level, String tag, String message, Throwable e) {
        switch (level) {
        case INF:
            Log.i(tag, message, e);
            break;

        case DBG:
            Log.d(tag, message, e);
            break;

        case WRN:
            Log.w(tag, message, e);
            break;

        case ERR:
            Log.e(tag, message, e);
            break;

        default:
            break;
        }
    }

    private LogcatLogWriter() {

    }

    private static volatile LogcatLogWriter sInstance;

    public static synchronized LogcatLogWriter getInstance() {
        if (sInstance == null) {
            sInstance = new LogcatLogWriter();
        }
        return sInstance;
    }
}
