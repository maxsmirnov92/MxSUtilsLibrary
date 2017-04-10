package net.maxsmr.networkutils.loadutil.managers;


import android.support.annotation.NonNull;

import net.maxsmr.networkutils.loadutil.managers.ids.AbsIdHolder;
import net.maxsmr.tasksutils.taskexecutor.AbsTaskRunnableExecutor;

public final class LoadManagersFacade<DP extends AbsIdHolder, UP extends AbsIdHolder> {

    public static <DP extends AbsIdHolder, UP extends AbsIdHolder> void initInstance(@NonNull DP downloadIdsPool, @NonNull UP uploadIdsPool) {
        if (sInstance == null) {
            synchronized (LoadManagersFacade.class) {
                sInstance = new LoadManagersFacade<>(downloadIdsPool, uploadIdsPool);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @NonNull
    public static <DP extends AbsIdHolder, UP extends AbsIdHolder> LoadManagersFacade<DP, UP> getInstance() {
        if (sInstance == null) {
            throw new IllegalStateException("initInstance() was not called");
        }
        return (LoadManagersFacade<DP, UP>) sInstance;
    }

    public static void releaseInstance() {
        if (sInstance != null) {
            synchronized (LoadManagersFacade.class) {
                sInstance.release();
            }
        }
    }

    private static LoadManagersFacade<?, ?> sInstance;

    private LoadManagersFacade(@NonNull DP downloadIdsPool, @NonNull UP uploadIdsPool) {
        initDownloadManager(downloadIdsPool);
        initUploadExecutor(uploadIdsPool);
    }

    private DP downloadIdsPool;
    private NetworkLoadManager downloadManager;

    private UP uploadIdsPool;
    private NetworkLoadManager uploadManager;

    public DP getDownloadIdsPool() {
        return downloadIdsPool;
    }

    public NetworkLoadManager getDownloadManager() {
        if (downloadManager == null) {
            throw new IllegalStateException("initDownloadManager() was not called");
        }
        return downloadManager;
    }

    public void initDownloadManager(@NonNull DP idsPool, int concurrentLoads) {
        if (downloadManager == null || downloadManager.isReleased()) {
            downloadIdsPool = idsPool;
            downloadManager = new NetworkLoadManager(AbsTaskRunnableExecutor.DEFAULT_TASKS_LIMIT, concurrentLoads);
        }
    }

    public void releaseDownloadManager() {
        if (downloadManager != null) {
            if (!downloadManager.isReleased()) {
                downloadManager.release();
            }
            downloadManager = null;
        }
        if (downloadIdsPool != null) {
//            downloadIdsPool.clear();
            downloadIdsPool = null;
        }
    }

    public UP getUploadIdsPool() {
        return uploadIdsPool;
    }

    public NetworkLoadManager getUploadManager() {
        if (uploadManager == null) {
            throw new IllegalStateException("initUploadManager() was not called");
        }
        return uploadManager;
    }

    public void initUploadExecutor(@NonNull UP idsPool) {
        if (uploadManager == null || uploadManager.isReleased()) {
            uploadIdsPool = idsPool;
            uploadManager = new NetworkLoadManager(AbsTaskRunnableExecutor.DEFAULT_TASKS_LIMIT, 1);
        }
    }

    public void releaseUploadExecutor() {
        if (uploadManager != null) {
            if (!uploadManager.isReleased()) {
                uploadManager.release();
            }
            uploadManager = null;
        }
        if (uploadIdsPool != null) {
//            uploadIdsPool.clear();
            uploadIdsPool = null;
        }
    }

    public void release() {
        releaseDownloadManager();
        releaseUploadExecutor();
    }

}
