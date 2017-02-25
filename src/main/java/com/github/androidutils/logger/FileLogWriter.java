package com.github.androidutils.logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import android.content.Context;
import android.util.Log;

import com.github.androidutils.logger.Logger.LogLevel;
import com.github.androidutils.logger.Logger.LogWriter;

public class FileLogWriter implements LogWriter {
    private final DateFormat df;
    private final DateFormat dtf;
    private final boolean useSeparateFileForEachDay;
    private final Context context;

    private FileLogWriter(Context context, boolean useSeparateFileForEachDay) {
        df = new SimpleDateFormat("yyyy-MM-dd");
        dtf = new SimpleDateFormat("dd-MM HH:mm:ss");

        this.useSeparateFileForEachDay = useSeparateFileForEachDay;
        this.context = context;
    }

    @Override
    public void write(LogLevel level, String tag, String message) {
        write(level, tag, message, null);
    }

    @Override
    public void write(LogLevel level, String tag, String message, Throwable throwable) {
        Calendar today = Calendar.getInstance();
        String date = df.format(today.getTime());
        final File logFile;
        if (useSeparateFileForEachDay) {
            logFile = new File(context.getFilesDir(), date + "_applog.log");
        } else {
            logFile = new File(context.getFilesDir(), "applog.log");
        }
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
                Log.d(getClass().getName(), "Created a new file");
            } catch (final IOException e) {
                Log.d(getClass().getName(), "Creating new file failed - " + e.getMessage());
                return;
            }
        }

        try {
            // BufferedWriter for performance, true to set append to file flag
            final FileWriter fileWriter = new FileWriter(logFile, true);
            final BufferedWriter buf = new BufferedWriter(fileWriter);
            final Date timeStamp = new Date(System.currentTimeMillis());
            buf.append(dtf.format(timeStamp));
            buf.append(" ");
            buf.append(level.name());
            buf.append(" ");
            buf.append(tag);
            buf.append(" ");
            buf.append(message);
            if (throwable != null) {
                final PrintStream stream = new PrintStream(logFile);
                throwable.printStackTrace(stream);
            }
            buf.newLine();
            buf.flush();
            buf.close();
            fileWriter.close();
        } catch (final IOException e) {
            Log.d(getClass().getName(), "Writing failed - " + e.getMessage());
        }
    }

    private static volatile FileLogWriter sInstance;

    public static synchronized FileLogWriter getInstance(Context context, boolean useSeparateFileForEachDay) {
        if (sInstance == null) {
            sInstance = new FileLogWriter(context, useSeparateFileForEachDay);
        }
        return sInstance;
    }
}
