package net.maxsmr.networkutils.loadutil.executors;


import android.support.annotation.NonNull;

import net.maxsmr.networkutils.loadutil.executors.base.LoadExecutor;
import net.maxsmr.networkutils.loadutil.executors.base.download.DownloadExecutor;
import net.maxsmr.networkutils.loadutil.executors.base.upload.UploadExecutor;

public final class LoadExecutorFacade<DP extends AbsIdsPool, UP extends AbsIdsPool> {

    public static <DP extends AbsIdsPool, UP extends AbsIdsPool> void initInstance(@NonNull DP downloadIdsPool, @NonNull UP uploadIdsPool) {
        if (sInstance == null) {
            synchronized (LoadExecutorFacade.class) {
                sInstance = new LoadExecutorFacade<>(downloadIdsPool, uploadIdsPool);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @NonNull
    public static <DP extends AbsIdsPool, UP extends AbsIdsPool> LoadExecutorFacade<DP, UP> getInstance() {
        if (sInstance == null) {
            throw new IllegalStateException("initInstance() was not called");
        }
        return (LoadExecutorFacade<DP, UP>) sInstance;
    }

    private static LoadExecutorFacade<?, ?> sInstance;

    private LoadExecutorFacade(@NonNull DP downloadIdsPool, @NonNull UP uploadIdsPool) {
        initDownloadExecutor(downloadIdsPool);
        initUploadExecutor(uploadIdsPool);
    }

    public static final int DOWNLOAD_RETRY_LIMIT = 5;

    private DP downloadIdsPool;
    private DownloadExecutor downloadExecutor;

    public DP getDownloadIdsPool() {
        return downloadIdsPool;
    }

    public DownloadExecutor getDownloadExecutor() {
        if (downloadExecutor == null) {
            throw new IllegalStateException("initDownloadExecutor() was not called");
        }
        return downloadExecutor;
    }

    public void initDownloadExecutor(@NonNull DP idsPool) {
        if (downloadExecutor == null || downloadExecutor.isShutdown()) {
            downloadIdsPool = idsPool;
            downloadExecutor = new DownloadExecutor(LoadExecutor.DEFAULT_LOADS_LIMIT, 1);
        }
    }

    public void releaseDownloadExecutor() {
        if (downloadExecutor != null) {
            if (!downloadExecutor.isShutdown()) {
                downloadExecutor.shutdown();
            }
            downloadExecutor = null;
        }
        if (downloadIdsPool != null) {
            downloadIdsPool.clearIds();
            downloadIdsPool = null;
        }
    }

    public static final int UPLOAD_RETRY_LIMIT = 5;

    private UP uploadIdsPool;
    private UploadExecutor uploadExecutor;

    public UP getUploadIdsPool() {
        return uploadIdsPool;
    }

    public UploadExecutor getUploadExecutor() {
        if (uploadExecutor == null) {
            throw new IllegalStateException("initUploadExecutor() was not called");
        }
        return uploadExecutor;
    }

    public void initUploadExecutor(@NonNull UP idsPool) {
        if (uploadExecutor == null || uploadExecutor.isShutdown()) {
            uploadIdsPool = idsPool;
            uploadExecutor = new UploadExecutor(LoadExecutor.DEFAULT_LOADS_LIMIT, 1);
        }
    }

    public void releaseUploadExecutor() {
        if (uploadExecutor != null) {
            if (!uploadExecutor.isShutdown()) {
                uploadExecutor.shutdown();
            }
            uploadExecutor = null;
        }
        if (uploadIdsPool != null) {
            uploadIdsPool.clearIds();
            uploadIdsPool = null;
        }
    }

}
