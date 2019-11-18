package net.maxsmr.tasksutils.storage.sync.collection;

import net.maxsmr.tasksutils.taskexecutor.RunnableInfo;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;


public class ListSyncStorage<I extends RunnableInfo> extends AbstractCollectionSyncStorage<I> {

    @NotNull
    private final List<I> dataList;

    /**
     * {@inheritDoc}
     */
    public ListSyncStorage(@Nullable String storageDirPath, @Nullable String extension,
                           Class<I> clazz,
                           boolean sync, int maxSize, @NotNull IAddRule<I> addRule,boolean startRestore) {
        super(storageDirPath, extension, clazz, sync, maxSize, addRule, startRestore);
        dataList = new ArrayList<>(maxSize);
    }

    @Override
    public synchronized int getSize() {
        if (isDisposed()) {
            throw new IllegalStateException("release() was called");
        }
        return dataList.size();
    }

    @NotNull
    @Override
    public synchronized Iterator<I> iterator() {
        if (isDisposed()) {
            throw new IllegalStateException("release() was called");
        }
        return new WrappedIterator(dataList.iterator());
    }

    @Override
    @NotNull
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
    protected boolean addInternal(I info, int pos) {
        dataList.add(pos, info);
        return true;
    }

    @Override
    protected boolean setInternal(@NotNull I info, int pos) {
        dataList.set(pos, info);
        return true;
    }

    @Nullable
    @Override
    protected I removeInternal(int index) {
        return dataList.remove(index);
    }

    @Override
    protected void clearNoDelete() {
        dataList.clear();
    }

    @Override
    protected Class<?> getLoggerClass() {
        return ListSyncStorage.class;
    }
}
