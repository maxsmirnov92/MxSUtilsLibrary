package net.maxsmr.tasksutils.storage.sync.collection;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import net.maxsmr.tasksutils.taskexecutor.RunnableInfo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;


public class QueueSyncStorage<I extends RunnableInfo> extends AbstractCollectionSyncStorage<I> {

    private final ArrayDeque<I> dataList = new ArrayDeque<>();

    /**
     * {@inheritDoc}
     */
    public QueueSyncStorage(@Nullable String listDirPath,
                           Class<I> clazz,
                           boolean sync, int maxSize, @NonNull IAddRule<I> addRule) {
        super(listDirPath, clazz, sync, maxSize, addRule);
    }


    public synchronized int getSize() {
        if (isDisposed()) {
            throw new IllegalStateException("release() was called");
        }
        return dataList.size();
    }

    @Override
    @NonNull
    public Iterator<I> iterator() {
        if (isDisposed()) {
            throw new IllegalStateException("release() was called");
        }
        return new WrappedIterator(dataList.iterator());
    }

    /** copy of queue */
    @Override
    @NonNull
    public synchronized List<I> getAll() {

        if (isDisposed()) {
            throw new IllegalStateException("release() was called");
        }

        final Iterator<I> it = dataList.iterator();

        final List<I> list = new ArrayList<>();
        while (it.hasNext()) {
            list.add(it.next());
        }
        return Collections.unmodifiableList(list);
    }

    @Nullable
    @Override
    public I get(int index) {
        throw new UnsupportedOperationException("get from specified position is not supported");
    }

    @Nullable
    private synchronized I poll(boolean first) {

        if (isDisposed()) {
            throw new IllegalStateException("release() was called");
        }

        I info = peek(first);
        remove(info);
        return info;
    }

    /**
     * removes first element of dataList
     */
    @Nullable
    @Override
    public synchronized I pollFirst() {
        return poll(true);
    }

    /**
     * removes last element of dataList
     */
    @Nullable
    @Override
    public synchronized I pollLast() {
        return poll(false);
    }

    @Nullable
    private synchronized I peek(boolean first) {
        if (isDisposed()) {
            throw new IllegalStateException("release() was called");
        }
        if (!isEmpty()) {
            return first ? dataList.peekFirst() : dataList.peekLast();
        }
        return null;
    }

    @Nullable
    @Override
    public I peekFirst() {
        return peek(true);
    }

    @Nullable
    @Override
    public I peekLast() {
        return peek(false);
    }

    @Override
    protected boolean addNoSync(I info, int index) {
        return dataList.add(info);
    }

    @Override
    protected boolean setNoSync(@NonNull I info, int index) {
        throw new UnsupportedOperationException("setting element at specified position is not supported");
    }

    @Override
    @Nullable
    public I removeNoSync(int index) {

        I removed = null;

        Iterator<I> iterator = dataList.iterator();
        for (int i = 0; iterator.hasNext(); i++) {
            I next = iterator.next();
            if (i == index) {
                iterator.remove();
                removed = next;
                break;
            }
        }

        return removed;
    }

    @Override
    protected synchronized void clearNoDelete() {
        if (isDisposed()) {
            throw new IllegalStateException("release() was called");
        }
        dataList.clear();
    }


}
