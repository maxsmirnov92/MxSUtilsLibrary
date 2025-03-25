package net.maxsmr.commonutils

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

fun <T : Any> T.getPersistableKey(kProperty: KProperty<*>): String =
    "${this::class.simpleName}.${kProperty.name}"

fun <T> SavedStateHandle.persistableValue(
    onSetValue: ((T?) -> Unit)? = null
): PersistableValue<T> =
    PersistableValue(this, onSetValue)

fun <T> SavedStateHandle.persistableValueInitial(
    initialValue: T,
    onSetValue: ((T) -> Unit)? = null,
): PersistableValueInitial<T> =
    PersistableValueInitial(this, initialValue, onSetValue)

fun <T> SavedStateHandle.persistableLiveData(): PersistableLiveData<T> =
    PersistableLiveData(this)

fun <T> SavedStateHandle.persistableLiveDataInitial(
    initialValue: T
): PersistableLiveDataInitial<T> =
    PersistableLiveDataInitial(this, initialValue)

fun <T> SavedStateHandle.persistableStateFlow(
    scope: CoroutineScope,
    initialValue: T
): PersistableStateFlow<T> =
    PersistableStateFlow(scope, this, initialValue)

/**
 * Используйте, если нужно поле, переживающее пересоздание процесса приложения
 *
 * @param onSetValue доп. действие при смене значения
 */
class PersistableValue<T>(
    private val state: SavedStateHandle,
    private val onSetValue: ((T?) -> Unit)? = null,
) : ReadWriteProperty<Any, T?> {

    override fun getValue(thisRef: Any, property: KProperty<*>): T? {
        return state[thisRef.getPersistableKey(property)]
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T?) {
        state[thisRef.getPersistableKey(property)] = value
        onSetValue?.invoke(value)
    }
}

/**
 * Используйте, если нужно поле, переживающее пересоздание процесса приложения, с начальным значением.
 * Начальное значение гарантирует, что [getValue] не вернет null.
 *
 * @param initial начальное значение
 * @param onSetValue доп. действие при смене значения
 */
class PersistableValueInitial<T>(
    private val state: SavedStateHandle,
    private val initial: T,
    private val onSetValue: ((T) -> Unit)? = null,
) : ReadWriteProperty<Any, T> {

    override fun getValue(thisRef: Any, property: KProperty<*>): T {
        return state[thisRef.getPersistableKey(property)] ?: initial
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        state[thisRef.getPersistableKey(property)] = value
        onSetValue?.invoke(value)
    }
}

/**
 * Используйте, если нужна [MutableLiveData], переживающая пересоздание процесса приложения
 */
class PersistableLiveData<T>(
    private val state: SavedStateHandle,
) : ReadOnlyProperty<Any, MutableLiveData<T>> {

    override fun getValue(thisRef: Any, property: KProperty<*>): MutableLiveData<T> {
        return state.getLiveData(thisRef.getPersistableKey(property))
    }
}

/**
 * Используйте, если нужна LiveData, переживающая пересоздание процесса приложения, с начальным значением.
 * Начальное значение гарантирует, что LiveData всегда содержит какое-либо значение и field.value
 * не вернет null.
 */
class PersistableLiveDataInitial<T>(
    private val state: SavedStateHandle,
    private val initialValue: T,
) : ReadOnlyProperty<Any, MutableLiveData<T>> {

    override fun getValue(thisRef: Any, property: KProperty<*>): MutableLiveData<T> {
        return state.getLiveData(thisRef.getPersistableKey(property), initialValue)
    }
}

/**
 * Используйте, если нужна [StateFlow], переживающая пересоздание процесса приложения
 */
class PersistableStateFlow<T>(
    private val scope: CoroutineScope,
    private val state: SavedStateHandle,
    private val initialValue: T,
) : ReadOnlyProperty<Any, MutableStateFlow<T>> {

    private val cache = ConcurrentHashMap<PropertyKey, MutableStateFlow<T>>()

    override fun getValue(thisRef: Any, property: KProperty<*>): MutableStateFlow<T> {
        val key = PropertyKey(thisRef, property)
        // SavedStateHandle.getStateFlow из вредности возвращает не MutableStateFlow, поэтому так
        return cache.getOrPut(key) { createFlow(thisRef, property) }
    }

    private fun createFlow(thisRef: Any, property: KProperty<*>): MutableStateFlow<T> {
        val key = thisRef.getPersistableKey(property)
        val stateFlow = MutableStateFlow(state.get<T>(key) ?: initialValue)
        scope.launch {
            stateFlow.collect { state[key] = it }
        }
        return stateFlow
    }

    private data class PropertyKey(
        val thisRef: Any,
        val property: KProperty<*>
    )
}