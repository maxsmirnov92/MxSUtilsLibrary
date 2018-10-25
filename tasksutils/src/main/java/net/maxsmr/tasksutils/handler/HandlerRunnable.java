package net.maxsmr.tasksutils.handler;


import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import org.jetbrains.annotations.NotNull;

public abstract class HandlerRunnable<T> implements Runnable {

    @NotNull
    protected final Handler handler;

    @NotNull
    private volatile AsyncTask.Status status = AsyncTask.Status.PENDING;

    @NotNull
    public final AsyncTask.Status getStatus() {
        return status;
    }

    public HandlerRunnable() {
        this(new Handler(Looper.getMainLooper()));
    }

    public HandlerRunnable(@NotNull Handler handler) {
        this.handler = handler;
    }

    protected void preExecute() {

    }

    protected abstract T doWork();

    protected void postExecute(T result) {
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

        status = AsyncTask.Status.RUNNING;

        handler.post(new Runnable() {
            @Override
            public void run() {
                preExecute();
            }
        });

        final T result = doWork();

        handler.post(new Runnable() {
            @Override
            public void run() {
                postExecute(result);
            }
        });

        status = AsyncTask.Status.FINISHED;
    }


}
