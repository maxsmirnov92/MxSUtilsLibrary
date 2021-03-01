package net.maxsmr.commonutils.logger;

import org.jetbrains.annotations.Nullable;
import android.util.Log;

public class LogcatLogger extends BaseTagLogger {

    public LogcatLogger(@Nullable String tag) {
        super(tag);
    }

    @Override
    public void v(String message) {
        if (isLoggingEnabled()) {
            if (message != null) {
                Log.v(tag, message);
            }
        }
    }

    @Override
    public void v(Throwable exception) {
        if (isLoggingEnabled()) {
            if (exception != null) {
                Log.v(tag, exception.getMessage(), exception);
            }
        }
    }

    @Override
    public void v(String message, Throwable exception) {
        if (isLoggingEnabled()) {
            if (message != null && exception != null) {
                Log.v(tag, message, exception);
            } else if (message != null) {
                v(message);
            } else if (exception != null) {
                v(exception);
            }
        }
    }

    @Override
    public void d(String message) {
        if (isLoggingEnabled()) {
            if (message != null) {
                Log.d(tag, message);
            }
        }
    }

    @Override
    public void d(Throwable exception) {
        if (isLoggingEnabled()) {
            if (exception != null) {
                Log.d(tag, exception.getMessage(), exception);
            }
        }
    }

    @Override
    public void d(String message, Throwable exception) {
        if (isLoggingEnabled()) {
            if (message != null && exception != null) {
                Log.d(tag, message, exception);
            } else if (message != null) {
                d(message);
            } else if (exception != null) {
                d(exception);
            }
        }
    }

    @Override
    public void i(String message) {
        if (isLoggingEnabled()) {
            if (message != null) {
                Log.i(tag, message);
            }
        }
    }

    @Override
    public void i(Throwable exception) {
        if (isLoggingEnabled()) {
            if (exception != null) {
                Log.i(tag, exception.getMessage(), exception);
            }
        }
    }

    @Override
    public void i(String message, Throwable exception) {
        if (isLoggingEnabled()) {
            if (message != null && exception != null) {
                Log.i(tag, message, exception);
            } else if (message != null) {
                i(message);
            } else if (exception != null) {
                i(exception);
            }
        }
    }

    @Override
    public void w(String message) {
        if (isLoggingEnabled()) {
            if (message != null) {
                Log.w(tag, message);
            }
        }
    }

    @Override
    public void w(Throwable exception) {
        if (isLoggingEnabled()) {
            if (exception != null) {
                Log.w(tag, exception.getMessage(), exception);
            }
        }
    }

    @Override
    public void w(String message, Throwable exception) {
        if (isLoggingEnabled()) {
            if (message != null && exception != null) {
                Log.w(tag, message, exception);
            } else if (message != null) {
                w(message);
            } else if (exception != null) {
                w(exception);
            }
        }
    }

    @Override
    public void e(String message) {
        if (isLoggingEnabled()) {
            if (message != null) {
                Log.e(tag, message);
            }
        }
    }

    @Override
    public void e(Throwable exception) {
        if (isLoggingEnabled()) {
            if (exception != null) {
                Log.e(tag, exception.getMessage(), exception);
            }
        }
    }

    @Override
    public void e(String message, Throwable exception) {
        if (isLoggingEnabled()) {
            if (message != null && exception != null) {
                Log.e(tag, message, exception);
            } else if (message != null) {
                e(message);
            } else if (exception != null) {
                e(exception);
            }
        }
    }

    @Override
    public void wtf(String message) {
        if (isLoggingEnabled()) {
            if (message != null) {
                Log.wtf(tag, message);
            }
        }
    }

    @Override
    public void wtf(Throwable exception) {
        if (isLoggingEnabled()) {
            if (exception != null) {
                Log.wtf(tag, exception.getMessage(), exception);
            }
        }
    }

    @Override
    public void wtf(String message, Throwable exception) {
        if (isLoggingEnabled()) {
            if (message != null && exception != null) {
                Log.wtf(tag, message, exception);
            } else if (message != null) {
                wtf(message);
            } else if (exception != null) {
                wtf(exception);
            }
        }
    }
}
