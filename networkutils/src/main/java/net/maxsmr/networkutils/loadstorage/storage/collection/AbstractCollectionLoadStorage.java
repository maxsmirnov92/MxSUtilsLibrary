package net.maxsmr.networkutils.loadstorage.storage.collection;

import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

import net.maxsmr.commonutils.data.FileHelper;
import net.maxsmr.networkutils.loadstorage.LoadInfo;
import net.maxsmr.networkutils.loadstorage.storage.AbstractLoadStorage;

public abstract class AbstractCollectionLoadStorage<I extends LoadInfo> extends AbstractLoadStorage<I> {

    private final static Logger logger = LoggerFactory.getLogger(AbstractCollectionLoadStorage.class);

    protected final static String FILE_EXT_DAT = "dat";

    protected boolean syncWithFiles = true;

    protected boolean allowDeleteFiles = true;

    protected String storageDirPath;

    /**
     * @param sync             is synchronization needed when adding and removing to storage
     * @param allowDeleteFiles allow delete files to upload when clearing queue
     * @param storageDirPath   path when serialized {@link LoadInfo} files stored
     */
    protected AbstractCollectionLoadStorage(Class<I> clazz, boolean sync, boolean allowDeleteFiles, @Nullable String storageDirPath) {
        super(clazz);
        init(sync, allowDeleteFiles, storageDirPath);
    }

    private void init(boolean sync, boolean allowDeleteFiles, @Nullable String storageDirPath) {
        logger.debug("init(), sync=" + sync + ", allowDeleteFiles=" + allowDeleteFiles + ", storageDirPath=" + storageDirPath);

        if (sync && !FileHelper.testDirNoThrow(storageDirPath)) {
            throw new RuntimeException("incorrect queue dir path: " + storageDirPath);
        }

        this.syncWithFiles = sync;
        this.allowDeleteFiles = allowDeleteFiles;
        this.storageDirPath = storageDirPath;
    }

    private Thread restoreThread;

    protected boolean isRestoreThreadRunning() {
        return (restoreThread != null && restoreThread.isAlive());
    }

    protected void startRestoreThread() {
        logger.debug("startRestoreThread()");

        if (isRestoreThreadRunning()) {
            logger.debug("restoreThread is already running");
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
        logger.debug("stopRestoreThread()");

        if (!isRestoreThreadRunning()) {
            logger.debug("restoreThread is not running");
            return;
        }

        restoreThread.interrupt();
        restoreThread = null;
    }

    protected final boolean addNoSerialize(@NonNull I info) {
        final int size = getSize();
        return addNoSerialize(info, size > 0 ? size - 1 : 0);
    }

    protected static <I extends LoadInfo> boolean checkUploadFile(@NonNull I info) {
        if (!FileHelper.isFileCorrect(info.uploadFile)) {
            logger.error("uploadFile is incorrect: " + info.uploadFile);
            return false;
        } else {
            logger.debug("uploadFile: " + info.uploadFile + " | size: " + info.uploadFile.length());
            return true;
        }
    }

    @CallSuper
    protected boolean addNoSerialize(@NonNull I info, int pos) {
        return checkUploadFile(info);
    }

    /**
     * may take a long time to restore
     */
    protected synchronized boolean restoreFromFiles() {
        logger.debug("restoreFromFiles()");

        List<File> filesList = FileHelper.getFiles(new File(storageDirPath), false, null);

        if (filesList.isEmpty()) {
            logger.error("filesList is null or empty");
            return false;
        }

        FileHelper.sortFilesByLastModified(filesList, true, false);

        int restoredCount = 0;

        final long startTime = System.currentTimeMillis();

        logger.info("restoring LoadInfo objects by files...");

        for (File f : filesList) {

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
                    uploadInfo = LoadInfo.fromByteArray(FileHelper.readBytesFromFile(f), loadInfoClass);
                } catch (IllegalArgumentException e) {
                    logger.error("an IllegalArgumentException occured", e);
                }

                logger.debug("uploadInfo from byte array: " + uploadInfo);
                if (uploadInfo != null && addNoSerialize(uploadInfo)) {
                    restoredCount++;
                } else {
                    logger.error("uploadInfo " + uploadInfo + " was not added to deque, deleting file " + f + "...");
                    if (!f.delete()) {
                        logger.error("can't delete file");
                    }
                }

            } else {
                logger.error("incorrect queue file: " + f + ", deleting...");
                if (!f.delete()) {
                    logger.error("can't delete file: " + f);
                }
            }
        }

        logger.info("restoring complete, time: " + (System.currentTimeMillis() - startTime) + " ms");
        logger.info("restored LoadInfo objects count: " + restoredCount + ", queues total size: " + getSize());
        dispatchStorageRestored(restoredCount);
        return true;
    }

    protected boolean writeLoadInfoToFile(@Nullable I uploadInfo) {
        logger.debug("writeLoadInfoToFile(), uploadInfo=" + uploadInfo);

        if (syncWithFiles) {

            if (uploadInfo == null) {
                logger.error("uploadInfo is null");
                return false;
            }

            if (uploadInfo.uploadFile == null) {
                logger.error("uploadFile is null");
                return false;
            }

            if (!FileHelper.testDirNoThrow(storageDirPath)) {
                logger.error("incorrect storage dir path: " + storageDirPath);
            }

            final String uploadInfoFileName = uploadInfo.uploadFile.getName() + "." + FILE_EXT_DAT;

            // logger.debug("writing upload info (queue type: " + uploadInfo.getQueueType() + ", upload file: "
            // + uploadInfo.getUploadFile().getName() + ") to file...");

            return (FileHelper.writeBytesToFile(uploadInfo.toByteArray(), uploadInfoFileName, storageDirPath, false) != null);
        }

        return false;
    }

    protected boolean deleteFileByLoadInfo(@Nullable I uploadInfo) {
        logger.debug("deleteFileByLoadInfo(), uploadInfo=" + uploadInfo);

        if (syncWithFiles) {

            if (uploadInfo == null) {
                logger.error("uploadInfo is null");
                return false;
            }

            if (uploadInfo.uploadFile == null) {
                logger.error("uploadFile is null");
                return false;
            }

            final String uploadInfoFileName = uploadInfo.uploadFile.getName() + "." + FILE_EXT_DAT;

            // logger.debug("deleting file with upload info (queue type: " + uploadInfo.getQueueType() +
            // ", upload file: "
            // + uploadInfo.getUploadFile().getName() + ")...");

            return FileHelper.deleteFile(uploadInfoFileName, storageDirPath);
        }

        return false;
    }


    public synchronized void clear(boolean sync, boolean deleteFiles) {
        if (isDisposed()) {
            throw new IllegalStateException("release() was called");
        }
        while (!isEmpty()) {
            I info = sync && syncWithFiles ? pollFirst() : peekFirst();
            if (deleteFiles && allowDeleteFiles && info.uploadFile != null && info.uploadFile.isFile() && info.uploadFile.exists()) {
                if (!info.uploadFile.delete()) {
                    logger.error("can't delete file: " + info.uploadFile);
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
}
