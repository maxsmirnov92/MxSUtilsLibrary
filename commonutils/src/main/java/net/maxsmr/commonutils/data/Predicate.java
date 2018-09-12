package net.maxsmr.commonutils.data;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface Predicate<V> {

    boolean apply(V element);

    class Methods {

        public static <V> boolean contains(Collection<V> elements, Predicate<V> predicate) {
            return findWithIndex(elements, predicate) != null;
        }

        public static <V> Pair<Integer, V> findWithIndex(Collection<V> elements, Predicate<V> predicate) {
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

        public static <V> V find(Collection<V> elements, Predicate<V> predicate) {
            Pair<Integer, V> result = findWithIndex(elements, predicate);
            return result != null ? result.second : null;
        }

        @NonNull
        public static <V> List<V> filter(Collection<V> elements, @NonNull Predicate<V> predicate) {
            List<V> result = new ArrayList<>();
            if (elements != null) {
                for (V elem : elements) {
                    if (predicate.apply(elem)) {
                        result.add(elem);
                    }
                }
            }
            return result;
        }

        public static <K, V> List<K> entriesToKeys(Collection<Map.Entry<K, V>> entries) {
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

        public static <K, V> List<V> entriesToValues(Collection<Map.Entry<K, V>> entries) {
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
