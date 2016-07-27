package net.maxsmr.commonutils.android.preferences;


import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public interface PreferenceChangeListener {
    <T> void onPreferenceChanged(String preferenceName, @NonNull String key, @Nullable T oldValue, @Nullable T newValue);

    void onPreferenceRemoved(String preferenceName, @NonNull String key);

    void onAllPreferencesRemoved(String preferenceName);
}
