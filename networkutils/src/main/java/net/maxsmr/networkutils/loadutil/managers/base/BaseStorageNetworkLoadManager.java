package net.maxsmr.networkutils.loadutil.managers.base;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import net.maxsmr.networkutils.loadstorage.LoadInfo;
import net.maxsmr.networkutils.loadstorage.storage.AbstractLoadStorage;
import net.maxsmr.networkutils.loadstorage.storage.collection.QueueLoadStorage;
import net.maxsmr.networkutils.loadutil.managers.LoadListener;
import net.maxsmr.networkutils.loadutil.managers.NetworkLoadManager;
import net.maxsmr.networkutils.loadutil.managers.base.info.LoadRunnableInfo;
import net.maxsmr.networkutils.loadutil.managers.base.info.WrappedLoadRunnableInfo;
import net.maxsmr.networkutils.loadutil.managers.ids.AbsIdHolder;
import net.maxsmr.tasksutils.ScheduledThreadPoolExecutorManager;
import net.maxsmr.tasksutils.taskexecutor.RunnableInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


public abstract class BaseStorageNetworkLoadManager<B extends LoadRunnableInfo.Body, I extends LoadInfo<B>> implements LoadListener<WrappedLoadRunnableInfo<B, I>>, AbstractLoadStorage.StorageListener {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    public static final int DEFAULT_UPLOAD_LIST_SYNCHRONIZE_INTERVAL = 20000;

    private int synchronizeInterval = DEFAULT_UPLOAD_LIST_SYNCHRONIZE_INTERVAL;

    private boolean allowRemoveFailedLoads = false;

    private boolean isReleased = false;

    @NonNull
    protected final NetworkLoadManager loadManager;

    @NonNull
    protected final QueueLoadStorage<I> uploadStorage;

    private final ScheduledThreadPoolExecutorManager uploadListSynchronizer = new ScheduledThreadPoolExecutorManager(ScheduledThreadPoolExecutorManager.ScheduleMode.FIXED_DELAY, getClass().getSimpleName() + "Synchronizer");

    public BaseStorageNetworkLoadManager(@NonNull NetworkLoadManager loadManager, @NonNull Class<I> clazzInstance, String path) {
        logger.debug("BaseStorageNetworkLoadManager(), loadManager=" + loadManager + ", clazzInstance=" + clazzInstance + ", path=" + path);
        this.uploadStorage = new QueueLoadStorage<>(clazzInstance, AbstractLoadStorage.MAX_SIZE_UNLIMITED, true, true, path, this);
        this.loadManager = loadManager;
        this.loadManager.addLoadListener((LoadListener) this);
        restartUploadSynchronizer();
    }

    protected final void checkReleased() {
        if (isReleased()) {
            throw new IllegalStateException(getClass().getSimpleName() + " was released");
        }
    }

    protected final synchronized boolean isReleased() {
        return isReleased;
    }

    protected abstract void releaseLoadManager();

    protected synchronized void release() {
        logger.debug("release()");
        checkReleased();
        stopUploadSynchronizer();
        loadManager.removeLoadListener((LoadListener) this);
        releaseLoadManager();
        uploadStorage.release();
        isReleased = true;
    }

    public int getSynchronizeInterval() {
        return synchronizeInterval;
    }

    public void setSynchronizeInterval(int synchronizeInterval) {
        if (synchronizeInterval != this.synchronizeInterval) {
            this.synchronizeInterval = synchronizeInterval;
            restartUploadSynchronizer();
        }
    }

    public boolean isAllowRemoveFailedLoads() {
        return allowRemoveFailedLoads;
    }

    public void setAllowRemoveFailedLoads(boolean allowRemoveFailedLoads) {
        this.allowRemoveFailedLoads = allowRemoveFailedLoads;
    }

    public final boolean isUploadSynchronizerRunning() {
        return uploadListSynchronizer.isRunning();
    }

    public final void restartUploadSynchronizer() {
        checkReleased();
        logger.debug("synchronize interval: " + synchronizeInterval);
        uploadListSynchronizer.addRunnableTask(new SynchronizeUploadListRunnable());
        uploadListSynchronizer.restart(synchronizeInterval);
    }

    public final void startUploadSynchronizer() {
        checkReleased();
        if (!isUploadSynchronizerRunning()) {
            restartUploadSynchronizer();
        }
    }

    public final void stopUploadSynchronizer() {
        checkReleased();
        uploadListSynchronizer.stop(false, 0);
    }

    public final boolean isStorageRestoreCompleted() {
        checkReleased();
        return uploadStorage.isRestoreCompleted();
    }

    @NonNull
    public final List<I> getStorageList() {
        if (!isStorageRestoreCompleted()) {
            throw new IllegalStateException("storage restore is not completed");
        }
        return uploadStorage.getAll();
    }

