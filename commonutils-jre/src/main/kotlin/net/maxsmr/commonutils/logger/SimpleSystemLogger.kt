package net.maxsmr.commonutils.logger

import net.maxsmr.commonutils.format.formatDate
import net.maxsmr.commonutils.logger.BaseLogger.Level.*
import java.util.*

class SimpleSystemLogger(tag: String) : BaseLogger(tag) {

    override fun v(message: String) {
        if (isLoggingEnabled) {
            log(VERBOSE, message)
        }
    }

    override fun v(exception: Throwable) {
        v(exception.toString())
    }

    override fun v(message: String, exception: Throwable?) {
        v(message)
        exception?.let {
            v(it)
        }
    }

    override fun d(message: String) {
        if (isLoggingEnabled) {
            log(DEBUG, message)
        }
    }

    override fun d(exception: Throwable) {
        d(exception.toString())
    }

    override fun d(message: String, exception: Throwable?) {
        d(message)
        exception?.let {
            d(it)
        }
    }

    override fun i(message: String) {
        if (isLoggingEnabled) {
            log(INFO, message)
        }
    }

    override fun i(exception: Throwable) {
        i(exception.toString())
    }

    override fun i(message: String, exception: Throwable?) {
        i(message)
        exception?.let {
            i(it)
        }
    }

    override fun w(message: String) {
        if (isLoggingEnabled) {
            log(WARN, message)
        }
    }

    override fun w(exception: Throwable) {
        w(exception.toString())
    }

    override fun w(message: String, exception: Throwable?) {
        w(message)
        exception?.let {
            w(it)
        }
    }

    override fun e(message: String) {
        if (isLoggingEnabled) {
            log(ERROR, message)
        }
    }

    override fun e(exception: Throwable) {
        e(exception.toString())
    }

    override fun e(message: String, exception: Throwable?) {
        e(message)
        exception?.let {
            e(it)
        }
    }

    override fun wtf(message: String) {
        if (isLoggingEnabled) {
            log(WTF, message)
        }
    }

    override fun wtf(exception: Throwable) {
        wtf(exception.toString())
    }

    override fun wtf(message: String, exception: Throwable?) {
        wtf(message)
        exception?.let {
            wtf(it)
        }
    }

    private fun log(level: Level, message: String) {
        val logEntry = LogEntry(level, tag, message, System.currentTimeMillis())
        if (level == ERROR || level == WTF) {
            System.err.println(logEntry.toString())
        } else {
            println(logEntry.toString())
        }
    }

    private class LogEntry constructor(private val level: Level, private val tag: String, private val message: String, private val timestamp: Long) {

        override fun toString(): String {
            return "[" + formatDate(Date(timestamp), "dd.MM.yyyy HH:mm:ss") + "] " + level.name + " " + tag + ": " + message
        }
    }
}
