package net.maxsmr.commonutils.prefs

import org.jetbrains.annotations.NotNull

interface PreferenceChangeListener {

    fun <T> onPreferenceChanged(name: String, key: String, oldValue: T?, newValue: T?, prefType: SharedPrefsHolder.PrefType?)

    fun onPreferenceRemoved(name: String, @NotNull key: String)

    fun onAllPreferencesRemoved(name: String)

    fun onAllPreferencesRefreshed(name: String)
}
