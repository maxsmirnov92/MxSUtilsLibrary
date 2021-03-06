package net.maxsmr.commonutils;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @see class {@linkplain android.database.Observable}
 */
public abstract class Observable<T> {

    @NotNull
    protected final Set<T> observers = new LinkedHashSet<>();

    @NotNull
    public Set<T> getObservers() {
        return Collections.unmodifiableSet(observers);
    }

    @NotNull
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
