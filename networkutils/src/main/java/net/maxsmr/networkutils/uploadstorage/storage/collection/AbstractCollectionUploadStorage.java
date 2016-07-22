package net.maxsmr.networkutils.uploadstorage.storage.collection;

import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

import net.maxsmr.commonutils.data.FileHelper;
import net.maxsmr.networkutils.uploadstorage.UploadInfo;
import net.maxsmr.networkutils.uploadstorage.storage.AbstractUploadStorage;

public abstract class AbstractCollectionUploadStorage<I extends UploadInfo> extends AbstractUploadStorage<I> {

    private final static Logger logger = LoggerFactory.getLogger(AbstractCollectionUploadStorage.class);

    protected final static String FILE_EXT_DAT = "dat";

    protected boolean syncWithFiles;

    protected boolean allowDeleteFiles;

    protected String storageDirPath;

    /**
     * @param sync             is synchronization needed when adding and removing to storage
     * @param allowDeleteFiles allow delete files to upload when clearing queue
     * @param storageDirPath   path when serialized {@link UploadInfo} files stored
     */
    protected AbstractCollectionUploadStorage(Class<I> clazz, boolean sync, boolean allowDeleteFiles, @Nullable String storageDirPath) {
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
        }, AbstractCollectionUploadStorage.class.getSimpleName() + ":RestoreThread");

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

    protected static <I extends UploadInfo> boolean checkUploadFile(@NonNull I info) {
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
        return super.add(info, pos) && checkUploadFile(info);
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

        int restoreCount = 0;

        final long startTime = System.currentTimeMillis();

        logger.info("restoring UploadInfo objects by files...");

        for (File f : filesList) {

            if (Thread.currentThread().isInterrupted()) {
                return false;
            }

            if (f == null || f.isDirectory()) {
                continue;
            }

            final String ext = FileHelper.getFileExtension(f.getName());

            if (f.isFile() && f.length() > 0 && ext != null && ext.equalsIgnoreCase(FILE_EXT_DAT)) {

                I uploadInfo = null;

                try {
                    uploadInfo = UploadInfo.fromByteArray(FileHelper.readBytesFromFile(f), uploadInfoClass);
                } catch (IllegalArgumentException e) {
                    logger.error("an IllegalArgumentException occured", e);
                }

                logger.debug("uploadInfo from byte array: " + uploadInfo);
                if (uploadInfo != null && addNoSerialize(uploadInfo)) {
                    restoreCount++;
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
        logger.info("restored UploadInfo objects count: " + restoreCount + ", queues total size: " + getSize());

        synchronized (storageListeners) {
            if (storageListeners.size() > 0) {
                for (StorageListener l : storageListeners) {
                    l.onStorageRestored(restoreCount);
                }
            }
        }

        return true;
    }

    protected boolean writeUploadInfoToFile(@Nullable I uploadInfo) {
        logger.debug("writeUploadInfoToFile(), uploadInfo=" + uploadInfo);

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

    protected boolean deleteFileByUploadInfo(@Nullable I uploadInfo) {
        logger.debug("deleteFileByUploadInfo(), uploadInfo=" + uploadInfo);

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


    public void clearWithDeleteFiles() {

        if (isDisposed()) {
            throw new IllegalStateException("release() was called");
        }

        if (syncWithFiles) {
            while (!isEmpty()) {
                UploadInfo info = pollFirst();
                if (allowDeleteFiles && info.uploadFile != null && info.uploadFile.isFile() && info.uploadFile.exists()) {
                    if (!info.uploadFile.delete()) {
                        logger.error("can't delete file: " + info.uploadFile);
                    }
                }
            }
        }

        clear();
    }

    public void release() {
        logger.debug("release()");
        super.release();

        stopRestoreThread();

        synchronized (storageListeners) {
            storageListeners.clear();
        }
    }
}
