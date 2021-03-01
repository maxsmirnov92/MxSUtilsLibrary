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
        preferencesName: String = EMPTY_STRING,
        mode: Int = Context.MODE_PRIVATE
) {

    private val preferences: SharedPreferences =
            getSharedPreferences(context, preferencesName, mode)

    private val preferencesName: String =
            if (!isEmpty(preferencesName)) preferencesName else context.packageName + "_preferences"

    private val changeObservable = ChangeObservable()

    @Throws(NullPointerException::class)
    fun addPreferenceChangeListener(listener: PreferenceChangeListener) {
        changeObservable.registerObserver(listener)
    }

    fun removePreferenceChangeListener(listener: PreferenceChangeListener) {
        changeObservable.unregisterObserver(listener)
    }

    @Synchronized
    fun hasKey(key: String) = SharedPrefsHolder.hasKey(preferences, key)

    fun <V> getOrCreateValue(
            key: String,
            type: PrefType,
            defaultValue: V?,
            async: Boolean = true): V? {
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

    @Synchronized
    fun <V> getValue(
            key: String,
            type: PrefType,
            defaultValue: V? = null
    ): V? = SharedPrefsHolder.getValue(preferences, key, type, defaultValue)

    /**
     * @return true if successfully saved
     */
    @Synchronized
    fun <V : Any?> setValue(
            key: String,
            value: V?,
            async: Boolean = true
    ): Boolean = SharedPrefsHolder.setValue(preferences, key, value) {
        changeObservable.dispatchChanged(preferencesName, key, it, value)
    }

    /**
     * @return true if successfully saved
     */
    @Synchronized
    fun removeKey(key: String, async: Boolean = true): Boolean {
        if (SharedPrefsHolder.removeKey(preferences, key, async)) {
            changeObservable.dispatchRemoved<Any>(preferencesName, key)
            return true
        }
        return false
    }

    /**
     * @return true if successfully saved
     */
    @SuppressLint("CommitPrefEdits")
    @Synchronized
    fun clear(async: Boolean = true): Boolean {
        if (SharedPrefsHolder.clear(preferences, async)) {
            changeObservable.dispatchAllRemoved(preferencesName)
            return true
        }
        return false
    }

    private class ChangeObservable : Observable<PreferenceChangeListener>() {

        fun <T> dispatchChanged(name: String, key: String, oldValue: T?, newValue: T?) {
            synchronized(observers) {
                for (l in observers) {
                    l.onPreferenceChanged(name, key, oldValue, newValue)
                }
            }
        }

        fun <T> dispatchRemoved(name: String, key: String) {
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
    }

    companion object {

        fun getSharedPreferences(
                context: Context,
                name: String,
                mode: Int = Context.MODE_PRIVATE
        ): SharedPreferences =
                if (name.isEmpty()) {
                    PreferenceManager.getDefaultSharedPreferences(context)
                } else {
                    context.getSharedPreferences(name, mode)
                }
    }
}
