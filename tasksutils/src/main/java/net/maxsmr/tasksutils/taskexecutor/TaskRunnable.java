package net.maxsmr.tasksutils.taskexecutor;

import android.support.annotation.NonNull;

import java.util.Collection;
import java.util.List;

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

    public interface ITaskResultValidator<I extends RunnableInfo, T extends TaskRunnable<I>> {

        boolean needToReAddTask(TaskRunnable<I> runnable);
    }

    public interface ITaskRestorer<I extends RunnableInfo, T extends TaskRunnable<I>> {

        List<T> fromRunnableInfos(Collection<I> runnableInfos);
    }

}
