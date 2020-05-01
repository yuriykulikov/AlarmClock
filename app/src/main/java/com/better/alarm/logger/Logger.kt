package com.better.alarm.logger

/**
 * Log writing strategy
 *
 * @author Yuriy
 */
interface LogWriter {
    fun write(level: Logger.LogLevel, tag: String, message: String, e: Throwable?)
}

interface LoggerFactory {
    fun createLogger(tag: String): Logger
}

class Logger private constructor(
        private val writers: List<LogWriter> = emptyList(),
        val logLevel: LogLevel = LogLevel.DBG,
        private val tag: String = ""
) {
    private fun logIfApplicable(requestedLogLevel: LogLevel, message: Any?, throwable: Throwable?) {
        if (requestedLogLevel.ordinal <= logLevel.ordinal) {
            write(message, throwable)
        }
    }

    fun write(message: Any?, throwable: Throwable?) {
        for (writer in writers) {
            writer.write(logLevel, "[${tag.padStart(20, ' ')}]", message?.toString()
                    ?: "null", throwable)
        }
    }

    inline fun debug(supplier: () -> String) {
        if (LogLevel.DBG <= logLevel) {
            write(supplier(), null)
        }
    }

    inline fun warning(supplier: () -> String) {
        if (LogLevel.WRN <= logLevel) {
            write(supplier(), null)
        }
    }

    inline fun info(supplier: () -> String) {
        if (LogLevel.INF <= logLevel) {
            write(supplier(), null)
        }
    }

    inline fun error(e: Throwable? = null, supplier: () -> String) {
        if (LogLevel.ERR <= logLevel) {
            write(supplier(), e)
        }
    }

    /** Java */
    @Deprecated("Use debug", replaceWith = ReplaceWith("this.debug { message }"))
    fun d(message: Any) {
        logIfApplicable(LogLevel.DBG, message, null)
    }

    /** Java */
    fun w(message: Any) {
        logIfApplicable(LogLevel.WRN, message, null)
    }

    /** Java */
    fun e(message: Any) {
        logIfApplicable(LogLevel.ERR, message, null)
    }

    /** Java */
    fun e(message: Any, throwable: Throwable) {
        logIfApplicable(LogLevel.ERR, message, throwable)
    }

    enum class LogLevel {
        ERR, WRN, DBG, INF
    }

    companion object {
        private fun formatTag(): String {
            val caller = Thread.currentThread().stackTrace[5]
            val fileName = caller.fileName
            val logClass = fileName.substring(0, fileName.length - 5)
            val methodName = caller.methodName
            return "[$logClass.$methodName]"
        }

        @JvmStatic
        fun create(): Logger {
            return Logger()
        }

        @JvmStatic
        fun create(vararg writer: LogWriter): Logger {
            return Logger(writer.toList())
        }

        @JvmStatic
        fun factory(vararg writer: LogWriter): LoggerFactory {
            return object : LoggerFactory {
                override fun createLogger(tag: String): Logger {
                    return Logger(writers = writer.toList(), tag = tag)
                }
            }
        }
    }
}
