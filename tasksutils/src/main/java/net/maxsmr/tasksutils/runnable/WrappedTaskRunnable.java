package net.maxsmr.tasksutils.runnable;

import net.maxsmr.tasksutils.taskexecutor.RunnableInfo;
import net.maxsmr.tasksutils.taskexecutor.TaskRunnable;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class WrappedTaskRunnable<I extends RunnableInfo, ProgressInfo, Result, T extends TaskRunnable<I, ProgressInfo, Result>> extends WrappedRunnable<I, T> {

    public WrappedTaskRunnable(@NotNull T command) {
        super(command, null);
    }

    public WrappedTaskRunnable(@NotNull T command, @Nullable ExceptionHandler exceptionHandler) {
        super(command, exceptionHandler);
    }
}