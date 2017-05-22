package net.maxsmr.networkutils.loadutil.managers.base;


import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import net.maxsmr.commonutils.data.Observable;
import net.maxsmr.networkutils.loadutil.managers.LoadListener;
import net.maxsmr.networkutils.loadutil.managers.NetworkLoadManager;
import net.maxsmr.networkutils.loadutil.managers.base.info.LoadRunnableInfo;
import net.maxsmr.tasksutils.taskexecutor.AbsTaskRunnableExecutor;
import net.maxsmr.tasksutils.taskexecutor.ExecInfo;
import net.maxsmr.tasksutils.taskexecutor.RunnableInfo;
import net.maxsmr.tasksutils.taskexecutor.TaskRunnable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;


/**
 * @author maxsmirnov
 */
public abstract class BaseNetworkLoadManager<I extends LoadRunnableInfo, R extends TaskRunnable<I>> implements AbsTaskRunnableExecutor.Callbacks {

    private static final Logger logger = LoggerFactory.getLogger(BaseNetworkLoadManager.class);

    protected final AbsTaskRunnableExecutor mExecutor;

    protected final LoadObservable<I> mLoadObservable = new LoadObservable<>();

    public BaseNetworkLoadManager(int limit, int concurrentDownloadsCount) {
        mExecutor = new AbsTaskRunnableExecutor(limit, concurrentDownloadsCount, AbsTaskRunnableExecutor.DEFAULT_KEEP_ALIVE_TIME, TimeUnit.SECONDS, getClass().getName(), false, null) {
            @Override
            protected boolean restoreTaskRunnablesFromFiles() {
                return false;
            }
        };
        mExecutor.getCallbacksObservable().registerObserver(this);
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

    @Nullable
    public final LoadListener<I> findLoadListenerById(int id) {
        return mLoadObservable.findLoadListenerById(id);
    }

    public final void addLoadListener(@NonNull LoadListener<I> listener) {
        checkReleased();
        mLoadObservable.registerObserver(listener);
    }

    public final void removeLoadListener(@NonNull LoadListener<I> listener) {
        mLoadObservable.unregisterObserver(listener);
    }

    private void clearLoadListeners() {
        mLoadObservable.unregisterAll();
    }

    public final void enqueueLoad(@NonNull I rInfo) {
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

    protected abstract R newRunnable(I rInfo);

    @Override
    public void onAddedToQueue(TaskRunnable<?> r, int waitingLoads, int activeLoads) {
        mLoadObservable.notifyLoadAddedToQueue((I) r.rInfo, waitingLoads, activeLoads);
    }

    @Override
    public void onBeforeExecute(Thread t, TaskRunnable<?> r, @NonNull ExecInfo execInfo) {

    }

    @Override
    public void onAfterExecute(TaskRunnable<?> r, Throwable t, @NonNull ExecInfo execInfo) {
        mLoadObservable.notifyLoadRemovedFromQueue((I) r.rInfo, mExecutor.getWaitingTasksCount(), mExecutor.getActiveTasksCount());
    }

    @NonNull
    private List<R> getWaitingLoadRunnables() {
        synchronized (mExecutor) {
            checkReleased();
            List<R> loadRunnables = new ArrayList<>();
            List<TaskRunnable<?>> runnables = mExecutor.getWaitingTasks();
            for (TaskRunnable<?> r : runnables) {
                loadRunnables.add((R) r);
            }
            return Collections.unmodifiableList(loadRunnables);
        }
    }

    @NonNull
    public List<I> getWaitingLoadRunnableInfos() {
        synchronized (mExecutor) {
            checkReleased();
            List<I> loadInfos = new ArrayList<>();
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
            List<R> loadRunnables = new ArrayList<>();
            List<TaskRunnable<?>> runnables = mExecutor.getActiveTasks();
            for (TaskRunnable<?> r : runnables) {
                loadRunnables.add((R) r);
            }
            return Collections.unmodifiableList(loadRunnables);
        }
    }

    @NonNull
    public List<I> getActiveLoadRunnableInfos() {
        synchronized (mExecutor) {
            checkReleased();
            List<I> loadInfos = new ArrayList<>();
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
    public I findLoadById(int id) {
        synchronized (mExecutor) {
            checkReleased();
            return (I) mExecutor.findRunnableInfoById(id);
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
                mExecutor.getCallbacksObservable().unregisterObserver(this);
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
        public final LoadListener<I> findLoadListenerById(int id) {
            LoadListener<I> target = null;
            synchronized (mObservers) {
                for (LoadListener<I> l : copyOfObservers()) {
                    if (l != null && l.getId() == id) {
                        target = l;
                        break;
                    }
                }
            }
            return target;
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

        public final void notifyStateChanged(@NonNull LoadListener.STATE state, @NonNull I info, @NonNull NetworkLoadManager.LoadProcessInfo loadProcessInfo, Throwable t) {
            synchronized (mObservers) {
                for (LoadListener<I> l : copyOfObservers()) {
                    int id = l.getId(info);
                    if (id == RunnableInfo.NO_ID || id == info.id) {
                        l.onUpdateState(state, info, loadProcessInfo, t);
                    }
                }
            }
        }
    }
}
