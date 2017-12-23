package net.maxsmr.tasksutils.taskexecutor;

import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import net.maxsmr.commonutils.data.Observable;
import net.maxsmr.tasksutils.NamedThreadFactory;
import net.maxsmr.tasksutils.storage.sync.AbstractSyncStorage;
import net.maxsmr.tasksutils.taskexecutor.TaskRunnable.ITaskRestorer;
import net.maxsmr.tasksutils.taskexecutor.TaskRunnable.ITaskResultValidator;

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


public abstract class AbsTaskRunnableExecutor<I extends RunnableInfo> extends ThreadPoolExecutor {

    public static final int TASKS_NO_LIMIT = 0;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Object lock = new Object();

    private final Map<Integer, WrappedTaskRunnable> activeTasksRunnables = new LinkedHashMap<>();

    private final Map<Integer, ExecInfo> tasksRunnablesExecInfo = new LinkedHashMap<>();

    private final CallbacksObservable callbacksObservable = new CallbacksObservable();

    public int queuedTasksLimit;

    @Nullable
    private ITaskResultValidator<I, TaskRunnable<I>> resultValidator;

    @Nullable
    private AbstractSyncStorage<I> syncStorage;

    public AbsTaskRunnableExecutor(int queuedTasksLimit, int concurrentTasksLimit, long keepAliveTime, TimeUnit unit, String poolName,
                                   @Nullable ITaskResultValidator<I, TaskRunnable<I>> resultValidator,
                                   @Nullable final AbstractSyncStorage<I> syncStorage,
                                   @Nullable final ITaskRestorer<I, TaskRunnable<I>> restorer
    ) {
        super(concurrentTasksLimit, concurrentTasksLimit, keepAliveTime, unit, new LinkedBlockingQueue<Runnable>(), new NamedThreadFactory(poolName));
        logger.debug("AbstractSyncThreadPoolExecutor(), queuedTasksLimit=" + queuedTasksLimit + ", concurrentTasksLimit=" + concurrentTasksLimit
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

    public Observable<Callbacks> getCallbacksObservable() {
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


    public boolean isTasksLimitExceeded() {
        return queuedTasksLimit != TASKS_NO_LIMIT && getWaitingTasksCount() >= queuedTasksLimit;
    }

    @NonNull
    public List<TaskRunnable<?>> getAllTasks() {
        List<TaskRunnable<?>> list = new LinkedList<>();
        list.addAll(getWaitingTasks());
        list.addAll(getActiveTasks());
        return Collections.unmodifiableList(list);
    }

    @NonNull
    public List<TaskRunnable<?>> getWaitingTasks() {
        synchronized (lock) {
            List<TaskRunnable<?>> list = new LinkedList<>();
            for (Runnable r : getQueue()) {
                WrappedTaskRunnable taskRunnable;
                if (!(r instanceof WrappedTaskRunnable)) {
                    throw new RuntimeException("incorrect runnable type: " + r.getClass() + ", must be: " + WrappedTaskRunnable.class.getName());
                }
                taskRunnable = (WrappedTaskRunnable) r;
                list.add(taskRunnable.command);
            }
            return Collections.unmodifiableList(list);
        }
    }

    @NonNull
    public List<RunnableInfo> getWaitingRunnableInfos() {
        synchronized (lock) {
            List<RunnableInfo> runnableInfos = new ArrayList<>();
            Queue<Runnable> queue = getQueue();
            for (Runnable r : queue) {
                if (r != null) {
                    if (!(r instanceof WrappedTaskRunnable)) {
                        throw new RuntimeException("incorrect runnable type: " + r.getClass() + ", must be: " + WrappedTaskRunnable.class.getName());
                    }
                    runnableInfos.add(((WrappedTaskRunnable) r).command.rInfo);
                }
            }
            return Collections.unmodifiableList(runnableInfos);
        }
    }

    @NonNull
    public List<TaskRunnable<?>> getActiveTasks() {
        synchronized (lock) {
            List<TaskRunnable<?>> list = new LinkedList<>();
            for (WrappedTaskRunnable r : activeTasksRunnables.values()) {
                list.add(r.command);
            }
            return Collections.unmodifiableList(list);
        }
    }

    @NonNull
    public List<RunnableInfo> getActiveRunnableInfos() {
        synchronized (lock) {
            List<RunnableInfo> runnableInfos = new ArrayList<>();
            for (WrappedTaskRunnable r : activeTasksRunnables.values()) {
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

    public boolean containsTask(RunnableInfo r) {
        return r != null && findRunnableById(r.id) != null;
    }

    public boolean containsTask(TaskRunnable r) {
        return r != null && containsTask(r.rInfo);
    }

    public boolean containsTask(int id, @NonNull RunnableType type) {
        return findRunnableById(id, type) != null;
    }

    public boolean containsTask(RunnableInfo r, @NonNull RunnableType type) {
        return r != null && findRunnableById(r.id, type) != null;
    }

    public boolean containsTask(TaskRunnable r, @NonNull RunnableType type) {
        return r != null && containsTask(r.rInfo, type);
    }

    @Nullable
    public TaskRunnable<?> findRunnableById(int id) {
        TaskRunnable<?> r = findRunnableById(id, RunnableType.ACTIVE);
        if (r == null) {
            r = findRunnableById(id, RunnableType.WAITING);
        }
        return r;
    }

    @Nullable
    public TaskRunnable<?> findRunnableById(int id, @NonNull RunnableType type) {
        synchronized (lock) {
            if (id < 0) {
                throw new IllegalArgumentException("incorrect id: " + id);
            }
            TaskRunnable targetRunnable = null;
            List<TaskRunnable<?>> l = type == RunnableType.WAITING ? getWaitingTasks() : getActiveTasks();
            for (TaskRunnable r : l) {
                if (r.rInfo.id == id) {
                    targetRunnable = r;
                    break;
                }
            }
            return targetRunnable;
        }
    }

    @Nullable
    public RunnableInfo findRunnableInfoById(int id) {
        RunnableInfo i = findRunnableInfoById(id, RunnableType.ACTIVE);
        if (i == null) {
            i = findRunnableInfoById(id, RunnableType.WAITING);
        }
        return i;
    }

    @Nullable
    public RunnableInfo findRunnableInfoById(int id, @NonNull RunnableType type) {
        synchronized (lock) {
            TaskRunnable r = findRunnableById(id, type);
            return r != null ? r.rInfo : null;
        }
    }

    public boolean isTaskRunning(int id) {
        RunnableInfo runnableInfo = findRunnableInfoById(id, RunnableType.ACTIVE);
        return runnableInfo != null && runnableInfo.isRunning;
    }

    public boolean isTaskCancelled(int id) {
        RunnableInfo runnableInfo = findRunnableInfoById(id);
        return runnableInfo == null || runnableInfo.isCancelled();
    }

    public boolean cancelTask(int id) {
        logger.debug("cancelTask(), id=" + id);
        synchronized (lock) {
            RunnableInfo info = findRunnableInfoById(id);
            if (info != null) {
                logger.debug("runnable found, cancelling...");
                info.cancel();
                return true;
            }
            logger.error("can't cancel: runnable not found");
            return false;
        }
    }

    public boolean cancelTask(@Nullable RunnableInfo rInfo) {
        logger.debug("cancelTask(), rInfo=" + rInfo);
        return rInfo != null && cancelTask(rInfo.id);
    }


    public void cancelAllTasks() {
        logger.debug("cancelAllTasks()");
        synchronized (lock) {
            for (RunnableInfo rInfo : getWaitingRunnableInfos()) {
                rInfo.cancel();
            }
            for (RunnableInfo rInfo : getActiveRunnableInfos()) {
                rInfo.cancel();
            }
        }
    }

    public void executeAll(Collection<TaskRunnable<I>> commands) {
        if (commands != null) {
            for (TaskRunnable<I> c : commands) {
                execute(c);
            }
        }
    }

    public void execute(TaskRunnable<I> command) {
        execute((Runnable) command);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void execute(Runnable command) {

        if (!isRunning()) {
            throw new IllegalStateException("can't run " + command + ": not running (isShutdown=" + isShutdown() + ", isTerminated=" + isTerminated() + ", taskCount=" + getTaskCount());
        }

        if (command == null) {
            throw new NullPointerException("command is null");
        }

        if (!(command instanceof TaskRunnable)) {
            throw new RuntimeException("incorrect type: " + command.getClass() + ", must be: " + TaskRunnable.class.getName());
        }

        if (((TaskRunnable<I>) command).rInfo.id < 0) {
            throw new RuntimeException("incorrect id: " + ((TaskRunnable) command).rInfo.id);
        }

        if (isTasksLimitExceeded()) {
            logger.error("can't add new task: tasks limit exceeded: " + queuedTasksLimit);
            return;
        }

        if (containsTask((TaskRunnable<I>) command)) {
            logger.error("can't add new task: task list already contains " + TaskRunnable.class.getSimpleName() + " with id " + ((TaskRunnable) command).rInfo.id);
            return;
        }

        if (syncStorage != null) {
            syncStorage.addLast(((TaskRunnable<I>) command).rInfo);
        }

        WrappedTaskRunnable wrapped = new WrappedTaskRunnable((TaskRunnable) command);
        getExecInfoForRunnable(wrapped).reset().setTimeWhenAddedToQueue(System.currentTimeMillis());
        super.execute(wrapped);
        callbacksObservable.dispatchAddedToQueue((TaskRunnable) command, getWaitingTasksCount(), getActiveCount());
    }

    @Override
    @CallSuper
    protected void beforeExecute(Thread t, Runnable r) {
        super.beforeExecute(t, r);
        WrappedTaskRunnable taskRunnable;
        if (!(r instanceof WrappedTaskRunnable)) {
            throw new RuntimeException("incorrect command type: " + r.getClass() + ", must be: " + WrappedTaskRunnable.class.getName());
        }
        taskRunnable = (WrappedTaskRunnable) r;
        taskRunnable.command.rInfo.isRunning = true;
        synchronized (lock) {
            activeTasksRunnables.put(taskRunnable.command.rInfo.id, taskRunnable);
        }
        callbacksObservable.dispatchBeforeExecute(t, taskRunnable.command, getExecInfoForRunnable(taskRunnable).finishedWaitingInQueue(System.currentTimeMillis()));
    }

    @Override
    @CallSuper
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        WrappedTaskRunnable taskRunnable;
        if (!(r instanceof WrappedTaskRunnable)) {
            throw new RuntimeException("incorrect command type: " + r.getClass() + ", must be: " + WrappedTaskRunnable.class.getName());
        }
        taskRunnable = (WrappedTaskRunnable) r;
        taskRunnable.command.rInfo.isRunning = false;

        if (syncStorage != null) {
            syncStorage.removeById(((TaskRunnable<I>) taskRunnable.command).rInfo.id);
        }

        synchronized (lock) {
            if (!activeTasksRunnables.containsKey(taskRunnable.command.rInfo.id)) {
                throw new RuntimeException("no runnable with id " + taskRunnable.command.rInfo.id);
            }
            activeTasksRunnables.remove(taskRunnable.command.rInfo.id);
        }

        callbacksObservable.dispatchAfterExecute(taskRunnable.command, t, getExecInfoForRunnable(taskRunnable).finishedExecution(System.currentTimeMillis()));
        removeExecInfoForRunnable(taskRunnable);

        if (resultValidator != null && resultValidator.needToReAddTask((TaskRunnable<I>) taskRunnable.command)) {
            execute(r);
        }
    }

    @NonNull
    private ExecInfo getExecInfoForRunnable(@NonNull WrappedTaskRunnable r) {
        synchronized (lock) {
            ExecInfo execInfo = tasksRunnablesExecInfo.get(r.command.rInfo.id);
            if (execInfo == null) {
                execInfo = new ExecInfo(r.command);
                tasksRunnablesExecInfo.put(r.command.rInfo.id, execInfo);
            }
            return execInfo;
        }
    }

    @Nullable
    private ExecInfo removeExecInfoForRunnable(@NonNull WrappedTaskRunnable r) {
        synchronized (lock) {
            return tasksRunnablesExecInfo.remove(r.command.rInfo.id);
        }
    }

    private class CallbacksObservable extends Observable<Callbacks> {

        private void dispatchAddedToQueue(TaskRunnable<?> r, int waitingCount, int activeCount) {
            synchronized (mObservers) {
                for (Callbacks c : copyOfObservers()) {
                    c.onAddedToQueue(r, waitingCount, activeCount);
                }
            }
        }

        private void dispatchBeforeExecute(Thread t, TaskRunnable<?> r, ExecInfo execInfo) {
            synchronized (mObservers) {
                for (Callbacks c : copyOfObservers()) {
                    c.onBeforeExecute(t, r, execInfo);
                }
            }
        }

        private void dispatchAfterExecute(TaskRunnable<?> r, Throwable t, ExecInfo execInfo) {
            synchronized (mObservers) {
                for (Callbacks c : copyOfObservers()) {
                    c.onAfterExecute(r, t, execInfo);
                }
            }
        }
    }

    public interface Callbacks {

        void onAddedToQueue(TaskRunnable<?> r, int waitingCount, int activeCount);

        void onBeforeExecute(Thread t, TaskRunnable<?> r, ExecInfo execInfo);

        void onAfterExecute(TaskRunnable<?> r, Throwable t, ExecInfo execInfo);
    }

    public enum RunnableType {
        WAITING, ACTIVE
    }

    /** for seeing in LogCat */
    static final class WrappedTaskRunnable implements Runnable {

        @NonNull
        final TaskRunnable<?> command;

        WrappedTaskRunnable(@NonNull TaskRunnable<?> command) {
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
