package net.maxsmr.tasksutils.taskexecutor;

import android.support.annotation.NonNull;

public abstract class TaskRunnable<I extends RunnableInfo> implements Runnable {

    @NonNull
    public final I rInfo;

    public TaskRunnable(@NonNull I rInfo) {
        this.rInfo = rInfo;
    }

    @Override
    public String toString() {
        return "TaskRunnable [rInfo=" + rInfo + "]";
    }

}
