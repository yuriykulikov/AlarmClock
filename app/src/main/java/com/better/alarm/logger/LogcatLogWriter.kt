package com.better.alarm.logger

import android.util.Log

import com.better.alarm.logger.Logger.LogLevel

class LogcatLogWriter private constructor() : LogWriter {
    override fun write(level: LogLevel, tag: String, message: String, e: Throwable?) {
        when (level) {
            LogLevel.INF -> Log.i(tag, message, e)
            LogLevel.DBG -> Log.d(tag, message, e)
            LogLevel.WRN -> Log.w(tag, message, e)
            LogLevel.ERR -> Log.e(tag, message, e)
        }
    }

    companion object {
        @JvmStatic
        fun create(): LogcatLogWriter {
            return LogcatLogWriter()
        }
    }
}
