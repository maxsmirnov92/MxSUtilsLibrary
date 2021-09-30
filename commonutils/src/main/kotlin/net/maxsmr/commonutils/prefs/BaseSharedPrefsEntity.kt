package net.maxsmr.commonutils.prefs

import androidx.lifecycle.LiveData

abstract class BaseSharedPrefsEntity<T>(val sharedPrefs: SharedPrefsManager) {

    /**
     * Записать отдельные поля [data] в prefs, но не вызывать подтверждение изменений
     */
    protected abstract fun SharedPrefsManager.serializeNoSave(data: T)

    protected abstract fun SharedPrefsManager.deserialize(): T?

    fun save(t: T) {
        with(sharedPrefs) {
            serializeNoSave(t)
            saveChanges()
        }
    }

    fun get(): T? = sharedPrefs.deserialize()


    /**
     * Метод получает объект из prefs, изменяет его и сохраняет измененный объект
     */
    fun update(updateFunction: T.() -> Unit) {
        get()?.apply(updateFunction)?.let(::save)
    }

    fun clear() {
        sharedPrefs.clear()
    }

    @JvmOverloads
    fun getAsLiveData(onPrefChangedFunc: ((T?) -> Boolean)? = null): LiveData<T> =
        SharedPrefEntityLiveData(this, onPrefChangedFunc)
}