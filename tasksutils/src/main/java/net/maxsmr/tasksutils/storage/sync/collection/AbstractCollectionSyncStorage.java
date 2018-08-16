package net.maxsmr.tasksutils.storage.sync.collection;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import net.maxsmr.commonutils.data.FileHelper;
import net.maxsmr.tasksutils.storage.sync.AbstractSyncStorage;
import net.maxsmr.tasksutils.taskexecutor.RunnableInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

public abstract class AbstractCollectionSyncStorage<I extends RunnableInfo> extends AbstractSyncStorage<I> {

    protected final static String FILE_EXT_DAT = "dat";

    protected final String storageDirPath;

    protected final String extension;

    /**
     * @param storageDirPath path when serialized {@link I} files stored
     * @param sync           is synchronization needed when adding and removing to storage
     * @param maxSize        max list elements
     * @param addRule        how react on full storage
     */
    protected AbstractCollectionSyncStorage(
            @Nullable String storageDirPath, @Nullable String extension,
            Class<I> clazz,
            boolean sync, int maxSize, @NonNull IAddRule<I> addRule) {
        super(clazz, sync, maxSize, addRule);
        if (sync && !FileHelper.checkDirNoThrow(storageDirPath)) {
            throw new RuntimeException("incorrect queue dir path: " + storageDirPath);
        }
        this.storageDirPath = storageDirPath;
        this.extension = TextUtils.isEmpty(extension) && sync ? FILE_EXT_DAT : extension;
        startRestoreThread();
    }

    @Override
    protected final int restoreStorage() {
        logger.debug("restoreStorage()");

        int restoredCount = 0;

        if (allowSync) {

            if (!FileHelper.checkDirNoThrow(storageDirPath)) {
                logger.error("incorrect storage dir path: " + storageDirPath);
                return restoredCount;
            }

            Set<File> files = FileHelper.getFiles(Collections.singleton(new File(storageDirPath)), FileHelper.GetMode.FILES, null, null, 0);

            if (files.isEmpty()) {
                logger.info("no files to restore");
                return restoredCount;
            }

            FileHelper.sortFilesByLastModified(files, true, false);

            logger.info("restoring " + runnableInfoClass.getSimpleName() + " objects by files...");

            for (File f : files) {

                if (Thread.currentThread().isInterrupted()) {
                    return restoredCount;
                }

                if (f == null || f.isDirectory()) {
                    continue;
                }

                final String ext = FileHelper.getFileExtension(f.getName());

                if (FileHelper.isFileCorrect(f) && extension.equalsIgnoreCase(ext)) {

                    I runnableInfo = null;

                    FileInputStream fis = null;
                    try {
                        fis = new FileInputStream(f);
                        runnableInfo = deserializeRunnableInfoFromInputStream(fis);
                    } catch (IOException e) {
                        logger.error("an Exception occurred", e);
                    } finally {
                        if (fis != null) {
                            try {
                                fis.close();
                            } catch (IOException e) {
                                logger.error("an Exception occurred", e);
                            }
                        }
                    }

                    logger.debug("runnableInfo from byte array: " + runnableInfo);
                    if (runnableInfo != null && checkRunnableInfo(runnableInfo) && addInternal(runnableInfo)) {
                        restoredCount++;
                    } else {
                        logger.error("runnableInfo " + runnableInfo + " was not added to deque, deleting file " + f + "...");
                        if (!FileHelper.deleteFile(f)) {
                            logger.error("can't delete file");
                        }
                    }

                } else {
                    logger.error("incorrect storage file: " + f + ", deleting...");
                    if (!FileHelper.deleteFile(f)) {
                        logger.error("can't delete file: " + f);
                    }
                }
            }
        }


        return restoredCount;
    }

    @Nullable
    public File getStorageDirPath() {
        return !TextUtils.isEmpty(storageDirPath) ? new File(storageDirPath) : null;
    }

    @Override
    protected boolean serializeRunnableInfo(I info) {
        logger.debug("serializeRunnableInfo(), info=" + info);

        if (allowSync) {

            if (!checkRunnableInfo(info)) {
                logger.error("incorrect info: " + info);
                return false;
            }

            if (!FileHelper.checkDirNoThrow(storageDirPath)) {
                logger.error("incorrect storage dir path: " + storageDirPath);
                return false;
            }

            final String infoFileName = getFileNameByInfo(info);

            FileOutputStream fos = null;
            try {
                info.toOutputStream(fos = new FileOutputStream((new File(storageDirPath, infoFileName)), false));
                return true;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        return false;
    }

    @Override
    protected boolean deleteSerializedRunnableInfo(I info) {
        logger.debug("deleteSerializedRunnableInfo(), info=" + info);

        if (allowSync) {

            if (!checkRunnableInfo(info)) {
                logger.error("incorrect info: " + info);
                return false;
            }

            return FileHelper.deleteFile(getFileNameByInfo(info), storageDirPath);
        }

        return false;
    }

    @Override
    protected boolean deleteAllSerializedRunnableInfos() {
        FileHelper.delete(Collections.singletonList(new File(storageDirPath)), false, null, null, new FileHelper.IDeleteNotifier() {
            @Override
            public boolean onProcessing(@NonNull File current, @NonNull Set<File> deleted, int currentLevel) {
                return true;
            }

            @Override
            public boolean confirmDeleteFile(File file) {
                return extension.equals(FileHelper.getFileExtension(file.getName()));
            }

            @Override
            public boolean confirmDeleteFolder(File folder) {
                return true;
            }

            @Override
            public void onDeleteFileFailed(File file) {

            }

            @Override
            public void onDeleteFolderFailed(File folder) {

            }
        }, 1);
        return true;
    }

    @NonNull
    protected String getFileNameByInfo(@NonNull RunnableInfo info) {
        return info.name + "_" + info.id + "." + extension;
    }

    protected class WrappedIterator implements Iterator<I> {

        final Iterator<I> iterator;

        I next;

        protected WrappedIterator(Iterator<I> iterator) {
            this.iterator = iterator;
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public I next() {
            return next = iterator.next();
        }

        @Override
        public void remove() {
            int prev = getSize();
            iterator.remove();
            if (!deleteSerializedRunnableInfo(next)) {
                logger.error("can't delete file by info " + next);
            }
            storageObservable.dispatchStorageSizeChanged(getSize(), prev, callbacksHandler);
            next = null;
        }
    }

//    @Override
//    protected boolean checkRunnableInfo(@NonNull I info) {
//        return FileHelper.isFileCorrect(info.getFile());
//    }
//
//    public synchronized void clear(boolean deleteSourceFiles) {
//        if (isDisposed()) {
//            throw new IllegalStateException("release() was called");
//        }
//        while (!isEmpty()) {
//            I info = pollFirst();
//            File file = info.getFile();
//            if (deleteSourceFiles) {
//                if (!FileHelper.deleteFile(file)) {
//                    logger.error("can't delete file: " + file);
//                }
//            }
//        }
//    }

}
