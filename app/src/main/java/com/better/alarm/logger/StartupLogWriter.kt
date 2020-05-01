package com.better.alarm.logger

import androidx.collection.CircularArray
import com.better.alarm.logger.Logger.LogLevel
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class StartupLogWriter private constructor() : LogWriter {
    private val bufferSize = 1024
    private val dtf: DateFormat = SimpleDateFormat("dd-MM HH:mm:ss", Locale.GERMANY)

    /**
     * guarded by [lock], because of the ContentProvider
     */
    private val messages = CircularArray<String>()
    private val lock = ReentrantLock()

    fun getMessagesAsString(): String {
        return lock.withLock {
            generateSequence { if (messages.isEmpty) null else messages.popFirst() }
                    .joinToString("\n")
        }
    }

    override fun write(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        val timeStamp = Date(System.currentTimeMillis())
        val exceptionMessage = throwable?.message ?: ""

        lock.withLock {
            if (messages.size() == bufferSize) {
                messages.popFirst()
            }
            messages.addLast("${dtf.format(timeStamp)} ${level.name} $tag $message $exceptionMessage")
        }
    }

    companion object {
        @JvmStatic
        fun create(): StartupLogWriter {
            return StartupLogWriter()
        }
    }
}
