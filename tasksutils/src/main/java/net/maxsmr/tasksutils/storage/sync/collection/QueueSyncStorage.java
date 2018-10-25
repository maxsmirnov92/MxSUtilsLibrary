package net.maxsmr.tasksutils.storage.sync.collection;

import org.jetbrains.annotations.NotNull;
import android.support.annotation.Nullable;

import net.maxsmr.tasksutils.taskexecutor.RunnableInfo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;


public class QueueSyncStorage<I extends RunnableInfo> extends AbstractCollectionSyncStorage<I> {

    private final ArrayDeque<I> dataQueue = new ArrayDeque<>();

    /**
     * {@inheritDoc}
     */
    public QueueSyncStorage( @Nullable String storageDirPath, @Nullable String extension,
                           Class<I> clazz,
                           boolean sync, int maxSize, @NotNull IAddRule<I> addRule, boolean startRestore) {
        super(storageDirPath, extension, clazz, sync, maxSize, addRule, startRestore);
    }


    public synchronized int getSize() {
        if (isDisposed()) {
            throw new IllegalStateException("release() was called");
        }
        return dataQueue.size();
    }

    @Override
    @NotNull
    public synchronized Iterator<I> iterator() {
        if (isDisposed()) {
            throw new IllegalStateException("release() was called");
        }
        return new WrappedIterator(dataQueue.iterator());
    }

    /** copy of queue */
    @Override
    @NotNull
    public synchronized List<I> getAll() {

        if (isDisposed()) {
            throw new IllegalStateException("release() was called");
        }

        final Iterator<I> it = iterator();

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
     * removes first element of dataQueue
     */
    @Nullable
    @Override
    public synchronized I pollFirst() {
        return poll(true);
    }

    /**
     * removes last element of dataQueue
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
            return first ? dataQueue.peekFirst() : dataQueue.peekLast();
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
    protected boolean addInternal(I info, int index) {
        return dataQueue.add(info);
    }

    @Override
    protected boolean setInternal(@NotNull I info, int index) {
        throw new UnsupportedOperationException("setting element at specified position is not supported");
    }

    @Override
    @Nullable
    protected I removeInternal(int index) {
        I removed = null;
        Iterator<I> iterator = dataQueue.iterator();
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
        dataQueue.clear();
    }

    @Override
    protected Class<?> getLoggerClass() {
        return QueueSyncStorage.class;
    }
}
