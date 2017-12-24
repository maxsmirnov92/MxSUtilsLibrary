package net.maxsmr.tasksutils.taskexecutor;

import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import net.maxsmr.commonutils.data.Observable;
import net.maxsmr.tasksutils.NamedThreadFactory;
import net.maxsmr.tasksutils.storage.sync.AbstractSyncStorage;
import net.maxsmr.tasksutils.taskexecutor.TaskRunnable.ITaskRestorer;
import net.maxsmr.tasksutils.taskexecutor.TaskRunnable.ITaskResultValidator;
import net.maxsmr.tasksutils.taskexecutor.TaskRunnable.WrappedTaskRunnable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class TaskRunnableExecutor<I extends RunnableInfo, T extends TaskRunnable<I>> extends ThreadPoolExecutor {

    public final static int DEFAULT_KEEP_ALIVE_TIME = 60;

    public static final int TASKS_NO_LIMIT = 0;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Object lock = new Object();

    private final Map<Integer, WrappedTaskRunnable<I, T>> activeTasksRunnables = new LinkedHashMap<>();

    private final Map<Integer, ExecInfo<I, T>> tasksRunnablesExecInfo = new LinkedHashMap<>();

    private final CallbacksObservable<I, T> callbacksObservable = new CallbacksObservable<>();

    public int queuedTasksLimit;

    @Nullable
    private ITaskResultValidator<I, T> resultValidator;

    @Nullable
    private AbstractSyncStorage<I> syncStorage;

    public TaskRunnableExecutor(int queuedTasksLimit, int concurrentTasksLimit, long keepAliveTime, TimeUnit unit, String poolName,
                                @Nullable ITaskResultValidator<I, T> resultValidator,
                                @Nullable final AbstractSyncStorage<I> syncStorage,
                                @Nullable final ITaskRestorer<I, T> restorer
    ) {
        super(concurrentTasksLimit, concurrentTasksLimit, keepAliveTime, unit, new LinkedBlockingQueue<Runnable>(), new NamedThreadFactory(poolName));
        logger.debug("TaskRunnableExecutor(), queuedTasksLimit=" + queuedTasksLimit + ", concurrentTasksLimit=" + concurrentTasksLimit
                + ", keepAliveTime=" + keepAliveTime + ", unit=" + unit + ", poolName=" + poolName);

        setQueuedTasksLimit(queuedTasksLimit);
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

    public boolean isRunning() {
        return (!isShutdown() || !isTerminated()) /* && getTaskCount() > 0 */;
    }

    public Observable<Callbacks<I, T>> getCallbacksObservable() {
        return callbacksObservable;
    }

    public int getQueuedTasksLimit() {
        return queuedTasksLimit;
    }

    public void setQueuedTasksLimit(int queuedTasksLimit) {
        if (queuedTasksLimit < 0) {
            throw new IllegalArgumentException("incorrect taskLimit: " + queuedTasksLimit);
        }

        this.queuedTasksLimit = queuedTasksLimit;
    }

    @Nullable
    public ITaskResultValidator<I, T> getResultValidator() {
        return resultValidator;
    }

    public void setResultValidator(@Nullable ITaskResultValidator<I, T> resultValidator) {
        this.resultValidator = resultValidator;
    }

    @Nullable
    public AbstractSyncStorage<I> getSyncStorage() {
        return syncStorage;
    }

    public void setSyncStorage(@Nullable AbstractSyncStorage<I> syncStorage) {
        this.syncStorage = syncStorage;
    }


    public boolean isTasksLimitExceeded() {
        return queuedTasksLimit != TASKS_NO_LIMIT && getWaitingTasksCount() >= queuedTasksLimit;
    }

    @NonNull
    public List<T> getAllTasks() {
        List<T> list = new LinkedList<>();
        list.addAll(getWaitingTasks());
        list.addAll(getActiveTasks());
        return Collections.unmodifiableList(list);
    }

    @NonNull
    public List<T> getWaitingTasks() {
        synchronized (lock) {
            List<T> list = new LinkedList<>();
            for (Runnable r : getQueue()) {
                WrappedTaskRunnable<I, T> taskRunnable;
                if (!(r instanceof WrappedTaskRunnable)) {
                    throw new RuntimeException("incorrect runnable type: " + r.getClass() + ", must be: " + WrappedTaskRunnable.class.getName());
                }
                taskRunnable = (WrappedTaskRunnable<I, T>) r;
                list.add((T) taskRunnable.command);
            }
            return Collections.unmodifiableList(list);
        }
    }

    @NonNull
    public List<I> getWaitingRunnableInfos() {
        synchronized (lock) {
            List<I> runnableInfos = new ArrayList<>();
            Queue<Runnable> queue = getQueue();
            for (Runnable r : queue) {
                if (r != null) {
                    if (!(r instanceof WrappedTaskRunnable)) {
                        throw new RuntimeException("incorrect runnable type: " + r.getClass() + ", must be: " + WrappedTaskRunnable.class.getName());
                    }
                    runnableInfos.add(((WrappedTaskRunnable<I, T>) r).command.rInfo);
                }
            }
            return Collections.unmodifiableList(runnableInfos);
        }
    }

    @NonNull
    public List<T> getActiveTasks() {
        synchronized (lock) {
            List<T> list = new LinkedList<>();
            for (WrappedTaskRunnable<I, T> r : activeTasksRunnables.values()) {
                list.add(r.command);
            }
            return Collections.unmodifiableList(list);
        }
    }

    @NonNull
    public List<I> getActiveRunnableInfos() {
        synchronized (lock) {
            List<I> runnableInfos = new ArrayList<>();
            for (WrappedTaskRunnable<I, T> r : activeTasksRunnables.values()) {
                runnableInfos.add(r.command.rInfo);
            }
            return Collections.unmodifiableList(runnableInfos);
        }
    }

    public int getTotalTasksCount() {
        return getAllTasks().size();
    }

    public int getWaitingTasksCount() {
        return getQueue().size();
    }

    public int getActiveTasksCount() {
        return getActiveTasks().size();
    }

    public boolean containsTask(int id) {
        return findRunnableById(id) != null;
    }

    public boolean containsTask(I r) {
        return r != null && findRunnableById(r.id) != null;
    }

    public boolean containsTask(T r) {
        return r != null && containsTask(r.rInfo);
    }

    public boolean containsTask(int id, @NonNull RunnableType type) {
        return findRunnableById(id, type) != null;
    }

    public boolean containsTask(I r, @NonNull RunnableType type) {
        return r != null && findRunnableById(r.id, type) != null;
    }

    public boolean containsTask(T r, @NonNull RunnableType type) {
        return r != null && containsTask(r.rInfo, type);
    }

    @Nullable
    public T findRunnableById(int id) {
        T r = findRunnableById(id, RunnableType.ACTIVE);
        if (r == null) {
            r = findRunnableById(id, RunnableType.WAITING);
        }
        return r;
    }

    @Nullable
    public T findRunnableById(int id, @NonNull RunnableType type) {
        synchronized (lock) {
            if (id < 0) {
                throw new IllegalArgumentException("incorrect id: " + id);
            }
            T targetRunnable = null;
            List<T> l = type == RunnableType.WAITING ? getWaitingTasks() : getActiveTasks();
            for (T r : l) {
                if (r.getId() == id) {
                    targetRunnable = r;
                    break;
                }
            }
            return targetRunnable;
        }
    }

    @Nullable
    public I findRunnableInfoById(int id) {
        I i = findRunnableInfoById(id, RunnableType.ACTIVE);
        if (i == null) {
            i = findRunnableInfoById(id, RunnableType.WAITING);
        }
        return i;
    }

    @Nullable
    public I findRunnableInfoById(int id, @NonNull RunnableType type) {
        synchronized (lock) {
            T r = findRunnableById(id, type);
            return r != null ? r.rInfo : null;
        }
    }

    public boolean isTaskRunning(int id) {
        I runnableInfo = findRunnableInfoById(id, RunnableType.ACTIVE);
        return runnableInfo != null && runnableInfo.isRunning;
    }

    public boolean isTaskCancelled(int id) {
        T runnable = findRunnableById(id);
        return runnable == null || runnable.isCancelled();
    }

    public boolean cancelTask(int id) {
        logger.debug("cancelTask(), id=" + id);
        synchronized (lock) {
            T runnable = findRunnableById(id);
            if (runnable != null) {
                logger.debug("runnable found, cancelling...");
                runnable.cancel();
                return true;
            }
            logger.error("can't cancel: runnable not found");
            return false;
        }
    }

    public boolean cancelTask(@Nullable I rInfo) {
        logger.debug("cancelTask(), rInfo=" + rInfo);
        return rInfo != null && cancelTask(rInfo.id);
    }


    public void cancelAllTasks() {
        logger.debug("cancelAllTasks()");
        synchronized (lock) {
            for (T task : getWaitingTasks()) {
                task.cancel();
            }
            for (T task : getActiveTasks()) {
                task.cancel();
            }
        }
    }

    public void executeAll(Collection<T> commands) {
        if (commands != null) {
            for (T c : commands) {
                execute(c);
            }
        }
    }

    public void execute(T command) {
        execute((Runnable) command);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void execute(Runnable command) {

        if (!isRunning()) {
            throw new IllegalStateException("can't add " + command + ": not running (isShutdown=" + isShutdown() + ", isTerminated=" + isTerminated() + ", taskCount=" + getTaskCount());
        }

        if (command == null) {
            throw new NullPointerException("command is null");
        }

        if (!(command instanceof TaskRunnable)) {
            throw new RuntimeException("incorrect type: " + command.getClass().getName() + ", must be: " + TaskRunnable.class.getName());
        }
        
        T taskRunnable = (T) command;

        if (!taskRunnable.rInfo.isValid()) {
            throw new RuntimeException("incorrect task: " +  taskRunnable);
        }

        if (taskRunnable.isCancelled()) {
            throw new RuntimeException("can't add task: " + taskRunnable + ": cancelled");
        }

        if (isTasksLimitExceeded()) {
            throw new RuntimeException("can't add task " + taskRunnable + ": limit exceeded: " + queuedTasksLimit);
        }

        if (containsTask(taskRunnable)) {
            throw new RuntimeException("can't add task " + taskRunnable + ": already added");
        }

        if (syncStorage != null) {
            syncStorage.addLast(taskRunnable.rInfo);
        }

        WrappedTaskRunnable<I, T> wrapped = new WrappedTaskRunnable<>(taskRunnable);
        getExecInfoForRunnable(wrapped).reset().setTimeWhenAddedToQueue(System.currentTimeMillis());
        super.execute(wrapped);
        callbacksObservable.dispatchAddedToQueue(taskRunnable, getWaitingTasksCount(), getActiveCount());
    }

    @Override
    @CallSuper
    protected void beforeExecute(Thread t, Runnable r) {
        super.beforeExecute(t, r);
        WrappedTaskRunnable<I, T> taskRunnable;
        if (!(r instanceof WrappedTaskRunnable)) {
            throw new RuntimeException("incorrect command type: " + r.getClass() + ", must be: " + WrappedTaskRunnable.class.getName());
        }
        taskRunnable = (WrappedTaskRunnable<I, T>) r;
        if (taskRunnable.command.isCancelled()) {
            throw new RuntimeException("can't run task: " + taskRunnable.command + ": cancelled");
        }
        taskRunnable.command.rInfo.isRunning = true;
        synchronized (lock) {
            activeTasksRunnables.put(taskRunnable.command.getId(), taskRunnable);
        }
        callbacksObservable.dispatchBeforeExecute(t, (T) taskRunnable.command, getExecInfoForRunnable(taskRunnable).finishedWaitingInQueue(System.currentTimeMillis()));
    }

    @Override
    @CallSuper
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        WrappedTaskRunnable<I, T> taskRunnable;
        if (!(r instanceof WrappedTaskRunnable)) {
            throw new RuntimeException("incorrect command type: " + r.getClass() + ", must be: " + WrappedTaskRunnable.class.getName());
        }
        taskRunnable = (WrappedTaskRunnable<I, T>) r;
        taskRunnable.command.rInfo.isRunning = false;

        boolean reAdd = resultValidator != null && resultValidator.needToReAddTask(taskRunnable.command, t);

        if (syncStorage != null) {
            syncStorage.removeById(taskRunnable.command.getId());
        }

        synchronized (lock) {
            if (!activeTasksRunnables.containsKey(taskRunnable.command.getId())) {
                throw new RuntimeException("no runnable with id " + taskRunnable.command.getId());
            }
            activeTasksRunnables.remove(taskRunnable.command.getId());
        }

        ExecInfo<I, T> execInfo = getExecInfoForRunnable(taskRunnable).finishedExecution(System.currentTimeMillis());
        removeExecInfoForRunnable(taskRunnable);
        callbacksObservable.dispatchAfterExecute(taskRunnable.command, t, execInfo);

        if (reAdd) {
            execute(r);
        }
    }

    @NonNull
    private ExecInfo<I, T> getExecInfoForRunnable(@NonNull WrappedTaskRunnable<I, T> r) {
        synchronized (lock) {
            ExecInfo<I, T> execInfo = tasksRunnablesExecInfo.get(r.command.getId());
            if (execInfo == null) {
                execInfo = new ExecInfo<>(r.command);
                tasksRunnablesExecInfo.put(r.command.getId(), execInfo);
            }
            return execInfo;
        }
    }

    @Nullable
    private ExecInfo<I, T> removeExecInfoForRunnable(@NonNull WrappedTaskRunnable<I, T> r) {
        synchronized (lock) {
            return tasksRunnablesExecInfo.remove(r.command.getId());
        }
    }

    public interface Callbacks<I extends RunnableInfo, T extends TaskRunnable<I>> {

        void onAddedToQueue(T r, int waitingCount, int activeCount);

        void onBeforeExecute(Thread t, T r, ExecInfo<I, T> execInfo);

        void onAfterExecute(T r, Throwable t, ExecInfo<I, T> execInfo);
    }

    public enum RunnableType {
        WAITING, ACTIVE
    }

    private static class CallbacksObservable<I extends RunnableInfo, T extends TaskRunnable<I>> extends Observable<Callbacks<I, T>> {

        private void dispatchAddedToQueue(T r, int waitingCount, int activeCount) {
            synchronized (mObservers) {
                for (Callbacks<I, T> c : mObservers) {
                    c.onAddedToQueue(r, waitingCount, activeCount);
                }
            }
        }

        private void dispatchBeforeExecute(Thread t, T r, ExecInfo<I, T> execInfo) {
            synchronized (mObservers) {
                for (Callbacks<I, T> c : mObservers) {
                    c.onBeforeExecute(t, r, execInfo);
                }
            }
        }

        private void dispatchAfterExecute(T r, Throwable t, ExecInfo<I, T> execInfo) {
            synchronized (mObservers) {
                for (Callbacks<I, T> c : mObservers) {
                    c.onAfterExecute(r, t, execInfo);
                }
            }
        }
    }

}
