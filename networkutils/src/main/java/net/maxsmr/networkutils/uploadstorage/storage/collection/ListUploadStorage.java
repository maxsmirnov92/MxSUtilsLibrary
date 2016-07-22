package net.maxsmr.networkutils.uploadstorage.storage.collection;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import net.maxsmr.networkutils.uploadstorage.UploadInfo;


public class ListUploadStorage<I extends UploadInfo> extends AbstractCollectionUploadStorage<I> {

    private final static Logger logger = LoggerFactory.getLogger(ListUploadStorage.class);

    private int maxSize = MAX_SIZE_UNLIMITED;

    private List<I> uploadList;

    /**
     * @param maxSize                max list elements
     * @param sync                   is synchronization needed when adding and removing to queue
     * @param allowDeleteFiles       allow delete files to upload when clearing queue
     * @param listDirPath            path when serialized {@link UploadInfo} files stored
     * @param restoreQueuesFromFiles allow restore {@link UploadInfo} from files on create
     */
    public ListUploadStorage(Class<I> clazz, int maxSize, boolean sync, boolean allowDeleteFiles, @Nullable String listDirPath, boolean restoreQueuesFromFiles, @Nullable StorageListener listener) {
        super(clazz, sync, allowDeleteFiles, listDirPath);
        init(maxSize);

        addStorageListener(listener);

        if (restoreQueuesFromFiles) {
            startRestoreThread();
        }
    }

    private void init(int maxListSize) {
        logger.debug("init(), maxListSize=" + maxListSize);
        this.uploadList = (maxSize = maxListSize) > 0 ? new ArrayList<I>(maxListSize) : new ArrayList<I>();
    }

    @Override
    protected boolean isDisposed() {
        return uploadList == null;
    }

    @Override
    public int getMaxSize() {

        if (isDisposed()) {
            throw new IllegalStateException("release() was called");
        }

        return maxSize;
    }

    @Override
    public int getSize() {

        if (isDisposed()) {
            throw new IllegalStateException("release() was called");
        }

        return uploadList.size();
    }

    @Override
    public boolean contains(@Nullable I info) {

        if (isDisposed()) {
            throw new IllegalStateException("release() was called");
        }

        return uploadList.contains(info);
    }

    @Override
    @NonNull
    public List<I> getAll() {

        if (isDisposed()) {
            throw new IllegalStateException("release() was called");
        }

        return new ArrayList<>(uploadList);
    }

    @Override
    @NonNull
    public I pollFirst() {

        if (isDisposed()) {
            throw new IllegalStateException("release() was called");
        }

        try {
            return get(0);
        } finally {
            remove(0);
        }
    }

    @NonNull
    @Override
    public I pollLast() {
        if (isDisposed()) {
            throw new IllegalStateException("release() was called");
        }

        try {
            return get(getSize() - 1);
        } finally {
            remove(getSize() - 1);
        }
    }

    @Override
    protected boolean addNoSerialize(@NonNull I info, int pos) {
        if (super.addNoSerialize(info, pos)) {
            synchronized (uploadList) {

                synchronized (storageListeners) {
                    if (storageListeners.size() > 0) {
                        for (StorageListener l : storageListeners) {
                            l.onStorageSizeChanged(getSize());
                        }
                    }
                }

                uploadList.add(pos, info);
                return contains(info);
            }
        }

        return false;
    }

    @Override
    public boolean add(@NonNull I info, int pos) {

        if (!super.add(info, pos) || !addNoSerialize(info, pos)) {
            return false;
        }

        if (!writeUploadInfoToFile(info)) {
            logger.error("can't write upload info " + info.uploadFile + " to file");
        }

        return true;
    }

    @Override
    public boolean set(@NonNull I info, int pos) {

        if (!super.set(info, pos)) {
            return false;
        }

        synchronized (uploadList) {
            I previous = uploadList.set(pos, info);
            if (!deleteFileByUploadInfo(previous)) {
                logger.error("can't delete file by upload info " + info);
            }
            if (!writeUploadInfoToFile(info)) {
                logger.error("can't write upload info " + info + " to file");
            }
            return true;
        }
    }

    @Override
    @Nullable
    public I remove(@NonNull I info) {

        if (isDisposed()) {
            throw new IllegalStateException("release() was called");
        }

        synchronized (uploadList) {

            if (isEmpty()) {
                logger.error("queue is empty");
                return null;
            }

            if (uploadList.remove(info)) {

                if (!deleteFileByUploadInfo(info)) {
                    logger.error("can't delete file by upload info " + info.uploadFile);
                }

                synchronized (storageListeners) {
                    if (storageListeners.size() > 0) {
                        for (StorageListener l : storageListeners) {
                            l.onStorageSizeChanged(getSize());
                        }
                    }
                }

                return info;
            }

            return null;
        }
    }

    @Override
    public void clear() {

        if (isDisposed()) {
            throw new IllegalStateException("release() was called");
        }

        synchronized (uploadList) {
            uploadList.clear();
            uploadList = null;
        }
    }
}
