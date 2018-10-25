package net.maxsmr.commonutils.logger.holder;

import net.maxsmr.commonutils.logger.BaseLogger;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class BaseLoggerHolder {

    private static BaseLoggerHolder sInstance;

    public static BaseLoggerHolder getInstance() {
        synchronized (BaseLoggerHolder.class) {
            if (sInstance == null) {
                throw new IllegalStateException(BaseLoggerHolder.class.getSimpleName() + " is not initialized");
            }
            return sInstance;
        }
    }

    public static void initInstance(@NotNull ILoggerHolderProvider<?> provider) {
        synchronized (BaseLoggerHolder.class) {
            if (sInstance == null) {
                sInstance = provider.provideHolder();
            }
        }
    }

    private final Map<Class<?>, BaseLogger> loggersMap = new LinkedHashMap<>();

    private final boolean isNullInstancesAllowed;

    protected BaseLoggerHolder(boolean isNullInstancesAllowed) {
        this.isNullInstancesAllowed = isNullInstancesAllowed;
    }

    public Map<Class<?>, BaseLogger> getLoggersMap() {
        synchronized (loggersMap) {
            return Collections.unmodifiableMap(loggersMap);
        }
    }

    public int getLoggersCount() {
        return getLoggersMap().size();
    }

    public boolean isNullInstancesAllowed() {
        return isNullInstancesAllowed;
    }

    /**
     * @param clazz object class to get/create logger for
     * */
    @SuppressWarnings("unchecked")
    public <L extends BaseLogger> L getLogger(Class<?> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("clazz is null");
        }
        synchronized (loggersMap) {
            BaseLogger logger = loggersMap.get(clazz);
            if (logger == null) {
                boolean addToMap = true;
                logger = createLogger(clazz);
                if (logger == null) {
                    if (!isNullInstancesAllowed) {
                        throw new RuntimeException("Logger was not created for class: " + clazz);
                    }
                    logger = new BaseLogger.Stub();
                    addToMap = false;
                }
                if (addToMap) {
                    loggersMap.put(clazz, logger);
                }
            }
            return (L) logger;
        }
    }

    protected abstract BaseLogger createLogger(@NotNull Class<?> clazz);

}
