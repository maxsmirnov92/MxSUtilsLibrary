package net.maxsmr.commonutils.logger

import net.maxsmr.commonutils.format.formatDate
import net.maxsmr.commonutils.logger.BaseLogger.Level.*
import java.util.*

class SimpleSystemLogger(tag: String) : BaseLogger(tag) {

    override fun _v(message: String) {
        log(VERBOSE, message)
    }

    override fun _v(exception: Throwable) {
        v(exception.toString())
    }

    override fun _v(message: String, exception: Throwable?) {
        v(message)
        exception?.let {
            v(it)
        }
    }

    override fun _d(message: String) {
        log(DEBUG, message)
    }

    override fun _d(exception: Throwable) {
        d(exception.toString())
    }

    override fun _d(message: String, exception: Throwable?) {
        d(message)
        exception?.let {
            d(it)
        }
    }

    override fun _i(message: String) {
        log(INFO, message)
    }

    override fun _i(exception: Throwable) {
        i(exception.toString())
    }

    override fun _i(message: String, exception: Throwable?) {
        i(message)
        exception?.let {
            i(it)
        }
    }

    override fun _w(message: String) {
        log(WARN, message)
    }

    override fun _w(exception: Throwable) {
        w(exception.toString())
    }

    override fun _w(message: String, exception: Throwable?) {
        w(message)
        exception?.let {
            w(it)
        }
    }

    override fun _e(message: String) {
        log(ERROR, message)
    }

    override fun _e(exception: Throwable) {
        e(exception.toString())
    }

    override fun _e(message: String, exception: Throwable?) {
        e(message)
        exception?.let {
            e(it)
        }
    }

    override fun _wtf(message: String) {
        log(WTF, message)
    }

    override fun _wtf(exception: Throwable) {
        wtf(exception.toString())
    }

    override fun _wtf(message: String, exception: Throwable?) {
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

    private class LogEntry constructor(
        private val level: Level,
        private val tag: String,
        private val message: String,
        private val timestamp: Long
    ) {

        override fun toString(): String {
            return "[" + formatDate(Date(timestamp), "dd.MM.yyyy HH:mm:ss") + "] " + level.name + " " + tag + ": " + message
        }
    }
}
