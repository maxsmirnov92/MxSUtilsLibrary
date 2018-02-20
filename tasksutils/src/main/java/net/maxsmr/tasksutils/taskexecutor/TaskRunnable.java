package net.maxsmr.tasksutils.taskexecutor;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import net.maxsmr.commonutils.data.Predicate;

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

    @Nullable
    public static <I extends RunnableInfo, T extends TaskRunnable<I>> T findRunnableById(final int id, Collection<T> tasks) {
        if (id < 0) {
            throw new IllegalArgumentException("incorrect id: " + id);
        }
        return Predicate.Methods.find(tasks, new Predicate<T>() {
            @Override
            public boolean apply(T element) {
                return element != null && id == element.getId();
            }
        });
    }


    @NonNull
    public static <I extends RunnableInfo, T extends TaskRunnable<I>> List<T> filter(final Collection<T> what, final Collection<T> by, final boolean contains) {
        return Predicate.Methods.filter(what, new Predicate<T>() {
            @Override
            public boolean apply(T element) {
                T runnable = element != null? findRunnableById(element.getId(), by) : null;
                return contains && runnable != null || !contains && runnable == null;
            }
        });
    }

    public static void setRunning(@Nullable Collection<? extends TaskRunnable<?>> tasks, boolean isRunning) {
        if (tasks != null) {
            for (TaskRunnable<?> t : tasks) {
                if (t != null) {
                    t.rInfo.isRunning = isRunning;
                }
            }
        }
    }

    public static void cancel(@Nullable Collection<? extends TaskRunnable<?>> tasks) {
        if (tasks != null) {
            for (TaskRunnable<?> t : tasks) {
                if (t != null) {
                    t.cancel();
                }
            }
        }
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
