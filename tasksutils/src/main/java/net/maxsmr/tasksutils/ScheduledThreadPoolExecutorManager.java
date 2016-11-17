package net.maxsmr.tasksutils;

import android.support.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ScheduledThreadPoolExecutorManager {

    private final static Logger logger = LoggerFactory.getLogger(ScheduledThreadPoolExecutorManager.class);

    private final List<Runnable> runnableList = new ArrayList<>();

    private final List<ScheduledFuture<?>> currentScheduledFutures = new ArrayList<>();

    private final String poolName;

    private ScheduledThreadPoolExecutor executor;

    private long delayMs = 0;

    private long intervalMs = 0;

    private int workersCount = 1;

    public ScheduledThreadPoolExecutorManager(String poolName) {
        logger.debug("ScheduledThreadPoolExecutorManager(), poolName=" + poolName);
        this.poolName = poolName;
    }


    public List<Runnable> getRunnableList() {
        return Collections.unmodifiableList(runnableList);
    }

    public void addRunnableTasks(Collection<Runnable> runnables) {
        if (runnables != null) {
            for (Runnable r : runnables) {
                addRunnableTask(r);
            }
        }
    }

    public void addRunnableTask(Runnable runnable) throws NullPointerException {
        logger.debug("addRunnableTask(), runnable=" + runnable);

        if (runnable == null) {
            throw new NullPointerException();
        }

        synchronized (runnableList) {
            runnableList.add(runnable);
        }

        if (isRunning()) {
            restart(delayMs, intervalMs, workersCount);
        }
    }

    public void removeRunnableTask(Runnable runnable) {
        logger.debug("removeRunnableTask(), runnable=" + runnable);

        synchronized (runnableList) {
            if (runnableList.contains(runnable)) {
                runnableList.remove(runnable);
            }
        }

        if (isRunning()) {
            restart(delayMs, intervalMs, workersCount);
        }
    }

    public void removeAllRunnableTasks() {

        if (isRunning()) {
            stop(false, 0);
        }

        synchronized (runnableList) {
            runnableList.clear();
        }
    }

    public List<ScheduledFuture<?>> getCurrentScheduledFutures() {
        return Collections.unmodifiableList(currentScheduledFutures);
    }

    public boolean isRunning() {
        return executor != null && (!executor.isShutdown() || !executor.isTerminated());

    }

    public long getDelayMs() {
        return delayMs;
    }

    public long getIntervalMs() {
        return intervalMs;
    }

    public int getWorkersCount() {
        return workersCount;
    }

    public synchronized void start(long intervalMs) {
        start(0, intervalMs, 1);
    }

    public synchronized void start(long delayMs, long intervalMs, int workersCount) {
        if (!isRunning()) {
            restart(delayMs, intervalMs, workersCount);
        }
    }

    public synchronized void restart(long intervalMs) {
        restart(0, intervalMs, 1);
    }

    public synchronized void restart(long delayMs, long intervalMs, int workersCount) {
        logger.debug("start(), delayMs=" + delayMs + ", intervalMs=" + intervalMs + ", workersCount=" + workersCount);

        if (intervalMs <= 0)
            throw new IllegalArgumentException("can't start executor: incorrect intervalMs: " + intervalMs);

        if (delayMs < 0)
            throw new IllegalArgumentException("can't start executor: incorrect delayMs: " + delayMs);

        if (workersCount < 1)
            throw new IllegalArgumentException("can't start executor: incorrect workersCount: " + workersCount);

        if (runnableList.isEmpty())
            throw new RuntimeException("no runnables to schedule");

        stop(false, 0);

        executor = new ScheduledThreadPoolExecutor(workersCount, new NamedThreadFactory(poolName));

        for (Runnable runnable : runnableList) {
            logger.debug("scheduling runnable " + runnable + " with interval " + intervalMs + " ms...");
            currentScheduledFutures.add(executor.scheduleAtFixedRate(new WrappedRunnable(runnable), this.delayMs = delayMs, this.intervalMs = intervalMs, TimeUnit.MILLISECONDS));
        }
    }

    public synchronized void stop(boolean await, long timeoutMs) {
        logger.debug("stop(), await=" + await + ", timeoutMs=" + timeoutMs);

        if (!isRunning()) {
            logger.debug("executor already not running");
            return;
        }

        // executor.remove(runnable);
        // executor.purge();

        executor.shutdown();
        if (await) {
            try {
                executor.awaitTermination(timeoutMs >= 0 ? timeoutMs : 0, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                logger.error("an InterruptedException occurred during awaitTermination(): " + e.getMessage());
                Thread.currentThread().interrupt();
            }
        }
        executor = null;

        currentScheduledFutures.clear();
    }

    static final class WrappedRunnable implements Runnable {

        @NonNull
        final Runnable command;

        WrappedRunnable(@NonNull Runnable command) {
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
    }

}
