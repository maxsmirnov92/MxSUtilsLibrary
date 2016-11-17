package net.maxsmr.commonutils.data.sort;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;

public abstract class AbsOptionableComparator<O extends ISortOption, T> implements Comparator<T> {

    @NonNull
    private final Set<SortOptionPair<O>> sortOptionPairs = new LinkedHashSet<>();

    protected AbsOptionableComparator(@Nullable Collection<SortOptionPair<O>> sortOptionPairs) {
        if (sortOptionPairs != null) {
            this.sortOptionPairs.addAll(sortOptionPairs);
        }
    }

    @NonNull
    public Set<SortOptionPair<O>> getSortOptionPairs() {
        return new LinkedHashSet<>(sortOptionPairs);
    }

    @Override
    public final int compare(T lhs, T rhs) {
        int result = 0;
        for (SortOptionPair<O> p : getSortOptionPairs()) {
            result = compare(lhs, rhs, p.option, p.ascending);
            if (result != 0) {
                break;
            }
        }
        return result;
    }

    protected abstract int compare(@Nullable T lhs, @Nullable T rhs, O option, boolean ascending);
}
