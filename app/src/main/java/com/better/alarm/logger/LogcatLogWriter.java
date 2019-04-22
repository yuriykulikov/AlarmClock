package com.better.alarm.logger;

import android.util.Log;

import com.better.alarm.logger.Logger.LogLevel;

public class LogcatLogWriter implements Logger.LogWriter {
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

    public static LogcatLogWriter create() {
        return new LogcatLogWriter();
    }
}
