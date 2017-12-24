package net.maxsmr.tasksutils.taskexecutor;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import net.maxsmr.tasksutils.storage.sync.AbstractSyncStorage;
import net.maxsmr.tasksutils.taskexecutor.TaskRunnable.ITaskRestorer;
import net.maxsmr.tasksutils.taskexecutor.TaskRunnable.ITaskResultValidator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import static net.maxsmr.tasksutils.taskexecutor.TaskRunnableExecutor.TASKS_NO_LIMIT;

public class SimpleWorkQueue<I extends RunnableInfo> {

    private static final Logger logger = LoggerFactory.getLogger(SimpleWorkQueue.class);

    @NonNull
    private final LinkedList<TaskRunnable<I>> runnableQueue = new LinkedList<>();

    private final List<PoolWorker> poolWorkers;

    private int maxRunnableQueueSize;

    @Nullable
    private ITaskResultValidator<I, TaskRunnable<I>> resultValidator;

    @Nullable
    private AbstractSyncStorage<I> syncStorage;

    public SimpleWorkQueue(int nPoolWorkers, String workQueueName,
                           int maxRunnableQueueSize,
                           @Nullable ITaskResultValidator<I, TaskRunnable<I>> resultValidator,
                           @Nullable final AbstractSyncStorage<I> syncStorage,
                           @Nullable final ITaskRestorer<I, TaskRunnable<I>> restorer) {

        if (nPoolWorkers <= 0)
            nPoolWorkers = 1;

        poolWorkers = new ArrayList<>(nPoolWorkers);

        for (int i = 0; i < nPoolWorkers; i++) {
            PoolWorker worker = new PoolWorker();
            poolWorkers.add(worker);
            if (workQueueName != null && !workQueueName.isEmpty()) {
                worker.setName(workQueueName + "_" + i);
            }
            worker.start();
        }

        setMaxRunnableQueueSize(maxRunnableQueueSize);
        setResultValidator(resultValidator);
        setSyncStorage(syncStorage);

        if (restorer != null && syncStorage != null) {
            if (!syncStorage.isRestoreCompleted()) {
                syncStorage.addStorageListener(new AbstractSyncStorage.IStorageListener() {
                    @Override
                    public void onStorageRestoreStarted(long startTime) {

                    }

                    @Override
                    public void onStorageRestoreFinished(long endTime, int restoredElementsCount) {
                        executeAll(restorer.fromRunnableInfos(syncStorage.getAll()));
                    }

                    @Override
                    public void onStorageSizeChanged(int currentSize, int previousSize) {

                    }
                });
            } else {
                executeAll(restorer.fromRunnableInfos(syncStorage.getAll()));
            }
        }
    }


    public int getMaxRunnableQueueSize() {
        return maxRunnableQueueSize;
    }

    public void setMaxRunnableQueueSize(int maxRunnableQueueSize) {
        this.maxRunnableQueueSize = maxRunnableQueueSize >= 0 ? maxRunnableQueueSize : TASKS_NO_LIMIT;
    }

    @Nullable
    public ITaskResultValidator<I, TaskRunnable<I>> getResultValidator() {
        return resultValidator;
    }

    public void setResultValidator(@Nullable ITaskResultValidator<I, TaskRunnable<I>> resultValidator) {
        this.resultValidator = resultValidator;
    }

    @Nullable
    public AbstractSyncStorage<I> getSyncStorage() {
        return syncStorage;
    }

    public void setSyncStorage(@Nullable AbstractSyncStorage<I> syncStorage) {
        this.syncStorage = syncStorage;
    }

    public int getRunnableQueueSize() {
        return runnableQueue.size();
    }

    public boolean executeAll(Collection<TaskRunnable<I>> commands) {
        boolean result = true;
        if (commands != null) {
            for (TaskRunnable<I> c : commands) {
                if (!execute(c)) {
                    result = false;
                }
            }
        }
        return result;
    }

    public boolean execute(TaskRunnable<I> command) {
        logger.debug("execute(), command=" + command);

        if (command == null) {
            throw new NullPointerException("command is null");
        }

        synchronized (runnableQueue) {
            if (runnableQueue.size() < maxRunnableQueueSize || maxRunnableQueueSize == TASKS_NO_LIMIT) {

                if (runnableQueue.contains(command)) {
                    logger.warn("runnableQueue already contains this runnable");
                    return true;
                }

                runnableQueue.addLast(command);

                if (syncStorage != null) {
                    syncStorage.addLast(command.rInfo);
                }

                runnableQueue.notify();

                return true;

            } else {
                logger.error("no capacity remains in runnable queue (" + runnableQueue.size() + "/" + maxRunnableQueueSize + ")");
                return false;
            }
        }
    }

    public void release() {
        logger.debug("release");

        for (PoolWorker worker : poolWorkers) {
            worker.interrupt();
        }
        poolWorkers.clear();

        runnableQueue.clear();
    }

    private class PoolWorker extends Thread {

        @Override
        public void run() {
            logger.debug("PoolWorker :: run()");

            TaskRunnable<I> r;

            while (!isInterrupted()) {

                synchronized (runnableQueue) {
                    while (runnableQueue.isEmpty()) {
                        try {
                            // logger.debug("waiting queue...");
                            runnableQueue.wait();
                        } catch (InterruptedException e) {
                            logger.error("an InterruptedException occurred during wait(): " + e.getMessage());
                            Thread.currentThread().interrupt();
                        }
                    }

                    // logger.debug("getting runnable...");
                    r = runnableQueue.removeFirst();
                }

                Exception exception = null;
                try {
                    // logger.debug("running runnable: " + r + "...");
                    r.run();
                } catch (Exception e) {
                    logger.error("a RuntimeException occurred during run()", exception = e);
                }

                if (syncStorage != null) {
                    syncStorage.removeById(r.rInfo.id);
                }

                if (resultValidator != null && resultValidator.needToReAddTask(r, exception)) {
                    execute(r);
                }
            }

        }
    }
}
