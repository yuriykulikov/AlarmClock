package com.github.androidutils.logger;

import android.util.Log;

import com.github.androidutils.logger.Logger.LogLevel;

public class LogcatLogWriterWithLines implements Logger.LogWriter {
    @Override
    public void write(LogLevel level, String tag, String message) {
        message = addLineToMessage(message);
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

        message = addLineToMessage(message);
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

    private String addLineToMessage(String message) {
        StringBuilder sb = new StringBuilder();
        sb.append(message);

        StackTraceElement[] stackTraceElement = Thread.currentThread().getStackTrace();
        StackTraceElement ste = stackTraceElement[6];
        String file = ste.getFileName();
        String lineNumber = String.valueOf(ste.getLineNumber());

        String string = " at " + "(" + file + ":" + lineNumber + ")";

        while (sb.length() + string.length() < 114) {
            sb.append(' ');
        }

        sb.append(string);
        return sb.toString();
    }

    private LogcatLogWriterWithLines() {

    }

    private static volatile LogcatLogWriterWithLines sInstance;

    public static synchronized LogcatLogWriterWithLines getInstance() {
        if (sInstance == null) {
            sInstance = new LogcatLogWriterWithLines();
        }
        return sInstance;
    }
}
