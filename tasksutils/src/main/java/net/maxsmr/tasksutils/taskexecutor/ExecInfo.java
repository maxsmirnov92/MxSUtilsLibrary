package net.maxsmr.tasksutils.taskexecutor;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static net.maxsmr.commonutils.data.conversion.format.DateFormatUtilsKt.formatDateNoThrow;

public class ExecInfo<I extends RunnableInfo, ProgressInfo, Result, T extends TaskRunnable<I, ProgressInfo, Result>> implements Serializable {

    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());

    @NotNull
    public final T taskRunnable;

    private long timeWaitingInQueue;

    private long timeExecuting;

    private long timeWhenAddedToQueue;

    private long timeWhenStarted;

    @Nullable
    private Throwable execException;

    public ExecInfo(@NotNull T taskRunnable) {
        this.taskRunnable = taskRunnable;
    }

    public ExecInfo(@NotNull ExecInfo<I, ProgressInfo, Result, T> execInfo) {
        this.taskRunnable = execInfo.taskRunnable;
        this.timeWaitingInQueue = execInfo.timeWaitingInQueue;
        timeExecuting = execInfo.timeExecuting;
        timeWhenAddedToQueue = execInfo.timeWhenAddedToQueue;
        timeWhenStarted = execInfo.timeWhenStarted;
    }

    synchronized public long getTimeWaitingInQueue() {
        return timeWaitingInQueue;
    }

    synchronized public String getTimeWhenAddedToQueueFormatted() {
        return formatDateNoThrow(new Date(timeWhenAddedToQueue), SDF, null);
    }

    synchronized public long getTimeExecuting() {
        return timeExecuting;
    }

    synchronized public String getTimeWhenStartedFormatted() {
        return formatDateNoThrow(new Date(timeWhenStarted), SDF, null);
    }

    synchronized public long getTotalTime() {
        return getTimeExecuting() + getTimeWaitingInQueue();
    }

    @Nullable
    synchronized public Throwable getExecException() {
        return execException;
    }

    @NotNull
    synchronized ExecInfo<I, ProgressInfo, Result, T> setTimeWhenAddedToQueue(long timeWhenAddedToQueue) {
        if (this.timeWhenAddedToQueue > 0) {
            throw new IllegalStateException("timeWhenAddedToQueue is already specified");
        }
        if (timeWaitingInQueue > 0) {
            throw new IllegalStateException("timeWaitingInQueue is already calculated");
        }
        if (timeWhenAddedToQueue < 0) {
            throw new IllegalArgumentException("incorrect timeWhenAddedToQueue: " + timeWhenAddedToQueue);
        }
        this.timeWhenAddedToQueue = timeWhenAddedToQueue;
        return this;
    }

    @NotNull
    synchronized ExecInfo<I, ProgressInfo, Result, T> setTimeWhenStarted(long timeWhenStarted) {
        if (this.timeWhenStarted > 0) {
            throw new IllegalStateException("timeWhenStarted is already specified");
        }
        if (timeExecuting > 0) {
            throw new IllegalStateException("timeExecuting is already calculated");
        }
        if (timeWhenAddedToQueue == 0) {
            throw new IllegalStateException("specify timeWhenAddedToQueue first");
        }
        if (timeWhenStarted < timeWhenAddedToQueue) {
            throw new IllegalArgumentException("incorrect timeWhenStarted: " + timeWhenStarted + " < timeWhenAddedToQueue: " + timeWhenAddedToQueue);
        }
        this.timeWhenStarted = timeWhenStarted;
        return this;
    }

    @NotNull
    synchronized ExecInfo<I, ProgressInfo, Result, T> finishedWaitingInQueue(long when) {
        if (timeWaitingInQueue > 0) {
            throw new IllegalStateException("timeWaitingInQueue is already calculated");
        }
        if (timeWhenAddedToQueue == 0) {
            throw new IllegalStateException("specify timeWhenAddedToQueue first");
        }
        if (when < timeWhenAddedToQueue) {
            throw new IllegalArgumentException("incorrect time waiting in queue: " + when + " < timeWhenAddedToQueue: " + timeWhenAddedToQueue);
        }
        timeWaitingInQueue = when - timeWhenAddedToQueue;
        setTimeWhenStarted(when);
        return this;
    }

    @NotNull
    synchronized ExecInfo<I, ProgressInfo, Result, T> finishedExecution(long when, @Nullable Throwable execException) {
        if (timeExecuting > 0) {
            throw new IllegalStateException("timeExecuting is already calculated");
        }
        if (timeWhenStarted == 0) {
            throw new IllegalStateException("specify timeWhenStarted first");
        }
        if (when < timeWhenStarted) {
            throw new IllegalArgumentException("incorrect execution time: " + when + " < timeWhenStarted: " + timeWhenStarted);
        }
        timeExecuting = when - timeWhenStarted;
        this.execException = execException;
        return this;
    }

    @NotNull
    synchronized ExecInfo<I, ProgressInfo, Result, T> reset() {
        timeWaitingInQueue = 0;
        timeExecuting = 0;
        timeWhenAddedToQueue = 0;
        timeWhenStarted = 0;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ExecInfo<?, ?, ?, ?> execInfo = (ExecInfo<?, ?, ?, ?>) o;

        if (timeWaitingInQueue != execInfo.timeWaitingInQueue) return false;
        if (timeExecuting != execInfo.timeExecuting) return false;
        if (timeWhenAddedToQueue != execInfo.timeWhenAddedToQueue) return false;
        if (timeWhenStarted != execInfo.timeWhenStarted) return false;
        if (!taskRunnable.equals(execInfo.taskRunnable)) return false;
        return execException != null ? execException.equals(execInfo.execException) : execInfo.execException == null;
    }

    @Override
    public int hashCode() {
        int result = taskRunnable.hashCode();
        result = 31 * result + (int) (timeWaitingInQueue ^ (timeWaitingInQueue >>> 32));
        result = 31 * result + (int) (timeExecuting ^ (timeExecuting >>> 32));
        result = 31 * result + (int) (timeWhenAddedToQueue ^ (timeWhenAddedToQueue >>> 32));
        result = 31 * result + (int) (timeWhenStarted ^ (timeWhenStarted >>> 32));
        result = 31 * result + (execException != null ? execException.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ExecInfo{" +
                "taskRunnable=" + taskRunnable +
                ", time waiting in queue: " + timeWaitingInQueue + " ms" +
                ", time executing: " + timeExecuting + " ms" +
                ", time when added to queue: " + SDF.format(new Date(timeWhenAddedToQueue)) +
                ", time when started: " + SDF.format(new Date(timeWhenStarted)) +
                ", exec exception: " + execException +
                '}';
    }
}
