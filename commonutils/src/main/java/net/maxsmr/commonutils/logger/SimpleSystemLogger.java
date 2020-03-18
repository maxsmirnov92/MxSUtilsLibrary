package net.maxsmr.commonutils.logger;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static net.maxsmr.commonutils.data.conversion.format.DateFormatUtilsKt.formatDateNoThrow;

public class SimpleSystemLogger extends BaseTagLogger {

    protected SimpleSystemLogger(@Nullable String tag) {
        super(tag);
    }

    @Override
    public void v(String message) {
        if (isLoggingEnabled()) {
            log(Level.VERBOSE, message);
        }
    }

    @Override
    public void v(Throwable exception) {
        if (exception != null) {
            v(exception.toString());
        }
    }

    @Override
    public void v(String message, Throwable exception) {
        v(message);
        v(exception);
    }

    @Override
    public void d(String message) {
        if (isLoggingEnabled()) {
            log(Level.DEBUG, message);
        }
    }

    @Override
    public void d(Throwable exception) {
        d(exception.toString());
    }

    @Override
    public void d(String message, Throwable exception) {
        d(message);
        d(exception);

    }

    @Override
    public void i(String message) {
        if (isLoggingEnabled()) {
            log(Level.INFO, message);
        }
    }

    @Override
    public void i(Throwable exception) {
        i(exception.toString());
    }

    @Override
    public void i(String message, Throwable exception) {
        i(message);
        i(exception);
    }

    @Override
    public void w(String message) {
        if (isLoggingEnabled()) {
            log(Level.WARN, message);
        }
    }

    @Override
    public void w(Throwable exception) {
        w(exception.toString());
    }

    @Override
    public void w(String message, Throwable exception) {
        w(message);
        w(exception);
    }

    @Override
    public void e(String message) {
        if (isLoggingEnabled()) {
            log(Level.ERROR, message);
        }
    }

    @Override
    public void e(Throwable exception) {
        e(exception.toString());
    }

    @Override
    public void e(String message, Throwable exception) {
        e(message);
        e(exception);
    }

    @Override
    public void wtf(String message) {
        if (isLoggingEnabled()) {
            log(Level.WTF, message);
        }
    }

    @Override
    public void wtf(Throwable exception) {
        if (exception != null) {
            wtf(exception.toString());
        }
    }

    @Override
    public void wtf(String message, Throwable exception) {
        wtf(message);
        wtf(exception);
    }

    private void log(@NotNull Level level, @Nullable String message) {
        if (message != null) {
            LogEntry logEntry = new LogEntry(level, tag, message, System.currentTimeMillis());
            if (level == Level.ERROR || level == Level.WTF) {
                System.err.println(logEntry.toString());
            } else {
                System.out.println(logEntry.toString());
            }
        }
    }

    private static class LogEntry {

        private final SimpleDateFormat SDF = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault());

        @NotNull
        private final Level level;

        @NotNull
        private final String tag;

        @NotNull
        private final String message;

        private final long timestamp;

        private LogEntry(@NotNull Level level, @NotNull String tag, @NotNull String message, long timestamp) {
            this.level = level;
            this.tag = tag;
            this.message = message;
            this.timestamp = timestamp;
        }

        @Override
        @NotNull
        public String toString() {
            return "[" + formatDateNoThrow(new Date(timestamp), SDF, null) + "] " + level.name() + " " + tag + ": " + message;
        }
    }
}