    /**
     * @return true if upload added to queue
     */
    protected final boolean enqueue(I uploadInfo) {
        logger.debug("enqueue(), uploadInfo=" + uploadInfo);
        if (uploadInfo == null) {
            logger.error("can't start upload: uploadInfo is null");
            return false;
        }
        AddMode mode = getAddMode(uploadInfo);
        switch (mode) {
            case LOAD_QUEUE:
                enqueueLoad(uploadInfo);
                break;
            case STORAGE:
                if (!uploadStorage.contains(uploadInfo)) {
                    uploadStorage.add(uploadInfo);
                    return true;
                }
                break;
            default:
                throw new RuntimeException("unknown mode: " + mode);
        }

        return false;
    }

    protected boolean enqueueLoad(I uploadInfo) {
        final WrappedLoadRunnableInfo<B, I> uploadRunnableInfo = makeWrappedLoadRunnableInfo(uploadInfo);
        if (uploadRunnableInfo == null) {
            logger.error("can't start upload: uploadRunnableInfo is null");
            return false;
        }
        if (!loadManager.containsLoad(uploadRunnableInfo.id)) {
            loadManager.enqueueLoad(uploadRunnableInfo);
            return true;
        }
        return false;
    }

    @Override
    public void onLoadAddedToQueue(int id, int waitingLoads, int activeLoads) {
        LoadRunnableInfo rInfo = loadManager.findLoadById(id);
        if (rInfo instanceof WrappedLoadRunnableInfo) { // && LoadManagersFacade.getInstance().loadIdsPool.contains(rInfo.id)
            uploadStorage.add(((WrappedLoadRunnableInfo<B, I>) rInfo).loadInfo);
        }
    }

    @Override
    public void onLoadRemovedFromQueue(int id, int waitingLoads, int activeLoads) {

    }

    @Override
    public final int getId(@NonNull WrappedLoadRunnableInfo<B, I> loadInfo) {
        return RunnableInfo.NO_ID;
    }

    @Override
    public long getProcessingNotifyInterval(@NonNull WrappedLoadRunnableInfo<B, I> loadInfo) {
        return LoadListener.DEFAULT_PROCESSING_NOTIFY_INTERVAL;
    }

    protected boolean allowRemoveFromStorageWhenRetriesExceeded() {
        return allowRemoveFailedLoads;
    }

    @NonNull
    protected AddMode getAddMode(I uploadInfo) {
        return AddMode.LOAD_QUEUE;
    }

    @Override
    public void onUpdateState(@NonNull STATE state, @NonNull WrappedLoadRunnableInfo<B, I> loadInfo, @NonNull NetworkLoadManager.LoadProcessInfo loadProcessInfo, @Nullable Throwable t) {
        switch (state) {
            case CANCELLED:
            case SUCCESS:
            case FAILED_RETRIES_EXCEEDED:
                if (uploadStorage.contains(loadInfo.id)) {
                    if (state != STATE.FAILED_RETRIES_EXCEEDED || loadInfo.body == null || loadInfo.body.isEmpty() || allowRemoveFromStorageWhenRetriesExceeded()) {
                        logger.debug("removing info with id " + loadInfo.id + " from storage...");
                        uploadStorage.removeById(loadInfo.id);
                    }
                }
                break;
        }
    }

    @Override
    public void onResponse(@NonNull WrappedLoadRunnableInfo<B, I> loadInfo, @NonNull NetworkLoadManager.LoadProcessInfo loadProcessInfo, @NonNull NetworkLoadManager.Response response) {
        logger.debug("onResponse(), loadInfo=" + loadInfo + ", loadProcessInfo=" + loadProcessInfo + ", response=" + response);
    }

    @Override
    public void onStorageSizeChanged(int currentSize, int previousSize) {
        logger.debug("onStorageSizeChanged(), currentSize=" + currentSize + ", previousSize=" + previousSize);
    }

    @Override
    public void onStorageRestored(int restoredElementsCount) {
        logger.debug("onStorageRestored(), restoredElementsCount=" + restoredElementsCount);
//        doSynchronizeWithUploadManager();
    }

    private void doSynchronizeWithUploadManager() {
        logger.debug("doSynchronizeWithUploadManager()");

        checkReleased();

        if (!isStorageRestoreCompleted()) {
            logger.warn("upload storage restore was not completed");
            return;
        }

        if (uploadStorage.isEmpty()) {
            logger.debug("upload storage is empty");
            return;
        }

        List<I> infos = uploadStorage.getAll();
        logger.debug("current upload " + uploadStorage.getLoadInfoClass().getSimpleName() + " list: " + infos);

//        logger.debug("current upload queue: " + loadManager.getQueue());
        if (!handleStorageSynchronize(infos)) {
            for (I info : infos) {
                enqueueLoad(info);
            }
        }
    }

    protected boolean handleStorageSynchronize(@NonNull List<I> infos) {
        return false;
    }

    @Nullable
    protected abstract WrappedLoadRunnableInfo<B, I> makeWrappedLoadRunnableInfo(@NonNull I info);

    public static int newId(@NonNull AbsIdHolder idHolder) {
        return idHolder.incrementAndGet();
    }

    protected enum AddMode {
        STORAGE, LOAD_QUEUE
    }

    private class SynchronizeUploadListRunnable implements Runnable {

        @Override
        public void run() {
            doSynchronizeWithUploadManager();
        }
    }
}
