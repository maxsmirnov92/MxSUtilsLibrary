package net.maxsmr.networkutils.loadutil.managers.base;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import net.maxsmr.networkutils.loadstorage.LoadInfo;
import net.maxsmr.networkutils.loadutil.managers.LoadListener;
import net.maxsmr.networkutils.loadutil.managers.NetworkLoadManager;
import net.maxsmr.networkutils.loadutil.managers.base.info.LoadRunnableInfo;
import net.maxsmr.networkutils.loadutil.managers.base.info.WrappedLoadRunnableInfo;
import net.maxsmr.tasksutils.ScheduledThreadPoolExecutorManager;
import net.maxsmr.tasksutils.storage.sync.AbstractSyncStorage;
import net.maxsmr.tasksutils.storage.sync.collection.QueueSyncStorage;
import net.maxsmr.tasksutils.taskexecutor.RunnableInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static net.maxsmr.tasksutils.storage.sync.AbstractSyncStorage.MAX_SIZE_UNLIMITED;

@Deprecated
public abstract class BaseStorageNetworkLoadManager<B extends LoadRunnableInfo.Body, I extends LoadInfo<B>>
        implements LoadListener<WrappedLoadRunnableInfo<B, I>>, AbstractSyncStorage.IStorageListener {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    public static final int DEFAULT_UPLOAD_LIST_SYNCHRONIZE_INTERVAL = 20000;

    private int synchronizeInterval = DEFAULT_UPLOAD_LIST_SYNCHRONIZE_INTERVAL;

    private boolean allowRemoveFailedLoads = false;

    private boolean isReleased = false;

    @NonNull
    protected final NetworkLoadManager<WrappedLoadRunnableInfo<B, I>> loadManager;

    @NonNull
    protected final QueueSyncStorage<WrappedLoadRunnableInfo<B, I>> uploadStorage;

    private final ScheduledThreadPoolExecutorManager uploadListSynchronizer = new ScheduledThreadPoolExecutorManager(ScheduledThreadPoolExecutorManager.ScheduleMode.FIXED_DELAY, getClass().getSimpleName() + "Synchronizer");

    public BaseStorageNetworkLoadManager(@NonNull NetworkLoadManager loadManager, @NonNull Class<WrappedLoadRunnableInfo<B, I>> clazzInstance, String path) {
        logger.debug("BaseStorageNetworkLoadManager(), loadManager=" + loadManager + ", clazzInstance=" + clazzInstance + ", path=" + path);
        this.uploadStorage = new QueueSyncStorage<>(path, "dat", clazzInstance, true,
                MAX_SIZE_UNLIMITED, new AbstractSyncStorage.IAddRule<WrappedLoadRunnableInfo<B, I>>() {
            @Override
            public boolean allowAddIfFull() {
                return false;
            }

            @Override
            public void removeAny(AbstractSyncStorage fromStorage) {
                fromStorage.removeFirst();
            }
        });
        this.loadManager = loadManager;
        this.loadManager.addLoadListener(this);
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
        loadManager.removeLoadListener(this);
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
        List<I> storageList = new ArrayList<>();
        for (WrappedLoadRunnableInfo<B, I> i : uploadStorage.getAll()) {
            if (i != null) {
                storageList.add(i.loadInfo);
            }
        }
        return storageList;
    }

    protected boolean enqueueLoad(I uploadInfo) {
        checkReleased();
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
        WrappedLoadRunnableInfo<B, I> rInfo = loadManager.findLoadById(id);
        if (rInfo != null) {
            uploadStorage.addLast(rInfo);
        }
    }

    @Override
    public void onLoadRemovedFromQueue(int id, int waitingLoads, int activeLoads) {

    }

    @Override
    public int getId() {
        return RunnableInfo.NO_ID;
    }

    @Override
    public long getProcessingNotifyInterval(@NonNull WrappedLoadRunnableInfo<B, I> loadInfo) {
        return LoadListener.DEFAULT_PROCESSING_NOTIFY_INTERVAL;
    }

    @Override
    public void onUpdateState(@NonNull WrappedLoadRunnableInfo<B, I> loadInfo, @NonNull NetworkLoadManager.LoadProcessInfo loadProcessInfo, @Nullable Throwable t) {
        STATE state = loadProcessInfo.getState();
        switch (state) {
            case CANCELLED:
            case SUCCESS:
            case FAILED_RETRIES_EXCEEDED:
                if (uploadStorage.contains(loadInfo.id)) {
                    if (state != STATE.FAILED_RETRIES_EXCEEDED || allowRemoveFailedLoads) {
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
    public void onStorageRestoreStarted(long startTime) {
        logger.debug("onStorageRestoreStarted()");
    }

    @Override
    public void onStorageRestoreFinished(long endTime, long processingTime, int restoredElementsCount) {
        logger.debug("onStorageRestoreFinished(), processingTime=" + processingTime + ", restoredElementsCount=" + restoredElementsCount);
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

        List<I> infos = getStorageList();

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

    private class SynchronizeUploadListRunnable implements Runnable {

        @Override
        public void run() {
            doSynchronizeWithUploadManager();
        }
    }
}
