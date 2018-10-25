package net.maxsmr.commonutils.android.preferences;


import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface PreferenceChangeListener {
    <T> void onPreferenceChanged(String preferenceName, @NotNull String key, @Nullable T oldValue, @Nullable T newValue);

    void onPreferenceRemoved(String preferenceName, @NotNull String key);

    void onAllPreferencesRemoved(String preferenceName);
}
