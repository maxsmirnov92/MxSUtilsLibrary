package net.maxsmr.tasksutils.taskexecutor;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
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
        return rInfo.isRunning();
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

    // TODO change by Predicate<>
    @Nullable
    public static <I extends RunnableInfo, T extends TaskRunnable<I>> T findRunnableById(int id, Collection<T> tasks) {
        if (id < 0) {
            throw new IllegalArgumentException("incorrect id: " + id);
        }
        T targetRunnable = null;
        for (T r : tasks) {
            if (id == r.getId()) {
                targetRunnable = r;
                break;
            }
        }
        return targetRunnable;
    }


    @NonNull
    public static <I extends RunnableInfo, T extends TaskRunnable<I>> List<T> filter(Collection<T> what, Collection<T> by, boolean contains) {
        List<T> tasks = new ArrayList<>();
        if (what != null) {
            for (T t : what) {
                if (t != null) {
                    T runnable = findRunnableById(t.getId(), by);
                    if (contains && runnable != null || !contains && runnable == null) {
                        tasks.add(t);
                    }
                }
            }
        }
        return tasks;
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

        @Override
        public String toString() {
            return "WrappedTaskRunnable{" +
                    "command=" + command +
                    '}';
        }
    }

}
