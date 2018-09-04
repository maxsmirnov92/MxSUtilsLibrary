package net.maxsmr.networkutils.loadutil.managers.base;


import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
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
import net.maxsmr.tasksutils.taskexecutor.TaskRunnable;
import net.maxsmr.tasksutils.taskexecutor.TaskRunnableExecutor;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static net.maxsmr.tasksutils.taskexecutor.TaskRunnableExecutor.DEFAULT_KEEP_ALIVE_TIME;


public abstract class BaseNetworkLoadManager<B extends LoadRunnableInfo.Body, LI extends LoadRunnableInfo<B>,
        R extends TaskRunnable<LI, Void, Void>> implements TaskRunnableExecutor.Callbacks<LI, Void, Void, R> {

    public static final int LOADS_NO_LIMIT = TaskRunnableExecutor.TASKS_NO_LIMIT;

    protected final BaseLogger logger = BaseLoggerHolder.getInstance().getLogger(getLoggerClass());

    protected final LoadObservable<LI> mLoadObservable = new LoadObservable<>();

    protected final TaskRunnableExecutor<LI, Void, Void, R> mExecutor;

    public BaseNetworkLoadManager(int limit, int concurrentLoadsCount, @Nullable AbstractSyncStorage<LI> storage,
                                  @Nullable TaskRunnable.ITaskResultValidator<LI, Void, Void, R> validator, @Nullable final TaskRunnable.ITaskRestorer<LI, Void, Void, R> restorer) {
        this(limit, concurrentLoadsCount, storage, validator, new Handler(Looper.getMainLooper()));
    }

    public BaseNetworkLoadManager(int limit, int concurrentLoadsCount, @Nullable AbstractSyncStorage<LI> storage,
                                  @Nullable TaskRunnable.ITaskResultValidator<LI, Void, Void, R> validator,
                                  @Nullable Handler callbacksHandler) {
        mExecutor = new TaskRunnableExecutor<>(limit, concurrentLoadsCount, DEFAULT_KEEP_ALIVE_TIME, TimeUnit.SECONDS, getClass().getSimpleName(), validator, storage, callbacksHandler);
        mExecutor.registerCallback(this);
    }

    public final boolean isReleased() {
        synchronized (mExecutor) {
            return mExecutor.isShutdown();
        }
    }

    protected final void checkReleased() {
        synchronized (mExecutor) {
            if (isReleased()) {
                throw new IllegalStateException(getClass() + " is released");
            }
        }
    }

    protected void doRestore(@NonNull final TaskRunnable.ITaskRestorer<LI, Void, Void, R> restorer) {
        mExecutor.restoreQueueByRestorer(restorer);
    }

    public final void registerLoadListener(@NonNull LoadListener<LI> listener) {
        checkReleased();
        mLoadObservable.registerObserver(listener);
    }

    public final void unregisterLoadListener(@NonNull LoadListener<LI> listener) {
        mLoadObservable.unregisterObserver(listener);
    }

    @Nullable
    public final LoadListener<LI> findLoadListenerById(int id) {
        return mLoadObservable.findLoadListenerById(id);
    }

    @NonNull
    public final Set<LoadListener<LI>> getLoadListeners() {
        return mLoadObservable.copyOfObservers();
    }

    private void clearLoadListeners() {
        mLoadObservable.unregisterAll();
    }

    public final void enqueueLoad(@NonNull LI rInfo) {
        logger.d("enqueueLoad(), rInfo=" + rInfo);
        synchronized (mExecutor) {
            if (rInfo.id < 0) {
                throw new IllegalArgumentException("incorrect load id: " + rInfo.id);
            }
            if (containsLoad(rInfo.id)) {
                throw new IllegalStateException("load with id " + rInfo.id + " already exists");
            }
            mExecutor.execute(newRunnable(rInfo));
        }
    }

    protected abstract R newRunnable(@NonNull LI rInfo);

    @Override
    public void onAddedToQueue(@NonNull R r, int waitingCount, int activeCount) {
        mLoadObservable.notifyLoadAddedToQueue(r.rInfo, waitingCount, activeCount);
    }

    @Override
    public void onBeforeExecute(@NonNull Thread t, @NonNull R r, @NonNull ExecInfo<LI, Void, Void, R> execInfo, int waitingCount, int activeCount) {

    }

    @Override
    public void onAfterExecute(@NonNull R r, Throwable t, @NonNull ExecInfo<LI, Void, Void, R> execInfo, @NonNull StatInfo<LI, Void, Void, R> statInfo, int waitingCount, int activeCount) {
        mLoadObservable.notifyLoadRemovedFromQueue(r.rInfo, mExecutor.getWaitingTasksCount(), mExecutor.getActiveTasksCount());
    }

    @NonNull
    public List<LI> getAllLoadRunnableInfos() {
        checkReleased();
        List<LI> loadInfos = new ArrayList<>();
        Set<R> loadRunnables = getAllLoadRunnables();
        for (R r : loadRunnables) {
            loadInfos.add(r.rInfo);
        }
        return Collections.unmodifiableList(loadInfos);
    }

    @NonNull
    public List<LI> getWaitingLoadRunnableInfos() {
        checkReleased();
        List<LI> loadInfos = new ArrayList<>();
        List<R> loadRunnables = getWaitingLoadRunnables();
        for (R r : loadRunnables) {
            loadInfos.add(r.rInfo);
        }
        return Collections.unmodifiableList(loadInfos);
    }

    @NonNull
    public List<LI> getActiveLoadRunnableInfos() {
        checkReleased();
        List<LI> loadInfos = new ArrayList<>();
        List<R> loadRunnables = getActiveLoadRunnables();
        for (R r : loadRunnables) {
            loadInfos.add(r.rInfo);
        }
        return Collections.unmodifiableList(loadInfos);
    }

    @NonNull
    protected Set<R> getAllLoadRunnables() {
        synchronized (mExecutor) {
            checkReleased();
            return mExecutor.getAllTasks();
        }
    }

    @NonNull
    protected List<R> getWaitingLoadRunnables() {
        synchronized (mExecutor) {
            checkReleased();
            return mExecutor.getWaitingTasks();
        }
    }

    @NonNull
    protected List<R> getActiveLoadRunnables() {
        synchronized (mExecutor) {
            checkReleased();
            return mExecutor.getActiveTasks();
        }
    }

    public boolean containsLoad(int id) {
        synchronized (mExecutor) {
            checkReleased();
            return mExecutor.containsTask(id);
        }
    }

    @SuppressWarnings("unchecked")
    public LI findLoadById(int id) {
        synchronized (mExecutor) {
            checkReleased();
            return mExecutor.findRunnableInfoById(id);
        }
    }


    public boolean isLoadRunning(int id) {
        synchronized (mExecutor) {
            checkReleased();
            return mExecutor.isTaskRunning(id);
        }
    }

    public boolean isLoadCancelled(int id) {
        synchronized (mExecutor) {
            checkReleased();
            return mExecutor.isTaskCancelled(id);
        }
    }

    public boolean cancelLoad(int id) {
        logger.d("cancelLoad(), id=" + id);
        synchronized (mExecutor) {
            checkReleased();
            return mExecutor.cancelTask(id);
        }
    }

    public void cancelAllLoads() {
        logger.d("cancelAllLoads()");
        synchronized (mExecutor) {
            checkReleased();
            mExecutor.cancelAllTasks();
        }
    }

    public void release() {
        logger.d("release()");
        synchronized (mExecutor) {
            if (!isReleased()) {
                clearLoadListeners();
                mExecutor.cancelAllTasks();
                mExecutor.shutdown();
                mExecutor.unregisterCallback(this);
            }
        }
    }

    protected abstract Class<?> getLoggerClass();

    public static boolean isResponseOk(@NonNull HttpURLConnection conn) throws IOException {
        return isResponseOk(conn.getResponseCode());
    }

    public static boolean isResponseOk(int responseCode) {
        return 200 <= responseCode && responseCode <= 299;
    }

    protected static class LoadObservable<I extends LoadRunnableInfo> extends Observable<LoadListener<I>> {

        @Nullable
        public final LoadListener<I> findLoadListenerById(final int id) {
            synchronized (observers) {
                return Predicate.Methods.find(getObservers(), element -> element != null && element.getId() == id);
            }
        }

        public final void notifyLoadAddedToQueue(@NonNull I info, int waitingLoads, int activeLoads) {
            synchronized (observers) {
                for (LoadListener<I> l : copyOfObservers()) {
                    l.onLoadAddedToQueue(info.id, waitingLoads, activeLoads);
                }
            }
        }

        public final void notifyLoadRemovedFromQueue(@NonNull I info, int waitingLoads, int activeLoads) {
            synchronized (observers) {
                for (LoadListener<I> l : copyOfObservers()) {
                    l.onLoadRemovedFromQueue(info.id, waitingLoads, activeLoads);
                }
            }
        }

        public final void notifyStateChanged(@NonNull final I info, @NonNull NetworkLoadManager.LoadProcessInfo loadProcessInfo, Throwable t) {
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
