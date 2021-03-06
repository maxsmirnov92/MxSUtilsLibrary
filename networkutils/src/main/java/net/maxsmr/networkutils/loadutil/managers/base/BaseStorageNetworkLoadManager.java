package net.maxsmr.networkutils.loadutil.managers.base;

import net.maxsmr.commonutils.logger.BaseLogger;
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder;
import net.maxsmr.networkutils.loadutil.managers.LoadListener;
import net.maxsmr.networkutils.loadutil.managers.NetworkLoadManager;
import net.maxsmr.networkutils.loadutil.managers.base.info.LoadRunnableInfo;
import net.maxsmr.tasksutils.ScheduledThreadPoolExecutorManager;
import net.maxsmr.tasksutils.runnable.RunnableInfoRunnable;
import net.maxsmr.tasksutils.storage.sync.AbstractSyncStorage;
import net.maxsmr.tasksutils.storage.sync.collection.QueueSyncStorage;
import net.maxsmr.tasksutils.taskexecutor.RunnableInfo;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static net.maxsmr.tasksutils.ScheduledThreadPoolExecutorManager.ScheduleMode.FIXED_DELAY;
import static net.maxsmr.tasksutils.storage.sync.AbstractSyncStorage.MAX_SIZE_UNLIMITED;

@Deprecated
/**
 * storage synchronization already implemented in {@linkplain net.maxsmr.tasksutils.taskexecutor.TaskRunnableExecutor}
 */
public abstract class BaseStorageNetworkLoadManager<B extends LoadRunnableInfo.Body, LI extends LoadRunnableInfo<B>>
        implements LoadListener<LI>, AbstractSyncStorage.IStorageListener {

    protected final BaseLogger logger = BaseLoggerHolder.getInstance().getLogger(getClass());

    public static final int DEFAULT_LOAD_LIST_SYNCHRONIZE_INTERVAL = 20000;

    private int synchronizeInterval = DEFAULT_LOAD_LIST_SYNCHRONIZE_INTERVAL;

    private boolean allowRemoveFailedLoads = false;

    private boolean isReleased = false;

    @NotNull
    protected final NetworkLoadManager<B, LI> loadManager;

    @NotNull
    protected final QueueSyncStorage<LI> uploadStorage;

    private final ScheduledThreadPoolExecutorManager uploadListSynchronizer = new ScheduledThreadPoolExecutorManager(getClass().getSimpleName() + "_Synchronizer");

    public BaseStorageNetworkLoadManager(@NotNull NetworkLoadManager<B, LI> loadManager, @NotNull Class<LI> clazzInstance, String path) {
        logger.d("BaseStorageNetworkLoadManager(), loadManager=" + loadManager + ", clazzInstance=" + clazzInstance + ", path=" + path);
        this.uploadStorage = new QueueSyncStorage<>(path, "dat", clazzInstance, true,
                MAX_SIZE_UNLIMITED, new AbstractSyncStorage.IAddRule<LI>() {
            @Override
            public boolean allowAddIfFull() {
                return true;
            }

            @Override
            public void removeAny(AbstractSyncStorage fromStorage) {
                fromStorage.removeFirst();
            }
        }, true);
        this.loadManager = loadManager;
        this.loadManager.registerLoadListener(this);
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
        logger.d("release()");
        checkReleased();
        stopUploadSynchronizer();
        loadManager.unregisterLoadListener(this);
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
        logger.d("synchronize interval: " + synchronizeInterval);
        uploadListSynchronizer.removeAllRunnableTasks();
        uploadListSynchronizer.addRunnableTask(new SynchronizeUploadListRunnable(1), new ScheduledThreadPoolExecutorManager.RunOptions(0, synchronizeInterval, FIXED_DELAY));
        uploadListSynchronizer.restart(1);
    }

    public final void startUploadSynchronizer() {
        checkReleased();
        if (!isUploadSynchronizerRunning()) {
            restartUploadSynchronizer();
        }
    }

    public final void stopUploadSynchronizer() {
        checkReleased();
        uploadListSynchronizer.stop();
    }

    public final boolean isStorageRestoreCompleted() {
        checkReleased();
        return uploadStorage.isRestoreCompleted();
    }

    @NotNull
    public final List<LI> getStorageList() {
        if (!isStorageRestoreCompleted()) {
            throw new IllegalStateException("storage restore is not completed");
        }
        return uploadStorage.getAll();
    }

    protected boolean enqueueLoad(LI uploadInfo) {
        checkReleased();
        if (!loadManager.containsLoad(uploadInfo.id)) {
            loadManager.enqueueLoad(uploadInfo);
            return true;
        }
        return false;
    }

    @Override
    public void onLoadAddedToQueue(int id, int waitingLoads, int activeLoads) {
        LI rInfo = loadManager.findLoadById(id);
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
    public long getProcessingNotifyInterval(@NotNull LoadRunnableInfo loadInfo) {
        return LoadListener.DEFAULT_PROCESSING_NOTIFY_INTERVAL;
    }

    @Override
    public void onUpdateState(@NotNull LI loadInfo, @NotNull NetworkLoadManager.LoadProcessInfo loadProcessInfo, @Nullable Throwable t) {
        STATE state = loadProcessInfo.getState();
        switch (state) {
            case CANCELLED:
            case SUCCESS:
            case FAILED_RETRIES_EXCEEDED:
                if (uploadStorage.contains(loadInfo.id)) {
                    if (state != STATE.FAILED_RETRIES_EXCEEDED || allowRemoveFailedLoads) {
                        logger.d("removing info with id " + loadInfo.id + " from storage...");
                        uploadStorage.removeById(loadInfo.id);
                    }
                }
                break;
        }
    }

    @Override
    public void onResponse(@NotNull LI loadInfo, @NotNull NetworkLoadManager.LoadProcessInfo loadProcessInfo, @NotNull NetworkLoadManager.Response response) {
        logger.d("onResponse(), loadInfo=" + loadInfo + ", loadProcessInfo=" + loadProcessInfo + ", response=" + response);
    }

    @Override
    public void onStorageSizeChanged(int currentSize, int previousSize) {
        logger.d("onStorageSizeChanged(), currentSize=" + currentSize + ", previousSize=" + previousSize);
    }

    @Override
    public void onStorageRestoreStarted(long startTime) {
        logger.d("onStorageRestoreStarted()");
    }

    @Override
    public void onStorageRestoreFinished(long endTime, long processingTime, int restoredElementsCount) {
        logger.d("onStorageRestoreFinished(), processingTime=" + processingTime + ", restoredElementsCount=" + restoredElementsCount);
    }

    private void doSynchronizeWithUploadManager() {
        logger.d("doSynchronizeWithUploadManager()");

        checkReleased();

        if (!isStorageRestoreCompleted()) {
            logger.w("upload storage restore was not completed");
            return;
        }

        if (uploadStorage.isEmpty()) {
            logger.d("upload storage is empty");
            return;
        }

        List<LI> infos = getStorageList();

        if (!handleStorageSynchronize(infos)) {
            for (LI info : infos) {
                enqueueLoad(info);
            }
        }
    }

    protected boolean handleStorageSynchronize(@NotNull List<LI> infos) {
        return false;
    }

    private class SynchronizeUploadListRunnable extends RunnableInfoRunnable<RunnableInfo> {

        protected SynchronizeUploadListRunnable(int id) {
            super(new RunnableInfo(1));
        }

        @Override
        public void run() {
            doSynchronizeWithUploadManager();
        }
    }
}
