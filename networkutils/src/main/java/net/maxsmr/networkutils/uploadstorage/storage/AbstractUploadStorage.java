package net.maxsmr.networkutils.uploadstorage.storage;


import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.LinkedList;
import java.util.List;

import net.maxsmr.networkutils.uploadstorage.UploadInfo;

public abstract class AbstractUploadStorage<I extends UploadInfo> {

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
    protected final Class<I> uploadInfoClass;

    public AbstractUploadStorage(@NonNull Class<I> uploadInfoClass) {
        this.uploadInfoClass = uploadInfoClass;
    }

    protected abstract boolean isDisposed();

    public static final int MAX_SIZE_UNLIMITED = 0;

    public abstract int getMaxSize();

    public boolean isEmpty() {
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

    public abstract boolean contains(@Nullable I info);

    /**
     * @return array list containing elements of storage
     */
    @NonNull
    public abstract List<I> getAll();

    @NonNull
    public final I get(int index) {
        if (index < 0 || index >= getSize()) {
            throw new IllegalArgumentException("index " + index + " is out of bounds");
        }
        return getAll().get(index);
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

    protected boolean checkAddElement(@NonNull I info, int pos) {

        if (isDisposed()) {
            throw new IllegalStateException("release() was called");
        }

        boolean empty = pos == 0 && getSize() == 0;
        if ((pos < 0 || pos >= getSize()) && !empty) {
            throw new IllegalArgumentException("index " + pos + " is out of bounds");
        }

        return !contains(info) && findById(info.id) == null && (getSize() < getMaxSize() || getMaxSize() == MAX_SIZE_UNLIMITED);
    }

    protected boolean checkSetElement(@NonNull I info, int pos) {

        if (isDisposed()) {
            throw new IllegalStateException("release() was called");
        }

        if (pos < 0 || pos >= getSize()) {
            throw new IllegalArgumentException("index " + pos + " is out of bounds");
        }

        return !contains(info) && findById(info.id) == null;
    }

    @Nullable
    public I remove(int index) {
        return remove(get(index));
    }

    @Nullable
    public abstract I remove(@NonNull I info);

    @Nullable
    public I removeById(int id) {
        I info = findById(id);
        return info != null ? remove(info) : null;
    }

    /**
     * dispose storage (must be re-created for next use)
     */
    public abstract void clear();

    public void release() {

        if (isDisposed()) {
            throw new IllegalStateException("release() was called");
        }

        clear();
    }
}
