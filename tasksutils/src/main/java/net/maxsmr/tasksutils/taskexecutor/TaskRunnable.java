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

    public int getId() {
        return rInfo.id;
    }

    public boolean isRunning() {
        return rInfo.isRunning;
    }

    public boolean isCancelled() {
        return rInfo.isCancelled();
    }

    public void cancel() {
        rInfo.cancel();
    }

    @Override
    public String toString() {
        return "TaskRunnable [rInfo=" + rInfo + "]";
    }

    public interface ITaskResultValidator<I extends RunnableInfo, T extends TaskRunnable<I>> {

        boolean needToReAddTask(T runnable, Throwable t);
    }

    public interface ITaskRestorer<I extends RunnableInfo, T extends TaskRunnable<I>> {

        List<T> fromRunnableInfos(Collection<I> runnableInfos);
    }

    /** for seeing in LogCat */
    public static final class WrappedTaskRunnable<I extends RunnableInfo, T extends TaskRunnable<I>> implements Runnable {

        @NonNull
        final T command;

        public WrappedTaskRunnable(@NonNull T command) {
            this.command = command;
        }

        @Override
        public void run() {
            try {
                command.run();
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("an exception was occurred during run()", e);
            }
        }
    }

}
