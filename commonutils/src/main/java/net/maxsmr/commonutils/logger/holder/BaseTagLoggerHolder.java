package net.maxsmr.commonutils.logger.holder;

import android.support.annotation.NonNull;
import android.text.TextUtils;
import net.maxsmr.commonutils.logger.BaseTagLogger;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class BaseTagLoggerHolder extends BaseLoggerHolder {

    /** %1s - app prefix, %2s - log tag */
    private static final String TAG_FORMAT = "%1s/%2s";

    private static final Map<Class, String> TAGS = Collections.synchronizedMap(new HashMap<Class, String>());

    private final String logTag;

    public BaseTagLoggerHolder(String logTag) {
        if (TextUtils.isEmpty(logTag)) {
            throw new IllegalArgumentException("log tag is empty");
        }
        this.logTag = logTag;
    }

    protected abstract BaseTagLogger createLogger(@NonNull Class<?> clazz);

    @Override
    public BaseTagLogger getLogger(Class<?> clazz) {
        return (BaseTagLogger) super.getLogger(clazz);
    }

    protected String getTag(Class clazz) {
        String tag = TAGS.get(clazz);

        if (tag == null) {
            tag = String.format(TAG_FORMAT, logTag, clazz.getSimpleName());
            TAGS.put(clazz, tag);
        }

        return tag;
    }
}
