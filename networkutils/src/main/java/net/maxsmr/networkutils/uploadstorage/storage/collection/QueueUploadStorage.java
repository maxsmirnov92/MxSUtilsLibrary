package net.maxsmr.networkutils.uploadstorage.storage.collection;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

import net.maxsmr.networkutils.uploadstorage.UploadInfo;

public class QueueUploadStorage<I extends UploadInfo> extends AbstractCollectionUploadStorage<I> {

    private final static Logger logger = LoggerFactory.getLogger(QueueUploadStorage.class);

    private LinkedBlockingDeque<I> queue;

    /**
     * @param maxSize                max queue elements
     * @param sync                   is synchronization needed when adding and removing to queue
     * @param allowDeleteFiles       allow delete files to upload when clearing queue
     * @param queueDirPath           path when serialized {@link UploadInfo} files stored
     * @param restoreQueuesFromFiles allow restore {@link UploadInfo} from files on create
     */
    public QueueUploadStorage(Class<I> clazz, int maxSize, boolean sync, boolean allowDeleteFiles, @Nullable String queueDirPath, boolean restoreQueuesFromFiles, @Nullable StorageListener listener) {
        super(clazz, sync, allowDeleteFiles, queueDirPath);
        init(maxSize);

        addStorageListener(listener);

        if (restoreQueuesFromFiles) {
            startRestoreThread();
        }
    }

    private void init(int maxQueueSize) {
        logger.debug("init(), maxQueueSize=" + maxQueueSize);
        this.queue = maxQueueSize > 0 ? new LinkedBlockingDeque<I>(maxQueueSize) : new LinkedBlockingDeque<I>();
    }

    @Override
    protected boolean isDisposed() {
        return queue == null;
    }

    @Override
    public int getMaxSize() {

        if (isDisposed()) {
            throw new IllegalStateException("release() was called");
        }

        return queue.size() + queue.remainingCapacity();
    }

    public int getSize() {

        if (isDisposed()) {
            throw new IllegalStateException("release() was called");
        }

        return queue.size();
    }

    @Override
    public boolean contains(@Nullable I info) {
        if (isDisposed()) {
            throw new IllegalStateException("release() was called");
        }
        return queue.contains(info);
    }

    @Override
    @NonNull
    public List<I> getAll() {

        if (isDisposed()) {
            throw new IllegalStateException("release() was called");
        }

        final Iterator<I> it = queue.iterator();

        final List<I> list = new ArrayList<>();

        while (it.hasNext()) {
            list.add(it.next());
        }
        return list;
//        return (List<I>) new ArrayList<I>(Arrays.asList(queue.toArray())); // new UploadInfo[getSize()]
    }

    @NonNull
    private I poll(boolean first) {

        if (isDisposed()) {
            throw new IllegalStateException("release() was called");
        }

        synchronized (queue) {

            // logger.debug("queue size is: " + queue.size());

            if (isEmpty()) {
                logger.error("queue is empty");
                return null;
            }

            I uploadInfo = first? queue.pollFirst() : queue.pollLast();

            if (uploadInfo != null) {

                // logger.info("this queue size: " + queue.size());
                // logger.info("all queues size: " + getUploadDequesSize());

                if (!deleteFileByUploadInfo(uploadInfo)) {
                    logger.error("can't delete file by upload info with upload file " + uploadInfo.uploadFile);
                }

                synchronized (storageListeners) {
                    if (storageListeners.size() > 0) {
                        for (StorageListener l : storageListeners) {
                            l.onStorageSizeChanged(getSize());
                        }
                    }
                }

            } else {
                logger.error("polled uploadInfo is null");
            }

            return uploadInfo;
        }
    }

    /**
     * removes first element of queue
     */
    @NonNull
    @Override
    public I pollFirst() {
        return poll(true);
    }

    /**
     * removes last element of queue
     */
    @NonNull
    @Override
    public I pollLast() {
        return poll(false);
    }


    @Override
    protected boolean addNoSerialize(@NonNull I info, int pos) {
        if (super.addNoSerialize(info, pos)) {

            synchronized (queue) {

                if (queue.remainingCapacity() > 0) {

                    logger.debug("adding upload info to queue (file: " + info.uploadFile + ")...");

                    try {

                        final boolean addResult = queue.add(info);

                        if (addResult) {

                            synchronized (storageListeners) {
                                if (storageListeners.size() > 0) {
                                    for (StorageListener l : storageListeners) {
                                        l.onStorageSizeChanged(getSize());
                                    }
                                }
                            }

                        }

                        return addResult;

                    } catch (Exception e) {
                        logger.error("an Exception occured during add(): " + e.getMessage());
                    }
                } else {
                    logger.error("no capacity remains in this queue");
                }
            }

        }

        return false;
    }

    @Override
    public boolean add(@NonNull I info, int pos) {
        if (pos != (getSize() > 0? getSize() - 1 : 0)) {
            throw new UnsupportedOperationException("adding to specified position is not supported");
        }
        if (!super.add(info, pos) || !addNoSerialize(info, pos)) {
            return false;
        }

        if (!writeUploadInfoToFile(info)) {
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
    public I remove(@NonNull I info) {

        if (isDisposed()) {
            throw new IllegalStateException("release() was called");
        }

        synchronized (queue) {

            if (isEmpty()) {
                logger.error("queue is empty");
                return null;
            }

            if (!queue.contains(info)) {
                logger.error("queue not contains " + info);
                return null;
            }

            if (queue.remove(info)) {

                if (!deleteFileByUploadInfo(info)) {
                    logger.error("can't delete file by upload info " + info);
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
        }

        return null;
    }

    @Override
    public void clear() {

        if (isDisposed()) {
            throw new IllegalStateException("release() was called");
        }

        queue.clear();
        queue = null;
    }
}
