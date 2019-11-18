package net.maxsmr.tasksutils.storage.ids.pool;


import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class SetIdsPool extends AbstractIdsPool {

    @NotNull
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

    @NotNull
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
