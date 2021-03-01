package net.maxsmr.tasksutils;

import net.maxsmr.commonutils.Predicate;
import net.maxsmr.commonutils.logger.BaseLogger;
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder;
import net.maxsmr.tasksutils.runnable.RunnableInfoRunnable;
import net.maxsmr.tasksutils.runnable.WrappedRunnable;
import net.maxsmr.tasksutils.runnable.WrappedRunnable.ExceptionHandler;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ScheduledThreadPoolExecutorManager {

    private static final BaseLogger logger = BaseLoggerHolder.getInstance().getLogger(ScheduledThreadPoolExecutorManager.class);

    private final Object lock = new Object();

    private final Map<RunnableInfoRunnable<?>, RunOptions> runnablesMap = new LinkedHashMap<>();

    private final Map<RunnableInfoRunnable<?>, ScheduledFuture<?>> currentScheduledFutures = new LinkedHashMap<>();

    private final String poolName;

    @Nullable
    private ExceptionHandler exceptionHandler;

    private ScheduledThreadPoolExecutor executor;

    private int workersCount = 1;

    public ScheduledThreadPoolExecutorManager(String poolName) {
        this(poolName, ExceptionHandler.STUB);
    }

    public ScheduledThreadPoolExecutorManager(String poolName, @Nullable ExceptionHandler exceptionHandler) {
        this.poolName = poolName;
        this.setExceptionHandler(exceptionHandler);
    }

    public void setExceptionHandler(@Nullable ExceptionHandler exceptionHandler) {
        synchronized (lock) {
            this.exceptionHandler = exceptionHandler;
        }
    }

    public Map<Runnable, RunOptions> getRunnableTasks() {
        synchronized (lock) {
            return new LinkedHashMap<>(runnablesMap);
        }
    }

    public void addRunnableTasks(Map<RunnableInfoRunnable<?>, RunOptions> runnables) {
        if (runnables != null) {
            for (Map.Entry<RunnableInfoRunnable<?>, RunOptions> r : runnables.entrySet()) {
                addRunnableTask(r.getKey(), r.getValue());
            }
        }
    }

    public void addRunnableTask(@NotNull RunnableInfoRunnable runnable, @NotNull RunOptions options) throws NullPointerException {
        synchronized (lock) {
            if (!Predicate.Methods.contains(runnablesMap.keySet(), element -> element.rInfo.id == runnable.rInfo.id)) {
                runnablesMap.put(runnable, options);
                if (isRunning()) {
                    scheduleRunnableTask(runnable, options);
                }
//            if (isRunning()) {
//                restart(workersCount);
//            }
            }
        }
    }

    public void removeRunnableTask(RunnableInfoRunnable runnable) {
        synchronized (lock) {
            if (Predicate.Methods.contains(runnablesMap.keySet(), element -> element.rInfo.id == runnable.rInfo.id)) {
                runnablesMap.remove(runnable);
                final ScheduledFuture<?> future = currentScheduledFutures.get(runnable);
                if (future != null) {
                    future.cancel(true);
                }
//                if (isRunning()) {
//                    restart(workersCount);
//                }
            }
        }
    }

    public void removeAllRunnableTasks() {
        synchronized (lock) {
            if (isRunning()) {
                stop();
            }
            runnablesMap.clear();
        }
    }

    public List<ScheduledFuture<?>> getCurrentScheduledFutures() {
        synchronized (lock) {
            return new ArrayList<>(currentScheduledFutures.values());
        }
    }

    public boolean isRunning() {
        synchronized (lock) {
            return executor != null && !executor.isShutdown();
        }
    }

    public int getWorkersCount() {
        return workersCount;
    }

    public void start() {
        start(1);
    }

    public void start(int workersCount) {
        if (!isRunning()) {
            restart(workersCount);
        }
    }

    public void restart() {
        restart(1);
    }

    public void restart(int workersCount) {
        synchronized (lock) {

            if (workersCount < 1)
                throw new IllegalArgumentException("can't start executor: incorrect workersCount: " + workersCount);

            stop();

            executor = new ScheduledThreadPoolExecutor(workersCount, new NamedThreadFactory(poolName));

            for (Map.Entry<RunnableInfoRunnable<?>, RunOptions> e : runnablesMap.entrySet()) {
                scheduleRunnableTask(e.getKey(), e.getValue());
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

            for (ScheduledFuture<?> future : currentScheduledFutures.values()) {
                if (future != null) {
                    future.cancel(true);
                }
            }

            currentScheduledFutures.clear();
        }
    }

    @SuppressWarnings("unchecked")
    private void scheduleRunnableTask(RunnableInfoRunnable<?> runnable, RunOptions options) {

        if (options.intervalMs <= 0)
            throw new IllegalArgumentException("can't start executor: incorrect intervalMs: " + options.intervalMs);

        if (options.initialDelayMs < 0)
            throw new IllegalArgumentException("can't start executor: incorrect initialDelayMs: " + options.initialDelayMs);

        switch (options.scheduleMode) {
            case FIXED_RATE:
                currentScheduledFutures.put(runnable, executor.scheduleAtFixedRate(new WrappedRunnable(runnable, exceptionHandler), options.initialDelayMs, options.intervalMs, TimeUnit.MILLISECONDS));
                break;
            case FIXED_DELAY:
                currentScheduledFutures.put(runnable, executor.scheduleWithFixedDelay(new WrappedRunnable(runnable, exceptionHandler), options.initialDelayMs, options.intervalMs, TimeUnit.MILLISECONDS));
                break;
        }
    }

    public enum ScheduleMode {
        FIXED_RATE, FIXED_DELAY
    }

    public static class RunOptions {

        public final long initialDelayMs;

        public final long intervalMs;

        @NotNull
        public final ScheduleMode scheduleMode;

        public RunOptions(
                long initialDelayMs,
                long intervalMs,
                @NotNull
                ScheduleMode scheduleMode
        ) {
            this.initialDelayMs = initialDelayMs;
            this.intervalMs = intervalMs;
            this.scheduleMode = scheduleMode;
        }

        @Override
        @NotNull
        public String toString() {
            return "RunOptions{" +
                    "initialDelayMs=" + initialDelayMs +
                    ", intervalMs=" + intervalMs +
                    ", scheduleMode=" + scheduleMode +
                    '}';
        }
    }
}
