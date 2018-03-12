package net.maxsmr.tasksutils.storage.sync;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;

import net.maxsmr.commonutils.data.Observable;
import net.maxsmr.tasksutils.CustomHandlerThread;
import net.maxsmr.tasksutils.handler.HandlerRunnable;
import net.maxsmr.tasksutils.taskexecutor.RunnableInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import static net.maxsmr.tasksutils.taskexecutor.RunnableInfo.NO_ID;
import static net.maxsmr.tasksutils.taskexecutor.RunnableInfo.fromByteArray;
import static net.maxsmr.tasksutils.taskexecutor.RunnableInfo.fromInputStream;

public abstract class AbstractSyncStorage<I extends RunnableInfo> {

    public static final int MAX_SIZE_UNLIMITED = 0;

    protected final StorageObservable storageObservable = new StorageObservable();

    protected final Logger logger;

    @NonNull
    protected final Class<I> runnableInfoClass;

    protected boolean allowSync;

    protected int maxSize = MAX_SIZE_UNLIMITED;

    protected IAddRule<I> addRule;

    @Nullable
    protected Handler callbacksHandler;

    private boolean isDisposed;

    private boolean isRestoreCompleted;

    private CustomHandlerThread restoreThread;

    public AbstractSyncStorage(@NonNull Class<I> runnableInfoClass,
                               boolean allowSync, int maxSize, @Nullable IAddRule<I> addRule) {
        this.logger = LoggerFactory.getLogger(getClass());
        this.runnableInfoClass = runnableInfoClass;
        setAllowSync(allowSync);
        setMaxSize(maxSize);
        setAddRule(addRule);
    }

    public boolean isRestoreCompleted() {
        return isRestoreCompleted;
    }

    protected boolean isRestoreThreadRunning() {
        return (restoreThread != null && restoreThread.isAlive());
    }

    protected void startRestoreThread() {

        if (isRestoreThreadRunning()) {
            return;
        }

        restoreThread = new CustomHandlerThread(getClass().getSimpleName() + ":RestoreThread",
                new CustomHandlerThread.ILooperPreparedListener() {
                    @Override
                    public void onLooperPrepared() {
                        restoreThread.addTask(new RestoreRunnable());
                    }
                });
        restoreThread.start();
    }

    protected void stopRestoreThread() {

        if (!isRestoreThreadRunning()) {
            return;
        }

        restoreThread.interrupt();
        restoreThread.quit();
        restoreThread = null;
    }

    protected abstract int restoreStorage();

    @NonNull
    public Class<I> getRunnableInfoClass() {
        return runnableInfoClass;
    }

    public void addStorageListener(@NonNull IStorageListener listener) {
        storageObservable.registerObserver(listener);
    }

    public void removeStorageListener(@NonNull IStorageListener listener) {
        storageObservable.unregisterObserver(listener);
    }

    public final boolean isDisposed() {
        return isDisposed;
    }

    public final boolean isAllowSync() {
        return allowSync;
    }

    public void setAllowSync(boolean allowSync) {
        this.allowSync = allowSync;
    }

    public boolean isEmpty() {
        if (isDisposed()) {
            throw new IllegalStateException("release() was called");
        }
        return getSize() == 0;
    }

    public abstract int getSize();

    public final int getMaxSize() {
        if (isDisposed()) {
            throw new IllegalStateException("release() was called");
        }
        return maxSize;
    }

    public void setMaxSize(int maxSize) {
        if (isDisposed()) {
            throw new IllegalStateException("release() was called");
        }
        if (maxSize <= 0 && maxSize != MAX_SIZE_UNLIMITED) {
            throw new IllegalArgumentException("incorrect max size: " + maxSize);
        }
        this.maxSize = maxSize;
    }

    @Nullable
    public IAddRule getAddRule() {
        return addRule;
    }

    public void setAddRule(@Nullable IAddRule<I> addRule) {
        this.addRule = addRule;
    }


    @Nullable
    public Handler getCallbacksHandler() {
        return callbacksHandler;
    }

    public void setCallbacksHandler(@Nullable Handler callbacksHandler) {
        this.callbacksHandler = callbacksHandler;
    }

    @Nullable
    public Pair<Integer, I> findByMinMaxId(boolean min) {
        if (isDisposed()) {
            throw new IllegalStateException("release() was called");
        }
        Pair<Integer, I> result = null;
        Iterator<I> it = iterator();
        for (int index = 0; it.hasNext(); index++) {
            I item = it.next();
            if (item != null) {
                if (result == null) {
                    result = new Pair<>(index, item);
                }
                if (min ? item.id < result.second.id : item.id > result.second.id || result.second.id == NO_ID) {
                    result = new Pair<>(index, item);
                }
            }
        }
        return result;
    }

