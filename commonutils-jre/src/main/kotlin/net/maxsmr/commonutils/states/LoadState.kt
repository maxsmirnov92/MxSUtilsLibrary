package net.maxsmr.commonutils.states

import java.io.Serializable

fun <D> createEmptyState() = null.getOrCreate<D>().first

/**
 * Статус ресурса
 */
enum class Status {
    SUCCESS,
    ERROR,
    LOADING
}

/**
 * @return новый или существующий изменённый [LoadState] + флаг факта изменения
 */
fun <D> ILoadState<D>?.getOrCreate(
        data: D? = null,
        isLoading: Boolean? = null,
        error: Throwable? = null,
        createEmptyStateFunc: () -> ILoadState<D> = { LoadState() }
): Pair<ILoadState<D>, Boolean> {
    var result = this
    var hasChanged = false
    if (result == null) {
        result = createEmptyStateFunc()
        hasChanged = true
    }
    isLoading?.let {
        if (result.isLoading != it) {
            result.isLoading = it
            hasChanged = true
        }
    }
    if (result.data != data) {
        result.data = data
        hasChanged = true
    }
    if (result.error != error) {
        result.error = error
        hasChanged = true
    }
    return Pair(result, hasChanged)
}

/**
 * Базовый контейнер для состояния загрузки
 */
interface ILoadState<D> : Serializable {

    /**
     * Флаг о том, что загрузка завершилась хотя бы один раз с любым результатом
     */
    var wasLoaded: Boolean

    var isLoading: Boolean

    var data: D?

    var error: Throwable?

    fun preLoad(): Boolean
    fun successLoad(result: D): Boolean
    fun errorLoad(error: Throwable): Boolean

    @JvmDefault
    fun isSuccessLoad(dataValidator: ((D?) -> Boolean)? = { it != null }) =
            !isLoading && error == null
                    && (dataValidator == null || dataValidator(data))

    @JvmDefault
    fun getStatus(dataValidator: ((D?) -> Boolean)? = { it != null }): Status = when {
            isLoading -> Status.LOADING
            isSuccessLoad(dataValidator) -> Status.SUCCESS
            else -> Status.ERROR
        }

    @JvmDefault
    fun hasData() = data != null
}

/**
 * Базовый контейнер для состояния загрузки с пагинацией
 */
interface IPgnLoadState<D> : ILoadState<D> {

    fun prePgnLoading(): Boolean
}

/**
 * Контейнер для состояния загрузки с флажком загрузки
 */
data class LoadState<D>(
        override var isLoading: Boolean = false,
        override var data: D? = null,
        override var error: Throwable? = null
) : ILoadState<D> {

    override var wasLoaded: Boolean = false

    override fun preLoad(): Boolean {
        var hasChanged = false
        if (!isLoading) {
            isLoading = true
            hasChanged = true
        }
        if (error != null) {
            error = null
            hasChanged = true
        }
        return hasChanged
    }

    override fun successLoad(result: D): Boolean {
        var hasChanged = false
        if (!wasLoaded) {
            wasLoaded = true
            hasChanged = true
        }
        if (isLoading) {
            isLoading = false
            hasChanged = true
        }
        if (data != result) {
            data = result
            hasChanged = true
        }
        if (error != null) {
            error = null
            hasChanged = true
        }
        return hasChanged
    }

    override fun errorLoad(error: Throwable): Boolean {
        var hasChanged = false
        if (!wasLoaded) {
            wasLoaded = true
            hasChanged = true
        }
        if (isLoading) {
            isLoading = false
            hasChanged = true
        }
        if (data != null) {
            data = null
            hasChanged = true
        }
        if (this.error != error) {
            this.error = error
            hasChanged = true
        }
        return hasChanged
    }
}


/**
 * Контейнер для состояния загрузки с [PgnLoading]
 */
data class PgnLoadState<D>(
        var loadingState: PgnLoading = PgnLoading(),
        override var data: D? = null,
        override var error: Throwable? = null
) : IPgnLoadState<D> {

    override var wasLoaded: Boolean = false

    override var isLoading: Boolean
        get() = loadingState.state.isLoading()
        set(value) {
            loadingState = if (value) PgnLoading(PgnState.MAIN_LOAD) else PgnLoading()
        }

    override fun preLoad(): Boolean {
        var hasChanged = false
        val newLoadingState = loadingState.copy(state = PgnState.MAIN_LOAD)
        if (loadingState != newLoadingState) {
            loadingState = newLoadingState
            hasChanged = true
        }
        if (error != null) {
            error = null
            hasChanged = true
        }
        return hasChanged
    }

    override fun prePgnLoading(): Boolean {
        var hasChanged = false
        val newLoadingState = loadingState.copy(state = PgnState.PGN_LOAD)
        if (loadingState != newLoadingState) {
            loadingState = newLoadingState
            hasChanged = true
        }
        if (error != null) {
            error = null
            hasChanged = true
        }
        return hasChanged
    }

    override fun successLoad(result: D): Boolean {
        var hasChanged = false
        if (!wasLoaded) {
            wasLoaded = true
            hasChanged = true
        }
        val newLoadingState = loadingState.copy(state = PgnState.STANDBY)
        if (loadingState != newLoadingState) {
            loadingState = newLoadingState
            hasChanged = true
        }
        if (data != result) {
            data = result
            hasChanged = true
        }
        if (error != null) {
            error = null
            hasChanged = true
        }
        return hasChanged
    }

    override fun errorLoad(error: Throwable): Boolean {
        var hasChanged = false
        if (!wasLoaded) {
            wasLoaded = true
            hasChanged = true
        }
        val newLoadingState = loadingState.copy(state = PgnState.STANDBY)
        if (loadingState != newLoadingState) {
            loadingState = newLoadingState
            hasChanged = true
        }
        if (data != null) {
            data = null
            hasChanged = true
        }
        if (this.error != error) {
            this.error = error
            hasChanged = true
        }
        return hasChanged
    }
}


data class PgnLoading(val state: PgnState = PgnState.STANDBY, val pgnComplete: Boolean = false)

enum class PgnState {
    STANDBY, MAIN_LOAD, PGN_LOAD;

    fun isLoading() = this == MAIN_LOAD || this == PGN_LOAD
}

/**
 * Пустые данные, например, для случая результата Completable
 */
object EmptyData