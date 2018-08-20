package net.maxsmr.commonutils.logger;


import android.util.Log;

import net.maxsmr.commonutils.logger.base.BaseLogger;

public class AndroidSimpleLogger extends BaseLogger {

    private final String tag;

    public AndroidSimpleLogger(String tag) {
        this.tag = tag;
    }

    @Override
    public void info(String message) {
        if (isLoggingEnabled()) {
            if (message != null) {
                Log.i(tag, message);
            }
        }
    }

    @Override
    public void info(String message, Throwable exception) {
        if (isLoggingEnabled()) {
            if (message != null && exception != null) {
                Log.i(tag, message, exception);
            }
        }
    }

    @Override
    public void debug(String message) {
        if (isLoggingEnabled()) {
            if (message != null) {
                Log.d(tag, message);
            }
        }
    }

    @Override
    public void debug(String message, Throwable exception) {
        if (isLoggingEnabled()) {
            if (message != null && exception != null) {
                Log.d(tag, message, exception);
            }
        }
    }

    @Override
    public void warn(String message) {
        if (isLoggingEnabled()) {
            if (message != null) {
                Log.w(tag, message);
            }
        }
    }

    @Override
    public void warn(Throwable exception) {
        if (isLoggingEnabled()) {
            if (exception != null) {
                Log.w(tag, exception.getMessage(), exception);
            }
        }
    }

    @Override
    public void warn(String message, Throwable exception) {
        if (isLoggingEnabled()) {
            Log.w(tag, message, exception);
        }
    }

    @Override
    public void error(String message) {
        if (isLoggingEnabled()) {
            if (message != null) {
                Log.e(tag, message);
            }
        }
    }

    @Override
    public void error(Throwable exception) {
        if (isLoggingEnabled()) {
            if (exception != null) {
                Log.e(tag, exception.getMessage(), exception);
            }
        }
    }

    @Override
    public void error(String message, Throwable exception) {
        if (isLoggingEnabled()) {
            Log.e(tag, message, exception);
        }
    }
}
