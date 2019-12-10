package net.maxsmr.testapp;

import android.app.Application;
import org.jetbrains.annotations.NotNull;

import net.maxsmr.commonutils.logger.BaseLogger;
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder;
import net.maxsmr.testapp.logger.ConfigureLog4J;
import net.maxsmr.testapp.logger.Slf4Logger;

import org.apache.log4j.Level;
import org.slf4j.LoggerFactory;

import java.io.File;

public class TestApplication extends Application {

    static {
        BaseLoggerHolder.initInstance(() -> new BaseLoggerHolder(true) {
            @Override
            protected BaseLogger createLogger(@NotNull String className) {
                if (ConfigureLog4J.getInstance().isInitialized()) {
                    return new Slf4Logger(LoggerFactory.getLogger(className));
                } else {
                    return null;
                }
            }
        });
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ConfigureLog4J.getInstance().configure(Level.ALL, false, new File(getFilesDir(), "1.log").getAbsolutePath(), ConfigureLog4J.DEFAULT_MIN_FILE_SIZE, 1);
    }
}
