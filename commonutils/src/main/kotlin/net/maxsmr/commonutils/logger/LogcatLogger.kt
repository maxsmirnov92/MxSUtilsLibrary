package net.maxsmr.commonutils.logger

import android.util.Log

class LogcatLogger(tag: String) : BaseLogger(tag) {

    override fun v(message: String) {
        if (isLoggingEnabled) {
                Log.v(tag, message)
        }
    }

    override fun v(exception: Throwable) {
        if (isLoggingEnabled) {
                Log.v(tag, exception.message, exception)
        }
    }

    override fun v(message: String, exception: Throwable?) {
        if (isLoggingEnabled) {
            Log.v(tag, message, exception)
        }
    }

    override fun d(message: String) {
        if (isLoggingEnabled) {
                Log.d(tag, message)
        }
    }

    override fun d(exception: Throwable) {
        if (isLoggingEnabled) {
                Log.d(tag, exception.message, exception)
        }
    }

    override fun d(message: String, exception: Throwable?) {
        if (isLoggingEnabled) {
            Log.d(tag, message, exception)
        }
    }

    override fun i(message: String) {
        if (isLoggingEnabled) {
            Log.i(tag, message)
        }
    }

    override fun i(exception: Throwable) {
        if (isLoggingEnabled) {
            Log.i(tag, exception.message, exception)
        }
    }

    override fun i(message: String, exception: Throwable?) {
        if (isLoggingEnabled) {
            Log.i(tag, message, exception)
        }
    }

    override fun w(message: String) {
        if (isLoggingEnabled) {
            Log.w(tag, message)
        }
    }

    override fun w(exception: Throwable) {
        if (isLoggingEnabled) {
            Log.w(tag, exception.message, exception)
        }
    }

    override fun w(message: String, exception: Throwable?) {
        if (isLoggingEnabled) {
            Log.w(tag, message, exception)
        }
    }

    override fun e(message: String) {
        if (isLoggingEnabled) {
            Log.e(tag, message)
        }
    }

    override fun e(exception: Throwable) {
        if (isLoggingEnabled) {
            Log.e(tag, exception.message, exception)
        }
    }

    override fun e(message: String, exception: Throwable?) {
        if (isLoggingEnabled) {
            Log.e(tag, message, exception)
        }
    }

    override fun wtf(message: String) {
        if (isLoggingEnabled) {
            Log.wtf(tag, message)
        }
    }

    override fun wtf(exception: Throwable) {
        if (isLoggingEnabled) {
            Log.wtf(tag, exception.message, exception)
        }
    }

    override fun wtf(message: String, exception: Throwable?) {
        if (isLoggingEnabled) {
            Log.wtf(tag, message, exception)
        }
    }
}
