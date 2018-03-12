package net.maxsmr.commonutils.logger.base;

public abstract class BaseSimpleLogger implements Logger {

    private boolean mLoggingEnabled = true;

    public boolean isLoggingEnabled() {
        return mLoggingEnabled;
    }

    public void setLoggingEnabled(boolean mLoggingEnabled) {
        this.mLoggingEnabled = mLoggingEnabled;
    }
}
