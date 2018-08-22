package net.maxsmr.commonutils.logger;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

public abstract class BaseTagLogger extends BaseLogger {

    @NonNull
    protected final String tag;

    protected BaseTagLogger(@Nullable String tag) {
        if (TextUtils.isEmpty(tag)) {
            throw new IllegalArgumentException("log tag is empty");
        }
        this.tag = tag;
    }
}
