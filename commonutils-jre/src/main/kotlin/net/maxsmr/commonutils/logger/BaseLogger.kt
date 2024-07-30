package net.maxsmr.commonutils.logger

import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder

abstract class BaseLogger(protected val tag: String) {

    abstract fun _v(message: String)

    abstract fun _v(exception: Throwable)

    abstract fun _v(message: String, exception: Throwable?)

    abstract fun _d(message: String)

    abstract fun _d(exception: Throwable)

    abstract fun _d(message: String, exception: Throwable?)

    abstract fun _i(message: String)

    abstract fun _i(exception: Throwable)

    abstract fun _i(message: String, exception: Throwable?)

    abstract fun _w(message: String)

    abstract fun _w(exception: Throwable)

    abstract fun _w(message: String, exception: Throwable?)

    abstract fun _e(message: String)

    abstract fun _e(exception: Throwable)

    abstract fun _e(message: String, exception: Throwable?)

    abstract fun _wtf(message: String)

    abstract fun _wtf(exception: Throwable)

    abstract fun _wtf(message: String, exception: Throwable?)

    fun v(message: String) {
        if (BaseLoggerHolder.instance.loggerLevel.isVerbose) {
            _v(message)
        }
    }

    fun v(exception: Throwable) {
        if (BaseLoggerHolder.instance.loggerLevel.isVerbose) {
            _v(exception)
        }
    }

    fun v(message: String, exception: Throwable?) {
        if (BaseLoggerHolder.instance.loggerLevel.isVerbose) {
            _v(message, exception)
        }
    }

    fun d(message: String) {
        if (BaseLoggerHolder.instance.loggerLevel.isDebug) {
            _d(message)
        }
    }

    fun d(exception: Throwable) {
        if (BaseLoggerHolder.instance.loggerLevel.isDebug) {
            _d(exception)
        }
    }

    fun d(message: String, exception: Throwable?) {
        if (BaseLoggerHolder.instance.loggerLevel.isDebug) {
            _d(message, exception)
        }
    }

    fun i(message: String) {
        if (BaseLoggerHolder.instance.loggerLevel.isInfo) {
            _i(message)
        }
    }

    fun i(exception: Throwable) {
        if (BaseLoggerHolder.instance.loggerLevel.isInfo) {
            _i(exception)
        }
    }

    fun i(message: String, exception: Throwable?) {
        if (BaseLoggerHolder.instance.loggerLevel.isInfo) {
            _i(message, exception)
        }
    }

    fun w(message: String) {
        if (BaseLoggerHolder.instance.loggerLevel.isWarn) {
            _w(message)
        }
    }

    fun w(exception: Throwable) {
        if (BaseLoggerHolder.instance.loggerLevel.isWarn) {
            _w(exception)
        }
    }

    fun w(message: String, exception: Throwable?) {
        if (BaseLoggerHolder.instance.loggerLevel.isWarn) {
            _w(message, exception)
        }
    }

    fun e(message: String) {
        if (BaseLoggerHolder.instance.loggerLevel.isError) {
            _e(message)
        }
    }

    fun e(exception: Throwable) {
        if (BaseLoggerHolder.instance.loggerLevel.isError) {
            _e(exception)
        }
    }

    fun e(message: String, exception: Throwable?) {
        if (BaseLoggerHolder.instance.loggerLevel.isError) {
            _e(message, exception)
        }
    }

    fun wtf(message: String) {
        if (BaseLoggerHolder.instance.loggerLevel.isWtf) {
            _wtf(message)
        }
    }

    fun wtf(exception: Throwable) {
        if (BaseLoggerHolder.instance.loggerLevel.isWtf) {
            _wtf(exception)
        }
    }

    fun wtf(message: String, exception: Throwable?) {
        if (BaseLoggerHolder.instance.loggerLevel.isWtf) {
            _wtf(message, exception)
        }
    }

    enum class Level {
        VERBOSE, DEBUG, INFO, WARN, ERROR, WTF;

        val isVerbose: Boolean
            get() = isInRange(VERBOSE)

        val isDebug: Boolean
            get() = isInRange(DEBUG)

        val isInfo: Boolean
            get() = isInRange(INFO)

        val isWarn: Boolean
            get() = isInRange(WARN)

        val isError: Boolean
            get() = isInRange(ERROR)

        val isWtf: Boolean
            get() = isInRange(WTF)

        private fun isInRange(level: Level): Boolean {
            return ordinal <= level.ordinal
        }
    }

    class Stub(tag: String) : BaseLogger(tag) {

        override fun _v(message: String) {
        }

        override fun _v(exception: Throwable) {
        }

        override fun _v(message: String, exception: Throwable?) {
        }

        override fun _d(message: String) {
        }

        override fun _d(exception: Throwable) {
        }

        override fun _d(message: String, exception: Throwable?) {
        }

        override fun _i(message: String) {
        }

        override fun _i(exception: Throwable) {
        }

        override fun _i(message: String, exception: Throwable?) {
        }

        override fun _w(message: String) {
        }

        override fun _w(exception: Throwable) {
        }

        override fun _w(message: String, exception: Throwable?) {
        }

        override fun _e(message: String) {
        }

        override fun _e(exception: Throwable) {
        }

        override fun _e(message: String, exception: Throwable?) {
        }

        override fun _wtf(message: String) {
        }

        override fun _wtf(exception: Throwable) {
        }

        override fun _wtf(message: String, exception: Throwable?) {
        }
    }
}
