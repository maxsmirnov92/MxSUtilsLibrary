package net.maxsmr.tasksutils.runnable;

import android.os.Handler;
import android.os.Looper;

import net.maxsmr.commonutils.logger.BaseLogger;
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder;
import net.maxsmr.tasksutils.taskexecutor.RunnableInfo;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WrappedRunnable<I extends RunnableInfo, R extends RunnableInfoRunnable<I>> implements Runnable {

    private static final BaseLogger logger = BaseLoggerHolder.getInstance().getLogger(WrappedRunnable.class);

    @NotNull
    public final R command;

    @Nullable
    private ExceptionHandler exceptionHandler;

    public WrappedRunnable(@NotNull R command) {
        this(command, null);
    }

    public WrappedRunnable(@NotNull R command, @Nullable ExceptionHandler exceptionHandler) {
        this.command = command;
        this.exceptionHandler = exceptionHandler;
    }

    @Override
    public void run() {
        try {
            command.run();
        } catch (Throwable e) {
            final String message = "An Exception occurred during run(): " + e.getMessage();
            logger.e(message, e);
            synchronized (command) {
                if (exceptionHandler != null) {
                    exceptionHandler.onRunnableCrash(e);
                } else {
                    throw new RuntimeException(message, e);
                }
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WrappedRunnable)) return false;

        WrappedRunnable<?, ?> that = (WrappedRunnable<?, ?>) o;

        if (!command.equals(that.command)) return false;
        return exceptionHandler != null ? exceptionHandler.equals(that.exceptionHandler) : that.exceptionHandler == null;
    }

    @Override
    public int hashCode() {
        int result = command.hashCode();
        result = 31 * result + (exceptionHandler != null ? exceptionHandler.hashCode() : 0);
        return result;
    }

    public interface ExceptionHandler {

        ExceptionHandler STUB = e -> {
        };

        void onRunnableCrash(Throwable e);

        class MainExceptionHandler implements ExceptionHandler {

            private final Handler handler = new Handler(Looper.getMainLooper());

            @Override
            public void onRunnableCrash(final Throwable e) {
                handler.post(() -> {
                    throw new RuntimeException(e);
                });
            }
        }
    }
}