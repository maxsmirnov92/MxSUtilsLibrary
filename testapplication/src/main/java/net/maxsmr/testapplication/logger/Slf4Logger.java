package net.maxsmr.testapplication.logger;

import org.jetbrains.annotations.NotNull;

import net.maxsmr.commonutils.logger.BaseLogger;

import org.slf4j.Logger;

public class Slf4Logger extends BaseLogger {

    @NotNull
    private final Logger logger;

    public Slf4Logger(@NotNull Logger logger) {
        this.logger = logger;
    }

    @Override
    public void v(String message) {
        if (isLoggingEnabled()) {
            logger.trace(message);
        }
    }

    @Override
    public void v(Throwable exception) {
        if (isLoggingEnabled() && exception != null) {
            logger.trace(exception.getMessage());
        }
    }

    @Override
    public void v(String message, Throwable exception) {
        if (isLoggingEnabled()) {
            logger.trace(message, exception);
        }
    }

    @Override
    public void d(String message) {
        if (isLoggingEnabled()) {
            logger.debug(message);
        }
    }

    @Override
    public void d(Throwable exception) {
        if (isLoggingEnabled() && exception != null) {
            logger.debug(exception.getMessage());
        }
    }

    @Override
    public void d(String message, Throwable exception) {
        if (isLoggingEnabled()) {
            logger.debug(message, exception);
        }
    }

    @Override
    public void i(String message) {
        if (isLoggingEnabled()) {
            logger.info(message);
        }
    }

    @Override
    public void i(Throwable exception) {
        if (isLoggingEnabled() && exception != null) {
            logger.info(exception.getMessage());
        }
    }

    @Override
    public void i(String message, Throwable exception) {
        if (isLoggingEnabled()) {
            logger.info(message, exception);
        }
    }

    @Override
    public void w(String message) {
        if (isLoggingEnabled()) {
            logger.warn(message);
        }
    }

    @Override
    public void w(Throwable exception) {
        if (isLoggingEnabled() && exception != null) {
            logger.warn(exception.getMessage());
        }
    }

    @Override
    public void w(String message, Throwable exception) {
        if (isLoggingEnabled()) {
            logger.warn(message, exception);
        }
    }

    @Override
    public void e(String message) {
        if (isLoggingEnabled()) {
            logger.error(message);
        }
    }

    @Override
    public void e(Throwable exception) {
        if (isLoggingEnabled() && exception != null) {
            logger.error(exception.getMessage());
        }
    }

    @Override
    public void e(String message, Throwable exception) {
        if (isLoggingEnabled()) {
            logger.error(message, exception);
        }
    }

    @Override
    public void wtf(String message) {
        e(message);
    }

    @Override
    public void wtf(Throwable exception) {
        e(exception);
    }

    @Override
    public void wtf(String message, Throwable exception) {
        e(message, exception);
    }


}
