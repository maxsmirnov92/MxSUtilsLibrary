package net.maxsmr.tasksutils.storage.sync.collection;

import net.maxsmr.commonutils.FileUtilsKt;
import net.maxsmr.commonutils.GetMode;
import net.maxsmr.commonutils.IDeleteNotifier;
import net.maxsmr.commonutils.text.TextUtilsKt;
import net.maxsmr.tasksutils.storage.sync.AbstractSyncStorage;
import net.maxsmr.tasksutils.taskexecutor.RunnableInfo;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

import static net.maxsmr.commonutils.CompareUtilsKt.stringsEqual;
import static net.maxsmr.commonutils.FileUtilsKt.checkDir;
import static net.maxsmr.commonutils.FileUtilsKt.deleteFiles;
import static net.maxsmr.commonutils.FileUtilsKt.deleteFile;
import static net.maxsmr.commonutils.FileUtilsKt.getFileExtension;
import static net.maxsmr.commonutils.FileUtilsKt.getFiles;
import static net.maxsmr.commonutils.FileUtilsKt.isFileValid;
import static net.maxsmr.commonutils.FileUtilsKt.sortFilesByLastModified;

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
        if (sync && !checkDir(storageDirPath)) {
            throw new RuntimeException("incorrect queue dir path: " + storageDirPath);
        }
        this.storageDirPath = storageDirPath;
        this.extension = TextUtilsKt.isEmpty(extension) && sync ? FILE_EXT_DAT : extension;
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

            if (!checkDir(storageDirPath)) {
                logger.e("incorrect storage dir path: " + storageDirPath);
                return restoredCount;
            }

            Set<File> files = getFiles(new File(storageDirPath), GetMode.FILES, null);

            if (files.isEmpty()) {
                logger.i("no files to restore");
                return restoredCount;
            }

            sortFilesByLastModified(files);

            logger.i("restoring " + runnableInfoClass.getSimpleName() + " objects by files...");

            for (File f : files) {

                if (Thread.currentThread().isInterrupted()) {
                    return restoredCount;
                }

                if (f == null || f.isDirectory()) {
                    continue;
                }

                final String ext = getFileExtension(f.getName());

                if (isFileValid(f) && extension.equalsIgnoreCase(ext)) {

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
                        if (!deleteFile(f)) {
                            logger.e("can't delete file");
                        }
                    }

                } else {
                    logger.e("incorrect storage file: " + f + ", deleting...");
                    if (!deleteFile(f)) {
                        logger.e("can't delete file: " + f);
                    }
                }
            }
        }


        return restoredCount;
    }

    @Nullable
    public File getStorageDirPath() {
        return !TextUtilsKt.isEmpty(storageDirPath) ? new File(storageDirPath) : null;
    }

    @Override
    protected boolean serializeRunnableInfo(I info) {
        logger.d("serializeRunnableInfo(), info=" + info);

        if (allowSync) {

            if (!checkRunnableInfo(info)) {
                logger.e("incorrect info: " + info);
                return false;
            }

            if (!checkDir(storageDirPath)) {
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

            return deleteFile(new File(storageDirPath, getFileNameByInfo(info)));
        }

        return false;
    }

    @Override
    protected boolean deleteAllSerializedRunnableInfos() {
        FileUtilsKt.deleteFiles(new File(storageDirPath), false, null, 1, 0, new IDeleteNotifier() {
            @Override
            public boolean confirmDeleteFile(@NotNull File file) {
                return stringsEqual(extension, getFileExtension(file.getName()), true);
            }
        });
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
//        return isFileValid(info.getFile());
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
//                if (!deleteFile(file)) {
//                    logger.e("can't delete file: " + file);
//                }
//            }
//        }
//    }

}
