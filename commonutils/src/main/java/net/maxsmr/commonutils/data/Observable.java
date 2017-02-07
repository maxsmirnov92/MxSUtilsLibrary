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

    @NonNull
    public Set<T> copyOfObservers() {
        synchronized (mObservers) {
            return new LinkedHashSet<>(mObservers);
        }
    }

    public boolean registerObserver(T observer) {
        if (observer != null) {
            synchronized (mObservers) {
                return mObservers.add(observer);
            }
        }
        return false;
    }

    public boolean unregisterObserver(T observer) {
        synchronized (mObservers) {
            if (mObservers.contains(observer)) {
                return mObservers.remove(observer);
            }
        }
        return false;
    }

    public void unregisterAll() {
        synchronized(mObservers) {
            mObservers.clear();
        }
    }
}
