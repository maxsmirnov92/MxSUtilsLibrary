package net.maxsmr.networkutils.loadutil.managers;

import net.maxsmr.networkutils.loadutil.managers.base.info.LoadRunnableInfo;
import net.maxsmr.tasksutils.taskexecutor.RunnableInfo;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface LoadListener<I extends LoadRunnableInfo> {

    long INTERVAL_NOT_SPECIFIED = 0;

    long DEFAULT_PROCESSING_NOTIFY_INTERVAL = 2000;

    enum STATE {

        UNKNOWN(-1), STARTING(0), CONNECTING(1), CONNECTED(2),
        UPLOADING(3), DOWNLOADING(4),
        CANCELLED(5),
        FAILED_FILE_REASON(6), FAILED(7), FAILED_RETRIES_EXCEEDED(8),
        SUCCESS(9);

        private final int value;

        STATE(int value) {
            this.value = value;
        }

        public final int getValue() {
            return value;
        }

        public boolean isRunning() {
            return this == STARTING || this == CONNECTING || this == UPLOADING || this == DOWNLOADING;
        }

        public boolean isFailed() {
            return this == FAILED_FILE_REASON || this == FAILED || this == FAILED_RETRIES_EXCEEDED;
        }

        @NotNull
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
    int getId(@NotNull I loadInfo);

    long getProcessingNotifyInterval(@NotNull I loadInfo);

    void onUpdateState(@NotNull I loadInfo, @NotNull NetworkLoadManager.LoadProcessInfo loadProcessInfo, @Nullable Throwable t);

    void onResponse(@NotNull I loadInfo, @NotNull NetworkLoadManager.LoadProcessInfo loadProcessInfo, @NotNull NetworkLoadManager.Response response);
}
