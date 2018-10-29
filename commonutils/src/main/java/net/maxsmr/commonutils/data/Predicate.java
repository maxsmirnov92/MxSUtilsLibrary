package net.maxsmr.commonutils.data;

import android.support.v4.util.Pair;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public interface Predicate<V> {

    boolean apply(V element);

    class Methods {

        public static <V> boolean contains(@Nullable Collection<V> elements, @NotNull Predicate<V> predicate) {
            return findWithIndex(elements, predicate) != null;
        }

        @Nullable
        public static <V> Pair<Integer, V> findWithIndex(@Nullable Collection<V> elements, @NotNull Predicate<V> predicate) {
            int targetIndex = -1;
            V result = null;
            if (elements != null) {
                int index = 0;
                for (V elem : elements) {
                    if (predicate.apply(elem)) {
                        result = elem;
                        targetIndex = index;
                        break;
                    }
                    index++;
                }
            }
            return targetIndex >= 0 ? new Pair<>(targetIndex, result) : null;
        }

        @Nullable
        public static <V> V find(@Nullable Collection<V> elements, @NotNull Predicate<V> predicate) {
            Pair<Integer, V> result = findWithIndex(elements, predicate);
            return result != null ? result.second : null;
        }

        @NotNull
        public static <V> Map<Integer, V> filterWithIndex(@Nullable Collection<V> elements, @NotNull Predicate<V> predicate) {
            Map<Integer, V> result = new LinkedHashMap<>();
            if (elements != null) {
                int index = 0;
                for (V elem : elements) {
                    if (predicate.apply(elem)) {
                        result.put(index, elem);
                    }
                    index++;
                }
            }
            return result;
        }

        @NotNull
        public static <V> List<V> filter(@Nullable Collection<V> elements, @NotNull Predicate<V> predicate) {
            Map<Integer, V> map = filterWithIndex(elements, predicate);
            return entriesToValues(map.entrySet());
        }

        @NotNull
        public static <K, V> List<K> entriesToKeys(@Nullable Collection<Map.Entry<K, V>> entries) {
            List<K> result = new ArrayList<>();
            if (entries != null) {
                for (Map.Entry<K, V> e : entries) {
                    if (e != null) {
                        result.add(e.getKey());
                    }
                }
            }
            return result;
        }

        @NotNull
        public static <K, V> List<V> entriesToValues(@Nullable Collection<Map.Entry<K, V>> entries) {
            List<V> result = new ArrayList<>();
            if (entries != null) {
                for (Map.Entry<K, V> e : entries) {
                    if (e != null) {
                        result.add(e.getValue());
                    }
                }
            }
            return result;
        }
    }
}
