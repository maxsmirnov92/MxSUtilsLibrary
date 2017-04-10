package net.maxsmr.tasksutils.handler;


import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

public abstract class HandlerRunnable<T> implements Runnable {

    @NonNull
    protected final Handler handler;

    @NonNull
    private volatile AsyncTask.Status mStatus = AsyncTask.Status.PENDING;

    @NonNull
    public final AsyncTask.Status getStatus() {
        return mStatus;
    }

    public HandlerRunnable() {
        this(new Handler(Looper.getMainLooper()));
    }

    public HandlerRunnable(@NonNull Handler handler) {
        this.handler = handler;
    }

    protected void preExecute() {

    }

    protected abstract T doWork();

    protected void postExecute(T result) {
    }

    @Override
    public final void run() {

        if (mStatus != AsyncTask.Status.PENDING) {
            switch (mStatus) {
                case RUNNING:
                    throw new IllegalStateException("Cannot execute task:"
                            + " the task is already running.");
                case FINISHED:
                    throw new IllegalStateException("Cannot execute task:"
                            + " the task has already been executed "
                            + "(a task can be executed only once)");
            }
        }

        mStatus = AsyncTask.Status.RUNNING;

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

        mStatus = AsyncTask.Status.FINISHED;
    }


}
