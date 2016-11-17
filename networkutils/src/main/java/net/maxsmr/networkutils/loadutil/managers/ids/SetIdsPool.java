package net.maxsmr.networkutils.loadutil.managers.ids;


import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class SetIdsPool extends AbsIdsPool {

    @NonNull
    private final LinkedHashSet<Integer> ids = new LinkedHashSet<>();


    @Override
    public boolean contains(int id) {
        return ids.contains(id);
    }

    @Override
    public boolean hasIds() {
        return! ids.isEmpty();
    }

    @Override
    public synchronized void add(int newId) {
        if (newId < 0) {
            throw new IllegalArgumentException("incorrect id: " + newId);
        }
        ids.add(newId);
    }

    @NonNull
    @Override
    public synchronized Set<Integer> copyOf() {
        return Collections.unmodifiableSet(ids);
    }

    @Override
    public synchronized void set(@Nullable Collection<Integer> ids) {
        this.ids.clear();
        if (ids != null) {
            this.ids.addAll(ids);
        }
    }
}
