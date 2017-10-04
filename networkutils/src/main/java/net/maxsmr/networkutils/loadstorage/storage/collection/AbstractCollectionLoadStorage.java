package net.maxsmr.networkutils.loadstorage.storage.collection;

import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import net.maxsmr.commonutils.data.FileHelper;
import net.maxsmr.networkutils.loadstorage.LoadInfo;
import net.maxsmr.networkutils.loadstorage.storage.AbstractLoadStorage;
import net.maxsmr.networkutils.loadutil.managers.base.info.LoadRunnableInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collections;
import java.util.Set;

public abstract class AbstractCollectionLoadStorage<I extends LoadInfo> extends AbstractLoadStorage<I> {

    private final static Logger logger = LoggerFactory.getLogger(AbstractCollectionLoadStorage.class);

    protected final static String FILE_EXT_DAT = "dat";

    protected final boolean syncWithFiles;

    protected final String storageDirPath;

    private boolean isRestoreCompleted = false;

    /**
     * @param sync             is synchronization needed when adding and removing to storage
     * @param allowDeleteFiles allow delete files to load when clearing queue
     * @param storageDirPath   path when serialized {@link LoadInfo} files stored
     */
    protected AbstractCollectionLoadStorage(Class<I> clazz, boolean sync, boolean allowDeleteFiles, @Nullable String storageDirPath) {
        super(clazz);
        logger.debug("AbstractCollectionLoadStorage(), sync=" + sync + ", allowDeleteFiles=" + allowDeleteFiles + ", storageDirPath=" + storageDirPath);

        if (sync && !FileHelper.checkDirNoThrow(storageDirPath)) {
            throw new RuntimeException("incorrect queue dir path: " + storageDirPath);
        }

        this.syncWithFiles = sync;
        this.storageDirPath = storageDirPath;

        if (syncWithFiles) {
            startRestoreThread();
        } else {
            isRestoreCompleted = true;
        }
    }

    public boolean isRestoreCompleted() {
        return isRestoreCompleted;
    }

    private Thread restoreThread;

    protected boolean isRestoreThreadRunning() {
        return (restoreThread != null && restoreThread.isAlive());
    }

    protected void startRestoreThread() {

        if (isRestoreThreadRunning()) {
            return;
        }

        restoreThread = new Thread(new Runnable() {

            @Override
            public void run() {
                restoreFromFiles();
            }
        }, AbstractCollectionLoadStorage.class.getSimpleName() + ":RestoreThread");

        restoreThread.start();
    }

    protected void stopRestoreThread() {

        if (!isRestoreThreadRunning()) {
            return;
        }

        restoreThread.interrupt();
        restoreThread = null;
    }

    protected final boolean addNoSerialize(@NonNull I info) {
        final int size = getSize();
        return addNoSerialize(info, size > 0 ? size - 1 : 0);
    }

    @CallSuper
    protected boolean addNoSerialize(@NonNull I info, int pos) {
        return checkLoadInfo(info);
    }

    /**
     * may take a long time to restore
     */
    protected synchronized boolean restoreFromFiles() {
        logger.debug("restoreFromFiles()");

        isRestoreCompleted = false;

        int restoredCount = 0;

        final long startTime = System.currentTimeMillis();

        try {

            Set<File> files = FileHelper.getFiles(Collections.singleton(new File(storageDirPath)), FileHelper.GetMode.FILES, null, null, 0);

            if (files.isEmpty()) {
                logger.info("no files to restore");
                return false;
            }

            FileHelper.sortFilesByLastModified(files, true, false);

            logger.info("restoring LoadInfo objects by files...");

            for (File f : files) {

                if (Thread.currentThread().isInterrupted()) {
                    return false;
                }

                if (f == null || f.isDirectory()) {
                    continue;
                }

                final String ext = FileHelper.getFileExtension(f.getName());

                if (f.isFile() && f.length() > 0 && ext.equalsIgnoreCase(FILE_EXT_DAT)) {

                    I uploadInfo = null;

                    try {
                        byte[] b = FileHelper.readBytesFromFile(f);
                        if (b != null && b.length > 0) {
                            uploadInfo = LoadInfo.fromByteArray(b, loadInfoClass);
                        }
                    } catch (RuntimeException e) {
                        logger.error("an Exception occurred", e);
                    }

                    logger.debug("uploadInfo from byte array: " + uploadInfo);
                    if (uploadInfo != null && addNoSerialize(uploadInfo)) {
                        restoredCount++;
                    } else {
                        logger.error("uploadInfo " + uploadInfo + " was not added to deque, deleting file " + f + "...");
                        if (!FileHelper.deleteFile(f)) {
                            logger.error("can't delete file");
                        }
                    }

                } else {
                    logger.error("incorrect queue file: " + f + ", deleting...");
                    if (!FileHelper.deleteFile(f)) {
                        logger.error("can't delete file: " + f);
                    }
                }
            }


            return true;

        } finally {
            isRestoreCompleted = true;
            logger.info("restoring complete, time: " + (System.currentTimeMillis() - startTime) + " ms");
            logger.info("restored LoadInfo objects count: " + restoredCount + ", queues total size: " + getSize());
            storageObservable.dispatchStorageRestored(restoredCount);
        }

    }

