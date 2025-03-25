package net.maxsmr.commonutils.prefs

interface PreferenceChangeListener {

    fun <T> onPreferenceChanged(name: String, key: String, oldValue: T?, newValue: T?, prefType: SharedPrefsHolder.PrefType?)

    fun onPreferenceRemoved(name: String, key: String)

    fun onAllPreferencesRemoved(name: String)

    fun onAllPreferencesRefreshed(name: String)
}
