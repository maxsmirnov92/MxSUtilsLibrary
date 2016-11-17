package net.maxsmr.tasksutils.taskexecutor;

import android.support.annotation.NonNull;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class ExecInfo implements Serializable {

    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    public final TaskRunnable<?> taskRunnable;

    private long timeWaitingInQueue;

    private long timeExecuting;

    private long timeWhenAddedToQueue;

    private long timeWhenStarted;

    public ExecInfo(TaskRunnable<?> taskRunnable) {
        this.taskRunnable = taskRunnable;
    }

    public long getTimeWaitingInQueue() {
        return timeWaitingInQueue;
    }

    public String getTimeWhenAddedToQueueFormatted() {
        return SDF.format(new Date(timeWhenAddedToQueue));
    }

    public long getTimeExecuting() {
        return timeExecuting;
    }

    public String getTimeWhenStartedFormatted() {
        return SDF.format(new Date(timeWhenStarted));
    }

    public long getTotalTime() {
        return getTimeExecuting() + getTimeWaitingInQueue();
    }

    @NonNull
    ExecInfo setTimeWhenAddedToQueue(long timeWhenAddedToQueue) {
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

    @NonNull
    ExecInfo setTimeWhenStarted(long timeWhenStarted) {
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

    @NonNull
    ExecInfo finishedWaitingInQueue(long when) {
        if (timeWaitingInQueue > 0) {
            throw new IllegalStateException("timeWaitingInQueue is already calculated");
        }
        if (timeWhenAddedToQueue == 0) {
            throw new IllegalStateException("specify timeWhenAddedToQueue first");
        }
        if (when < timeWhenAddedToQueue) {
            throw new IllegalArgumentException("incorrect when: " + when + " < timeWhenAddedToQueue: " + timeWhenAddedToQueue);
        }
        timeWaitingInQueue = when - timeWhenAddedToQueue;
        setTimeWhenStarted(when);
        return this;
    }

    @NonNull
    ExecInfo finishedExecution(long when) {
        if (timeExecuting > 0) {
            throw new IllegalStateException("timeExecuting is already calculated");
        }
        if (timeWhenStarted == 0) {
            throw new IllegalStateException("specify timeWhenStarted first");
        }
        if (when < timeWhenStarted) {
            throw new IllegalArgumentException("incorrect when: " + when + " < timeWhenStarted: " + timeWhenStarted);
        }
        timeExecuting = when - timeWhenStarted;
        return this;
    }

    @NonNull
    ExecInfo reset() {
        timeWaitingInQueue = 0;
        timeExecuting = 0;
        timeWhenAddedToQueue = 0;
        timeWhenStarted = 0;
        return this;
    }

    @Override
    public String toString() {
        return "ExecInfo{" +
                "taskRunnable=" + taskRunnable +
                ", timeWaitingInQueue=" + timeWaitingInQueue +
                ", timeExecuting=" + timeExecuting +
                ", timeWhenAddedToQueue=" + timeWhenAddedToQueue +
                ", timeWhenStarted=" + timeWhenStarted +
                '}';
    }
}
