package net.maxsmr.testapp

import android.app.Application
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.testapp.logger.ConfigureLog4J
import net.maxsmr.testapp.logger.Slf4Logger
import org.apache.log4j.Level
import org.slf4j.LoggerFactory
import java.io.File

class TestApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        ConfigureLog4J.configure(Level.ALL,
                File (externalCacheDir ?: cacheDir, "test.log"))
    }

    companion object {

        init {
            // можно вызвать и после configure в onCreate
            // если обращение к getLogger здесь будет раньше configure -> падение
            BaseLoggerHolder.initInstance {
                object : BaseLoggerHolder() {

                    override fun createLogger(className: String): BaseLogger =
                            Slf4Logger(LoggerFactory.getLogger(className))
                }
            }
        }
    }
}