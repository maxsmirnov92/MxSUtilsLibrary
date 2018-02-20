package net.maxsmr.networkutils.loadutil.managers;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import net.maxsmr.networkutils.loadutil.managers.base.info.LoadRunnableInfo;
import net.maxsmr.tasksutils.taskexecutor.RunnableInfo;

public interface LoadListener<I extends LoadRunnableInfo> {

    long INTERVAL_NOT_SPECIFIED = 0;

    long DEFAULT_PROCESSING_NOTIFY_INTERVAL = 2000;

    enum STATE {

        UNKNOWN(-1), STARTING(0), CONNECTING(1), UPLOADING(2), DOWNLOADING(3), CANCELLED(4), FAILED_FILE_REASON(5), FAILED(6), FAILED_RETRIES_EXCEEDED(7), SUCCESS(8);

        private final int value;

        STATE(int value) {
            this.value = value;
        }

        public final int getValue() {
            return value;
        }

        public static boolean isRunning(@NonNull STATE state) {
            return state == STARTING || state == CONNECTING || state == UPLOADING || state == DOWNLOADING;
        }

        @NonNull
        public static STATE fromValue(int value) {
            for (STATE state : values()) {
                if (state.value == value)
                    return state;
            }
            throw new IllegalArgumentException("Incorrect value " + value + " for enum type " + STATE.class.getName());
        }
    }

    void onLoadAddedToQueue(int id, int waitingLoads, int activeLoads);

    void onLoadRemovedFromQueue(int id, int waitingLoads, int activeLoads);


    /** @return personal id */
    int getId();

    /** @return id for notify. if {@link RunnableInfo#NO_ID} - notify for all */
    int getId(@NonNull I loadInfo);

    long getProcessingNotifyInterval(@NonNull I loadInfo);

    void onUpdateState(@NonNull I loadInfo, @NonNull NetworkLoadManager.LoadProcessInfo loadProcessInfo, @Nullable Throwable t);

    void onResponse(@NonNull I loadInfo, @NonNull NetworkLoadManager.LoadProcessInfo loadProcessInfo, @NonNull NetworkLoadManager.Response response);
}
