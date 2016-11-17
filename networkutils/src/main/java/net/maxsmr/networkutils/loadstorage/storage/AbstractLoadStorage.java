package net.maxsmr.networkutils.loadstorage.storage;


import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import net.maxsmr.networkutils.loadstorage.LoadInfo;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static net.maxsmr.tasksutils.taskexecutor.RunnableInfo.NO_ID;

public abstract class AbstractLoadStorage<I extends LoadInfo> {

    public interface StorageListener {

        void onStorageSizeChanged(int size);

        void onStorageRestored(int restoredElementsCount);
    }

    protected final Set<StorageListener> storageListeners = new LinkedHashSet<>();

    @NonNull
    protected final Class<I> loadInfoClass;

    public AbstractLoadStorage(@NonNull Class<I> loadInfoClass) {
        this.loadInfoClass = loadInfoClass;
    }

    @NonNull
    public Class<I> getLoadInfoClass() {
        return loadInfoClass;
    }

    public void addStorageListener(@NonNull StorageListener listener) throws NullPointerException {
        synchronized (storageListeners) {
            storageListeners.add(listener);
        }
    }

    public void removeStorageListener(@NonNull StorageListener listener) {
        synchronized (storageListeners) {
            storageListeners.remove(listener);
        }
    }


    protected abstract boolean isDisposed();

    public static final int MAX_SIZE_UNLIMITED = 0;

    public abstract int getMaxSize();

    public boolean isEmpty() {
        if (isDisposed()) {
            throw new IllegalStateException("release() was called");
        }
        return getSize() == 0;
    }

    public abstract int getSize();

    public int getMaxId() {
        if (isDisposed()) {
            throw new IllegalStateException("release() was called");
        }
        int maxId = NO_ID;
        Iterator<I> it = iterator();
        while (it.hasNext()) {
            I elem = it.next();
            if (elem != null && elem.id > maxId) {
                maxId = elem.id;
            }
        }
        return maxId;
    }

    public int getMinId() {
        if (isDisposed()) {
            throw new IllegalStateException("release() was called");
        }
        int minId = NO_ID;
        Iterator<I> it = iterator();
        while (it.hasNext()) {
            I elem = it.next();
            if (elem != null && (elem.id < minId || minId == NO_ID)) {
                minId = elem.id;
            }
        }
        return minId;
    }

    @Nullable
    public final I findById(int id) {
        if (isDisposed()) {
            throw new IllegalStateException("release() was called");
        }
        for (I i : getAll()) {
            if (i != null && i.id == id) {
                return i;
            }
        }
        return null;
    }

    public synchronized final boolean contains(int id) {
        return findById(id) != null;
    }

    public synchronized final boolean contains(@Nullable I info) {
        return info != null && contains(info.id);
    }

    @NonNull
    public abstract Iterator<I> iterator();

    /**
     * @return array list containing elements of storage
     */
    @NonNull
    public abstract List<I> getAll();

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

    protected boolean checkAddElement(@NonNull I info, int pos) {
        if (isDisposed()) {
            throw new IllegalStateException("release() was called");
        }
        checkRangeAdd(pos);
        int size = getSize();
        int maxSize = getMaxSize();
        return !contains(info) && (size < maxSize || maxSize == MAX_SIZE_UNLIMITED);
    }

    protected boolean checkSetElement(@NonNull I info, int pos) {
        if (isDisposed()) {
            throw new IllegalStateException("release() was called");
        }
        checkRange(pos);
        return findById(info.id) == null;
    }

    @NonNull
    public abstract I get(int index);

    /**
     * @return retrieved and removed first storage element
     */
    @NonNull
    public abstract I pollFirst();

    /**
     * @return retrieved and removed last storage element
     */
    @NonNull
    public abstract I pollLast();

    /**
     * @return retrieved (but not removed) first storage element
     */
    @NonNull
    public abstract I peekFirst();

    /**
     * @return retrieved (but not removed) last storage element
     */
    @NonNull
    public abstract I peekLast();

    /**
     * adding to the end of storage
     */
    @CallSuper
    public final boolean add(@NonNull I info) {
        final int size = getSize();
        return add(info, size > 0 ? size - 1 : 0);
    }

    @CallSuper
    public boolean add(@NonNull I info, int pos) {
        return checkAddElement(info, pos);
    }

    @CallSuper
    public boolean set(@NonNull I info, int pos) {
        return checkSetElement(info, pos);
    }

    @Nullable
    public I remove(int index) {
        return remove(get(index));
    }

    @Nullable
    public abstract I remove(@Nullable I info);

    @Nullable
    public I removeById(int id) {
        I info = findById(id);
        return info != null ? remove(info) : null;
    }

    /**
     * dispose storage (must be re-created for next use)
     */
    public abstract void clear();

    @CallSuper
    public void release() {
        if (isDisposed()) {
            throw new IllegalStateException("release() was called");
        }
        clear();
        synchronized (storageListeners) {
            storageListeners.clear();
        }
    }

    protected final void dispatchStorageRestored(int elemCount) {
        synchronized (storageListeners) {
            if (storageListeners.size() > 0) {
                for (StorageListener l : storageListeners) {
                    l.onStorageRestored(elemCount);
                }
            }
        }
    }

    protected final void dispatchStorageSizeChanged(int previousSize) {
        if (previousSize < 0) {
            throw new IllegalArgumentException("incorrect previousSize: " + previousSize);
        }
        int size = getSize();
        if (previousSize != size) {
            synchronized (storageListeners) {
                if (storageListeners.size() > 0) {
                    for (StorageListener l : storageListeners) {
                        l.onStorageSizeChanged(size);
                    }
                }
            }
        }
    }
}
