package net.maxsmr.tasksutils.storage.sync.collection;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import net.maxsmr.tasksutils.taskexecutor.RunnableInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;


public class ListSyncStorage<I extends RunnableInfo> extends AbstractCollectionSyncStorage<I> {

    @NonNull
    private final List<I> dataList;

    /**
     * {@inheritDoc}
     */
    public ListSyncStorage(@Nullable String listDirPath,
                           Class<I> clazz,
                           boolean sync, int maxSize, @NonNull IAddRule<I> addRule) {
        super(listDirPath, clazz, sync, maxSize, addRule);
        dataList = new ArrayList<I>(maxSize);
    }

    @Override
    public synchronized int getSize() {
        if (isDisposed()) {
            throw new IllegalStateException("release() was called");
        }
        return dataList.size();
    }

    @NonNull
    @Override
    public Iterator<I> iterator() {
        if (isDisposed()) {
            throw new IllegalStateException("release() was called");
        }
        return new WrappedIterator(dataList.iterator());
    }

    @Override
    @NonNull
    public synchronized List<I> getAll() {
        if (isDisposed()) {
            throw new IllegalStateException("release() was called");
        }
        return Collections.unmodifiableList(dataList);
    }

    @Nullable
    @Override
    public synchronized I get(int index) {
        checkRange(index);
        return dataList.get(index);
    }

    @Nullable
    @Override
    public synchronized I pollFirst() {
        if (!isEmpty()) {
            try {
                return get(0);
            } finally {
                remove(0);
            }
        }
        return null;
    }

    @Nullable
    @Override
    public synchronized I pollLast() {
        if (!isEmpty()) {
            int size = getSize();
            int index = size < 1 ? 0 : size - 1;
            try {
                return get(index);
            } finally {
                remove(index);
            }
        }
        return null;
    }

    @Nullable
    @Override
    public synchronized I peekFirst() {
        if (!isEmpty()) {
            return get(0);
        }
        return null;
    }

    @Nullable
    @Override
    public synchronized I peekLast() {
        if (!isEmpty()) {
            int size = getSize();
            int index = size < 1 ? 0 : size - 1;
            return get(index);
        }
        return null;
    }

    @Override
    protected boolean addNoSync(I info, int pos) {
        dataList.add(pos, info);
        return true;
    }

    @Override
    protected boolean setNoSync(@NonNull I info, int pos) {
        dataList.set(pos, info);
        return true;
    }

    @Nullable
    @Override
    protected I removeNoSync(int index) {
        return dataList.remove(index);
    }

    @Override
    protected void clearNoDelete() {
        if (isDisposed()) {
            throw new IllegalStateException("release() was called");
        }
        dataList.clear();
    }


}
