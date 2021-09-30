package net.maxsmr.commonutils.prefs

import androidx.lifecycle.LiveData

/**
 * @param onPrefChangedFunc if returned true with specified key - LD value will be refreshed
 */
abstract class SharedPrefKeyLiveData<K> private constructor(
    protected val sharedPrefs: SharedPrefsManager,
    protected val key: String,
    protected val type: SharedPrefsHolder.PrefType,
    protected val defValue: K?,
    protected val onPrefChangedFunc: (String?, K?, K?) -> Boolean =
        { listenerKey, oldValue, newValue -> listenerKey == key }
) : LiveData<K>() {

    val prefValue get() = sharedPrefs.getValue(key, type, defValue)

    private val preferenceChangeListener = object : PreferenceChangeListener {

        @Suppress("UNCHECKED_CAST")
        override fun <V> onPreferenceChanged(
            name: String,
            key: String,
            oldValue: V?,
            newValue: V?,
            prefType: SharedPrefsHolder.PrefType?
        ) {
            if (prefType == type && onPrefChangedFunc(key, oldValue as K?, newValue as K?)) {
                refreshValue()
            }
        }

        override fun onPreferenceRemoved(name: String, key: String) {
            refreshValueWithCheck(key)
        }

        override fun onAllPreferencesRemoved(name: String) {
            value = null
        }

        override fun onAllPreferencesRefreshed(name: String) {
            refreshValueWithCheck(key)
        }

        private fun refreshValueWithCheck(key: String) {
            if (onPrefChangedFunc(key, null, prefValue)) {
                refreshValue()
            }
        }
    }

    override fun onActive() {
        super.onActive()
        refreshValue()
        sharedPrefs.addPreferenceChangeListener(preferenceChangeListener)
    }

    override fun onInactive() {
        sharedPrefs.removePreferenceChangeListener(preferenceChangeListener)
        super.onInactive()
    }

    protected fun refreshValue() {
        value = prefValue
    }

    class SharedPrefIntLiveData @JvmOverloads constructor(
        sharedPrefs: SharedPrefsManager,
        key: String,
        defValue: Int? = null
    ) :
        SharedPrefKeyLiveData<Int>(sharedPrefs, key, SharedPrefsHolder.PrefType.INT, defValue)

    class SharedPrefStringLiveData @JvmOverloads constructor(
        sharedPrefs: SharedPrefsManager,
        key: String,
        defValue: String? = null
    ) :
        SharedPrefKeyLiveData<String>(sharedPrefs, key, SharedPrefsHolder.PrefType.STRING, defValue)

    class SharedPrefBooleanLiveData @JvmOverloads constructor(
        sharedPrefs: SharedPrefsManager,
        key: String,
        defValue: Boolean? = null
    ) :
        SharedPrefKeyLiveData<Boolean>(sharedPrefs, key, SharedPrefsHolder.PrefType.BOOLEAN, defValue)

    class SharedPrefFloatLiveData @JvmOverloads constructor(
        sharedPrefs: SharedPrefsManager,
        key: String,
        defValue: Float? = null
    ) :
        SharedPrefKeyLiveData<Float>(sharedPrefs, key, SharedPrefsHolder.PrefType.FLOAT, defValue)

    class SharedPrefDoubleLiveData @JvmOverloads constructor(
        sharedPrefs: SharedPrefsManager,
        key: String,
        defValue: Double? = null
    ) :
        SharedPrefKeyLiveData<Double>(sharedPrefs, key, SharedPrefsHolder.PrefType.DOUBLE, defValue)

    class SharedPrefLongLiveData @JvmOverloads constructor(
        sharedPrefs: SharedPrefsManager,
        key: String,
        defValue: Long? = null
    ) :
        SharedPrefKeyLiveData<Long>(sharedPrefs, key, SharedPrefsHolder.PrefType.LONG, defValue)

    class SharedPrefStringSetLiveData @JvmOverloads constructor(
        sharedPrefs: SharedPrefsManager,
        key: String,
        defValue: Set<String>? = null
    ) :
        SharedPrefKeyLiveData<Set<String>>(
            sharedPrefs,
            key,
            SharedPrefsHolder.PrefType.STRING_SET,
            defValue
        )
}