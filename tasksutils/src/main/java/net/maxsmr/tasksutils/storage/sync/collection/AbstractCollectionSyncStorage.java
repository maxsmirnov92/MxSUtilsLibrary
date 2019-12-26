package net.maxsmr.tasksutils.storage.sync.collection;

;

import net.maxsmr.commonutils.data.FileHelper;
import net.maxsmr.commonutils.data.StringUtils;
import net.maxsmr.tasksutils.storage.sync.AbstractSyncStorage;
import net.maxsmr.tasksutils.taskexecutor.RunnableInfo;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
            boolean sync, int maxSize, @NotNull IAddRule<I> addRule, boolean startRestore) {
        super(clazz, sync, maxSize, addRule);
        if (sync && !FileHelper.checkDirNoThrow(storageDirPath)) {
            throw new RuntimeException("incorrect queue dir path: " + storageDirPath);
        }
        this.storageDirPath = storageDirPath;
        this.extension = StringUtils.isEmpty(extension) && sync ? FILE_EXT_DAT : extension;
        if (startRestore) {
            startRestoreThread();
        }
    }

    @Override
    protected final int restoreStorage() {
        logger.d("restoreStorage()");

        int restoredCount = 0;

        if (allowSync) {

            if (Thread.currentThread().isInterrupted()) {
                return restoredCount;
            }

            if (!FileHelper.checkDirNoThrow(storageDirPath)) {
                logger.e("incorrect storage dir path: " + storageDirPath);
                return restoredCount;
            }

            Set<File> files = FileHelper.getFiles(Collections.singleton(new File(storageDirPath)), FileHelper.GetMode.FILES, null, null, 0);

            if (files.isEmpty()) {
                logger.i("no files to restore");
                return restoredCount;
            }

            FileHelper.sortFilesByLastModified(files, true, false);

            logger.i("restoring " + runnableInfoClass.getSimpleName() + " objects by files...");

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
                        logger.e("an Exception occurred", e);
                    } finally {
                        if (fis != null) {
                            try {
                                fis.close();
                            } catch (IOException e) {
                                logger.e("an Exception occurred", e);
                            }
                        }
                    }

                    logger.d("runnableInfo from byte array: " + runnableInfo);
                    if (runnableInfo != null && checkRunnableInfo(runnableInfo) && addInternal(runnableInfo)) {
                        restoredCount++;
                    } else {
                        logger.e("runnableInfo " + runnableInfo + " was not added to deque, deleting file " + f + "...");
                        if (!FileHelper.deleteFile(f)) {
                            logger.e("can't delete file");
                        }
                    }

                } else {
                    logger.e("incorrect storage file: " + f + ", deleting...");
                    if (!FileHelper.deleteFile(f)) {
                        logger.e("can't delete file: " + f);
                    }
                }
            }
        }


        return restoredCount;
    }

    @Nullable
    public File getStorageDirPath() {
        return !StringUtils.isEmpty(storageDirPath) ? new File(storageDirPath) : null;
    }

    @Override
    protected boolean serializeRunnableInfo(I info) {
        logger.d("serializeRunnableInfo(), info=" + info);

        if (allowSync) {

            if (!checkRunnableInfo(info)) {
                logger.e("incorrect info: " + info);
                return false;
            }

            if (!FileHelper.checkDirNoThrow(storageDirPath)) {
                logger.e("incorrect storage dir path: " + storageDirPath);
                return false;
            }

            final String infoFileName = getFileNameByInfo(info);

            FileOutputStream fos = null;
            try {
                return info.toOutputStream(fos = new FileOutputStream((new File(storageDirPath, infoFileName)), false));
            } catch (FileNotFoundException e) {
                logger.e("a FileNotFoundException occurred: " + e.getMessage(), e);
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e) {
                        logger.e("an IOException occurred during close(): " + e.getMessage(), e);
                    }
                }
            }
        }

        return false;
    }

    @Override
    protected boolean deleteSerializedRunnableInfo(I info) {
        logger.d("deleteSerializedRunnableInfo(), info=" + info);

        if (allowSync) {

            if (!checkRunnableInfo(info)) {
                logger.e("incorrect info: " + info);
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
            public boolean onProcessing(@NotNull File current, @NotNull Set<File> deleted, int currentLevel) {
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

    @NotNull
    protected String getFileNameByInfo(@NotNull RunnableInfo info) {
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
                logger.e("can't delete file by info " + next);
            }
            storageObservable.dispatchStorageSizeChanged(getSize(), prev, callbacksHandler);
            next = null;
        }
    }

//    @Override
//    protected boolean checkRunnableInfo(@NotNull I info) {
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
//                    logger.e("can't delete file: " + file);
//                }
//            }
//        }
//    }

}
