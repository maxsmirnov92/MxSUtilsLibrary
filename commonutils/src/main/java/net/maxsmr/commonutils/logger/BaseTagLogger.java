package net.maxsmr.commonutils.logger;

import android.text.TextUtils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class BaseTagLogger extends BaseLogger {

    @NotNull
    protected final String tag;

    protected BaseTagLogger(@Nullable String tag) {
        if (TextUtils.isEmpty(tag)) {
            throw new IllegalArgumentException("log tag is empty");
        }
        this.tag = tag;
    }

    public static class Stub extends BaseTagLogger {

        protected Stub(@Nullable String tag) {
            super(tag);
        }

        @Override
        public void v(String message) {

        }

        @Override
        public void v(Throwable exception) {

        }

        @Override
        public void v(String message, Throwable exception) {

        }

        @Override
        public void d(String message) {

        }

        @Override
        public void d(Throwable exception) {

        }

        @Override
        public void d(String message, Throwable exception) {

        }

        @Override
        public void i(String message) {

        }

        @Override
        public void i(Throwable exception) {

        }

        @Override
        public void i(String message, Throwable exception) {

        }

        @Override
        public void w(String message) {

        }

        @Override
        public void w(Throwable exception) {

        }

        @Override
        public void w(String message, Throwable exception) {

        }

        @Override
        public void e(String message) {

        }

        @Override
        public void e(Throwable exception) {

        }

        @Override
        public void e(String message, Throwable exception) {

        }

        @Override
        public void wtf(String message) {

        }

        @Override
        public void wtf(Throwable exception) {

        }

        @Override
        public void wtf(String message, Throwable exception) {

        }
    }

}
