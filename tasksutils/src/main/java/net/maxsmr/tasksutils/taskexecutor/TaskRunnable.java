package net.maxsmr.tasksutils.taskexecutor;

import android.os.AsyncTask;
import android.os.Handler;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import net.maxsmr.commonutils.data.Observable;
import net.maxsmr.commonutils.data.Predicate;
import net.maxsmr.commonutils.logger.BaseLogger;
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static android.os.AsyncTask.Status.FINISHED;
import static android.os.AsyncTask.Status.PENDING;
import static android.os.AsyncTask.Status.RUNNING;

public abstract class TaskRunnable<I extends RunnableInfo, ProgressInfo, Result> implements Runnable {

    private static final BaseLogger logger = BaseLoggerHolder.getInstance().getLogger(TaskRunnable.class);

    public static final int RETRY_DISABLED = -1;
    public static final int RETRY_NO_LIMIT = 0;

    private final Object lock = new Object();

    @NonNull
    protected final TaskObservable observable = new TaskObservable();

    @Nullable
    private Handler callbacksHandler = null;

    @NonNull
    public final I rInfo;

    private int retryCount = 0;

    @NonNull
    volatile AsyncTask.Status status = PENDING;

    public TaskRunnable(@NonNull I rInfo) {
        this.rInfo = rInfo;
    }

    @SuppressWarnings("unchecked")
    @NonNull
    public <T extends TaskRunnable<I, ProgressInfo, Result>> T registerCallbacks(@NonNull Callbacks<I, ProgressInfo, Result, TaskRunnable<I, ProgressInfo, Result>> callbacks) {
        observable.registerObserver(callbacks);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    @NonNull
    public <T extends TaskRunnable<I, ProgressInfo, Result>> T unregisterCallbacks(@NonNull Callbacks<I, ProgressInfo, Result, TaskRunnable<I, ProgressInfo, Result>> callbacks) {
        observable.unregisterObserver(callbacks);
        return (T) this;
    }

    public int getId() {
        return rInfo.id;
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
        return retryCount;
    }

    @NonNull
    public AsyncTask.Status getStatus() {
        return status;
    }

    public boolean isFinished() {
        return status == FINISHED;
    }

    @Nullable
    public Handler getCallbacksHandler() {
        synchronized (lock) {
            return callbacksHandler;
        }
    }

    public void setCallbacksHandler(@Nullable Handler callbacksHandler) {
        synchronized (lock) {
            this.callbacksHandler = callbacksHandler;
        }
    }

    @Override
    public final void run() {

        if (status != AsyncTask.Status.PENDING) {
            switch (status) {
                case RUNNING:
                    throw new IllegalStateException("Cannot execute task:"
                            + " the task is already running.");
                case FINISHED:
                    throw new IllegalStateException("Cannot execute task:"
                            + " the task has already been executed "
                            + "(a task can be executed only once)");
                default:
                    throw new IllegalStateException("Unknown " + AsyncTask.Status.class.getSimpleName() + ": " + status);
            }
        }

        onPreExecute();

        status = RUNNING;

        Result result = null;
        try {
            result = doWork();
        } catch (Throwable e) {

            boolean reRun = shouldReRunOnThrowable(e);
            onTaskFailed(e, reRun);

            int limit = getRetryLimit();
            if (reRun && (limit > 0 && retryCount < limit || limit == RETRY_NO_LIMIT)) {
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
                    status = PENDING;
                    run();
                } else {
                    logger.w("Can't re-run task due to it's cancelled: " + toString());
                }
            }
        }
        status = FINISHED;

        onPostExecute(result);
    }

    @CallSuper
    protected void onPreExecute() {
        observable.notifyPreExecute();
    }

    // check isCancelled(), call publishProgress (if needed)
    @Nullable
    protected abstract Result doWork() throws Throwable;

    @CallSuper
    protected void onProgress(@NonNull ProgressInfo info) {
        observable.notifyProgress(info);
    }