    protected boolean writeLoadInfoToFile(@Nullable I loadInfo) {
        logger.debug("writeLoadInfoToFile(), loadInfo=" + loadInfo);

        if (syncWithFiles) {

            if (loadInfo == null) {
                logger.error("loadInfo is null");
                return false;
            }

            if (!FileHelper.checkDirNoThrow(storageDirPath)) {
                logger.error("incorrect storage dir path: " + storageDirPath);
                return false;
            }

            final String uploadInfoFileName = loadInfo.name + "." + FILE_EXT_DAT;

            // logger.debug("writing upload info (queue type: " + loadInfo.getQueueType() + ", upload file: "
            // + loadInfo.getUploadFile().getName() + ") to file...");

            byte[] b = loadInfo.toByteArray();
            return b != null && b.length > 0 && FileHelper.writeBytesToFile(b, uploadInfoFileName, storageDirPath, false) != null;
        }

        return false;
    }

    protected boolean deleteFileByLoadInfo(@Nullable I loadInfo) {
        logger.debug("deleteFileByLoadInfo(), loadInfo=" + loadInfo);

        if (syncWithFiles) {

            if (loadInfo == null) {
                logger.error("loadInfo is null");
                return false;
            }

            final String uploadInfoFileName = loadInfo.name + "." + FILE_EXT_DAT;

            // logger.debug("deleting file with upload info (queue type: " + loadInfo.getQueueType() +
            // ", upload file: "
            // + loadInfo.getUploadFile().getName() + ")...");

            return FileHelper.deleteFile(uploadInfoFileName, storageDirPath);
        }

        return false;
    }


    public synchronized void clear(boolean deleteFiles) {
        if (isDisposed()) {
            throw new IllegalStateException("release() was called");
        }
        while (!isEmpty()) {
            I info = pollFirst();
            if (info.body instanceof LoadRunnableInfo.FileBody) {
                File uploadFile = ((LoadRunnableInfo.FileBody) info.body).getSourceFile();
                if (deleteFiles) {
                    if (!FileHelper.deleteFile(uploadFile)) {
                        logger.error("can't delete file: " + uploadFile);
                    }
                }
            }
        }
    }

    protected abstract void clearNoDelete();

    @CallSuper
    public void release() {
        super.release();
        logger.debug("release()");
        stopRestoreThread();
    }

    protected static <B extends LoadRunnableInfo.Body, I extends LoadInfo<B>> boolean checkLoadInfo(@NonNull I info) {

        if (info.body.isEmpty()) {
            logger.error("empty body: " + info.body);
            return false;
        }

        if (info.body instanceof LoadRunnableInfo.FileBody) {
            File loadFile = ((LoadRunnableInfo.FileBody) info.body).getSourceFile();
            if (!FileHelper.isFileCorrect(loadFile)) {
                logger.error("file is incorrect: " + loadFile);
                return false;
            } else {
                logger.debug("loadFile: " + loadFile + " | size: " + loadFile.length());
                return true;
            }
        }

        return true;
    }
}
