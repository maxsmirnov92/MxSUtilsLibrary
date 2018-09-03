package net.maxsmr.tasksutils;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import net.maxsmr.commonutils.logger.BaseLogger;
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class ScheduledThreadPoolExecutorManager {

    private static final BaseLogger logger = BaseLoggerHolder.getInstance().getLogger(ScheduledThreadPoolExecutorManager.class);

    private final Object lock = new Object();

    private final List<Runnable> runnableList = new ArrayList<>();

    private final List<ScheduledFuture<?>> currentScheduledFutures = new ArrayList<>();

    @NonNull
    private final ScheduleMode scheduleMode;

    private final String poolName;

    @Nullable
    private ExceptionHandler exceptionHandler;

    private ScheduledThreadPoolExecutor executor;

    private long initialDelayMs = 0;

    private long intervalMs = 0;

    private int workersCount = 1;

    public ScheduledThreadPoolExecutorManager(@NonNull ScheduleMode scheduleMode, String poolName) {
        this(scheduleMode, poolName, new ExceptionHandler.DefaultExceptionHandler());
    }

    public ScheduledThreadPoolExecutorManager(@NonNull ScheduleMode scheduleMode, String poolName, @Nullable ExceptionHandler exceptionHandler) {
        this.scheduleMode = scheduleMode;
        this.poolName = poolName;
        this.setExceptionHandler(exceptionHandler);
    }

    public void setExceptionHandler(@Nullable ExceptionHandler exceptionHandler) {
        synchronized (lock) {
            this.exceptionHandler = exceptionHandler;
        }
    }

    public List<Runnable> getRunnableTasks() {
        synchronized (lock) {
            return Collections.unmodifiableList(runnableList);
        }
    }

    public void addRunnableTasks(Collection<Runnable> runnables) {
        if (runnables != null) {
            for (Runnable r : runnables) {
                addRunnableTask(r);
            }
        }
    }

    public void addRunnableTask(@NonNull Runnable runnable) throws NullPointerException {

        synchronized (lock) {

            runnableList.add(runnable);

            if (isRunning()) {
                restart(initialDelayMs, intervalMs, workersCount);
            }

        }
    }

    public void removeRunnableTask(Runnable runnable) {

        synchronized (lock) {

            if (runnableList.contains(runnable)) {
                runnableList.remove(runnable);
                if (isRunning()) {
                    restart(initialDelayMs, intervalMs, workersCount);
                }
            }

        }
    }

    public void removeAllRunnableTasks() {
        synchronized (lock) {

            if (isRunning()) {
                stop();
            }

            runnableList.clear();
        }
    }

    public List<ScheduledFuture<?>> getCurrentScheduledFutures() {
        synchronized (lock) {
            return Collections.unmodifiableList(currentScheduledFutures);
        }
    }

    public boolean isRunning() {
        synchronized (lock) {
            return executor != null && !executor.isShutdown();
        }
    }

    public long getInitialDelayMs() {
        return initialDelayMs;
    }

    public long getIntervalMs() {
        return intervalMs;
    }

    public int getWorkersCount() {
        return workersCount;
    }

    public void start(long intervalMs) {
        start(0, intervalMs, 1);
    }

    public void start(long delayMs, long intervalMs, int workersCount) {
        if (!isRunning()) {
            restart(delayMs, intervalMs, workersCount);
        }
    }

    public void restart(long intervalMs) {
        restart(0, intervalMs, 1);
    }

    public void restart(long delayMs, long intervalMs, int workersCount) {

        synchronized (lock) {

            if (intervalMs <= 0)
                throw new IllegalArgumentException("can't start executor: incorrect intervalMs: " + intervalMs);

            if (delayMs < 0)
                throw new IllegalArgumentException("can't start executor: incorrect initialDelayMs: " + delayMs);

            if (workersCount < 1)
                throw new IllegalArgumentException("can't start executor: incorrect workersCount: " + workersCount);

            if (runnableList.isEmpty())
                throw new RuntimeException("no runnables to schedule");

            stop();

            executor = new ScheduledThreadPoolExecutor(workersCount, new NamedThreadFactory(poolName));

            for (Runnable runnable : runnableList) {
                switch (scheduleMode) {
                    case FIXED_RATE:
                        currentScheduledFutures.add(executor.scheduleAtFixedRate(new WrappedRunnable(runnable), this.initialDelayMs = delayMs, this.intervalMs = intervalMs, TimeUnit.MILLISECONDS));
                        break;
                    case FIXED_DELAY:
                        currentScheduledFutures.add(executor.scheduleWithFixedDelay(new WrappedRunnable(runnable), this.initialDelayMs = delayMs, this.intervalMs = intervalMs, TimeUnit.MILLISECONDS));
                        break;
                }
            }
        }
    }

    /**
     * not removing target runnables
     */
    public void stop() {
        stop(false, 0);
    }

    /**
     * not removing target runnables
     */
    public void stop(boolean await, long timeoutMs) {

        synchronized (lock) {

            if (!isRunning()) {
                return;
            }

            // executor.remove(runnable);
            // executor.purge();

            executor.shutdown();
            if (await) {
                try {
                    executor.awaitTermination(timeoutMs >= 0 ? timeoutMs : 0, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.e("an InterruptedException occurred during awaitTermination(): " + e.getMessage(), e);
                }
            }
            executor = null;

            for (ScheduledFuture<?> future : currentScheduledFutures) {
                if (future != null) {
                    future.cancel(true);
                }
            }

            currentScheduledFutures.clear();
        }
    }

    final class WrappedRunnable implements Runnable {

        @NonNull
        final Runnable command;

        WrappedRunnable(@NonNull Runnable command) {
            this.command = command;
        }

        @Override
        public void run() {
            try {
                command.run();
            } catch (Throwable e) {
                logger.e("an Exception occurred during run(): " + e.getMessage(), e);
                synchronized (lock) {
                    if (exceptionHandler != null) {
                        exceptionHandler.onRunnableCrash(e);
                    }
                }
            }
        }
    }

    public enum ScheduleMode {
        FIXED_RATE, FIXED_DELAY
    }

    public interface ExceptionHandler {

        void onRunnableCrash(Throwable e);

        class DefaultExceptionHandler implements ExceptionHandler {

            private final Handler handler = new Handler(Looper.getMainLooper());

            @Override
            public void onRunnableCrash(final Throwable e) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        throw new RuntimeException(e);
                    }
                });
            }
        }
    }

}