    @Nullable
    public synchronized final Pair<Integer, I> findById(int id) {
        if (isDisposed()) {
            throw new IllegalStateException("release() was called");
        }
        Pair<Integer, I> result = null;
        int index = 0;
        Iterator<I> iterator = iterator();
        while (iterator.hasNext()) {
            I item = iterator.next();
            if (item != null && item.id == id) {
                result = new Pair<>(index, item);
                break;
            }
            index++;
        }
        return result;
    }

    public boolean contains(int id) {
        return findById(id) != null;
    }

    public boolean contains(@Nullable I info) {
        return info != null && contains(info.id);
    }

    @NonNull
    public abstract Iterator<I> iterator();

    /**
     * @return array list containing elements of storage
     */
    @NonNull
    public abstract List<I> getAll();

    @Nullable
    public abstract I get(int index);

    /**
     * @return retrieved and removed first storage element
     */
    @Nullable
    public abstract I pollFirst();

    /**
     * @return retrieved and removed last storage element
     */
    @Nullable
    public abstract I pollLast();

    /**
     * @return retrieved (but not removed) first storage element
     */
    @Nullable
    public abstract I peekFirst();

    /**
     * @return retrieved (but not removed) last storage element
     */
    @Nullable
    public abstract I peekLast();

    public final boolean addFirst(I info) {
        return add(info, 0);
    }

    public final boolean addLast(I info) {
        int size = getSize();
        return add(info, size > 0 ? size - 1 : 0);
    }

    @CallSuper
    public synchronized final boolean add(I info, int index) {

        if (isDisposed()) {
            throw new IllegalStateException("release() was called");
        }

        boolean allowAddIfFull = addRule != null && addRule.allowAddIfFull();
        boolean result = checkAddElement(info, index, !allowAddIfFull);
        if (result) {
            if (allowAddIfFull && isMaxSizeReached()) {
                addRule.removeAny(this);
                result = !isMaxSizeReached();
            }
        }
        int previousSize = getSize();
        if (result && addInternal(info, index)) {
            if (!serializeRunnableInfo(info)) {
                logger.error("can't serialize info " + info);
            }
            storageObservable.dispatchStorageSizeChanged(getSize(), previousSize, callbacksHandler);
            return true;
        }
        return false;
    }

    protected final boolean addInternal(@NonNull I info) {
        final int size = getSize();
        return addInternal(info, size > 0 ? size - 1 : 0);
    }

    // no check needed
    protected abstract boolean addInternal(I info, int index);

    public final boolean set(I info, int index) {

        if (isDisposed()) {
            throw new IllegalStateException("release() was called");
        }

        if (!checkSetElement(info, index)) {
            return false;
        }

        I previous = get(index);

        if (setInternal(info, index)) {

            if (!deleteSerializedRunnableInfo(previous)) {
                logger.error("can't delete file by info " + info);
            }
            if (!serializeRunnableInfo(info)) {
                logger.error("can't write info " + info + " to file");
            }

            return true;
        }

        return false;
    }

    // no check needed
    protected abstract boolean setInternal(@NonNull I info, int index);

    @Nullable
    public final Pair<Integer, I> remove(@Nullable I info) {
        return removeById(info != null ? info.id : NO_ID);
    }

    @Nullable
    public final Pair<Integer, I> removeById(int id) {
        Pair<Integer, I> info = findById(id);
        if (info != null) {
            //noinspection ConstantConditions
            if (remove(info.first) != null) {
                return info;
            }
        }
        return null;
    }

    @Nullable
    public final synchronized I remove(int index) {

        int prev = getSize();

        I info = removeInternal(index);

        if (info != null) {
            if (!deleteSerializedRunnableInfo(info)) {
                logger.error("can't delete serialized info " + info);
            }
            storageObservable.dispatchStorageSizeChanged(getSize(), prev, callbacksHandler);
            return info;
        }

        return null;
    }

    @Nullable
    public final I removeFirst() {
        return !isEmpty() ? remove(0) : null;
    }

    @Nullable
    public final I removeLast() {
        return !isEmpty() ? remove(getSize() - 1) : null;
    }

    // no check needed
    @Nullable
    protected abstract I removeInternal(int index);

    /**
     * with removing serialized data
     */
    public final void clear() {
        if (isDisposed()) {
            throw new IllegalStateException("release() was called");
        }
        int prev = getSize();
        clearNoDelete();
        if (deleteAllSerializedRunnableInfos()) {
            logger.error("can't delete serialized infos");
        }
        storageObservable.dispatchStorageSizeChanged(getSize(), prev, callbacksHandler);
    }

    protected abstract void clearNoDelete();

    protected abstract boolean serializeRunnableInfo(I info);

    protected abstract boolean deleteSerializedRunnableInfo(I info);

    protected abstract boolean deleteAllSerializedRunnableInfos();

    protected I deserializeRunnableInfoFromByteArray(@Nullable byte[] array) {
        return fromByteArray(runnableInfoClass, array);
    }

    protected I deserializeRunnableInfoFromInputStream(@Nullable InputStream inputStream) {
        return fromInputStream(runnableInfoClass, inputStream);
    }

