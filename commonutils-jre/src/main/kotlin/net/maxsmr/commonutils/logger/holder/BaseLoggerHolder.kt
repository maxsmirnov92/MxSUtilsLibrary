package net.maxsmr.commonutils.logger.holder

import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.commonutils.text.isEmpty
import java.util.*
import kotlin.collections.HashMap

abstract class BaseLoggerHolder protected constructor() {

    private val loggersMap =  Collections.synchronizedMap(HashMap<String, BaseLogger>())

    val loggersCount: Int get() = loggersMap.size

    /**
     * @param clazz object class to get/create logger for
     */
    fun <L : BaseLogger?> getLogger(clazz: Class<*>): L =
            getLogger(clazz.name)

    /**
     * @param className object class to get/create logger for
     */
    @Suppress("UNCHECKED_CAST")
    fun <L : BaseLogger> getLogger(className: String): L {
        require(className.isNotEmpty()) { "className is empty" }
            var logger = loggersMap[className]
            if (logger == null) {
                var addToMap = true
                logger = createLogger(className)
                if (logger == null) {
                    logger = BaseLogger.Stub(EMPTY_STRING)
                    addToMap = false
                }
                if (addToMap) {
                    loggersMap[className] = logger
                }
            }
            return logger as L
    }

    protected abstract fun createLogger(className: String): BaseLogger?

    companion object {

        @JvmStatic
        val instance: BaseLoggerHolder
            get() {
                synchronized(BaseLoggerHolder::class.java) {
                    with(sInstance) {
                        checkNotNull(this) { "LoggerHolder is not initialized" }
                        return this
                    }
                }
            }

        private var sInstance: BaseLoggerHolder? = null

        @JvmStatic
        fun initInstance(provider: () -> BaseLoggerHolder) {
            synchronized(BaseLoggerHolder::class.java) {
                if (sInstance == null) {
                    sInstance = provider()
                }
            }
        }

        @JvmStatic
        fun releaseInstance() {
            synchronized(BaseLoggerHolder::class.java) { sInstance = null }
        }

        @JvmStatic
        @JvmOverloads
        fun logException(logger: BaseLogger, e: Throwable, description: String? = null) {
            logger.e(formatException(e, description), e)
        }

        @JvmStatic
        @JvmOverloads
        fun formatException(e: Throwable, description: String? = null): String {
            return ("A(n) ${e.javaClass.simpleName} occurred"
                    + (if (isEmpty(description)) EMPTY_STRING else " by $description") + ": ${e.message}")
        }

        @JvmStatic
        @JvmOverloads
        @Throws(RuntimeException::class)
        fun throwRuntimeException(e: Throwable, description: String? = null) {
            throw RuntimeException(formatException(e, description), e)
        }
    }
}
