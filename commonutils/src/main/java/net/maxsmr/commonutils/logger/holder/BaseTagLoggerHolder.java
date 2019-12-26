package net.maxsmr.commonutils.logger.holder;

import org.jetbrains.annotations.NotNull;

import net.maxsmr.commonutils.data.StringUtils;
import net.maxsmr.commonutils.logger.BaseTagLogger;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class BaseTagLoggerHolder extends BaseLoggerHolder {

    /** %1s - app prefix, %2s - log tag */
    private static final String TAG_FORMAT = "%1s/%2s";

    private static final Map<String, String> TAGS = Collections.synchronizedMap(new HashMap<>());

    private final String logTag;

    public BaseTagLoggerHolder(String logTag, boolean isNullInstancesAllowed) {
        super(isNullInstancesAllowed);
        if (StringUtils.isEmpty(logTag)) {
            throw new IllegalArgumentException("log tag is empty");
        }
        this.logTag = logTag;
    }

    protected abstract BaseTagLogger createLogger(@NotNull Class<?> clazz);

    protected String getTag(Class<?> clazz) {
        return getTag(clazz.getSimpleName());
    }

    protected String getTag(String className) {
        String tag = TAGS.get(className);
        if (tag == null) {
            tag = String.format(TAG_FORMAT, logTag, className);
            TAGS.put(className, tag);
        }
        return tag;
    }
}
