package net.maxsmr.tasksutils.taskexecutor;

import android.os.AsyncTask;
import android.os.Handler;

import androidx.annotation.CallSuper;

import net.maxsmr.commonutils.Observable;
import net.maxsmr.commonutils.Predicate;
import net.maxsmr.commonutils.logger.BaseLogger;
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder;
import net.maxsmr.tasksutils.runnable.RunnableInfoRunnable;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static net.maxsmr.tasksutils.taskexecutor.RunnableInfo.Status.FINISHED;
import static net.maxsmr.tasksutils.taskexecutor.RunnableInfo.Status.PENDING;
import static net.maxsmr.tasksutils.taskexecutor.RunnableInfo.Status.RUNNING;


public abstract class TaskRunnable<I extends RunnableInfo, ProgressInfo, Result> extends RunnableInfoRunnable<I> {

    private static final BaseLogger logger = BaseLoggerHolder.getInstance().getLogger(TaskRunnable.class);

    public static final int RETRY_DISABLED = -1;
    public static final int RETRY_NO_LIMIT = 0;

    @NotNull
    protected final TaskObservable observable = new TaskObservable();

    @Nullable
    private Handler callbacksHandler = null;

    private int retryCount = 0;

    private Result result;

    protected TaskRunnable(@NotNull I rInfo) {
        super(rInfo);
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public <T extends TaskRunnable<I, ProgressInfo, Result>> T registerCallbacks(@NotNull Callbacks<I, ProgressInfo, Result, TaskRunnable<I, ProgressInfo, Result>> callbacks) {
        observable.registerObserver(callbacks);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public <T extends TaskRunnable<I, ProgressInfo, Result>> T unregisterCallbacks(@NotNull Callbacks<I, ProgressInfo, Result, TaskRunnable<I, ProgressInfo, Result>> callbacks) {
        observable.unregisterObserver(callbacks);
        return (T) this;
    }

    public int getId() {
        synchronized (rInfo) {
            return rInfo.id;
        }
    }

    public boolean isRunning() {
        return rInfo.isRunning();
    }

    public boolean isCanceled() {
        return rInfo.isCanceled();
    }

    public void cancel() {
        rInfo.cancel();
    }

    public int getRetryCount() {
        synchronized (rInfo) {
            return retryCount;
        }
    }

    public Result getResult() {
        synchronized (rInfo) {
            return result;
        }
    }

    @NotNull
    public RunnableInfo.Status getStatus() {
        synchronized (rInfo) {
            return rInfo.status;
        }
    }

    public boolean isFinished() {
        return rInfo.isFinished();
    }

    @Nullable
    public Handler getCallbacksHandler() {
        synchronized (rInfo) {
            return callbacksHandler;
        }
    }

    public void setCallbacksHandler(@Nullable Handler callbacksHandler) {
        synchronized (rInfo) {
            this.callbacksHandler = callbacksHandler;
        }
    }

    @Override
    public final void run() {

        if (rInfo.status != PENDING) {
            switch (rInfo.status) {
                case RUNNING:
                    throw new IllegalStateException("Cannot execute task:"
                            + " the task is already running.");
                case FINISHED:
                    throw new IllegalStateException("Cannot execute task:"
                            + " the task has already been executed "
                            + "(a task can be executed only once)");
                default:
                    throw new IllegalStateException("Unknown " + AsyncTask.Status.class.getSimpleName() + ": " + rInfo.status);
            }
        }

        result = null;

        onPreExecute();

        rInfo.status = RUNNING;

        result = null;
        try {
            result = doWork();
        } catch (Throwable e) {

            boolean reRun = shouldReRunOnThrowable(e);
            onTaskFailed(e, reRun);

            int limit = getRetryLimit();
            if (reRun) {
                retryCount++;

                long sleepTime = getRetryDelayInMs();
                if (sleepTime > 0) {
                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException e1) {
                        Thread.currentThread().interrupt();
                        logger.e("an InterruptedException occurred during sleep(): " + e.getMessage(), e);
                        cancel();
                    }
                }

                if (!isCanceled()) {
                    rInfo.status = PENDING;
                    run();
                    return;
                } else {
                    logger.w("Can't re-run task due to it's cancelled: " + toString());
                }
            }
        }
        rInfo.status = FINISHED;

        onPostExecute(result);
    }

    @CallSuper
    public void onPreExecute() {
        observable.notifyPreExecute();
    }

    // check isCancelled(), call publishProgress (if needed)
    @Nullable
    public abstract Result doWork() throws Throwable;

    @CallSuper
    public void onProgress(@NotNull ProgressInfo info) {
        observable.notifyProgress(info);
    }

    @CallSuper
    public void onPostExecute(@Nullable Result result) {
        if (!isCanceled()) {
            observable.notifyPostExecute(result);
        } else {
            onCancel();
        }
    }

    public void onCancel() {
        observable.notifyCancelled();
    }

    public final void publishProgress(@NotNull ProgressInfo info) {
        onProgress(info);
    }

    public int getRetryLimit() {
        return RETRY_DISABLED;
    }

    public long getRetryDelayInMs() {
        return 0;
    }

    // override it and remove limit
    public boolean shouldReRunOnThrowable(@NotNull Throwable e) {
        final int retryLimit = getRetryLimit();
        return retryLimit > 0 || retryLimit == RETRY_NO_LIMIT;
    }

    @CallSuper
    public void onTaskFailed(@NotNull Throwable e, boolean reRun) {
        logger.e("Task " + this + "  failed with exception: " + e.getMessage(), e);
        observable.notifyFailed(e, retryCount, getRetryLimit());
        if (!reRun) {
            throw new RuntimeException(e); // rethrow to Executor
        }
    }

    @NotNull
    @Override
    public String toString() {
        return "TaskRunnable{" +
                "callbacksHandler=" + callbacksHandler +
                ", rInfo=" + rInfo +
                ", retryCount=" + retryCount +
                '}';
    }

    @Nullable
    public static <I extends RunnableInfo, ProgressInfo, Result, T extends TaskRunnable<I, ProgressInfo, Result>> T findRunnableById(final int id, @Nullable Collection<T> from) {
        return Predicate.Methods.find(from, element -> element != null && element.getId() == id);
    }

    @NotNull
    public static <I extends RunnableInfo, ProgressInfo, Result, T extends TaskRunnable<I, ProgressInfo, Result>> List<T> filter(@Nullable final Collection<T> what, @Nullable final Collection<T> by, final boolean contains) {
        final List<I> resultInfos = RunnableInfo.filter(toRunnableInfos(what), toRunnableInfos(by), contains);
        return Predicate.Methods.filter(what, task -> RunnableInfo.findRunnableInfoById(task.getId(), resultInfos) != null);
    }

    public static <I extends RunnableInfo, ProgressInfo, Result, T extends TaskRunnable<I, ProgressInfo, Result>> List<T> filter(@Nullable Collection<T> what, boolean isCancelled) {
        return Predicate.Methods.filter(what, element -> element != null && element.isCanceled() == isCancelled);
    }

    @NotNull
    public static <I extends RunnableInfo> List<I> toRunnableInfos(@Nullable Collection<? extends TaskRunnable<I, ?, ?>> what) {
        List<I> result = new ArrayList<>();
        if (what != null) {
            for (TaskRunnable<I, ?, ?> t : what) {
                if (t != null) {
                    result.add(t.rInfo);
                }
            }
        }
        return result;
    }

    public static <I extends RunnableInfo> void setRunning(@Nullable Collection<? extends TaskRunnable<I, ?, ?>> what, boolean isRunning) {
        RunnableInfo.setRunning(toRunnableInfos(what), isRunning);
    }

    public static <I extends RunnableInfo> void cancel(@Nullable Collection<? extends TaskRunnable<I, ?, ?>> what) {
        RunnableInfo.cancel(toRunnableInfos(what));
    }

    public interface ITaskResultValidator<I extends RunnableInfo, ProgressInfo, Result, T extends TaskRunnable<I, ProgressInfo, Result>> {

        boolean needToReAddTask(T runnable, Throwable t);
    }

    public interface ITaskRestorer<I extends RunnableInfo, ProgressInfo, Result, T extends TaskRunnable<I, ProgressInfo, Result>> {

        List<T> fromRunnableInfos(@NotNull Collection<I> runnableInfos);
    }

    public interface Callbacks<I extends RunnableInfo, ProgressInfo, Result, T extends TaskRunnable<I, ProgressInfo, Result>> {

        void onPreExecute(@NotNull T task);

        void onProgress(@NotNull T task, @Nullable ProgressInfo progressInfo);

        void onPostExecute(@NotNull T task, @Nullable Result result);

        void onFailed(@NotNull T task, @NotNull Throwable e, int runCount, int maxRunCount);

        void onCancelled(@NotNull T task);
    }

    private class TaskObservable extends Observable<Callbacks<I, ProgressInfo, Result, TaskRunnable<I, ProgressInfo, Result>>> {

        private void notifyPreExecute() {
            synchronized (observers) {
                Runnable r = () -> {
                    synchronized (observers) {
                        for (Callbacks<I, ProgressInfo, Result, TaskRunnable<I, ProgressInfo, Result>> c : observers) {
                            c.onPreExecute(TaskRunnable.this);
                        }
                    }
                };
                run(r);
            }
        }

        private void notifyProgress(final ProgressInfo progressInfo) {
            synchronized (observers) {
                final Runnable r = () -> {
                    synchronized (observers) {
                        for (Callbacks<I, ProgressInfo, Result, TaskRunnable<I, ProgressInfo, Result>> c : observers) {
                            c.onProgress(TaskRunnable.this, progressInfo);
                        }
                    }
                };
                run(r);
            }
        }

        private void notifyPostExecute(final Result result) {
            synchronized (observers) {
                final Runnable r = () -> {
                    synchronized (observers) {
                        for (Callbacks<I, ProgressInfo, Result, TaskRunnable<I, ProgressInfo, Result>> c : observers) {
                            c.onPostExecute(TaskRunnable.this, result);
                        }
                    }
                };
                run(r);
            }
        }

        private void notifyFailed(@NotNull final Throwable e, final int runCount, final int maxRunCount) {
            synchronized (observers) {
                final Runnable r = () -> {
                    synchronized (observers) {
                        for (Callbacks<I, ProgressInfo, Result, TaskRunnable<I, ProgressInfo, Result>> c : observers) {
                            c.onFailed(TaskRunnable.this, e, runCount, maxRunCount);
                        }
                    }
                };
                run(r);
            }
        }

        private void notifyCancelled() {
            synchronized (observers) {
                final Runnable r = () -> {
                    synchronized (observers) {
                        for (Callbacks<I, ProgressInfo, Result, TaskRunnable<I, ProgressInfo, Result>> c : observers) {
                            c.onCancelled(TaskRunnable.this);
                        }
                    }
                };
                run(r);
            }
        }

        private void run(Runnable r) {
            if (r != null) {
                if (callbacksHandler == null) {
                    r.run();
                } else {
                    callbacksHandler.post(r);
                }
            }
        }
    }



}
