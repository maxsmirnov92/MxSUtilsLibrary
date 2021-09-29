package net.maxsmr.commonutils.prefs

import androidx.lifecycle.LiveData

/**
 * @param onPrefChangedFunc if returned true with current entity (single key or all was changed) - LD value will be refreshed
 */
class SharedPrefEntityLiveData<E> constructor(
    protected val sharedPrefsEntity: BaseSharedPrefsEntity<E>,
    protected val onPrefChangedFunc: ((E?) -> Boolean)? = null
) : LiveData<E>() {

    val prefEntity = sharedPrefsEntity.get()

    private val preferenceChangeListener = object : PreferenceChangeListener {

        override fun <T> onPreferenceChanged(
            name: String,
            key: String,
            oldValue: T?,
            newValue: T?,
            prefType: SharedPrefsHolder.PrefType?
        ) {
            refreshWithCheck()
        }

        override fun onPreferenceRemoved(name: String, key: String) {
            refreshWithCheck()
        }

        override fun onAllPreferencesRemoved(name: String) {
            value = null
        }

        override fun onAllPreferencesRefreshed(name: String) {
            refreshWithCheck()
        }

        fun refreshWithCheck() {
            with(onPrefChangedFunc) {
                if (this == null || this(prefEntity)) {
                    refreshEntity()
                }
            }
        }
    }

    override fun onActive() {
        super.onActive()
        refreshEntity()
        sharedPrefsEntity.sharedPrefs.addPreferenceChangeListener(preferenceChangeListener)
    }

    override fun onInactive() {
        sharedPrefsEntity.sharedPrefs.removePreferenceChangeListener(preferenceChangeListener)
        super.onInactive()
    }

    private fun refreshEntity() {
        value = prefEntity
    }
}