package net.maxsmr.commonutils.data.sort;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class AbsOptionableComparator<O extends ISortOption, T> implements Comparator<T> {

    @NonNull
    private final Map<O, Boolean> sortOptions = new LinkedHashMap<>();

    protected AbsOptionableComparator(@Nullable Map<O, Boolean> sortOptions) {
        if (sortOptions != null) {
            this.sortOptions.putAll(sortOptions);
        }
    }

    @NonNull
    public final Map<O, Boolean> getSortOptions() {
        return Collections.unmodifiableMap(sortOptions);
    }

    @Override
    public final int compare(T lhs, T rhs) {
        int result = 0;
        for (Map.Entry<O, Boolean> e : sortOptions.entrySet()) {
            O option = e.getKey();
            Boolean ascending = e.getValue();
            if (option == null) {
                throw new RuntimeException("sort option can't be null");
            }
            if (ascending == null) {
                throw new RuntimeException("ascending can't be null");
            }
            result = compare(lhs, rhs, option, ascending);
            if (result != 0) {
                break;
            }
        }
        return result;
    }

    protected abstract int compare(@Nullable T lhs, @Nullable T rhs, @NonNull O option, boolean ascending);
}
