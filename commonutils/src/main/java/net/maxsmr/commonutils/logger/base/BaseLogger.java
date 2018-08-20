package net.maxsmr.commonutils.logger.base;

public abstract class BaseLogger implements ILogger {

    private boolean mLoggingEnabled = true;

    public boolean isLoggingEnabled() {
        return mLoggingEnabled;
    }

    public void setLoggingEnabled(boolean toggle) {
        this.mLoggingEnabled = toggle;
    }
}
