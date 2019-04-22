package com.better.alarm.logger;

import com.better.alarm.logger.Logger.LogLevel;

public class SysoutLogWriter implements Logger.LogWriter {
    @Override
    public void write(LogLevel level, String tag, String message, Throwable e) {
        switch (level) {
            case INF:
            case DBG:
                System.out.println(tag + ", " + message);
                if (e != null) e.printStackTrace();
                break;

            case WRN:
            case ERR:
            default:
                System.err.println(tag + ", " + message);
                if (e != null) e.printStackTrace();
                break;
        }
    }
}
