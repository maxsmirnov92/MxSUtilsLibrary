package net.maxsmr.commonutils.data;

import android.support.annotation.NonNull;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public abstract class Observable<T> {

    @NonNull
    protected final Set<T> mObservers = new LinkedHashSet<>();

    @NonNull
    public Set<T> getObservers() {
        return Collections.unmodifiableSet(mObservers);
    }

    public boolean registerObserver(@NonNull T observer) {
        synchronized (mObservers) {
            return mObservers.add(observer);
        }
    }

    public boolean unregisterObserver(@NonNull T observer) {
        synchronized (mObservers) {
            return mObservers.remove(observer);
        }
    }

    public void unregisterAll() {
        synchronized(mObservers) {
            mObservers.clear();
        }
    }
}
