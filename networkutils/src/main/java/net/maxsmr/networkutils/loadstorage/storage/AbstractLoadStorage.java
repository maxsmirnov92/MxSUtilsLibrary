package net.maxsmr.networkutils.loadstorage.storage;


import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.maxsmr.networkutils.loadstorage.LoadInfo;

public abstract class AbstractLoadStorage<I extends LoadInfo> {

    public interface StorageListener {

        void onStorageSizeChanged(int size);

        void onStorageRestored(int restoredElementsCount);
    }

    protected final LinkedList<StorageListener> storageListeners = new LinkedList<>();

    public void addStorageListener(StorageListener listener) throws NullPointerException {

        if (listener == null) {
            throw new NullPointerException();
        }

        synchronized (storageListeners) {
            if (!storageListeners.contains(listener)) {
                storageListeners.add(listener);
            }
        }
    }

    public void removeStorageListener(StorageListener listener) {
        synchronized (storageListeners) {
            if (storageListeners.contains(listener)) {
                storageListeners.remove(listener);
            }
        }
    }

    @NonNull
    protected final Class<I> loadInfoClass;

    public AbstractLoadStorage(@NonNull Class<I> loadInfoClass) {
        this.loadInfoClass = loadInfoClass;
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

    @Nullable
    public I findById(int id) {
        if (isDisposed()) {
            throw new IllegalStateException("release() was called");
        }
        List<I> infos = getAll();
        for (I i : infos) {
            if (i != null && i.id == id) {
                return i;
            }
        }
        return null;
    }

    public boolean contains(@Nullable I info) {
        if (info != null) {
            for (I i : getAll()) {
                if (i != null && i.id == info.id)
                    return true;
            }
        }
        return false;
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
        return findById(info.id) == null && (size < maxSize || maxSize == MAX_SIZE_UNLIMITED);
    }

    protected boolean checkSetElement(@NonNull I info, int pos) {
        if (isDisposed()) {
            throw new IllegalStateException("release() was called");
        }
        checkRange(pos);
        return findById(info.id) == null;
    }

    @SuppressWarnings("ConstantConditions")
    @NonNull
    @CallSuper
    public final I get(int index) {
        if (isDisposed()) {
            throw new IllegalStateException("release() was called");
        }
        checkRange(index);
        return null;
    }

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
