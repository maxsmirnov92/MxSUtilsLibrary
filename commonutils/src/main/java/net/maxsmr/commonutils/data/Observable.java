package net.maxsmr.commonutils.data;

import android.support.annotation.NonNull;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public abstract class Observable<T> {

    @NonNull
    protected final Set<T> observers = new LinkedHashSet<>();

    @NonNull
    public Set<T> getObservers() {
        return Collections.unmodifiableSet(observers);
    }

    @NonNull
    public Set<T> copyOfObservers() {
        synchronized (observers) {
            return new LinkedHashSet<>(observers);
        }
    }

    public boolean registerObserver(T observer) {
        if (observer != null) {
            synchronized (observers) {
                return observers.add(observer);
            }
        }
        return false;
    }

    public boolean unregisterObserver(T observer) {
        synchronized (observers) {
            if (observers.contains(observer)) {
                return observers.remove(observer);
            }
        }
        return false;
    }

    public void unregisterAll() {
        synchronized(observers) {
            observers.clear();
        }
    }
}
