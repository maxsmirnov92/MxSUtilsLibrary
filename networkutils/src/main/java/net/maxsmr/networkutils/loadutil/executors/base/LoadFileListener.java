package net.maxsmr.networkutils.loadutil.executors.base;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.List;

import net.maxsmr.tasksutils.taskrunnable.RunnableInfo;

public interface LoadFileListener<I extends LoadRunnableInfo> {

    long INTERVAL_NOT_SPECIFIED = 0;

    long DEFAULT_PROCESSING_NOTIFY_INTERVAL = 2000;

    enum STATE {

        STARTING(0), PROCESSING(1), CANCELLED(2), FAILED_NOT_STARTED(3), FAILED(4), FAILED_RETRIES_EXCEEDED(5), SUCCESS(6);

        private final int value;

        STATE(int value) {
            this.value = value;
        }

        public final int getValue() {
            return value;
        }

        public static STATE fromValue(int value) {
            for (STATE state : values()) {
                if (state.value == value)
                    return state;
            }
            throw new IllegalArgumentException("Incorrect value " + value + " for enum type " + STATE.class.getName());
        }
    }

    /** @return {@link RunnableInfo#NO_ID} - notify for all */
    int getId(@NonNull I loadInfo);

    long getProcessingNotifyInterval(@NonNull I loadInfo);

    void onUpdateState(@NonNull STATE state, @NonNull I loadInfo, long estimatedTime, float currentKBytes, float totalKBytes, @Nullable Throwable t);

    void onActiveLoadsCountChanged(int activeDownloads);

    void onResponseCode(@NonNull ResponseStatus status, @NonNull I loadInfo, int code, String responseMessage);

    void onResponseHeaders(@NonNull ResponseStatus status, @NonNull I loadInfo, @NonNull List<LoadRunnableInfo.Field> fields);

    /**
     * must be called after {@link #onResponseCode(ResponseStatus, LoadRunnableInfo, int, String)} and {@link #onResponseHeaders(ResponseStatus, LoadRunnableInfo, List)} ]}
     */
    void onResponseBody(@NonNull ResponseStatus status, @NonNull I loadInfo, @NonNull List<String> response);

    enum ResponseStatus {
        ACCEPTED, DECLINED
    }
}
