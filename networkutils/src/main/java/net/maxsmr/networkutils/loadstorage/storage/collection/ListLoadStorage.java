package net.maxsmr.networkutils.loadstorage.storage.collection;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.maxsmr.networkutils.loadstorage.LoadInfo;


public class ListLoadStorage<I extends LoadInfo> extends AbstractCollectionLoadStorage<I> {

    private final static Logger logger = LoggerFactory.getLogger(ListLoadStorage.class);

    private int maxSize = MAX_SIZE_UNLIMITED;

    private List<I> loadList;

    /**
     * @param maxSize                max list elements
     * @param sync                   is synchronization needed when adding and removing polto queue
     * @param allowDeleteFiles       allow delete files to upload when clearing queue
     * @param listDirPath            path when serialized {@link LoadInfo} files stored
     * @param restoreQueuesFromFiles allow restore {@link LoadInfo} from files on create
     */
    public ListLoadStorage(Class<I> clazz, int maxSize, boolean sync, boolean allowDeleteFiles, @Nullable String listDirPath, boolean restoreQueuesFromFiles, @Nullable StorageListener listener) {
        super(clazz, sync, allowDeleteFiles, listDirPath);
        init(maxSize);

        addStorageListener(listener);

        if (restoreQueuesFromFiles) {
            startRestoreThread();
        }
    }

    private void init(int maxListSize) {
        logger.debug("init(), maxListSize=" + maxListSize);
        loadList = (maxSize = maxListSize) > 0 ? new ArrayList<I>(maxListSize) : new ArrayList<I>();
    }

    @Override
    protected synchronized boolean isDisposed() {
        return loadList == null;
    }

    @Override
    public synchronized int getMaxSize() {
        if (isDisposed()) {
            throw new IllegalStateException("release() was called");
        }
        return maxSize;
    }

    @Override
    public synchronized int getSize() {
        if (isDisposed()) {
            throw new IllegalStateException("release() was called");
        }
        return loadList.size();
    }

    @Override
    public synchronized boolean contains(@Nullable I info) {
        if (isDisposed()) {
            throw new IllegalStateException("release() was called");
        }
        return loadList.contains(info);
    }

    @NonNull
    @Override
    public Iterator<I> iterator() {
        if (isDisposed()) {
            throw new IllegalStateException("release() was called");
        }
        return loadList.iterator();
    }

    @Override
    @NonNull
    public synchronized List<I> getAll() {
        if (isDisposed()) {
            throw new IllegalStateException("release() was called");
        }
        return new ArrayList<>(loadList);
    }

    @NonNull
    @Override
    public synchronized I pollFirst() {
        try {
            return get(0);
        } finally {
            remove(0);
        }
    }

    @NonNull
    @Override
    public synchronized I pollLast() {
        int size = getSize();
        int index = size < 1 ? 0 : size - 1;
        try {
            return get(index);
        } finally {
            remove(index);
        }
    }

    @NonNull
    @Override
    public synchronized I peekFirst() {
        return get(0);
    }

    @NonNull
    @Override
    public synchronized I peekLast() {
        int size = getSize();
        int index = size < 1 ? 0 : size - 1;
        return get(index);
    }

    @Override
    protected synchronized boolean addNoSerialize(@NonNull I info, int pos) {
        if (super.addNoSerialize(info, pos)) {
            int prev = getSize();
            loadList.add(pos, info);
            dispatchStorageSizeChanged(prev);
            return contains(info);
        }
        return false;
    }

    @Override
    public synchronized boolean add(@NonNull I info, int pos) {
        if (!super.add(info, pos) || !addNoSerialize(info, pos)) {
            return false;
        }
        if (!writeLoadInfoToFile(info)) {
            logger.error("can't write upload info " + info.uploadFile + " to file");
        }
        return true;
    }

    @Override
    public synchronized boolean set(@NonNull I info, int pos) {
        if (!super.set(info, pos)) {
            return false;
        }
        I previous = loadList.set(pos, info);
        if (!deleteFileByLoadInfo(previous)) {
            logger.error("can't delete file by upload info " + info);
        }
        if (!writeLoadInfoToFile(info)) {
            logger.error("can't write upload info " + info + " to file");
        }
        return true;
    }

    @Override
    @Nullable
    public synchronized I remove(@Nullable I info) {

        if (isDisposed()) {
            throw new IllegalStateException("release() was called");
        }

        if (isEmpty()) {
            throw new IllegalStateException("queue is empty");
        }

        if (!contains(info)) {
            logger.error("queue not contains " + info);
            return null;
        }

        int prev = getSize();
        if (loadList.remove(info)) {
            if (!deleteFileByLoadInfo(info)) {
                logger.error("can't delete file by upload info " + (info != null ? info.uploadFile : null));
            }
            dispatchStorageSizeChanged(prev);
            return info;
        }

        return null;
    }

    @Override
    public synchronized void clear() {
        clear(false, false);
    }

    @Override
    protected void clearNoDelete() {
        if (isDisposed()) {
            throw new IllegalStateException("release() was called");
        }
        loadList.clear();
    }

    @Override
    public synchronized void release() {
        super.release();
        loadList = null;
    }
}
