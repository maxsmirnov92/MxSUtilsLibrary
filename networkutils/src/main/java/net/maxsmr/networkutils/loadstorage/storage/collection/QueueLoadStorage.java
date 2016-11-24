package net.maxsmr.networkutils.loadstorage.storage.collection;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import net.maxsmr.networkutils.loadstorage.LoadInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;


public class QueueLoadStorage<I extends LoadInfo> extends AbstractCollectionLoadStorage<I> {

    private final static Logger logger = LoggerFactory.getLogger(QueueLoadStorage.class);

    private final int maxSize;

    private ArrayDeque<I> loadQueue;

    /**
     * @param maxSize                max loadQueue elements
     * @param sync                   is synchronization needed when adding and removing to loadQueue
     * @param allowDeleteFiles       allow delete files to load when clearing loadQueue
     * @param queueDirPath           path when serialized {@link LoadInfo} files stored
//     * @param restoreQueuesFromFiles allow restore {@link LoadInfo} from files on create
     */
    public QueueLoadStorage(Class<I> clazz, int maxSize, boolean sync, boolean allowDeleteFiles, @Nullable String queueDirPath, @Nullable StorageListener listener) {
        super(clazz, sync, allowDeleteFiles, queueDirPath);
        if (maxSize <= 0 && maxSize != MAX_SIZE_UNLIMITED) {
            throw new IllegalArgumentException("incorrect max size: " + maxSize);
        }
        this.maxSize = maxSize;
        this.loadQueue = new ArrayDeque<>();
        if (listener != null) {
            addStorageListener(listener);
        }
    }

    @Override
    protected synchronized boolean isDisposed() {
        return loadQueue == null;
    }

    @Override
    public synchronized int getMaxSize() {
        if (isDisposed()) {
            throw new IllegalStateException("release() was called");
        }
        return maxSize;
    }

    public synchronized int getSize() {
        if (isDisposed()) {
            throw new IllegalStateException("release() was called");
        }
        return loadQueue.size();
    }

//    @Override
//    public boolean contains(@Nullable I info) {
//        if (isDisposed()) {
//            throw new IllegalStateException("release() was called");
//        }
//        return loadQueue.contains(info);
//    }

    @Override
    @NonNull
    public Iterator<I> iterator() {
        if (isDisposed()) {
            throw new IllegalStateException("release() was called");
        }
        return new WrappedIterator(loadQueue.iterator());
    }

    /** copy of queue */
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
        return Collections.unmodifiableList(list);
    }

    @NonNull
    @Override
    public I get(int index) {
        throw new UnsupportedOperationException("get from specified position is not supported");
    }

    @NonNull
    private synchronized I poll(boolean first) {

        if (isDisposed()) {
            throw new IllegalStateException("release() was called");
        }

        // logger.debug("loadQueue size is: " + loadQueue.size());

        if (isEmpty()) {
            throw new IllegalStateException("loadQueue is empty");
        }

        int prev = getSize();

        I info = first ? loadQueue.pollFirst() : loadQueue.pollLast();
        // logger.info("this loadQueue size: " + loadQueue.size());
        // logger.info("all queues size: " + getUploadDequesSize());
        if (!deleteFileByLoadInfo(info)) {
            logger.error("can't delete file by info " + info);
        }
        storageObservable.dispatchStorageSizeChanged(getSize(), prev);
        return info;
    }

    /**
     * removes first element of loadQueue
     */
    @NonNull
    @Override
    public synchronized I pollFirst() {
        return poll(true);
    }

    /**
     * removes last element of loadQueue
     */
    @NonNull
    @Override
    public synchronized I pollLast() {
        return poll(false);
    }

    @NonNull
    private synchronized I peek(boolean first) {
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
    public synchronized I peekFirst() {
        return peek(true);
    }

    @NonNull
    @Override
    public I peekLast() {
        return peek(false);
    }

    @Override
    protected synchronized boolean addNoSerialize(@NonNull I info, int pos) {
        if (super.addNoSerialize(info, pos)) {
            if (getMaxSize() == MAX_SIZE_UNLIMITED || getMaxSize() - getSize() > 0) {
                logger.debug("adding info to loadQueue (file: " + info + ")...");
                int prev = getSize();
                final boolean addResult = loadQueue.add(info);
                if (addResult) {
                    storageObservable.dispatchStorageSizeChanged(getSize(), prev);
                }
                return addResult;
            } else {
                logger.error("no capacity remains in this loadQueue");
            }
        }
        return false;
    }

    @Override
    public synchronized boolean add(@NonNull I info, int pos) {
        int size = getSize();
        if (pos != (size > 0 ? size - 1 : 0)) {
            throw new UnsupportedOperationException("adding to specified position is not supported");
        }
        if (!super.add(info, pos) || !addNoSerialize(info, pos)) {
            return false;
        }
        if (!writeLoadInfoToFile(info)) {
            logger.error("can't write info " + info + " to file");
        }
        return true;
    }

    @Override
    public boolean set(@NonNull I info, int pos) {
        super.set(info, pos);
        throw new UnsupportedOperationException("setting element at specified position is not supported");
    }

    @Override
    @Nullable
    public synchronized I remove(@Nullable I info) {

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
        boolean removed = false;
        Iterator<I> iterator = loadQueue.iterator();
        while (iterator.hasNext()) {
            I it = iterator.next();
            if (it.id == info.id) {
                iterator.remove();
                removed = true;
                break;
            }
        }

        if (removed) {
            if (!deleteFileByLoadInfo(info)) {
                logger.error("can't delete file by info " + info);
            }
            storageObservable.dispatchStorageSizeChanged(getSize(), prev);
            return info;
        }

        return null;
    }

    @Override
    public synchronized void clear() {
        clear(false);
    }

    @Override
    protected synchronized void clearNoDelete() {
        if (isDisposed()) {
            throw new IllegalStateException("release() was called");
        }
        loadQueue.clear();
    }

    @Override
    public synchronized void release() {
        super.release();
        clearNoDelete();
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
                logger.error("can't delete file by info " + next);
            }
            storageObservable.dispatchStorageSizeChanged(getSize(), prev);
            next = null;
        }
    }
}
