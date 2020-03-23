package net.maxsmr.commonutils.logger.holder;

import net.maxsmr.commonutils.logger.BaseLogger;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static net.maxsmr.commonutils.data.text.TextUtilsKt.isEmpty;

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

    public static void releaseInstance() {
        synchronized (BaseLoggerHolder.class) {
            sInstance = null;
        }
    }

    private final Map<String, BaseLogger> loggersMap = new LinkedHashMap<>();

    private final boolean isNullInstancesAllowed;

    protected BaseLoggerHolder(boolean isNullInstancesAllowed) {
        this.isNullInstancesAllowed = isNullInstancesAllowed;
    }

    public Map<String, BaseLogger> getLoggersMap() {
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
        return getLogger(clazz.getName());
    }

    /**
     * @param className object class to get/create logger for
     * */
    @SuppressWarnings("unchecked")
    public <L extends BaseLogger> L getLogger(String className) {
        if (isEmpty(className)) {
            throw new IllegalArgumentException("className is empty");
        }
        synchronized (loggersMap) {
            BaseLogger logger = loggersMap.get(className);
            if (logger == null) {
                boolean addToMap = true;
                logger = createLogger(className);
                if (logger == null) {
                    if (!isNullInstancesAllowed) {
                        throw new RuntimeException("Logger was not created for className: " + className);
                    }
                    logger = new BaseLogger.Stub();
                    addToMap = false;
                }
                if (addToMap) {
                    loggersMap.put(className, logger);
                }
            }
            return (L) logger;
        }
    }

    @Nullable
    protected abstract BaseLogger createLogger(@NotNull String className);
}
