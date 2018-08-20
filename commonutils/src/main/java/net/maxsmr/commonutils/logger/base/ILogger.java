package net.maxsmr.commonutils.logger.base;

public interface ILogger {

    void info(String message);

    void info(String message, Throwable exception);

    void debug(String message);

    void debug(String message, Throwable exception);

    void warn(String message);

    void warn(Throwable exception);

    void warn(String message, Throwable exception);

    void error(String message);

    void error(Throwable exception);

    void error(String message, Throwable exception);

    class Stub implements ILogger {

        @Override
        public void info(String message) {

        }

        @Override
        public void info(String message, Throwable exception) {

        }

        @Override
        public void debug(String message) {

        }

        @Override
        public void debug(String message, Throwable exception) {

        }

        @Override
        public void warn(String message) {

        }

        @Override
        public void warn(Throwable exception) {

        }

        @Override
        public void warn(String message, Throwable exception) {

        }

        @Override
        public void error(String message) {

        }

        @Override
        public void error(Throwable exception) {

        }

        @Override
        public void error(String message, Throwable exception) {

        }
    }
}
