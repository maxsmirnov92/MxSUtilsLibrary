package net.maxsmr.commonutils.logger.holder;

import android.text.TextUtils;

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

    public static void logException(@NotNull BaseLogger logger, @NotNull Exception e) {
        logException(logger, e, null);
    }

    public static void logException(@NotNull BaseLogger logger, @NotNull Exception e, @Nullable String description) {
        logger.e(formatException(e, description), e);
    }

    public static String formatException(@NotNull Exception e) {
        return formatException(e, null);
    }

    public static String formatException(@NotNull Exception e, @Nullable String description) {
        return "A(n) " + e.getClass().getSimpleName() + " occurred"
                + (TextUtils.isEmpty(description)? "" : " during " + description) + ": " + e.getMessage();
    }

    public static void throwRuntimeException(@NotNull Exception e) throws RuntimeException {
        throwRuntimeException(e, null);
    }

    public static void throwRuntimeException(@NotNull Exception e, @Nullable String description) throws RuntimeException {
        throw new RuntimeException(formatException(e, description), e);
    }
}