    @CallSuper
    protected void onPostExecute(@Nullable Result result) {
        if (!isCanceled()) {
            observable.notifyPostExecute(result);
        } else {
            onCancel();
        }
    }

    protected void onCancel() {
        observable.notifyCancelled();
    }

    protected final void publishProgress(@NonNull ProgressInfo info) {
        onProgress(info);
    }

    public int getRetryLimit() {
        return RETRY_DISABLED;
    }

    public long getRetryDelayInMs() {
        return 0;
    }

    // override it and remove limit
    protected boolean shouldReRunOnThrowable(@NonNull Throwable e) {
        final int retryLimit = getRetryLimit();
        return retryLimit > 0 || retryLimit == RETRY_NO_LIMIT;
    }

    @CallSuper
    protected void onTaskFailed(@NonNull Throwable e, boolean reRun) {
        logger.e("Task " + this + "  failed with exception: " + e.getMessage(), e);
        observable.notifyFailed(e, retryCount, getRetryLimit());
        if (!reRun) {
            throw new RuntimeException(e); // rethrow to Executor
        }
    }

    @Override
    public String toString() {
        return "TaskRunnable{" +
                "callbacksHandler=" + callbacksHandler +
                ", rInfo=" + rInfo +
                ", retryCount=" + retryCount +
                ", status=" + status +
                '}';
    }

    @Nullable
    public static <I extends RunnableInfo, ProgressInfo, Result, T extends TaskRunnable<I, ProgressInfo, Result>> T findRunnableById(final int id, Collection<T> from) {
        final I resultInfo = RunnableInfo.findRunnableInfoById(id, toRunnableInfos(from));
        return resultInfo != null ? Predicate.Methods.find(from, new Predicate<T>() {
            @Override
            public boolean apply(T element) {
                return element != null && element.getId() == id;
            }
        }) : null;
    }


    @NonNull
    public static <I extends RunnableInfo, ProgressInfo, Result, T extends TaskRunnable<I, ProgressInfo, Result>> List<T> filter(final Collection<T> what, final Collection<T> by, final boolean contains) {

        final List<I> resultInfos = RunnableInfo.filter(toRunnableInfos(what), toRunnableInfos(by), contains);

        return Predicate.Methods.filter(what, new Predicate<T>() {
            @Override
            public boolean apply(final T task) {
                return Predicate.Methods.find(resultInfos, new Predicate<I>() {
                    @Override
                    public boolean apply(I info) {
                        return info != null && info.id == task.getId();
                    }
                }) != null;
            }
        });
    }

    @NonNull
    public static <I extends RunnableInfo, ProgressInfo, Result, T extends TaskRunnable<I, ProgressInfo, Result>> List<I> toRunnableInfos(Collection<T> what) {
        List<I> result = new ArrayList<>();
        if (what != null) {
            for (TaskRunnable<I, ProgressInfo, Result> t : what) {
                if (t != null) {
                    result.add(t.rInfo);
                }
            }
        }
        return result;
    }

    public static void setRunning(@Nullable Collection<? extends TaskRunnable<?, ?, ?>> tasks, boolean isRunning) {
        if (tasks != null) {
            for (TaskRunnable<?, ?, ?> t : tasks) {
                if (t != null) {
                    t.rInfo.isRunning = isRunning;
                }
            }
        }
    }

    public static void cancel(@Nullable Collection<? extends TaskRunnable<?, ?, ?>> tasks) {
        if (tasks != null) {
            for (TaskRunnable<?, ? ,?> t : tasks) {
                if (t != null) {
                    t.cancel();
                }
            }
        }
    }

    public interface ITaskResultValidator<I extends RunnableInfo, ProgressInfo, Result, T extends TaskRunnable<I, ProgressInfo, Result>> {

        boolean needToReAddTask(T runnable, Throwable t);

        class Stub<I extends RunnableInfo, ProgressInfo, Result, T extends TaskRunnable<I, ProgressInfo, Result>> implements ITaskResultValidator<I, ProgressInfo, Result, T> {

