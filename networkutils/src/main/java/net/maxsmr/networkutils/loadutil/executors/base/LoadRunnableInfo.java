package net.maxsmr.networkutils.loadutil.executors.base;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import net.maxsmr.tasksutils.taskrunnable.RunnableInfo;

import java.net.MalformedURLException;
import java.net.URL;

public abstract class LoadRunnableInfo extends RunnableInfo {

    public final static class LoadSettings {

        /**
         * no retries
         */
        public static int RETRY_LIMIT_NONE = -1;

        /**
         * infinite load attempts
         */
        public static int RETRY_LIMIT_UNLIMITED = 0;

        public final int connectionTimeout;

        public final int readTimeout;

        public final int retryLimit;

        public final int retryDelay;

        public final boolean notifyRead;

        public final boolean notifyWrite;

        public final boolean logRequestData;

        public final boolean logResponseData;

        public LoadSettings(int connectionTimeout, int readTimeout, int retryLimit, int retryDelay, boolean notifyRead, boolean notifyWrite, boolean logRequestData, boolean logResponseData) {

            if (connectionTimeout < 0) {
                throw new IllegalArgumentException("incorrect connectionTimeout: " + connectionTimeout);
            }

            if (readTimeout < 0) {
                throw new IllegalArgumentException("incorrect readTimeout: " + readTimeout);
            }

            if (retryLimit < 0) {
                throw new IllegalArgumentException("incorrect retryLimit: " + readTimeout);
            }

            if (retryDelay < 0) {
                throw new IllegalArgumentException("incorrect retryDelay: " + readTimeout);
            }

            this.connectionTimeout = connectionTimeout;
            this.readTimeout = readTimeout;
            this.retryLimit = retryLimit;
            this.retryDelay = retryDelay;

            this.notifyRead = notifyRead;
            this.notifyWrite = notifyWrite;

            this.logRequestData = logRequestData;
            this.logResponseData = logResponseData;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (object == null || getClass() != object.getClass()) return false;

            LoadSettings that = (LoadSettings) object;

            if (connectionTimeout != that.connectionTimeout) return false;
            if (readTimeout != that.readTimeout) return false;
            if (retryLimit != that.retryLimit) return false;
            if (retryDelay != that.retryDelay) return false;
            if (notifyRead != that.notifyRead) return false;
            if (notifyWrite != that.notifyWrite) return false;
            if (logRequestData != that.logRequestData) return false;
            return logResponseData == that.logResponseData;

        }

        @Override
        public int hashCode() {
            int result = connectionTimeout;
            result = 31 * result + readTimeout;
            result = 31 * result + retryLimit;
            result = 31 * result + retryDelay;
            result = 31 * result + (notifyRead ? 1 : 0);
            result = 31 * result + (notifyWrite ? 1 : 0);
            result = 31 * result + (logRequestData ? 1 : 0);
            result = 31 * result + (logResponseData ? 1 : 0);
            return result;
        }

        @Override
        public String toString() {
            return "LoadSettings{" +
                    "connectionTimeout=" + connectionTimeout +
                    ", readTimeout=" + readTimeout +
                    ", retryLimit=" + retryLimit +
                    ", retryDelay=" + retryDelay +
                    ", notifyRead=" + notifyRead +
                    ", notifyWrite=" + notifyWrite +
                    ", logRequestData=" + logRequestData +
                    ", logResponseData=" + logResponseData +
                    '}';
        }
    }

    protected LoadRunnableInfo(int id, String name, @Nullable URL url, @NonNull LoadSettings settings) throws MalformedURLException {
        super(id, name);
        this.url = url != null ? new URL(url.toString()) : null;
        this.settings = settings;
    }

    @Nullable
    public final URL url;

    public boolean verifyUrl() {
        return url != null;
    }

    @NonNull
    public final LoadSettings settings;

    @Override
    public String toString() {
        return "LoadRunnableInfo{" +
                "url=" + url +
                ", settings=" + settings +
                '}';
    }

    public static class Field {

        @NonNull
        public final String name;

        @Nullable
        public final String value;

        public Field(@NonNull String name, @Nullable String value) {

            if (TextUtils.isEmpty(name)) {
                throw new IllegalArgumentException("name can't be empty");
            }

            this.name = name;
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Field field = (Field) o;

            if (!name.equals(field.name)) return false;
            return !(value != null ? !value.equals(field.value) : field.value != null);

        }

        @Override
        public int hashCode() {
            int result = name.hashCode();
            result = 31 * result + (value != null ? value.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "Field{" +
                    "name='" + name + '\'' +
                    ", value='" + value + '\'' +
                    '}';
        }
    }

}
