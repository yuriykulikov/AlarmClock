package com.github.androidutils.logger;

import java.util.concurrent.CopyOnWriteArrayList;

public class Logger {
    public enum LogLevel {
        ERR, WRN, DBG, INF
    }

    /**
     * Log writing strategy
     * 
     * @author Yuriy
     * 
     */
    public interface LogWriter {
        public void write(LogLevel level, String tag, String message);

        public void write(LogLevel level, String tag, String message, Throwable e);
    }

    private final CopyOnWriteArrayList<LogWriter> writers;

    private LogLevel logLevel;

    public Logger() {
        writers = new CopyOnWriteArrayList<LogWriter>();
        logLevel = LogLevel.DBG;
    }

    public Logger addLogWriter(LogWriter logWriter) {
        writers.addIfAbsent(logWriter);
        return this;
    }

    public void removeLogWriter(LogWriter logWriter) {
        writers.remove(logWriter);
    }

    /**
     * For a given logClass only messages with logLevel above will be logged.
     * 
     * @param logLevel
     */
    public void setLogLevel(LogLevel logLevel) {
        this.logLevel = logLevel;
    }

    public LogLevel getLevel() {
        return logLevel;
    }

    /**
     * Logs the message if configured log level for the class is above requested
     * log level. If configured {@link LogLevel} is {@link LogLevel#WRN}, only
     * logs with {@link LogLevel#ERR} and {@link LogLevel#WRN} will be shown.
     * 
     * @param logLevel
     * @param message
     */
    public void log(LogLevel logLevel, String message) {
        logIfApplicable(logLevel, message, null);
    }

    private void logIfApplicable(LogLevel requestedLogLevel, Object message, Throwable throwable) {
        final boolean shouldBeLogged = requestedLogLevel.ordinal() <= logLevel.ordinal();
        if (shouldBeLogged) {
            // TODO cache tags! Use linenumber?
            final String formatTag = formatTag();
            for (final LogWriter writer : writers) {
                writer.write(logLevel, formatTag, message != null ? message.toString() : "null", throwable);
            }
        }
    }

    public void d(Object message) {
        logIfApplicable(LogLevel.DBG, message, null);
    }

    public void w(Object message) {
        logIfApplicable(LogLevel.WRN, message, null);
    }

    public void e(Object message) {
        logIfApplicable(LogLevel.ERR, message, null);
    }

    public void e(Object message, Throwable throwable) {
        logIfApplicable(LogLevel.ERR, message, throwable);
    }

    private static String formatTag() {
        final StackTraceElement caller = Thread.currentThread().getStackTrace()[5];
        final String fileName = caller.getFileName();
        final String logClass = fileName.substring(0, fileName.length() - 5);
        final String methodName = caller.getMethodName();
        final String tag = "[" + logClass + "." + methodName + "]";
        return tag;
    }

    public static synchronized Logger getDefaultLogger() {
        if (sDefaultLogger == null) {
            sDefaultLogger = new Logger();
        }
        return sDefaultLogger;
    }

    private static volatile Logger sDefaultLogger;
}
