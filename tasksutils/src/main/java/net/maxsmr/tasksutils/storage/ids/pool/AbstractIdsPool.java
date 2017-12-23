package net.maxsmr.tasksutils.storage.ids.pool;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;


public abstract class AbstractIdsPool {

    public abstract boolean contains(int id);

    public abstract boolean hasIds();

    public synchronized int incrementAndGet() {
        int lastId = Collections.max(copyOf());
        lastId++;
        add(lastId);
        return lastId;
    }

    public abstract void add(int newId);

    /** copy of actual ids set */
    @NonNull
    public abstract Set<Integer> copyOf();

    /**
     * @param ids null to clear
     */
    public abstract void set(@Nullable Collection<Integer> ids);

    public synchronized void clear() {
        set(null);
    }
}
