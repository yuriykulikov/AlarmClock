package com.better.alarm.logger

import com.better.alarm.logger.Logger.LogLevel
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.collections.ArrayList

class StartupLogWriter private constructor() : LogWriter {
    private val bufferSize = 500
    private val dtf: DateFormat = SimpleDateFormat("dd-MM HH:mm:ss", Locale.GERMANY)
    /**
     * guarded by [lock], because of the ContentProvider
     */
    private val messages = ArrayList<String>(100)
    private val lock = ReentrantLock()

    fun getMessagesAsString(): String {
        lock.lock()
        val ret = messages.joinToString("\r\n")
        lock.unlock()
        return ret
    }

    override fun write(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        val timeStamp = Date(System.currentTimeMillis())
        val exceptionMessage = throwable?.message ?: ""
        val message = "${dtf.format(timeStamp)} ${level.name} $tag $message $exceptionMessage"

        lock.lock()
        if (messages.size == bufferSize) {
            messages.removeAt(0)
        }
        messages.add(message)
        lock.unlock()
    }

    companion object {
        @JvmStatic
        fun create(): StartupLogWriter {
            return StartupLogWriter()
        }
    }
}
