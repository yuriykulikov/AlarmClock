package com.github.androidutils.logger;

import android.provider.Settings;
import android.util.Log;

import com.github.androidutils.logger.Logger.LogLevel;

public class SysoutLogWriter implements Logger.LogWriter {
    @Override
    public void write(LogLevel level, String tag, String message) {
        switch (level) {
            case INF:
                System.out.println(tag + ", " + message);
                break;

            case DBG:
                System.out.println(tag + ", " + message);
                break;

            case WRN:
                System.err.println(tag + ", " + message);
                break;

            case ERR:
                System.err.println(tag + ", " + message);
                break;

            default:
                break;
        }
    }

    @Override
    public void write(LogLevel level, String tag, String message, Throwable e) {
        switch (level) {
            case INF:
                System.err.println(tag + ", " + message);
                if (e != null) e.printStackTrace();
                break;

            case DBG:
                System.err.println(tag + ", " + message);
                if (e != null)  e.printStackTrace();
                break;

            case WRN:
                System.err.println(tag + ", " + message);
                if (e != null)  e.printStackTrace();
                break;

            case ERR:
                System.err.println(tag + ", " + message);
                if (e != null)   e.printStackTrace();
                break;

            default:
                break;
        }
    }
}
