package net.maxsmr.tasksutils.handler;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.CallSuper;

import net.maxsmr.commonutils.android.gui.progressable.Progressable;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class HandlerProgressableRunnable<T> extends HandlerRunnable<T> {

    @Nullable
    protected final Progressable progressable;

    public HandlerProgressableRunnable(@Nullable Progressable progressable) {
        this(progressable, new Handler(Looper.getMainLooper()));
    }

    public HandlerProgressableRunnable(@Nullable Progressable progressable, @NotNull Handler handler) {
        super(handler);
        this.progressable = progressable;
        if (getStatus() == AsyncTask.Status.RUNNING) {
            preExecute();
        }
    }

    @Override
    @CallSuper
    protected void preExecute() {
        super.preExecute();
        if (isAlive()) {
            if (progressable != null) {
                progressable.onStart();
            }
        }
    }

    @Override
    @CallSuper
    protected void postExecute(T result) {
        super.postExecute(result);
        if (isAlive()) {
            if (progressable != null) {
                progressable.onStop();
            }
        }
    }

    public abstract boolean isAlive();
}
