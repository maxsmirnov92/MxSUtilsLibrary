package net.maxsmr.networkutils.loadstorage.storage.collection;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

import net.maxsmr.networkutils.loadstorage.LoadInfo;


public class QueueLoadStorage<I extends LoadInfo> extends AbstractCollectionLoadStorage<I> {

    private final static Logger logger = LoggerFactory.getLogger(QueueLoadStorage.class);

    private LinkedBlockingDeque<I> loadQueue;

    /**
     * @param maxSize                max loadQueue elements
     * @param sync                   is synchronization needed when adding and removing to loadQueue
     * @param allowDeleteFiles       allow delete files to upload when clearing loadQueue
     * @param queueDirPath           path when serialized {@link LoadInfo} files stored
     * @param restoreQueuesFromFiles allow restore {@link LoadInfo} from files on create
     */
    public QueueLoadStorage(Class<I> clazz, int maxSize, boolean sync, boolean allowDeleteFiles, @Nullable String queueDirPath, boolean restoreQueuesFromFiles, @Nullable StorageListener listener) {
        super(clazz, sync, allowDeleteFiles, queueDirPath);
        init(maxSize);

        addStorageListener(listener);

        if (restoreQueuesFromFiles) {
            startRestoreThread();
        }
    }

    private void init(int maxQueueSize) {
        logger.debug("init(), maxQueueSize=" + maxQueueSize);
        loadQueue = maxQueueSize > 0 ? new LinkedBlockingDeque<I>(maxQueueSize) : new LinkedBlockingDeque<I>();
    }

    @Override
    protected synchronized boolean isDisposed() {
        return loadQueue == null;
    }

    @Override
    public int getMaxSize() {
        if (isDisposed()) {
            throw new IllegalStateException("release() was called");
        }
        return loadQueue.size() + loadQueue.remainingCapacity();
    }

    public int getSize() {
        if (isDisposed()) {
            throw new IllegalStateException("release() was called");
        }
        return loadQueue.size();
    }

    @Override
    public boolean contains(@Nullable I info) {
        if (isDisposed()) {
            throw new IllegalStateException("release() was called");
        }
        return loadQueue.contains(info);
    }

    @Override
    @NonNull
    public Iterator<I> iterator() {
        if (isDisposed()) {
            throw new IllegalStateException("release() was called");
        }
        return new WrappedIterator(loadQueue.iterator());
    }

    @Override
    @NonNull
    public synchronized List<I> getAll() {

        if (isDisposed()) {
            throw new IllegalStateException("release() was called");
        }

        final Iterator<I> it = loadQueue.iterator();

        final List<I> list = new ArrayList<>();
        while (it.hasNext()) {
            list.add(it.next());
        }
        return list;
    }

    @NonNull
    private I poll(boolean first) {

        if (isDisposed()) {
            throw new IllegalStateException("release() was called");
        }

        // logger.debug("loadQueue size is: " + loadQueue.size());

        if (isEmpty()) {
            throw new IllegalStateException("loadQueue is empty");
        }

        int prev = getSize();

        I uploadInfo = first ? loadQueue.pollFirst() : loadQueue.pollLast();
        // logger.info("this loadQueue size: " + loadQueue.size());
        // logger.info("all queues size: " + getUploadDequesSize());
        if (!deleteFileByLoadInfo(uploadInfo)) {
            logger.error("can't delete file by upload info with upload file " + uploadInfo.uploadFile);
        }
        dispatchStorageSizeChanged(prev);
        return uploadInfo;
    }

    /**
     * removes first element of loadQueue
     */
    @NonNull
    @Override
    public I pollFirst() {
        return poll(true);
    }

    /**
     * removes last element of loadQueue
     */
    @NonNull
    @Override
    public I pollLast() {
        return poll(false);
    }

    @NonNull
    private I peek(boolean first) {
        if (isDisposed()) {
            throw new IllegalStateException("release() was called");
        }
        // logger.debug("loadQueue size is: " + loadQueue.size());
        if (isEmpty()) {
            throw new IllegalStateException("loadQueue is empty");
        }
        return first ? loadQueue.peekFirst() : loadQueue.peekLast();
    }

    @NonNull
    @Override
    public I peekFirst() {
        return peek(true);
    }

    @NonNull
    @Override
    public I peekLast() {
        return peek(false);
    }

    @Override
    protected boolean addNoSerialize(@NonNull I info, int pos) {
        if (super.addNoSerialize(info, pos)) {
            if (loadQueue.remainingCapacity() > 0) {
                logger.debug("adding upload info to loadQueue (file: " + info.uploadFile + ")...");
                int prev = getSize();
                final boolean addResult = loadQueue.add(info);
                if (addResult) {
                    dispatchStorageSizeChanged(prev);
                }
                return addResult;
            } else {
                logger.error("no capacity remains in this loadQueue");
            }
        }
        return false;
    }

    @Override
    public boolean add(@NonNull I info, int pos) {
        int size = getSize();
        if (pos != (size > 0 ? size - 1 : 0)) {
            throw new UnsupportedOperationException("adding to specified position is not supported");
        }
        if (!super.add(info, pos) || !addNoSerialize(info, pos)) {
            return false;
        }
        if (!writeLoadInfoToFile(info)) {
            logger.error("can't write upload info with file " + info.uploadFile + " to file");
        }
        return true;
    }

    @Override
    public boolean set(@NonNull I info, int pos) {
        if (!super.set(info, pos)) {
            return false;
        }
        throw new UnsupportedOperationException("setting element at specified position is not supported");
    }

    @Override
    @Nullable
    public I remove(@Nullable I info) {

        if (isDisposed()) {
            throw new IllegalStateException("release() was called");
        }

        if (isEmpty()) {
            throw new IllegalStateException("loadQueue is empty");
        }

        if (!contains(info)) {
            logger.error("loadQueue not contains " + info);
            return null;
        }

        int prev = getSize();
        if (loadQueue.remove(info)) {
            if (!deleteFileByLoadInfo(info)) {
                logger.error("can't delete file by upload info " + info);
            }
            dispatchStorageSizeChanged(prev);
            return info;
        }

        return null;
    }

    @Override
    public void clear() {
        clear(false, false);
    }

    @Override
    protected void clearNoDelete() {
        if (isDisposed()) {
            throw new IllegalStateException("release() was called");
        }
        loadQueue.clear();
    }

    @Override
    public synchronized void release() {
        super.release();
        loadQueue = null;
    }

    private class WrappedIterator implements Iterator<I> {

        final Iterator<I> iterator;

        I next;

        private WrappedIterator(Iterator<I> iterator) {
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
            if (!deleteFileByLoadInfo(next)) {
                logger.error("can't delete file by upload info " + next);
            }
            dispatchStorageSizeChanged(prev);
            next = null;
        }
    }
}
