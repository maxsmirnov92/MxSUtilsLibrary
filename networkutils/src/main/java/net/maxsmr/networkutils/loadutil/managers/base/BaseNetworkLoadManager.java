package net.maxsmr.networkutils.loadutil.managers.base;


import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import net.maxsmr.commonutils.data.Observable;
import net.maxsmr.commonutils.data.Predicate;
import net.maxsmr.networkutils.loadutil.managers.LoadListener;
import net.maxsmr.networkutils.loadutil.managers.NetworkLoadManager;
import net.maxsmr.networkutils.loadutil.managers.base.info.LoadRunnableInfo;
import net.maxsmr.tasksutils.storage.sync.AbstractSyncStorage;
import net.maxsmr.tasksutils.taskexecutor.ExecInfo;
import net.maxsmr.tasksutils.taskexecutor.RunnableInfo;
import net.maxsmr.tasksutils.taskexecutor.TaskRunnable;
import net.maxsmr.tasksutils.taskexecutor.TaskRunnableExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static net.maxsmr.tasksutils.taskexecutor.TaskRunnableExecutor.DEFAULT_KEEP_ALIVE_TIME;


/**
 * @author maxsmirnov
 */
public abstract class BaseNetworkLoadManager<LI extends LoadRunnableInfo, R extends TaskRunnable<LI>> implements TaskRunnableExecutor.Callbacks<LI, R> {

    private static final Logger logger = LoggerFactory.getLogger(BaseNetworkLoadManager.class);

    protected final TaskRunnableExecutor<LI, R> mExecutor;

    protected final LoadObservable<LI> mLoadObservable = new LoadObservable<>();

    public BaseNetworkLoadManager(int limit, int concurrentLoadsCount, @Nullable AbstractSyncStorage<LI> storage,
                                  @Nullable TaskRunnable.ITaskResultValidator<LI, R> validator, @Nullable final TaskRunnable.ITaskRestorer<LI, R> restorer) {
        this(limit, concurrentLoadsCount, storage, validator, restorer, new Handler(Looper.getMainLooper()));
    }

    public BaseNetworkLoadManager(int limit, int concurrentLoadsCount, @Nullable AbstractSyncStorage<LI> storage,
                                  @Nullable TaskRunnable.ITaskResultValidator<LI, R> validator, @Nullable final TaskRunnable.ITaskRestorer<LI, R> restorer,
                                  @Nullable Handler callbacksHandler) {
        mExecutor = new TaskRunnableExecutor<>(limit, concurrentLoadsCount, DEFAULT_KEEP_ALIVE_TIME, TimeUnit.SECONDS, getClass().getName(), validator, storage, restorer, callbacksHandler);
        mExecutor.registerCallback(this);
    }

    public final boolean isReleased() {
        synchronized (mExecutor) {
            return !mExecutor.isRunning();
        }
    }

    protected final void checkReleased() {
        synchronized (mExecutor) {
            if (isReleased()) {
                throw new IllegalStateException(getClass() + " is released");
            }
        }
    }

    public final void addLoadListener(@NonNull LoadListener<LI> listener) {
        checkReleased();
        mLoadObservable.registerObserver(listener);
    }

