package net.maxsmr.testapplication;

import android.app.Application;
import org.jetbrains.annotations.NotNull;

import net.maxsmr.commonutils.logger.BaseLogger;
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder;
import net.maxsmr.commonutils.logger.holder.ILoggerHolderProvider;
import net.maxsmr.testapplication.logger.ConfigureLog4J;
import net.maxsmr.testapplication.logger.Slf4Logger;

import org.apache.log4j.Level;
import org.slf4j.LoggerFactory;

import java.io.File;

public class TestApplication extends Application {

    static {
        BaseLoggerHolder.initInstance(() -> new BaseLoggerHolder(true) {
            @Override
            protected BaseLogger createLogger(@NotNull Class<?> clazz) {
                if (ConfigureLog4J.getInstance().isInitialized()) {
                    return new Slf4Logger(LoggerFactory.getLogger(clazz));
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
