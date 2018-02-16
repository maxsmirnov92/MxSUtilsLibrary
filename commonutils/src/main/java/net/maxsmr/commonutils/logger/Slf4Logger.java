package net.maxsmr.commonutils.logger;

import android.support.annotation.NonNull;


import net.maxsmr.commonutils.logger.base.BaseSimpleLogger;

import org.slf4j.Logger;

public class Slf4Logger extends BaseSimpleLogger {

    @NonNull
    private final Logger logger;

    public Slf4Logger(@NonNull Logger logger) {
        this.logger = logger;
    }

    @Override
    public void info(String message) {
        if (isLoggingEnabled()) {
            logger.info(message);
        }
    }

    @Override
    public void debug(String message) {
        if (isLoggingEnabled()) {
            logger.debug(message);
        }
    }

    @Override
    public void warn(String message) {
        if (isLoggingEnabled()) {
            logger.warn(message);
        }
    }

    @Override
    public void warn(Throwable exception) {
        if (isLoggingEnabled()) {
            logger.warn("", exception);
        }
    }

    @Override
    public void warn(String message, Throwable exception) {
        if (isLoggingEnabled()) {
            logger.warn(message, exception);
        }
    }

    @Override
    public void error(String message) {
        if (isLoggingEnabled()) {
            logger.error(message);
        }
    }

    @Override
    public void error(Throwable exception) {
        if (isLoggingEnabled()) {
            logger.error("", exception);
        }
    }

    @Override
    public void error(String message, Throwable exception) {
        if (isLoggingEnabled()) {
            logger.error(message, exception);
        }
    }
}
