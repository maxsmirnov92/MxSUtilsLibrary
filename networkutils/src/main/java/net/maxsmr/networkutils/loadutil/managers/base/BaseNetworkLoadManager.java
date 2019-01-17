package net.maxsmr.networkutils.loadutil.managers.base;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;

import net.maxsmr.commonutils.data.Observable;
import net.maxsmr.commonutils.data.Predicate;
import net.maxsmr.commonutils.logger.BaseLogger;
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder;
import net.maxsmr.networkutils.loadutil.managers.LoadListener;
import net.maxsmr.networkutils.loadutil.managers.NetworkLoadManager;
import net.maxsmr.networkutils.loadutil.managers.base.info.LoadRunnableInfo;
import net.maxsmr.tasksutils.storage.sync.AbstractSyncStorage;
import net.maxsmr.tasksutils.taskexecutor.ExecInfo;
import net.maxsmr.tasksutils.taskexecutor.RunnableInfo;
import net.maxsmr.tasksutils.taskexecutor.StatInfo;
import net.maxsmr.tasksutils.taskexecutor.TaskRunnableExecutor;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static net.maxsmr.tasksutils.taskexecutor.TaskRunnableExecutor.DEFAULT_KEEP_ALIVE_TIME;

public abstract class BaseNetworkLoadManager<B extends LoadRunnableInfo.Body, LI extends LoadRunnableInfo<B>, Result,
        R extends BaseNetworkLoadManager.TaskRunnable<LI, Void, Result>> implements TaskRunnableExecutor.Callbacks<LI, Void, Result, R> {

    public static final int LOADS_NO_LIMIT = TaskRunnableExecutor.TASKS_NO_LIMIT;

    protected final BaseLogger logger = BaseLoggerHolder.getInstance().getLogger(getLoggerClass());

    protected final LoadObservable<LI> loadObservable = new LoadObservable<>();

    protected final TaskRunnableExecutor<LI, Void, Result, R> executor;

    public BaseNetworkLoadManager() {
        this(1, 1, null, null, (Handler) null);
    }

    public BaseNetworkLoadManager(int limit, int concurrentLoadsCount, @Nullable AbstractSyncStorage<LI> storage,
                                  @Nullable TaskRunnable.ITaskResultValidator<LI, Void, Result, R> validator, @Nullable final TaskRunnable.ITaskRestorer<LI, Void, Result, R> restorer) {
        this(limit, concurrentLoadsCount, storage, validator, new Handler(Looper.getMainLooper()));
    }

    public BaseNetworkLoadManager(int limit, int concurrentLoadsCount, @Nullable AbstractSyncStorage<LI> storage,
                                  @Nullable TaskRunnable.ITaskResultValidator<LI, Void, Result, R> validator,
                                  @Nullable Handler callbacksHandler) {
        executor = new TaskRunnableExecutor<>(limit, concurrentLoadsCount, DEFAULT_KEEP_ALIVE_TIME, TimeUnit.SECONDS, getClass().getSimpleName(), validator, storage, callbacksHandler);
        executor.registerCallback(this);
    }

    public final boolean isReleased() {
        synchronized (executor) {
            return executor.isShutdown();
        }
    }

    protected final void checkReleased() {
        synchronized (executor) {
            if (isReleased()) {
                throw new IllegalStateException(getClass() + " is released");
            }
        }
    }

    protected void doRestore(@NotNull final TaskRunnable.ITaskRestorer<LI, Void, Result, R> restorer) {
        executor.restoreQueueByRestorer(restorer);
    }

    public final void registerLoadListener(@NotNull LoadListener<LI> listener) {
        checkReleased();
        loadObservable.registerObserver(listener);
    }

    public final void unregisterLoadListener(@NotNull LoadListener<LI> listener) {
        loadObservable.unregisterObserver(listener);
    }

    @Nullable
    public final LoadListener<LI> findLoadListenerById(int id) {
        return loadObservable.findLoadListenerById(id);
    }

    @NotNull
    public final Set<LoadListener<LI>> getLoadListeners() {
        return loadObservable.copyOfObservers();
    }

    private void clearLoadListeners() {
        loadObservable.unregisterAll();
    }

    public void enqueueLoad(@NotNull LI rInfo) {
        logger.d("enqueueLoad(), rInfo=" + rInfo);
        synchronized (executor) {
            if (rInfo.id < 0) {
                throw new IllegalArgumentException("incorrect load id: " + rInfo.id);
            }
            if (containsLoad(rInfo.id)) {
                throw new IllegalStateException("load with id " + rInfo.id + " already exists");
            }
            executor.execute(newRunnable(rInfo));
        }
    }

    @Nullable
    public Result runLoad(@NotNull LI rInfo) {
        R r = newRunnable(rInfo);
        try {
            return r.doWork();
        } catch (Throwable e) {
            r.onTaskFailed(e, false);
            return null;
        }
    }

    @NotNull
    protected abstract R newRunnable(@NotNull LI rInfo);

    @Override
    public void onAddedToQueue(@NotNull R r, int waitingCount, int activeCount) {
        loadObservable.notifyLoadAddedToQueue(r.rInfo, waitingCount, activeCount);
    }

    @Override
    public void onBeforeExecute(@NotNull Thread t, @NotNull R r, @NotNull ExecInfo<LI, Void, Result, R> execInfo, int waitingCount, int activeCount) {

    }

    @Override
    public void onAfterExecute(@NotNull R r, Throwable t, @NotNull ExecInfo<LI, Void, Result, R> execInfo, @NotNull StatInfo<LI, Void, Result, R> statInfo, int waitingCount, int activeCount) {
        loadObservable.notifyLoadRemovedFromQueue(r.rInfo, executor.getWaitingTasksCount(), executor.getActiveTasksCount());
    }

    @NotNull
    public List<LI> getAllLoadRunnableInfos() {
        checkReleased();
        List<LI> loadInfos = new ArrayList<>();
        Set<R> loadRunnables = getAllLoadRunnables();
        for (R r : loadRunnables) {
            loadInfos.add(r.rInfo);
        }
        return Collections.unmodifiableList(loadInfos);
    }

    @NotNull
    public List<LI> getWaitingLoadRunnableInfos() {
        checkReleased();
        List<LI> loadInfos = new ArrayList<>();
        List<R> loadRunnables = getWaitingLoadRunnables();
        for (R r : loadRunnables) {
            loadInfos.add(r.rInfo);
        }
        return Collections.unmodifiableList(loadInfos);
    }

    @NotNull
    public List<LI> getActiveLoadRunnableInfos() {
        checkReleased();
        List<LI> loadInfos = new ArrayList<>();
        List<R> loadRunnables = getActiveLoadRunnables();
        for (R r : loadRunnables) {
            loadInfos.add(r.rInfo);
        }
        return Collections.unmodifiableList(loadInfos);
    }

    @NotNull
    protected Set<R> getAllLoadRunnables() {
        synchronized (executor) {
            checkReleased();
            return executor.getAllTasks();
        }
    }

    @NotNull
    protected List<R> getWaitingLoadRunnables() {
        synchronized (executor) {
            checkReleased();
            return executor.getWaitingTasks();
        }
    }

    @NotNull
    protected List<R> getActiveLoadRunnables() {
        synchronized (executor) {
            checkReleased();
            return executor.getActiveTasks();
        }
    }

    public boolean containsLoad(int id) {
        synchronized (executor) {
            checkReleased();
            return executor.containsTask(id);
        }
    }

    @SuppressWarnings("unchecked")
    public LI findLoadById(int id) {
        synchronized (executor) {
            checkReleased();
            return executor.findRunnableInfoById(id);
        }
    }


    public boolean isLoadRunning(int id) {
        synchronized (executor) {
            checkReleased();
            return executor.isTaskRunning(id);
        }
    }

    public boolean isLoadCancelled(int id) {
        synchronized (executor) {
            checkReleased();
            return executor.isTaskCancelled(id);
        }
    }

    public boolean cancelLoad(int id) {
        logger.d("cancelLoad(), id=" + id);
        synchronized (executor) {
            checkReleased();
            return executor.cancelTask(id);
        }
    }

    public void cancelAllLoads() {
        logger.d("cancelAllLoads()");
        synchronized (executor) {
            checkReleased();
            executor.cancelAllTasks();
        }
    }

    public void release() {
        logger.d("release()");
        synchronized (executor) {
            if (!isReleased()) {
                clearLoadListeners();
                executor.cancelAllTasks();
                executor.shutdown();
                executor.unregisterCallback(this);
            }
        }
    }

    protected abstract Class<?> getLoggerClass();

    public static boolean isResponseOk(@NotNull HttpURLConnection conn) throws IOException {
        return isResponseOk(conn.getResponseCode());
    }

    public static boolean isResponseOk(int responseCode) {
        return 200 <= responseCode && responseCode <= 299;
    }

    public static abstract class TaskRunnable<I extends RunnableInfo, ProgressInfo, Result> extends net.maxsmr.tasksutils.taskexecutor.TaskRunnable<I, ProgressInfo, Result> {

        public TaskRunnable(@NotNull I rInfo) {
            super(rInfo);
        }

        @Override
        public final int getRetryLimit() {
            return RETRY_DISABLED;
        }

        @Override
        public final long getRetryDelayInMs() {
            return 0;
        }
    }

    protected static class LoadObservable<I extends LoadRunnableInfo> extends Observable<LoadListener<I>> {

        @Nullable
        public final LoadListener<I> findLoadListenerById(final int id) {
            synchronized (observers) {
                return Predicate.Methods.find(getObservers(), element -> element != null && element.getId() == id);
            }
        }

        public final void notifyLoadAddedToQueue(@NotNull I info, int waitingLoads, int activeLoads) {
            synchronized (observers) {
                for (LoadListener<I> l : copyOfObservers()) {
                    l.onLoadAddedToQueue(info.id, waitingLoads, activeLoads);
                }
            }
        }

        public final void notifyLoadRemovedFromQueue(@NotNull I info, int waitingLoads, int activeLoads) {
            synchronized (observers) {
                for (LoadListener<I> l : copyOfObservers()) {
                    l.onLoadRemovedFromQueue(info.id, waitingLoads, activeLoads);
                }
            }
        }

        public final void notifyStateChanged(@NotNull final I info, @NotNull NetworkLoadManager.LoadProcessInfo loadProcessInfo, Throwable t) {
            synchronized (observers) {
                LoadListener<I> l = Predicate.Methods.find(copyOfObservers(), listener -> {
                    int id = listener.getId(info);
                    return id == RunnableInfo.NO_ID || id == info.id;
                });
                if (l != null) {
                    l.onUpdateState(info, loadProcessInfo, t);
                }
            }
        }
    }
}
