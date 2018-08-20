package net.maxsmr.commonutils.logger.holder;

import android.support.annotation.NonNull;

import net.maxsmr.commonutils.logger.base.BaseLogger;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class BaseLoggerHolder {

    private static BaseLoggerHolder sInstance;

    protected final Map<Class<?>, BaseLogger> loggersMap = new LinkedHashMap<>();

    public static BaseLoggerHolder getInstance() {
        synchronized (BaseLoggerHolder.class) {
            if (sInstance == null) {
                throw new IllegalStateException(BaseLoggerHolder.class.getSimpleName() + " is not initialized");
            }
            return sInstance;
        }
    }

    public static void initInstance(@NonNull ILoggerHolderProvider<?> provider) {
        synchronized (BaseLoggerHolder.class) {
            if (sInstance == null) {
                sInstance = provider.provideHolder();
            }
        }
    }

    public Map<Class<?>, BaseLogger> getLoggersMap() {
        synchronized (loggersMap) {
            return Collections.unmodifiableMap(loggersMap);
        }
    }

    public int getLoggersCount() {
        return getLoggersMap().size();
    }

    /**
     * @param clazz object class to get/create logger for
     * */
    public BaseLogger getLogger(Class<?> clazz/*, Class<L> loggerClass*/) {
        if (clazz == null) {
            throw new IllegalArgumentException("clazz is null");
        }
        synchronized (loggersMap) {
            BaseLogger logger = loggersMap.get(clazz);
            if (logger == null) {
                logger = createLogger(clazz);
                if (logger == null) {
                    throw new RuntimeException("Logger was not created for class: " + clazz);
                }
                loggersMap.put(clazz, logger);
            }
//        if (!loggerClass.isAssignableFrom(logger.getClass())) {
//            throw new IllegalStateException("Logger " + logger + " is incorrect for required class: " + loggerClass);
//        }
            return logger;
        }
    }

    protected abstract BaseLogger createLogger(@NonNull Class<?> clazz);

}
