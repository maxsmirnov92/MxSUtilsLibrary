package net.maxsmr.networkutils.loadutil.executors.base;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

import net.maxsmr.tasksutils.AbstractSyncThreadPoolExecutor;
import net.maxsmr.tasksutils.taskrunnable.RunnableInfo;
import net.maxsmr.tasksutils.taskrunnable.TaskRunnable;


public abstract class LoadExecutor<I extends LoadRunnableInfo, R extends TaskRunnable<I>> extends AbstractSyncThreadPoolExecutor {

    private static final Logger logger = LoggerFactory.getLogger(LoadExecutor.class);

    protected static final int BUF_SIZE = 4096;

    public final static int LOADS_NO_LIMIT = 0;
    public final static int DEFAULT_LOADS_LIMIT = LOADS_NO_LIMIT;
    public final int loadsLimit;

    private final Class<R> rClass;

    public LoadExecutor(String name, int limit, int corePoolSize, Class<R> rClass) {
        super(corePoolSize, corePoolSize, DEFAULT_KEEP_ALIVE_TIME, TimeUnit.SECONDS, name, false, null);
        this.loadsLimit = limit >= 0 ? limit : DEFAULT_LOADS_LIMIT;
        this.rClass = rClass;
    }

    public static boolean isResponseOk(@NonNull HttpURLConnection conn) throws IOException {
        return isResponseOk(conn.getResponseCode());
    }

    public static boolean isResponseOk(int responseCode) {
        return 200 <= responseCode && responseCode <= 299;
    }

    @Override
    protected final boolean restoreTaskRunnablesFromFiles() {
        return false;
    }

    protected final LinkedList<LoadFileListener<I>> loadListeners = new LinkedList<>();

    public void addLoadListener(LoadFileListener<I> listener) throws NullPointerException {

        if (listener == null) {
            throw new NullPointerException();
        }

        synchronized (loadListeners) {
            if (!loadListeners.contains(listener)) {
                logger.debug("adding listener " + listener + "...");
                loadListeners.add(listener);
            }
        }
    }

    public void removeLoadListener(LoadFileListener<I> listener) {
        synchronized (loadListeners) {
            if (loadListeners.contains(listener)) {
                logger.debug("removing listener " + listener + "...");
                loadListeners.remove(listener);
            }
        }
    }

    protected void notifyStateChanged(@NonNull LoadFileListener.STATE state, @NonNull I info, long estimatedTime, float currentKBytes, float totalKBytes, Throwable t) {
        synchronized (loadListeners) {
            if (loadListeners.size() > 0) {
                for (LoadFileListener<I> l : loadListeners) {
                    int id = l.getId(info);
                    if (id == RunnableInfo.NO_ID || id == info.id) {
                        l.onUpdateState(state, info, estimatedTime, currentKBytes, totalKBytes, t);
                    }
                }
            }
        }
    }

    private final LinkedList<I> runnableInfos = new LinkedList<>();

    /**
     * @return copy of actual list containing RunnableInfo
     */
    public LinkedList<I> getRunnableInfos() {
        return new LinkedList<>(runnableInfos);
    }

    @Nullable
    public I findRunnableInfoById(int id) {
        synchronized (runnableInfos) {
            if (runnableInfos.size() > 0) {
                for (I i : runnableInfos) {
                    if (i.id == id) {
                        return i;
                    }
                }
            }
        }
        return null;
    }

    protected synchronized final void doActionOnFile(I rInfo) throws NullPointerException {
        logger.debug("doActionFile(), rInfo=" + rInfo);

        if (rInfo == null)
            throw new NullPointerException("rInfo is null");

        execute(newRunnable(rInfo));
    }

    protected abstract R newRunnable(I rInfo);

    @SuppressWarnings("unchecked")
    @Override
    public void execute(Runnable command) {

        if (!isRunning()) {
            throw new IllegalStateException("can't run " + command + ": not running");
        }

        if (command == null) {
            throw new NullPointerException("command is null");
        }

        if (!rClass.isAssignableFrom(command.getClass())) {
            throw new IllegalArgumentException("command " + command.getClass() + " is not instance of " + rClass);
        }

        final I runnableInfo = ((R) command).rInfo;

//        if (!infoClass.isAssignableFrom(runnableInfo.getClass())) {
//            throw new RuntimeException("runnableInfo " + runnableInfo.getClass() + " is not instance of " + infoClass);
//        }

        synchronized (runnableInfos) {

            if (loadsLimit != LOADS_NO_LIMIT && runnableInfos.size() >= loadsLimit) {
                logger.error("can't add new task: loads limit exceeded: " + loadsLimit);
                return;
            }

            if (findRunnableInfoById(runnableInfo.id) != null) {
                logger.error("can't add new task: task list already contains runnable with id " + runnableInfo.id);
                return;
            }

            if (!runnableInfos.contains(runnableInfo)) {
                runnableInfos.add(runnableInfo);
            }

            synchronized (loadListeners) {
                if (loadListeners.size() > 0) {
                    for (LoadFileListener l : loadListeners) {
                        l.onActiveLoadsCountChanged(runnableInfos.size());
                    }
                }
            }
        }

        super.execute(command);
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        try {
            if (r == null) {
                throw new NullPointerException("r is null");
            }

            if (!rClass.isAssignableFrom(r.getClass())) {
                throw new IllegalArgumentException("command " + r.getClass() + " is not instance of " + rClass);
            }

            final I runnableInfo = ((R) r).rInfo;

//            if (!infoClass.isAssignableFrom(runnableInfo.getClass())) {
//                throw new RuntimeException("runnableInfo " + runnableInfo.getClass() + " is not instance of " + infoClass);
//            }

            synchronized (runnableInfos) {
                if (runnableInfos.contains(runnableInfo)) {
                    runnableInfos.remove(runnableInfo);
                }
            }

            synchronized (loadListeners) {
                if (loadListeners.size() > 0) {
                    for (LoadFileListener l : loadListeners) {
                        l.onActiveLoadsCountChanged(runnableInfos.size());
                    }
                }
            }

        } finally {
            super.afterExecute(r, t);
        }
    }

    @Override
    public void shutdown() {
        logger.debug("shutdown()");
        cancelAllLoads();
        super.shutdown();
    }


    public boolean cancelLoad(int id) throws IllegalArgumentException {
        logger.debug("cancelLoad(), id=" + id);

        synchronized (runnableInfos) {
            I info = findRunnableInfoById(id);
            if (info != null) {
                info.cancel();
            }
        }

        return false;
    }

    public boolean cancelLoad(I rInfo) throws NullPointerException {
        logger.debug("cancelLoad(), rInfo=" + rInfo);

        if (rInfo == null)
            throw new NullPointerException("rInfo is null");

        return cancelLoad(rInfo.id);
    }

    public boolean cancelAllLoads() {
        logger.debug("cancelAllLoads()");

        synchronized (runnableInfos) {
            if (runnableInfos.size() > 0) {
                for (I rInfo : runnableInfos) {
                    rInfo.cancel();
                    return true;
                }
            }
        }

        return false;
    }


}