    public final void removeLoadListener(@NonNull LoadListener<LI> listener) {
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
        logger.debug("enqueueLoad(), rInfo=" + rInfo);
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

    protected abstract R newRunnable(LI rInfo);

    @Override
    public void onAddedToQueue(R r, int waitingCount, int activeCount) {
        mLoadObservable.notifyLoadAddedToQueue(r.rInfo, waitingCount, activeCount);
    }

    @Override
    public void onBeforeExecute(Thread t, R r, ExecInfo<LI, R> execInfo, int waitingCount, int activeCount) {

    }

    @Override
    public void onAfterExecute(R r, Throwable t, ExecInfo<LI, R> execInfo, int waitingCount, int activeCount) {
        mLoadObservable.notifyLoadRemovedFromQueue(r.rInfo, mExecutor.getWaitingTasksCount(), mExecutor.getActiveTasksCount());
    }


    @NonNull
    private List<R> getWaitingLoadRunnables() {
        synchronized (mExecutor) {
            checkReleased();
            return mExecutor.getWaitingTasks();
        }
    }

    @NonNull
    public List<LI> getWaitingLoadRunnableInfos() {
        synchronized (mExecutor) {
            checkReleased();
            List<LI> loadInfos = new ArrayList<>();
            List<R> loadRunnables = getWaitingLoadRunnables();
            for (R r : loadRunnables) {
                loadInfos.add(r.rInfo);
            }
            return Collections.unmodifiableList(loadInfos);
        }
    }

    @NonNull
    private List<R> getActiveLoadRunnables() {
        synchronized (mExecutor) {
            checkReleased();
            return mExecutor.getActiveTasks();
        }
    }

    @NonNull
    public List<LI> getActiveLoadRunnableInfos() {
        synchronized (mExecutor) {
            checkReleased();
            List<LI> loadInfos = new ArrayList<>();
            List<R> loadRunnables = getActiveLoadRunnables();
            for (R r : loadRunnables) {
                loadInfos.add(r.rInfo);
            }
            return Collections.unmodifiableList(loadInfos);
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
        logger.debug("cancelLoad(), id=" + id);
        synchronized (mExecutor) {
            checkReleased();
            return mExecutor.cancelTask(id);
        }
    }

    public void cancelAllLoads() {
        logger.debug("cancelAllLoads()");
        synchronized (mExecutor) {
            checkReleased();
            mExecutor.cancelAllTasks();
        }
    }

    public void release() {
        logger.debug("release()");
        synchronized (mExecutor) {
            if (!isReleased()) {
                clearLoadListeners();
                mExecutor.cancelAllTasks();
                mExecutor.shutdown();
                mExecutor.unregisterCallback(this);
            }
        }
    }


    public static boolean isResponseOk(@NonNull HttpURLConnection conn) throws IOException {
        return isResponseOk(conn.getResponseCode());
    }

    public static boolean isResponseOk(int responseCode) {
        return 200 <= responseCode && responseCode <= 299;
    }

    protected static class LoadObservable<I extends LoadRunnableInfo> extends Observable<LoadListener<I>> {

        @Nullable
        public final LoadListener<I> findLoadListenerById(final int id) {
            synchronized (mObservers) {
                return Predicate.Methods.find(getObservers(), new Predicate<LoadListener<I>>() {
                    @Override
                    public boolean apply(LoadListener<I> element) {
                        return element != null && element.getId() == id;
                    }
                });
            }
        }

        public final void notifyLoadAddedToQueue(@NonNull I info, int waitingLoads, int activeLoads) {
            synchronized (mObservers) {
                for (LoadListener<I> l : copyOfObservers()) {
                    l.onLoadAddedToQueue(info.id, waitingLoads, activeLoads);
                }
            }
        }

        public final void notifyLoadRemovedFromQueue(@NonNull I info, int waitingLoads, int activeLoads) {
            synchronized (mObservers) {
                for (LoadListener<I> l : copyOfObservers()) {
                    l.onLoadRemovedFromQueue(info.id, waitingLoads, activeLoads);
                }
            }
        }

        public final void notifyStateChanged(@NonNull final I info, @NonNull NetworkLoadManager.LoadProcessInfo loadProcessInfo, Throwable t) {
            synchronized (mObservers) {
                LoadListener<I> l = Predicate.Methods.find(copyOfObservers(), new Predicate<LoadListener<I>>() {
                    @Override
                    public boolean apply(LoadListener<I> listener) {
                        int id = listener.getId(info);
                        return id == RunnableInfo.NO_ID || id == info.id;
                    }
                });
                if (l != null) {
                    l.onUpdateState(info, loadProcessInfo, t);
                }
            }
        }
    }
}
