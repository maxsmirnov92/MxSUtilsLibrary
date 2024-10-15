package net.maxsmr.commonutils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import kotlin.Pair;

public interface Predicate<V> {

    boolean apply(V element);

    class Methods {

        public static <V> boolean all(@Nullable Collection<V> elements, @NotNull Predicate<V> predicate) {
            return findIndexed(elements, element -> !predicate.apply(element)) == null;
        }

        public static <V> boolean contains(@Nullable Collection<V> elements, @NotNull Predicate<V> predicate) {
            // any
            return findIndexed(elements, predicate) != null;
        }

        @Nullable
        public static <V> Pair<Integer, V> findIndexed(@Nullable Collection<V> elements, @NotNull Predicate<V> predicate) {
            int resultIndex = -1;
            V result = null;
            if (elements != null) {
                int index = 0;
                for (V elem : elements) {
                    if (predicate.apply(elem)) {
                        result = elem;
                        resultIndex = index;
                        break;
                    }
                    index++;
                }
            }
            return resultIndex >= 0 ? new Pair<>(resultIndex, result) : null;
        }

        @Nullable
        public static <V> V find(@Nullable Collection<V> elements, @NotNull Predicate<V> predicate) {
            Pair<Integer, V> result = findIndexed(elements, predicate);
            return result != null ? result.getSecond() : null;
        }

        @NotNull
        public static <V> Map<Integer, V> filterIndexed(@Nullable Collection<V> elements, @NotNull Predicate<V> predicate) {
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
            Map<Integer, V> map = filterIndexed(elements, predicate);
            return entriesToValues(map.entrySet());
        }

        @Nullable
        public static <V> Pair<Integer, V> removeFirstIndex(@Nullable Iterable<V> elements, @NotNull Predicate<V> predicate) {
            int resultIndex = -1;
            V result = null;
            if (elements != null) {
                final Iterator<V> iterator = elements.iterator();
                int index = 0;
                while (iterator.hasNext()) {
                    V element = iterator.next();
                    if (predicate.apply(element)) {
                        iterator.remove();
                        result = element;
                        resultIndex = index;
                        break;
                    }
                    index++;
                }
            }
            return resultIndex >= 0 ? new Pair<>(resultIndex, result) : null;
        }

        @Nullable
        public static <V> V removeFirst(@Nullable Collection<V> elements, @NotNull Predicate<V> predicate) {
            Pair<Integer, V> result = removeFirstIndex(elements, predicate);
            return result != null ? result.getSecond() : null;
        }

        @NotNull
        public static <V> Map<Integer, V> removeAllIndexed(@Nullable Iterable<V> elements, @NotNull Predicate<V> predicate) {
            Map<Integer, V> removed = new LinkedHashMap<>();
            if (elements != null) {
                final Iterator<V> iterator = elements.iterator();
                int index = 0;
                while (iterator.hasNext()) {
                    V element = iterator.next();
                    if (predicate.apply(element)) {
                        iterator.remove();
                        removed.put(index, element);
                    }
                    index++;
                }
            }
            return removed;
        }

        @NotNull
        public static <V> List<V> removeAll(@Nullable Collection<V> elements, @NotNull Predicate<V> predicate) {
            Map<Integer, V> map = removeAllIndexed(elements, predicate);
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
