package net.maxsmr.commonutils.logger;


import net.maxsmr.commonutils.logger.base.BaseLogger;

public class SimpleLogger extends BaseLogger {

    @Override
    public void info(String message) {
        if (isLoggingEnabled()) {
            if (message != null) {
                System.out.println(message);
            }
        }
    }

    @Override
    public void info(String message, Throwable exception) {
        if (isLoggingEnabled()) {
            System.out.println(message);
            if (exception != null) {
                exception.printStackTrace();
            }
        }
    }

    @Override
    public void debug(String message) {
        if (isLoggingEnabled()) {
            if (message != null) {
                System.out.println(message);
            }
        }
    }

    @Override
    public void debug(String message, Throwable exception) {
        if (isLoggingEnabled()) {
            System.out.println(message);
            if (exception != null) {
                exception.printStackTrace();
            }
        }
    }

    @Override
    public void warn(String message) {
        if (isLoggingEnabled()) {
            if (message != null) {
                System.err.println(message);
            }
        }
    }

    @Override
    public void warn(Throwable exception) {
        if (isLoggingEnabled()) {
            if (exception != null) {
                exception.printStackTrace();
            }
        }
    }

    @Override
    public void warn(String message, Throwable exception) {
        if (isLoggingEnabled()) {
            System.err.println(message);
            if (exception != null) {
                exception.printStackTrace();
            }
        }
    }

    @Override
    public void error(String message) {
        if (isLoggingEnabled()) {
            if (message != null) {
                System.err.println(message);
            }
        }
    }

    @Override
    public void error(Throwable exception) {
        if (isLoggingEnabled()) {
            if (exception != null) {
                exception.printStackTrace();
            }
        }
    }

    @Override
    public void error(String message, Throwable exception) {
        if (isLoggingEnabled()) {
            System.err.println(message);
            if (exception != null) {
                exception.printStackTrace();
            }
        }
    }
}
