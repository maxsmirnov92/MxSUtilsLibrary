package net.maxsmr.commonutils.android.preferences;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import net.maxsmr.commonutils.data.CompareUtils;
import net.maxsmr.commonutils.data.Observable;
import net.maxsmr.commonutils.logger.BaseLogger;
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class PreferencesManager {

    private final static BaseLogger logger = BaseLoggerHolder.getInstance().getLogger(PreferencesManager.class);

    @NotNull
    protected final Context context;

    protected final SharedPreferences preferences;
    protected final String preferencesName;

    private final ChangeObservable changeObservable = new ChangeObservable();


    public PreferencesManager(@NotNull Context ctx, @Nullable String preferencesName) {
        this(ctx, preferencesName, Context.MODE_PRIVATE);
    }

    public PreferencesManager(@NotNull Context ctx, @Nullable String preferencesName, int mode) {
        if (!(mode == Context.MODE_PRIVATE || mode == Context.MODE_WORLD_READABLE || mode == Context.MODE_WORLD_WRITEABLE || mode == Context.MODE_APPEND)) {
            throw new IllegalArgumentException("incorrect mode value: " + mode);
        }
        this.context = ctx;
        this.preferences = !TextUtils.isEmpty(preferencesName) ? context.getSharedPreferences(preferencesName, mode) : android.preference.PreferenceManager.getDefaultSharedPreferences(context);
        this.preferencesName = !TextUtils.isEmpty(preferencesName) ? preferencesName : context.getPackageName() + "_preferences";
    }

    public void addPreferenceChangeListener(PreferenceChangeListener listener) throws NullPointerException {
        changeObservable.registerObserver(listener);
    }

    public void removePreferenceChangeListener(PreferenceChangeListener listener) {
        changeObservable.unregisterObserver(listener);
    }

    public synchronized boolean hasKey(@NotNull String key) {
        return preferences.contains(key);
    }

    public synchronized <V> V getValue(@NotNull String key, @NotNull Class<V> clazz) {
        return getValue(key, clazz, null);
    }

    @SuppressWarnings("unchecked")
    public synchronized <V> V getValue(@NotNull String key, @NotNull Class<V> clazz, @Nullable V defaultValue) {
        try {
            if (clazz.isAssignableFrom(String.class)) {
                return (V) preferences.getString(key, (String) defaultValue);
            } else if (clazz.isAssignableFrom(Integer.class)) {
                return (V) Integer.valueOf(preferences.getInt(key, defaultValue != null ? (Integer) defaultValue : 0));
            } else if (clazz.isAssignableFrom(Long.class)) {
                return (V) Long.valueOf(preferences.getLong(key, defaultValue != null ? (Long) defaultValue : 0));
            } else if (clazz.isAssignableFrom(Float.class)) {
                return (V) Float.valueOf(preferences.getFloat(key, defaultValue != null ? (Float) defaultValue : 0f));
            } else if (clazz.isAssignableFrom(Boolean.class)) {
                return (V) Boolean.valueOf(preferences.getBoolean(key, defaultValue != null ? (Boolean) defaultValue : false));
            } else if (clazz.isAssignableFrom(Set.class)) {
                return (V) preferences.getStringSet(key, defaultValue != null ? (Set<String>) defaultValue : null);
            } else {
                throw new UnsupportedOperationException("incorrect class: " + clazz);
            }
        } catch (ClassCastException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @SuppressLint("CommitPrefEdits")
    /**
     * @return true if successfully committed
     */
    public synchronized <V> boolean setValue(@NotNull String key, @Nullable V value) {
        final V oldValue = value != null ? getValue(key, (Class<V>) value.getClass(), null) : null;

        final SharedPreferences.Editor editor = preferences.edit();
        try {
            if (value != null) {
                if (value instanceof String) {
                    editor.putString(key, (String) value);
                } else if (value instanceof Integer) {
                    editor.putInt(key, (Integer) value);
                } else if (value instanceof Long) {
                    editor.putLong(key, (Long) value);
                } else if (value instanceof Float) {
                    editor.putFloat(key, (Float) value);
                } else if (value instanceof Boolean) {
                    editor.putBoolean(key, (Boolean) value);
                } else if (value instanceof Set) {
                    editor.putStringSet(key, (Set<String>) value);
                }
                else {
                    throw new UnsupportedOperationException("incorrect value type: " + value.getClass());
                }
            } else {
                editor.putString(key, null);
            }
        } catch (ClassCastException e) {
            logger.e(e);
        }
        if (editor.commit()) {

            if (!CompareUtils.objectsEqual(value, oldValue)) {
                changeObservable.dispatchChanged(preferencesName, key, oldValue, value);
            }

            return true;
        }

        return false;
    }

    /**
     * @return true if successfully committed
     */
    public synchronized boolean clearValue(@NotNull String key) {
        boolean b = preferences.edit().remove(key).commit();
        if (b) {
            changeObservable.dispatchRemoved(preferencesName, key);
        }
        return b;
    }

    /**
     * @return true if successfully committed
     */
    @SuppressLint("CommitPrefEdits")
    public synchronized boolean clear() {
        boolean b = preferences.edit().clear().commit();
        if (b) {
            changeObservable.dispatchAllRemoved(preferencesName);
        }
        return b;
    }

    private static class ChangeObservable extends Observable<PreferenceChangeListener> {

        private <T> void dispatchChanged(String name, String key, @Nullable T oldValue, @Nullable T newValue) {
            synchronized (observers) {
                for (PreferenceChangeListener l : observers) {
                    l.onPreferenceChanged(name, key, oldValue, newValue);
                }
            }
        }

        private <T> void dispatchRemoved(String name, String key) {
            synchronized (observers) {
                for (PreferenceChangeListener l : observers) {
                    l.onPreferenceRemoved(name, key);
                }
            }
        }

        private void dispatchAllRemoved(String name) {
            synchronized (observers) {
                for (PreferenceChangeListener l : observers) {
                    l.onAllPreferencesRemoved(name);
                }
            }
        }
    }

}
