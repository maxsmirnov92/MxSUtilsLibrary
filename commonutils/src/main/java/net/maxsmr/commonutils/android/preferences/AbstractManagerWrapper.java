package net.maxsmr.commonutils.android.preferences;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractManagerWrapper {

    @NotNull
    protected final PreferencesManager manager;

    protected AbstractManagerWrapper(@NotNull PreferencesManager manager) {
        this.manager = manager;
    }

    public void addPreferenceChangeListener(PreferenceChangeListener l) {
        manager.addPreferenceChangeListener(l);
    }

    public void removePreferenceChangeListener(PreferenceChangeListener l) {
        manager.removePreferenceChangeListener(l);
    }

    public boolean has(String key) {
//        if (Number.class.isAssignableFrom(clazz)) {
//            Object v = get(key, clazz);
//            return v != null && !CompareUtils.objectsEqual(v, 0.0);
//        } else {
//            return get(key, clazz) != null;
//        }
        return manager.hasKey(key);
    }

    public <V> V getOrCreate(String key, @NotNull Class<V> clazz, @Nullable V defaultValue) {
        V value = defaultValue;
        boolean has = false;
        if (manager.hasKey(key)) {
            has = true;
            value = manager.getValue(key, clazz, defaultValue);
            if (value == null) {
                has = false;
            }
        }
        if (!has) {
            manager.setValue(key, defaultValue);
            return defaultValue;
        } else {
            return value;
        }
    }

    public <V> V get(String key, @NotNull Class<V> clazz) {
        return manager.getValue(key, clazz);
    }

    public <V> V get(String key, @NotNull Class<V> clazz, @Nullable V defaultValue) {
        return manager.getValue(key, clazz, defaultValue);
    }

    public boolean set(String key, Object value) {
        return manager.setValue(key, value);
    }

    public void remove(String key) {
        manager.clearValue(key);
    }

    public void clear() {
        manager.clear();
    }
}
