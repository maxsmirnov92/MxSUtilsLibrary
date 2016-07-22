package net.maxsmr.networkutils.loadutil.executors;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Set;

import net.maxsmr.tasksutils.taskrunnable.RunnableInfo;


public abstract class AbsIdsPool {

    public static final int NO_ID = RunnableInfo.NO_ID;

    public synchronized boolean contains(int id) {
        return getIds().contains(id);
    }

    public synchronized final boolean hasIds() {
        return !getIds().isEmpty();
    }

    public synchronized final int getLast() {
        Set<Integer> ids = getIds();
        return ids.isEmpty() ? NO_ID : ids.toArray(new Integer[ids.size()])[ids.size() - 1];
    }

    public synchronized int incrementAndGet() {
        int newId = getLast() + 1;
        add(newId);
        return newId;
    }


    public abstract void add(int newId);

    @NonNull
    public abstract Set<Integer> getIds();

    /**
     * @param ids null to clear
     */
    public abstract void setIds(@Nullable Set<Integer> ids);

    public synchronized void clearIds() {
        setIds(null);
    }
}
