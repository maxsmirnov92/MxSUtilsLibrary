package net.maxsmr.commonutils.prefs

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import net.maxsmr.commonutils.prefs.SharedPrefsHolder.PrefType
import net.maxsmr.commonutils.Observable
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.commonutils.text.isEmpty

class SharedPrefsManager @JvmOverloads constructor(
    context: Context,
    _prefsName: String? = EMPTY_STRING,
    mode: Int = Context.MODE_PRIVATE
) {
    private val prefs: SharedPreferences

    private val prefsName: String

    private val changeObservable = ChangeObservable()

    init {
        with(getSharedPreferences(context, _prefsName, mode)) {
            prefs = first
            prefsName = second
        }
    }

    fun addPreferenceChangeListener(listener: PreferenceChangeListener) {
        changeObservable.registerObserver(listener)
    }

    fun removePreferenceChangeListener(listener: PreferenceChangeListener) {
        changeObservable.unregisterObserver(listener)
    }

    fun hasKey(key: String) = SharedPrefsHolder.hasKey(prefs, key)

    @JvmOverloads
    fun <V> getOrCreateValue(
        key: String,
        type: PrefType,
        defaultValue: V?,
        async: Boolean = true
    ): V? {
        var value = defaultValue
        var has = false
        if (hasKey(key)) {
            has = true
            value = getValue(key, type, defaultValue)
            if (value == null) {
                has = false
            }
        }
        return if (!has) {
            setValue(key, defaultValue, async)
            defaultValue
        } else {
            value
        }
    }

    @JvmOverloads
    fun <V> getValue(
        key: String,
        type: PrefType,
        defaultValue: V? = null
    ): V? = SharedPrefsHolder.getValue(prefs, key, type, defaultValue)

    @JvmOverloads
    fun <V> setValue(
        key: String,
        value: V?,
        async: Boolean = true,
        shouldSaveChanges: Boolean = true
    ): Boolean = SharedPrefsHolder.setValue(prefs, key, value, async, shouldSaveChanges) {
        changeObservable.dispatchChanged(prefsName, key, it, value, PrefType.fromValue(value))
    }

    @JvmOverloads
    fun removeKey(
        key: String,
        async: Boolean = true,
        shouldSaveChanges: Boolean = true
    ): Boolean {
        return if (SharedPrefsHolder.removeKey(prefs, key, async, shouldSaveChanges)) {
            changeObservable.dispatchRemoved(prefsName, key)
            true
        } else {
            false
        }
    }

    @SuppressLint("CommitPrefEdits")
    @JvmOverloads
    fun clear(async: Boolean = true, shouldSaveChanges: Boolean = true): Boolean {
        return if (SharedPrefsHolder.clear(prefs, async, shouldSaveChanges)) {
            changeObservable.dispatchAllRemoved(prefsName)
            true
        } else {
            false
        }
    }

    fun saveChanges(): Boolean {
        return if (SharedPrefsHolder.saveChanges(prefs)) {
            changeObservable.dispatchAllRefreshed(prefsName)
            true
        } else {
            false
        }
    }

    private class ChangeObservable : Observable<PreferenceChangeListener>() {

        fun <T> dispatchChanged(
            name: String,
            key: String,
            oldValue: T?,
            newValue: T?,
            prefType: PrefType?
        ) {
            synchronized(observers) {
                for (l in observers) {
                    l.onPreferenceChanged(name, key, oldValue, newValue, prefType)
                }
            }
        }

        fun dispatchRemoved(name: String, key: String) {
            synchronized(observers) {
                for (l in observers) {
                    l.onPreferenceRemoved(name, key)
                }
            }
        }

        fun dispatchAllRemoved(name: String) {
            synchronized(observers) {
                for (l in observers) {
                    l.onAllPreferencesRemoved(name)
                }
            }
        }

        fun dispatchAllRefreshed(name: String) {
            synchronized(observers) {
                for (l in observers) {
                    l.onAllPreferencesRefreshed(name)
                }
            }
        }
    }

    companion object {

        fun getSharedPreferences(
            context: Context,
            name: String?,
            mode: Int = Context.MODE_PRIVATE
        ): Pair<SharedPreferences, String> =
            if (name == null || name.isEmpty()) {
                @Suppress("DEPRECATION")
                Pair(
                    PreferenceManager.getDefaultSharedPreferences(context),
                    context.packageName + "_preferences"
                )
            } else {
                Pair(context.getSharedPreferences(name, mode), name)
            }
    }
}
