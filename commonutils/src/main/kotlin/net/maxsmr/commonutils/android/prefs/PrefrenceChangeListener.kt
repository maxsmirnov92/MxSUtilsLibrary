package net.maxsmr.commonutils.android.prefs

import org.jetbrains.annotations.NotNull

interface PreferenceChangeListener {

    fun <T> onPreferenceChanged(preferenceName: String, key: String, oldValue: T?, newValue: T?)

    fun onPreferenceRemoved(preferenceName: String, @NotNull key: String)

    fun onAllPreferencesRemoved(preferenceName: String)
}
