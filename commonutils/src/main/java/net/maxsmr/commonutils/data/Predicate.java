package net.maxsmr.commonutils.data;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public interface Predicate<V> {

    boolean apply(V element);

    class Methods {

        @Nullable
        public static <V> V find(Collection<V> elements, @NonNull Predicate<V> predicate) {
            V result = null;
            if (elements != null) {
                for (V elem : elements) {
                    if (predicate.apply(elem)) {
                        result = elem;
                        break;
                    }
                }
            }
            return result;
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
    }
}
