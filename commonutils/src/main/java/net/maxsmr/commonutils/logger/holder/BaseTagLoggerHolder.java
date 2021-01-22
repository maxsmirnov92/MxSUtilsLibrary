package net.maxsmr.commonutils.logger.holder;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static net.maxsmr.commonutils.data.text.TextUtilsKt.isEmpty;

public abstract class BaseTagLoggerHolder extends BaseLoggerHolder {

    /** %1s - app prefix, %2s - log tag */
    private static final String TAG_FORMAT = "%1s/%2s";

    private static final Map<String, String> TAGS = Collections.synchronizedMap(new HashMap<>());

    private final String logTag;

    public BaseTagLoggerHolder(String logTag, boolean isNullInstancesAllowed) {
        super(isNullInstancesAllowed);
        if (isEmpty(logTag)) {
            throw new IllegalArgumentException("log tag is empty");
        }
        this.logTag = logTag;
    }

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