            @Override
            public boolean needToReAddTask(T runnable, Throwable t) {
                return false;
            }
        }
    }

    public interface ITaskRestorer<I extends RunnableInfo, ProgressInfo, Result, T extends TaskRunnable<I, ProgressInfo, Result>> {

        List<T> fromRunnableInfos(Collection<I> runnableInfos);
    }

    public interface Callbacks<I extends RunnableInfo, ProgressInfo, Result, T extends TaskRunnable<I, ProgressInfo, Result>> {

        void onPreExecute(@NonNull T task);

        void onProgress(@NonNull T task, @Nullable ProgressInfo progressInfo);

        void onPostExecute(@NonNull T task, @Nullable Result result);

        void onFailed(@NonNull T task, @NonNull Throwable e, int runCount, int maxRunCount);

        void onCancelled(@NonNull T task);
    }

    private class TaskObservable extends Observable<Callbacks<I, ProgressInfo, Result, TaskRunnable<I, ProgressInfo, Result>>> {

        private void notifyPreExecute() {
            synchronized (lock) {
                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        synchronized (observers) {
                            for (Callbacks<I, ProgressInfo, Result, TaskRunnable<I, ProgressInfo, Result>> c : observers) {
                                c.onPreExecute(TaskRunnable.this);
                            }
                        }
                    }
                };
                if (callbacksHandler == null) {
                    r.run();
                } else {
                    callbacksHandler.post(r);
                }
            }
        }

        private void notifyProgress(final ProgressInfo progressInfo) {
            synchronized (lock) {
                final Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        synchronized (observers) {
                            for (Callbacks<I, ProgressInfo, Result, TaskRunnable<I, ProgressInfo, Result>> c : observers) {
                                c.onProgress(TaskRunnable.this, progressInfo);
                            }
                        }
                    }
                };
                if (callbacksHandler == null) {
                    r.run();
                } else {
                    callbacksHandler.post(r);
                }
            }
        }

        private void notifyPostExecute(final Result result) {
            synchronized (lock) {
                final Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        synchronized (observers) {
                            for (Callbacks<I, ProgressInfo, Result, TaskRunnable<I, ProgressInfo, Result>> c : observers) {
                                c.onPostExecute(TaskRunnable.this, result);
                            }
                        }
                    }
                };
                if (callbacksHandler == null) {
                    r.run();
                } else {
                    callbacksHandler.post(r);
                }
            }
        }

        private void notifyFailed(@NonNull final Throwable e, final int runCount, final int maxRunCount) {
            synchronized (lock) {
                final Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        synchronized (observers) {
                            for (Callbacks<I, ProgressInfo, Result, TaskRunnable<I, ProgressInfo, Result>> c : observers) {
                                c.onFailed(TaskRunnable.this, e, runCount, maxRunCount);
                            }
                        }
                    }
                };
                if (callbacksHandler == null) {
                    r.run();
                } else {
                    callbacksHandler.post(r);
                }
            }
        }

        private void notifyCancelled() {
            synchronized (lock) {
                final Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        synchronized (observers) {
                            for (Callbacks<I, ProgressInfo, Result, TaskRunnable<I, ProgressInfo, Result>> c : observers) {
                                c.onCancelled(TaskRunnable.this);
                            }
                        }
                    }
                };
                if (callbacksHandler == null) {
                    r.run();
                } else {
                    callbacksHandler.post(r);
                }
            }
        }
    }

    public static final class WrappedTaskRunnable<I extends RunnableInfo, ProgressInfo, Result, T extends TaskRunnable<I, ProgressInfo, Result>> implements Runnable {

        @NonNull
        final T command;

        public WrappedTaskRunnable(@NonNull T command) {
            this.command = command;
        }

        @Override
        public void run() {
            try {
                command.run();
            } catch (Throwable e) {
                logger.e("an Exception occurred during run(): " + e.getMessage(), e);
                throw new RuntimeException("an Exception occurred during run()", e);
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
