package net.maxsmr.commonutils.logger;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import android.text.TextUtils;

public abstract class BaseTagLogger extends BaseLogger {

    @NotNull
    protected final String tag;

    protected BaseTagLogger(@Nullable String tag) {
        if (TextUtils.isEmpty(tag)) {
            throw new IllegalArgumentException("log tag is empty");
        }
        this.tag = tag;
    }
}
