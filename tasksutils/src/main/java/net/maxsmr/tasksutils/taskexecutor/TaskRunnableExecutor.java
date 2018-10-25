package net.maxsmr.tasksutils.taskexecutor;

import android.os.AsyncTask;
import android.os.Handler;
import org.jetbrains.annotations.NotNull;
import android.support.annotation.Nullable;

import net.maxsmr.commonutils.data.Observable;
import net.maxsmr.commonutils.logger.BaseLogger;
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder;
import net.maxsmr.tasksutils.NamedThreadFactory;
import net.maxsmr.tasksutils.storage.sync.AbstractSyncStorage;
import net.maxsmr.tasksutils.taskexecutor.TaskRunnable.ITaskRestorer;
import net.maxsmr.tasksutils.taskexecutor.TaskRunnable.ITaskResultValidator;
import net.maxsmr.tasksutils.taskexecutor.TaskRunnable.WrappedTaskRunnable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class TaskRunnableExecutor<I extends RunnableInfo, ProgressInfo, Result, T extends TaskRunnable<I, ProgressInfo, Result>> {

    private static final BaseLogger logger = BaseLoggerHolder.getInstance().getLogger(TaskRunnableExecutor.class);

    public static final int DEFAULT_KEEP_ALIVE_TIME = 60;

    public static final int TASKS_NO_LIMIT = 0;

    private final Object lock = new Object();

    private final ThreadPoolExecutor executor;

    private final Map<Integer, WrappedTaskRunnable<I, ProgressInfo, Result, T>> activeTasksRunnables = new LinkedHashMap<>();

    private final Map<Integer, ExecInfo<I, ProgressInfo, Result, T>> tasksRunnableExecInfos = new LinkedHashMap<>();

    private final Map<Integer, StatInfo<I, ProgressInfo, Result, T>> tasksRunnableStatInfos = new LinkedHashMap<>();

    private final CallbacksObservable<I, ProgressInfo, Result, T> callbacksObservable = new CallbacksObservable<>();

    public int queuedTasksLimit;

    @Nullable
    private ITaskResultValidator<I, ProgressInfo, Result, T> resultValidator;

    @Nullable
    private AbstractSyncStorage<I> syncStorage;

    @Nullable
    private Handler callbacksHandler;

    public TaskRunnableExecutor(int queuedTasksLimit, int concurrentTasksLimit, long keepAliveTime, TimeUnit unit, String poolName,
                                @Nullable ITaskResultValidator<I, ProgressInfo, Result, T> resultValidator,
                                @Nullable final AbstractSyncStorage<I> syncStorage,
                                @Nullable Handler callbacksHandler) {
        logger.d("TaskRunnableExecutor(), queuedTasksLimit=" + queuedTasksLimit + ", concurrentTasksLimit=" + concurrentTasksLimit
                + ", keepAliveTime=" + keepAliveTime + ", unit=" + unit + ", poolName=" + poolName);

        executor = new ThreadPoolExecutorImpl(concurrentTasksLimit, concurrentTasksLimit, keepAliveTime, unit, new LinkedBlockingQueue<Runnable>(), new NamedThreadFactory(poolName));

        setQueuedTasksLimit(queuedTasksLimit);
        setResultValidator(resultValidator);
        setSyncStorage(syncStorage);
        setCallbacksHandler(callbacksHandler);
    }

    public void restoreQueueByRestorer(@NotNull final ITaskRestorer<I, ProgressInfo, Result, T> restorer) {
        logger.d("restoreQueueByRestorer(), restorer=" + restorer);
        if (!isRunning()) {
            throw new IllegalStateException(TaskRunnableExecutor.class.getSimpleName() + " was shutdown");
        }
        if (syncStorage != null) {
//            cancelAllTasks();
            if (!syncStorage.isRestoreCompleted()) {
                syncStorage.addStorageListener(new AbstractSyncStorage.IStorageListener() {
                    @Override
                    public void onStorageRestoreStarted(long startTime) {
                        logger.d("onStorageRestoreStarted(), startTime=" + startTime);
                    }

                    @Override
                    public void onStorageRestoreFinished(long endTime, long processingTime, int restoredElementsCount) {
                        logger.d("onStorageRestoreFinished(), endTime=" + endTime + ", processingTime=" + processingTime + ", restoredElementsCount=" + restoredElementsCount);
                        executeUniqueFromInfoList(restorer, syncStorage.getAll());
                        syncStorage.removeStorageListener(this);
                    }

                    @Override
                    public void onStorageSizeChanged(int currentSize, int previousSize) {

                    }
                });
            } else {
                executeUniqueFromInfoList(restorer, syncStorage.getAll());
            }
        }
    }

    public void registerCallback(Callbacks<I, ProgressInfo, Result, T> callbacks) {
        if (!isRunning()) {
            throw new IllegalStateException(TaskRunnableExecutor.class.getSimpleName() + " was shutdown");
        }
        callbacksObservable.registerObserver(callbacks);
    }

    public void unregisterCallback(Callbacks<I, ProgressInfo, Result, T> callbacks) {
        if (!isRunning()) {
            throw new IllegalStateException(TaskRunnableExecutor.class.getSimpleName() + " was shutdown");
        }
        callbacksObservable.unregisterObserver(callbacks);
    }

    public boolean isRunning() {
        return (!isShutdown() || !executor.isTerminated());
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
    public ITaskResultValidator<I, ProgressInfo, Result, T> getResultValidator() {
        synchronized (lock) {
            return resultValidator;
        }
    }

    public void setResultValidator(@Nullable ITaskResultValidator<I, ProgressInfo, Result, T> resultValidator) {
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

    @NotNull
    public Set<T> getAllTasks() {
        Set<T> set = new LinkedHashSet<>();
        set.addAll(getWaitingTasks());
        set.addAll(getActiveTasks());
        return set;
    }

    @NotNull
    public Set<I> getAllTasksRunnableInfos() {
        return new LinkedHashSet<>(TaskRunnable.toRunnableInfos(getAllTasks()));
    }

    @NotNull
    public List<T> getWaitingTasks() {
        synchronized (lock) {
            List<T> result = new LinkedList<>();
            for (Runnable r : executor.getQueue()) { // очередь содержит все добавленные таски, включая выполняемые в данный момент; вычищается после afterExecute
                WrappedTaskRunnable<I, ProgressInfo, Result, T> taskRunnable;
                if (!(r instanceof WrappedTaskRunnable)) {
                    throw new RuntimeException("incorrect runnable type: " + r.getClass() + ", must be: " + WrappedTaskRunnable.class.getName());
                }
                //noinspection unchecked
                taskRunnable = (WrappedTaskRunnable<I, ProgressInfo, Result, T>) r;
                if (!containsTask(taskRunnable.command, RunnableType.ACTIVE)) {
                    result.add((taskRunnable.command));
                }
            }
            return Collections.unmodifiableList(result);
        }
    }

    @NotNull
    public List<I> getWaitingRunnableInfos() {
        return TaskRunnable.toRunnableInfos(getWaitingTasks());
    }

    @NotNull
    public List<T> getActiveTasks() {
        synchronized (lock) {
            List<T> list = new LinkedList<>();
            for (WrappedTaskRunnable<I, ProgressInfo, Result, T> r : activeTasksRunnables.values()) {
                if (r.command.isRunning() || r.command.isFinished()) {
                    list.add(r.command);
                }
            }
            return Collections.unmodifiableList(list);
        }
    }

    @NotNull
    public List<I> getActiveRunnableInfos() {
        return TaskRunnable.toRunnableInfos(getActiveTasks());
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

    public boolean containsTask(int id, @NotNull RunnableType type) {
        return findRunnableById(id, type) != null;
    }

    public boolean containsTask(I r, @NotNull RunnableType type) {
        return r != null && findRunnableById(r.id, type) != null;
    }

    public boolean containsTask(T r, @NotNull RunnableType type) {
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
    public T findRunnableById(int id, @NotNull RunnableType type) {
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
    public I findRunnableInfoById(int id, @NotNull RunnableType type) {
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
        logger.d("cancelTask(), t=" + t);
        return t != null && cancelTask(t.getId());
    }

    public boolean cancelTask(@Nullable I rInfo) {
        logger.d("cancelTask(), rInfo=" + rInfo);
        return rInfo != null && cancelTask(rInfo.id);
    }

    public boolean cancelTask(int id) {
        logger.d("cancelTask(), id=" + id);
        synchronized (lock) {
            T runnable = findRunnableById(id);
            if (runnable != null) {
                logger.d("runnable found, cancelling...");
                runnable.cancel();
                return true;
            }
            logger.e("can't cancel: runnable not found");
            return false;
        }
    }

    public void cancelAllTasks() {
        logger.d("cancelAllTasks()");
        synchronized (lock) {
            for (T task : getAllTasks()) {
                task.cancel();
            }
        }
    }

    public List<StatInfo<I, ProgressInfo, Result, T>> getCompletedTasksStatInfos() {
        synchronized (lock) {
            return new ArrayList<>(tasksRunnableStatInfos.values());
        }
    }

    public List<T> getCompletedTasks() {
        synchronized (lock) {
            List<T> result = new LinkedList<>();
            for (StatInfo<I, ProgressInfo, Result, T> info : getCompletedTasksStatInfos()) {
                result.add(info.getTaskRunnable());
            }
            return result;
        }
    }

    public List<I> getCompletedRunnableInfos() {
        return TaskRunnable.toRunnableInfos(getCompletedTasks());
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
    public StatInfo<I, ProgressInfo, Result, T> findStatInfoById(int id) {
        T r = findCompletedRunnableById(id);
        return r != null ? getStatInfoForRunnable(r) : null;
    }

    private void executeUniqueFromInfoList(@NotNull ITaskRestorer<I, ProgressInfo, Result, T> restorer, Collection<I> target) {
        Set<I> currentTasks = getAllTasksRunnableInfos();
        List<I> filtered = RunnableInfo.filter(target, currentTasks, false);
        logger.d("runnable infos ids, target: " + RunnableInfo.idsFromInfos(target));
        logger.d("runnable infos ids, current: " + RunnableInfo.idsFromInfos(currentTasks));
        logger.d("runnable infos ids, filtered: " + RunnableInfo.idsFromInfos(filtered));
        RunnableInfo.setRunning(filtered, false);
        executeAll(TaskRunnable.filter(restorer.fromRunnableInfos(filtered), false));
    }

    public void executeAll(Collection<T> commands) throws RuntimeException {
        logger.d("executeAll(), commands count: " + (commands != null ? commands.size() : 0));
        if (commands != null) {
            for (T c : commands) {
                execute(c);
            }
        }
    }

    public void execute(T command) throws RuntimeException {
        executeInternal(command, false);
    }

    public void executeInternal(T command, boolean reAdd) throws RuntimeException {
        logger.d("executeInternal(), command=" + command + ", reAdd=" + reAdd);

        synchronized (lock) {

            if (isShutdown()) {
                throw new IllegalStateException("can't add " + command + ": not running (shutdown: " + isShutdown() + ", terminated: " + executor.isTerminated() + ", tasks count: " + getTotalTasksCount());
            }

            if (command == null) {
                throw new NullPointerException("command is null");
            }

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

            command.rInfo.status = AsyncTask.Status.PENDING;

            if (syncStorage != null) {
                syncStorage.addLast(command.rInfo);
            }

            WrappedTaskRunnable<I, ProgressInfo, Result, T> wrapped = new WrappedTaskRunnable<>(command);
            getExecInfoForRunnable(wrapped).reset().setTimeWhenAddedToQueue(System.currentTimeMillis());
            executor.execute(wrapped);
            callbacksObservable.dispatchAddedToQueue(command, getWaitingTasksCount(), getActiveTasksCount(), callbacksHandler);
        }
    }

    @NotNull
    private ExecInfo<I, ProgressInfo, Result, T> getExecInfoForRunnable(@NotNull WrappedTaskRunnable<I, ProgressInfo, Result, T> r) {
        synchronized (lock) {
            int id = r.command.getId();
            ExecInfo<I, ProgressInfo, Result, T> execInfo = tasksRunnableExecInfos.get(id);
            if (execInfo == null) {
                execInfo = new ExecInfo<>(r.command);
                tasksRunnableExecInfos.put(id, execInfo);
            }
            return execInfo;
        }
    }

    @Nullable
    private ExecInfo<I, ProgressInfo, Result, T> removeExecInfoForRunnable(@NotNull WrappedTaskRunnable<I, ProgressInfo, Result, T> r) {
        synchronized (lock) {
            return tasksRunnableExecInfos.remove(r.command.getId());
        }
    }

    @NotNull
    private StatInfo<I, ProgressInfo, Result, T> getStatInfoForRunnable(@NotNull T r) {
        synchronized (lock) {
            int id = r.getId();
            StatInfo<I, ProgressInfo, Result, T> statInfo = tasksRunnableStatInfos.get(id);
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

    public interface Callbacks<I extends RunnableInfo, ProgressInfo, Result, T extends TaskRunnable<I, ProgressInfo, Result>> {

        void onAddedToQueue(@NotNull T r, int waitingCount, int activeCount);

        void onBeforeExecute(@NotNull Thread t, @NotNull T r, @NotNull ExecInfo<I, ProgressInfo, Result, T> execInfo, int waitingCount, int activeCount);

        void onAfterExecute(@NotNull T r, @Nullable Throwable t, @NotNull ExecInfo<I, ProgressInfo, Result, T> execInfo, @NotNull StatInfo<I, ProgressInfo, Result, T> statInfo, int waitingCount, int activeCount);
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
            logger.d("beforeExecute(), t=" + t + ", r=" + r);

            if (isShutdown()) {
                logger.w(ThreadPoolExecutor.class.getSimpleName() + " was shutdown");
                return;
            }

            super.beforeExecute(t, r);

            final long time = System.currentTimeMillis();

            WrappedTaskRunnable<I, ProgressInfo, Result, T> taskRunnable;
            if (!(r instanceof WrappedTaskRunnable)) {
                throw new RuntimeException("incorrect command type: " + r.getClass() + ", must be: " + WrappedTaskRunnable.class.getName());
            }
            //noinspection unchecked
            taskRunnable = (WrappedTaskRunnable<I, ProgressInfo, Result, T>) r;

            callbacksObservable.dispatchBeforeExecute(t, taskRunnable.command,
                    getExecInfoForRunnable(taskRunnable).finishedWaitingInQueue(time), getWaitingTasksCount(), getActiveTasksCount(), callbacksHandler);

//            taskRunnable.command.rInfo.isRunning = true;

            if (taskRunnable.command.isCanceled()) {
                logger.w("task: " + taskRunnable.command + ": cancelled");
            }

            synchronized (lock) {
                activeTasksRunnables.put(taskRunnable.command.getId(), taskRunnable);
            }
        }

        @Override
        protected void afterExecute(Runnable r, Throwable t) {
            logger.d("afterExecute(), r=" + r + ", t=" + t);

            if (isShutdown()) {
                logger.w(ThreadPoolExecutor.class.getSimpleName() + " was shutdown");
                return;
            }

            super.afterExecute(r, t);

            final long time = System.currentTimeMillis();

            WrappedTaskRunnable<I, ProgressInfo, Result, T> taskRunnable;
            if (!(r instanceof WrappedTaskRunnable)) {
                throw new RuntimeException("incorrect command type: " + r.getClass() + ", must be: " + WrappedTaskRunnable.class.getName());
            }
            //noinspection unchecked
            taskRunnable = (WrappedTaskRunnable<I, ProgressInfo, Result, T>) r;

            boolean reAdd = !taskRunnable.command.isCanceled() &&
                    resultValidator != null && resultValidator.needToReAddTask(taskRunnable.command, t);

//            taskRunnable.command.rInfo.isRunning = false;

            if (syncStorage != null) {
                syncStorage.removeById(taskRunnable.command.getId());
            }

            synchronized (lock) {
                if (!activeTasksRunnables.containsKey(taskRunnable.command.getId())) {
                    throw new RuntimeException("no runnable with id " + taskRunnable.command.getId());
                }
                activeTasksRunnables.remove(taskRunnable.command.getId());
            }

            ExecInfo<I, ProgressInfo, Result, T> execInfo = getExecInfoForRunnable(taskRunnable).finishedExecution(time, t);
            removeExecInfoForRunnable(taskRunnable);

            getStatInfoForRunnable(taskRunnable.command).addExecInfo(new ExecInfo<>(execInfo));

            callbacksObservable.dispatchAfterExecute(taskRunnable.command, t, execInfo, getStatInfoForRunnable(taskRunnable.command), getWaitingTasksCount(), getActiveTasksCount(), callbacksHandler);

            if (reAdd && !isShutdown()) {
                executeInternal(taskRunnable.command, true);
            }
        }
    }

    private static class CallbacksObservable<I extends RunnableInfo, ProgressInfo, Result, T extends TaskRunnable<I, ProgressInfo, Result>> extends Observable<Callbacks<I, ProgressInfo, Result, T>> {

        private void dispatchAddedToQueue(final T r, final int waitingCount, final int activeCount, Handler handler) {
            final Runnable run = () -> {
                synchronized (observers) {
                    for (Callbacks<I, ProgressInfo, Result, T> c : observers) {
                        c.onAddedToQueue(r, waitingCount, activeCount);
                    }
                }
            };
            if (handler != null) {
                handler.post(run);
            } else {
                run.run();
            }
        }

        private void dispatchBeforeExecute(final Thread t, final T r, final ExecInfo<I, ProgressInfo, Result, T> execInfo, final int waitingCount, final int activeCount, Handler handler) {
            final Runnable run = () -> {
                synchronized (observers) {
                    for (Callbacks<I, ProgressInfo, Result, T> c : observers) {
                        c.onBeforeExecute(t, r, execInfo, waitingCount, activeCount);
                    }
                }
            };
            if (handler != null) {
                handler.post(run);
            } else {
                run.run();
            }
        }

        private void dispatchAfterExecute(@NotNull final T r, @Nullable final Throwable t, @NotNull final ExecInfo<I, ProgressInfo, Result, T> execInfo, @NotNull final StatInfo<I, ProgressInfo, Result, T> statInfo, final int waitingCount, final int activeCount, Handler handler) {
            final Runnable run = () -> {
                synchronized (observers) {
                    for (Callbacks<I, ProgressInfo, Result, T> c : observers) {
                        c.onAfterExecute(r, t, execInfo, statInfo, waitingCount, activeCount);
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
