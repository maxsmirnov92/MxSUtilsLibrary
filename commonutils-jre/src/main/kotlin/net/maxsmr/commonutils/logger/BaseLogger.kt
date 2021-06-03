package net.maxsmr.commonutils.logger

abstract class BaseLogger(protected val tag: String) {

    var isLoggingEnabled = true

    abstract fun v(message: String)

    abstract fun v(exception: Throwable)

    abstract fun v(message: String, exception: Throwable?)

    abstract fun d(message: String)

    abstract fun d(exception: Throwable)

    abstract fun d(message: String, exception: Throwable?)

    abstract fun i(message: String)

    abstract fun i(exception: Throwable)

    abstract fun i(message: String, exception: Throwable?)

    abstract fun w(message: String)

    abstract fun w(exception: Throwable)

    abstract fun w(message: String, exception: Throwable?)

    abstract fun e(message: String)

    abstract fun e(exception: Throwable)

    abstract fun e(message: String, exception: Throwable?)

    abstract fun wtf(message: String)

    abstract fun wtf(exception: Throwable)

    abstract fun wtf(message: String, exception: Throwable?)

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

        override fun v(message: String) {}

        override fun v(exception: Throwable) {}

        override fun v(message: String, exception: Throwable?) {}

        override fun d(message: String) {}

        override fun d(exception: Throwable) {}

        override fun d(message: String, exception: Throwable?) {}

        override fun i(message: String) {}

        override fun i(exception: Throwable) {}

        override fun i(message: String, exception: Throwable?) {}

        override fun w(message: String) {}

        override fun w(exception: Throwable) {}

        override fun w(message: String, exception: Throwable?) {}

        override fun e(message: String) {}

        override fun e(exception: Throwable) {}

        override fun e(message: String, exception: Throwable?) {}

        override fun wtf(message: String) {}

        override fun wtf(exception: Throwable) {}

        override fun wtf(message: String, exception: Throwable?) {}
    }
}
