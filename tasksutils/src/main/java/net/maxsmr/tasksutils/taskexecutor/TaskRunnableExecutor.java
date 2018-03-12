package net.maxsmr.tasksutils.taskexecutor;

import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import net.maxsmr.commonutils.data.Observable;
import net.maxsmr.commonutils.logger.AndroidSimpleLogger;
import net.maxsmr.commonutils.logger.base.BaseSimpleLogger;
import net.maxsmr.tasksutils.NamedThreadFactory;
import net.maxsmr.tasksutils.storage.sync.AbstractSyncStorage;
import net.maxsmr.tasksutils.taskexecutor.TaskRunnable.ITaskRestorer;
import net.maxsmr.tasksutils.taskexecutor.TaskRunnable.ITaskResultValidator;
import net.maxsmr.tasksutils.taskexecutor.TaskRunnable.WrappedTaskRunnable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class TaskRunnableExecutor<I extends RunnableInfo, T extends TaskRunnable<I>> {

    public static final int DEFAULT_KEEP_ALIVE_TIME = 60;

    public static final int TASKS_NO_LIMIT = 0;

    private static BaseSimpleLogger logger = new AndroidSimpleLogger(TaskRunnableExecutor.class.getSimpleName());

    private final Object lock = new Object();

    private final ThreadPoolExecutor executor;

    private final Map<Integer, WrappedTaskRunnable<I, T>> activeTasksRunnables = new LinkedHashMap<>();

    private final Map<Integer, ExecInfo<I, T>> tasksRunnableExecInfos = new LinkedHashMap<>();

    private final Map<Integer, StatInfo<I, T>> tasksRunnableStatInfos = new LinkedHashMap<>();

    private final CallbacksObservable<I, T> callbacksObservable = new CallbacksObservable<>();

    public int queuedTasksLimit;

    @Nullable
    private ITaskResultValidator<I, T> resultValidator;

    @Nullable
    private AbstractSyncStorage<I> syncStorage;

    @Nullable
    private Handler callbacksHandler;

    public TaskRunnableExecutor(int queuedTasksLimit, int concurrentTasksLimit, long keepAliveTime, TimeUnit unit, String poolName,
                                @Nullable ITaskResultValidator<I, T> resultValidator,
                                @Nullable final AbstractSyncStorage<I> syncStorage,
                                @Nullable Handler callbacksHandler) {
        logger.debug("TaskRunnableExecutor(), queuedTasksLimit=" + queuedTasksLimit + ", concurrentTasksLimit=" + concurrentTasksLimit
                + ", keepAliveTime=" + keepAliveTime + ", unit=" + unit + ", poolName=" + poolName);

        executor = new ThreadPoolExecutorImpl(concurrentTasksLimit, concurrentTasksLimit, keepAliveTime, unit, new LinkedBlockingQueue<Runnable>(), new NamedThreadFactory(poolName));

        setQueuedTasksLimit(queuedTasksLimit);
        setResultValidator(resultValidator);
        setSyncStorage(syncStorage);
        setCallbacksHandler(callbacksHandler);
    }

    public static void setLogger(BaseSimpleLogger logger) {
        TaskRunnableExecutor.logger = logger;
    }

    public void restoreQueueByRestorer(@NonNull final ITaskRestorer<I, T> restorer) {
        logger.debug("restoreQueueByRestorer(), restorer=" + restorer);
        if (!isRunning()) {
            throw new IllegalStateException(TaskRunnableExecutor.class.getSimpleName() + " was shutdown");
        }
        if (syncStorage != null) {
            cancelAllTasks();
            if (!syncStorage.isRestoreCompleted()) {
                syncStorage.addStorageListener(new AbstractSyncStorage.IStorageListener() {
                    @Override
                    public void onStorageRestoreStarted(long startTime) {
                        logger.debug("onStorageRestoreStarted(), startTime=" + startTime);
                    }

                    @Override
                    public void onStorageRestoreFinished(long endTime, long processingTime, int restoredElementsCount) {
                        logger.debug("onStorageRestoreFinished(), endTime=" + endTime + ", processingTime=" + processingTime + ", restoredElementsCount=" + restoredElementsCount);
                        List<I> storage = syncStorage.getAll();
                        List<I> currentTasks = getAllTasksRunnableInfos();
                        List<I> filtered = RunnableInfo.filter(storage, currentTasks, false);
                        logger.debug("runnable infos ids, storage: " + RunnableInfo.idsFromInfos(storage));
                        logger.debug("runnable infos ids, current: " + RunnableInfo.idsFromInfos(currentTasks));
                        logger.debug("runnable infos ids, filtered: " + RunnableInfo.idsFromInfos(filtered));
                        RunnableInfo.setRunning(filtered, false);
                        executeAll(restorer.fromRunnableInfos(filtered));
                        syncStorage.removeStorageListener(this);
                    }

                    @Override
                    public void onStorageSizeChanged(int currentSize, int previousSize) {

                    }
                });
            } else {
                executeAll(TaskRunnable.filter(restorer.fromRunnableInfos(syncStorage.getAll()), getAllTasks(), false));
            }
        }
    }

    public void registerCallback(Callbacks<I, T> callbacks) {
        if (!isRunning()) {
            throw new IllegalStateException(TaskRunnableExecutor.class.getSimpleName() + " was shutdown");
        }
        callbacksObservable.registerObserver(callbacks);
    }

    public void unregisterCallback(Callbacks<I, T> callbacks) {
        if (!isRunning()) {
            throw new IllegalStateException(TaskRunnableExecutor.class.getSimpleName() + " was shutdown");
        }
        callbacksObservable.unregisterObserver(callbacks);
    }

    public boolean isRunning() {
        return (!executor.isShutdown() || !executor.isTerminated());
    }

    public boolean isShutdown() {
        return executor.isShutdown();
    }

    public int getQueuedTasksLimit() {
        synchronized (lock) {
            return queuedTasksLimit;
        }
    }

    public void setQueuedTasksLimit(int queuedTasksLimit) {
        if (queuedTasksLimit < 0) {
            throw new IllegalArgumentException("incorrect taskLimit: " + queuedTasksLimit);
        }
        synchronized (lock) {
            this.queuedTasksLimit = queuedTasksLimit;
        }
    }

    @Nullable
    public ITaskResultValidator<I, T> getResultValidator() {
        synchronized (lock) {
            return resultValidator;
        }
    }

    public void setResultValidator(@Nullable ITaskResultValidator<I, T> resultValidator) {
        synchronized (lock) {
            this.resultValidator = resultValidator;
        }
    }

    @Nullable
    public AbstractSyncStorage<I> getSyncStorage() {
        synchronized (lock) {
            return syncStorage;
        }
    }

    public void setSyncStorage(@Nullable AbstractSyncStorage<I> syncStorage) {
        synchronized (lock) {
            this.syncStorage = syncStorage;
        }
    }

    @Nullable
    public Handler getCallbacksHandler() {
        synchronized (lock) {
            return callbacksHandler;
        }
    }

    public void setCallbacksHandler(@Nullable Handler callbacksHandler) {
        synchronized (lock) {
            this.callbacksHandler = callbacksHandler;
        }
    }

    public boolean isTasksLimitExceeded() {
        return queuedTasksLimit != TASKS_NO_LIMIT && getWaitingTasksCount() >= queuedTasksLimit;
    }

    public int getActiveThreadsCount() {
        return executor.getActiveCount();
    }

    @NonNull
    public List<T> getAllTasks() {
        List<T> list = new LinkedList<>();
        list.addAll(getWaitingTasks());
        list.addAll(getActiveTasks());
        return Collections.unmodifiableList(list);
    }

    @NonNull
    public List<I> getAllTasksRunnableInfos() {
        return RunnableInfo.fromTasks(getAllTasks());
    }

    @NonNull
    public List<T> getWaitingTasks() {
        synchronized (lock) {
            List<T> result = new LinkedList<>();
            for (Runnable r : executor.getQueue()) { // очередь содержит все добавленные таски, включая выполняемые в данный момент; вычищается после afterExecute
                WrappedTaskRunnable<I, T> taskRunnable;
                if (!(r instanceof WrappedTaskRunnable)) {
                    throw new RuntimeException("incorrect runnable type: " + r.getClass() + ", must be: " + WrappedTaskRunnable.class.getName());
                }
                taskRunnable = (WrappedTaskRunnable<I, T>) r;
                if (!containsTask(taskRunnable.command, RunnableType.ACTIVE)) {
                    result.add((taskRunnable.command));
                }
            }
            return Collections.unmodifiableList(result);
        }
    }

    @NonNull
    public List<I> getWaitingRunnableInfos() {
        return RunnableInfo.fromTasks(getWaitingTasks());
    }

    @NonNull
    public List<T> getActiveTasks() {
        synchronized (lock) {
            List<T> list = new LinkedList<>();
            for (WrappedTaskRunnable<I, T> r : activeTasksRunnables.values()) {
                if (r.command.isRunning()) {
                    list.add(r.command);
                }
            }
            return Collections.unmodifiableList(list);
        }
    }

    @NonNull
    public List<I> getActiveRunnableInfos() {
        return RunnableInfo.fromTasks(getActiveTasks());
    }

    public int getTotalTasksCount() {
        return getAllTasks().size();
    }

    public int getWaitingTasksCount() {
        return getWaitingTasks().size();
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
        if (!isRunning()) {
            throw new IllegalStateException(TaskRunnableExecutor.class.getSimpleName() + " was shutdown");
        }
        synchronized (lock) {
            return TaskRunnable.findRunnableById(id, type == RunnableType.WAITING ? getWaitingTasks() : getActiveTasks());
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
        return runnableInfo != null && runnableInfo.isRunning();
    }

    public boolean isTaskCancelled(int id) {
        T runnable = findRunnableById(id);
        return runnable == null || runnable.isCanceled();
    }

    public boolean cancelTask(@Nullable T t) {
        logger.debug("cancelTask(), t=" + t);
        return t != null && cancelTask(t.getId());
    }

    public boolean cancelTask(@Nullable I rInfo) {
        logger.debug("cancelTask(), rInfo=" + rInfo);
        return rInfo != null && cancelTask(rInfo.id);
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

    public List<StatInfo<I, T>> getCompletedTasksStatInfos() {
        synchronized (lock) {
            return new ArrayList<>(tasksRunnableStatInfos.values());
        }
    }

    public List<T> getCompletedTasks() {
        synchronized (lock) {
            List<T> result = new LinkedList<>();
            for (StatInfo<I, T> info : getCompletedTasksStatInfos()) {
                result.add(info.getTaskRunnable());
            }
            return result;
        }
    }

    public List<I> getCompletedRunnableInfos() {
        return RunnableInfo.fromTasks(getCompletedTasks());
    }

    public int getCompletedTasksCount() {
        synchronized (lock) {
            return tasksRunnableStatInfos.size();
        }
    }

    public boolean containsCompletedTask(int id) {
        return findCompletedRunnableById(id) != null;
    }

    @Nullable
    public T findCompletedRunnableById(int id) {
        return TaskRunnable.findRunnableById(id, getCompletedTasks());
    }

    @Nullable
    public I findCompletedRunnableInfoById(int id) {
        T t = findCompletedRunnableById(id);
        return t != null ? t.rInfo : null;
    }

    @Nullable
    public StatInfo<I, T> findStatInfoById(int id) {
        T r = findCompletedRunnableById(id);
        return r != null ? getStatInfoForRunnable(r) : null;
    }

    public void executeAll(Collection<T> commands) {
        logger.debug("executeAll(), commands count: " + (commands != null ? commands.size() : 0));
        if (commands != null) {
            for (T c : commands) {
                execute(c);
            }
        }
    }

    public void execute(T command) {
        executeInternal(command, false);
    }

    public void executeInternal(T command, boolean reAdd) {
        logger.debug("executeInternal(), command=" + command + ", reAdd=" + reAdd);

        synchronized (lock) {

            if (!isRunning()) {
                throw new IllegalStateException("can't add " + command + ": not running (shutdown: " + isShutdown() + ", terminated: " + executor.isTerminated() + ", tasks count: " + getTotalTasksCount());
            }

            if (command == null) {
                throw new NullPointerException("command is null");
            }

//        if (!(command instanceof TaskRunnable)) {
//            throw new RuntimeException("incorrect type: " + command.getClass().getName() + ", must be: " + TaskRunnable.class.getName());
//        }

            if (!command.rInfo.isValid()) {
                throw new RuntimeException("incorrect task: " + command);
            }

            if (command.isCanceled()) {
                throw new RuntimeException("can't add task: " + command + ": cancelled");
            }

            if (isTasksLimitExceeded()) {
                throw new RuntimeException("can't add task " + command + ": limit exceeded (" + queuedTasksLimit + ")");
            }

            if (!reAdd ? containsTask(command) : containsTask(command, RunnableType.ACTIVE)) {
                throw new RuntimeException("can't add task " + command + ": already added");
            }

            if (syncStorage != null) {
                syncStorage.addLast(command.rInfo);
            }

            WrappedTaskRunnable<I, T> wrapped = new WrappedTaskRunnable<>(command);
            getExecInfoForRunnable(wrapped).reset().setTimeWhenAddedToQueue(System.currentTimeMillis());
            executor.execute(wrapped);
            callbacksObservable.dispatchAddedToQueue(command, getWaitingTasksCount(), getActiveTasksCount(), callbacksHandler);
        }
    }

    @NonNull
    private ExecInfo<I, T> getExecInfoForRunnable(@NonNull WrappedTaskRunnable<I, T> r) {
        synchronized (lock) {
            int id = r.command.getId();
            ExecInfo<I, T> execInfo = tasksRunnableExecInfos.get(id);
            if (execInfo == null) {
                execInfo = new ExecInfo<>(r.command);
                tasksRunnableExecInfos.put(id, execInfo);
            }
            return execInfo;
        }
    }

    @Nullable
    private ExecInfo<I, T> removeExecInfoForRunnable(@NonNull WrappedTaskRunnable<I, T> r) {
        synchronized (lock) {
            return tasksRunnableExecInfos.remove(r.command.getId());
        }
    }

    @NonNull
    private StatInfo<I, T> getStatInfoForRunnable(@NonNull T r) {
        synchronized (lock) {
            int id = r.getId();
            StatInfo<I, T> statInfo = tasksRunnableStatInfos.get(id);
            if (statInfo == null) {
                statInfo = new StatInfo<>(r);
                tasksRunnableStatInfos.put(id, statInfo);
            }
            return statInfo;
        }
    }

    public void shutdown() {
        synchronized (lock) {
            if (!isRunning()) {
                throw new IllegalStateException(TaskRunnableExecutor.class.getSimpleName() + " was already shutdown");
            }
            cancelAllTasks();
            executor.shutdown();
            activeTasksRunnables.clear();
            tasksRunnableExecInfos.clear();
            tasksRunnableStatInfos.clear();
            callbacksObservable.unregisterAll();
        }
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return executor.awaitTermination(timeout, unit);
    }

    public interface Callbacks<I extends RunnableInfo, T extends TaskRunnable<I>> {

        void onAddedToQueue(@NonNull T r, int waitingCount, int activeCount);

        void onBeforeExecute(@NonNull Thread t, @NonNull T r, @NonNull ExecInfo<I, T> execInfo, int waitingCount, int activeCount);

        void onAfterExecute(@NonNull T r, @Nullable Throwable t, @NonNull ExecInfo<I, T> execInfo, @NonNull StatInfo<I, T> statInfo, int waitingCount, int activeCount);
    }

    public enum RunnableType {
        WAITING, ACTIVE
    }

    private final class ThreadPoolExecutorImpl extends ThreadPoolExecutor {

        public ThreadPoolExecutorImpl(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
            super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
        }

        @Override
        protected void beforeExecute(Thread t, Runnable r) {
            logger.debug("beforeExecute(), t=" + t + ", r=" + r);

            if (isShutdown()) {
                logger.warn(ThreadPoolExecutor.class.getSimpleName() + " was shutdown");
                return;
            }

            super.beforeExecute(t, r);

            final long time = System.currentTimeMillis();

            WrappedTaskRunnable<I, T> taskRunnable;
            if (!(r instanceof WrappedTaskRunnable)) {
                throw new RuntimeException("incorrect command type: " + r.getClass() + ", must be: " + WrappedTaskRunnable.class.getName());
            }
            taskRunnable = (WrappedTaskRunnable<I, T>) r;

            callbacksObservable.dispatchBeforeExecute(t, taskRunnable.command,
                    getExecInfoForRunnable(taskRunnable).finishedWaitingInQueue(time), getWaitingTasksCount(), getActiveTasksCount(), callbacksHandler);

            taskRunnable.command.rInfo.isRunning = true;

            if (taskRunnable.command.isCanceled()) {
                logger.warn("task: " + taskRunnable.command + ": cancelled");
            }

            synchronized (lock) {
                activeTasksRunnables.put(taskRunnable.command.getId(), taskRunnable);
            }
        }

        @Override
        protected void afterExecute(Runnable r, Throwable t) {
            logger.debug("afterExecute(), r=" + r + ", t=" + t);

            if (isShutdown()) {
                logger.warn(ThreadPoolExecutor.class.getSimpleName() + " was shutdown");
                return;
            }

            super.afterExecute(r, t);

            final long time = System.currentTimeMillis();

            WrappedTaskRunnable<I, T> taskRunnable;
            if (!(r instanceof WrappedTaskRunnable)) {
                throw new RuntimeException("incorrect command type: " + r.getClass() + ", must be: " + WrappedTaskRunnable.class.getName());
            }
            taskRunnable = (WrappedTaskRunnable<I, T>) r;

            boolean reAdd = !taskRunnable.command.isCanceled() &&
                    resultValidator != null && resultValidator.needToReAddTask(taskRunnable.command, t);

            taskRunnable.command.rInfo.isRunning = false;

            if (syncStorage != null) {
                syncStorage.removeById(taskRunnable.command.getId());
            }

            synchronized (lock) {
                if (!activeTasksRunnables.containsKey(taskRunnable.command.getId())) {
                    throw new RuntimeException("no runnable with id " + taskRunnable.command.getId());
                }
                activeTasksRunnables.remove(taskRunnable.command.getId());
            }

            ExecInfo<I, T> execInfo = getExecInfoForRunnable(taskRunnable).finishedExecution(time, t);
            removeExecInfoForRunnable(taskRunnable);

            getStatInfoForRunnable(taskRunnable.command).addExecInfo(new ExecInfo<>(execInfo));

            callbacksObservable.dispatchAfterExecute(taskRunnable.command, t, execInfo, getStatInfoForRunnable(taskRunnable.command), getWaitingTasksCount(), getActiveTasksCount(), callbacksHandler);

            if (reAdd && !isShutdown()) {
                executeInternal(taskRunnable.command, true);
            }
        }
    }

    private static class CallbacksObservable<I extends RunnableInfo, T extends TaskRunnable<I>> extends Observable<Callbacks<I, T>> {

        private void dispatchAddedToQueue(final T r, final int waitingCount, final int activeCount, Handler handler) {
            final Runnable run = new Runnable() {
                @Override
                public void run() {
                    synchronized (mObservers) {
                        for (Callbacks<I, T> c : mObservers) {
                            c.onAddedToQueue(r, waitingCount, activeCount);
                        }
                    }
                }
            };
            if (handler != null) {
                handler.post(run);
            } else {
                run.run();
            }
        }

        private void dispatchBeforeExecute(final Thread t, final T r, final ExecInfo<I, T> execInfo, final int waitingCount, final int activeCount, Handler handler) {
            final Runnable run = new Runnable() {
                @Override
                public void run() {
                    synchronized (mObservers) {
                        for (Callbacks<I, T> c : mObservers) {
                            c.onBeforeExecute(t, r, execInfo, waitingCount, activeCount);
                        }
                    }
                }
            };
            if (handler != null) {
                handler.post(run);
            } else {
                run.run();
            }
        }

        private void dispatchAfterExecute(@NonNull final T r, @Nullable final Throwable t, @NonNull final ExecInfo<I, T> execInfo, @NonNull final StatInfo<I, T> statInfo, final int waitingCount, final int activeCount, Handler handler) {
            final Runnable run = new Runnable() {
                @Override
                public void run() {
                    synchronized (mObservers) {
                        for (Callbacks<I, T> c : mObservers) {
                            c.onAfterExecute(r, t, execInfo, statInfo, waitingCount, activeCount);
                        }
                    }
                }
            };
            if (handler != null) {
                handler.post(run);
            } else {
                run.run();
            }
        }
    }

}