    @CallSuper
    public void release() {
        if (isDisposed()) {
            throw new IllegalStateException("already disposed");
        }
        stopRestoreThread();
        clearNoDelete();
        storageObservable.unregisterAll();
        isDisposed = true;
    }

    protected boolean isMaxSizeReached() {
        return maxSize != MAX_SIZE_UNLIMITED && getSize() >= maxSize;
    }

    protected final void checkRange(int index) {
        if (index < 0 || index >= getSize()) {
            throw new IndexOutOfBoundsException("index " + index + " is out of bounds");
        }
    }

    protected final void checkRangeAdd(int index) {
        if ((index < 0 || index > getSize())) {
            throw new IndexOutOfBoundsException("index " + index + " is out of bounds");
        }
    }

    protected boolean checkAddElement(@NonNull I info, int index, boolean checkMax) {
        if (isDisposed()) {
            throw new IllegalStateException("release() was called");
        }
        checkRangeAdd(index);
        checkRunnableInfo(info);
        return !contains(info) && (!isMaxSizeReached() || !checkMax);
    }

    protected boolean checkSetElement(@NonNull I info, int index) {
        if (isDisposed()) {
            throw new IllegalStateException("release() was called");
        }
        checkRange(index);
        checkRunnableInfo(info);
        return findById(info.id) == null;
    }

    protected boolean checkRunnableInfo(I info) {
        return info != null && info.isValid();
    }

    protected static class StorageObservable extends Observable<IStorageListener> {

        public void dispatchStorageRestoreStarted(final long startTime, @Nullable Handler handler) {
            Runnable run = new Runnable() {
                @Override
                public void run() {
                    synchronized (mObservers) {
                        for (IStorageListener l : mObservers) {
                            l.onStorageRestoreStarted(startTime);
                        }
                    }
                }
            };
            if (handler != null) {
                handler.post(run);
            } else {
                run.run();
            }
        }

        public void dispatchStorageRestoreFinished(final long endTime, final long processingTime, final int elemCount, @Nullable Handler handler) {
            Runnable run = new Runnable() {
                @Override
                public void run() {
                    synchronized (mObservers) {
                        for (IStorageListener l : mObservers) {
                            l.onStorageRestoreFinished(endTime, processingTime, elemCount);
                        }
                    }
                }
            };
            if (handler != null) {
                handler.post(run);
            } else {
                run.run();
            }
        }

        public void dispatchStorageSizeChanged(final int currentSize, final int previousSize, @Nullable Handler handler) {
            Runnable run = new Runnable() {
                @Override
                public void run() {
                    if (currentSize < 0) {
                        throw new IllegalArgumentException("incorrect currentSize: " + currentSize);
                    }
                    if (previousSize < 0) {
                        throw new IllegalArgumentException("incorrect previousSize: " + previousSize);
                    }
                    if (currentSize != previousSize) {
                        synchronized (mObservers) {
                            for (IStorageListener l : mObservers) {
                                l.onStorageSizeChanged(currentSize, previousSize);
                            }
                        }
                    }
                }
            };
            if (handler != null) {
                handler.post(run);
            } else {
                run.run();
            }
        }
    }

    public interface IAddRule<I extends RunnableInfo> {

        boolean allowAddIfFull();

        void removeAny(AbstractSyncStorage<I> fromStorage);
    }

    public interface IStorageListener {

        void onStorageRestoreStarted(long startTime);

        void onStorageRestoreFinished(long endTime, long processingTime, int restoredElementsCount);

        void onStorageSizeChanged(int currentSize, int previousSize);
    }

    protected class RestoreRunnable extends HandlerRunnable<Integer> {

        public RestoreRunnable() {
            super(new Handler(Looper.myLooper() != null? Looper.myLooper() : Looper.getMainLooper()));
        }

        private long startTime;

        private long endTime;

        @Override
        protected Integer doWork() {
            isRestoreCompleted = false;
            startTime = System.currentTimeMillis();
            try {
                return restoreStorage();
            } finally {
                endTime = System.currentTimeMillis();
                isRestoreCompleted = true;
            }
        }

        @Override
        protected void preExecute() {
            storageObservable.dispatchStorageRestoreStarted(startTime, callbacksHandler);
        }

        @Override
        protected void postExecute(Integer result) {
            long processingTime = endTime > startTime ? endTime - startTime : 0;
            logger.info("restoring complete, time: " + processingTime + " ms");
            logger.info("restored " + runnableInfoClass.getSimpleName() + " objects count: " + result + ", queues total size: " + getSize());
            storageObservable.dispatchStorageRestoreFinished(endTime, processingTime, result, callbacksHandler);
        }
    }

    public static class DefaultAddRule<I extends RunnableInfo> implements IAddRule<I> {

        @Override
        public boolean allowAddIfFull() {
            return false;
        }

        @Override
        public void removeAny(AbstractSyncStorage<I> fromStorage) {
            fromStorage.pollFirst();
        }
    }

}
