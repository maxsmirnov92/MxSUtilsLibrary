package net.maxsmr.networkutils.loadstorage;

import android.support.annotation.NonNull;

import net.maxsmr.networkutils.loadutil.managers.base.info.LoadRunnableInfo;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public abstract class IdsLoadInfo<B extends LoadRunnableInfo.Body> extends LoadInfo<B> {

    @NonNull
    private Set<Integer> storageIds = new LinkedHashSet<>();

    public IdsLoadInfo(int id, @NonNull Collection<Integer> storageIds, @NonNull B body) {
        super(id, "", body);
        if (storageIds.isEmpty()) {
            throw new IllegalArgumentException("storageIds can't be empty");
        }
        this.storageIds.addAll(storageIds);
    }

    @NonNull
    public final Set<Integer> getStorageIds() {
        return Collections.unmodifiableSet(storageIds);
    }

    @Override
    public String toString() {
        return "IdsLoadInfo{" +
                "storageIds=" + storageIds +
                '}';
    }
}
