package net.maxsmr.testapp.logger

import net.maxsmr.commonutils.logger.BaseLogger
import org.slf4j.Logger

class Slf4Logger(private val logger: Logger) : BaseLogger(logger.toString()) {
    
    override fun v(message: String) {
        if (isLoggingEnabled) {
            logger.trace(message)
        }
    }

    override fun v(exception: Throwable) {
        if (isLoggingEnabled) {
            logger.trace(exception.message)
        }
    }

    override fun v(message: String, exception: Throwable?) {
        if (isLoggingEnabled) {
            logger.trace(message, exception)
        }
    }

    override fun d(message: String) {
        if (isLoggingEnabled) {
            logger.debug(message)
        }
    }

    override fun d(exception: Throwable) {
        if (isLoggingEnabled) {
            logger.debug(exception.message)
        }
    }

    override fun d(message: String, exception: Throwable?) {
        if (isLoggingEnabled) {
            logger.debug(message, exception)
        }
    }

    override fun i(message: String) {
        if (isLoggingEnabled) {
            logger.info(message)
        }
    }

    override fun i(exception: Throwable) {
        if (isLoggingEnabled) {
            logger.info(exception.message)
        }
    }

    override fun i(message: String, exception: Throwable?) {
        if (isLoggingEnabled) {
            logger.info(message, exception)
        }
    }

    override fun w(message: String) {
        if (isLoggingEnabled) {
            logger.warn(message)
        }
    }

    override fun w(exception: Throwable) {
        if (isLoggingEnabled) {
            logger.warn(exception.message)
        }
    }

    override fun w(message: String, exception: Throwable?) {
        if (isLoggingEnabled) {
            logger.warn(message, exception)
        }
    }

    override fun e(message: String) {
        if (isLoggingEnabled) {
            logger.error(message)
        }
    }

    override fun e(exception: Throwable) {
        if (isLoggingEnabled) {
            logger.error(exception.message)
        }
    }

    override fun e(message: String, exception: Throwable?) {
        if (isLoggingEnabled) {
            logger.error(message, exception)
        }
    }

    override fun wtf(message: String) {
        e(message)
    }

    override fun wtf(exception: Throwable) {
        e(exception)
    }

    override fun wtf(message: String, exception: Throwable?) {
        e(message, exception)
    }
}
