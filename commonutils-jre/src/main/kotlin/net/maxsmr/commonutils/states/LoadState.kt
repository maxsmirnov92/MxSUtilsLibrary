package net.maxsmr.commonutils.states

import net.maxsmr.commonutils.states.ILoadState.Companion.stateOf
import java.io.Serializable

/**
 * Статус ресурса
 */
enum class Status {
    SUCCESS,
    ERROR,
    LOADING
}

/**
 * Базовый контейнер для состояния загрузки
 */
interface ILoadState<D> : Serializable {

    /**
     * Флаг о том, что загрузка завершилась с любым результатом
     */
    var wasLoaded: Boolean

    var isLoading: Boolean

    var data: D?

    var error: Throwable?

    fun preLoad(): Boolean

    /**
     * Выставление успешного состояния с данными или без
     */
    fun successLoad(result: D?): Boolean
    fun errorLoad(error: Throwable): Boolean

    fun hasData(dataValidator: ((D?) -> Boolean)? = null) = data != null && (dataValidator == null || dataValidator(data))

    /**
     * Статус успеха с данными или без
     */
    fun isSuccess() = !isLoading && wasLoaded && error == null

    fun isError() = getStatus() == Status.ERROR

    fun isSuccessWithData(dataValidator: ((D?) -> Boolean)? = null) =
        isSuccess() && hasData(dataValidator)

    fun getStatus(): Status = when {
        isLoading -> Status.LOADING
        isSuccess() -> Status.SUCCESS
        else -> Status.ERROR
    }

    companion object {

        /**
         * @return новый или существующий изменённый [LoadState] + флаг факта изменения
         */
        @JvmStatic
        @JvmOverloads
        fun <D, S : ILoadState<D>> stateOf(
            current: S?,
            data: D? = null,
            wasLoaded: Boolean? = null,
            isLoading: Boolean? = null,
            error: Throwable? = null,
            createEmptyStateFunc: () -> S
        ): Pair<S, Boolean> {
            var result = current
            var hasChanged = false
            if (result == null) {
                result = createEmptyStateFunc()
                hasChanged = true
            }
            wasLoaded?.let {
                if (result.wasLoaded != it) {
                    result.wasLoaded = it
                    hasChanged = true
                }
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

        @JvmStatic
        @JvmOverloads
        fun <D> ILoadState<*>.copyOf(data: D? = null): LoadState<D> {
            val result = LoadState<D>()
            result.data = data
            result.wasLoaded = this.wasLoaded
            result.isLoading = this.isLoading
            result.error = this.error
            return result
        }
    }
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
        return hasChanged
    }

    override fun successLoad(result: D?): Boolean {
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

    companion object {

        @JvmStatic
        fun <D> empty(): LoadState<D> =
            stateOf(null, createEmptyStateFunc = { LoadState<D>() }).first

        @JvmStatic
        @JvmOverloads
        fun <D> loading(data: D? = null): LoadState<D> =
            stateOf(null,
                wasLoaded = false,
                isLoading = true,
                data = data,
                createEmptyStateFunc = { LoadState<D>() }).first

        @JvmStatic
        fun <D> success(data: D): LoadState<D> =
            stateOf(null,
                wasLoaded = true,
                isLoading = false,
                data = data,
                createEmptyStateFunc = { LoadState<D>() }).first

        @JvmStatic
        fun <D> error(error: Throwable, data: D? = null): LoadState<D> =
            stateOf(null,
                wasLoaded = true,
                isLoading = false,
                data = data,
                error = error,
                createEmptyStateFunc = { LoadState<D>() }).first
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

    override fun successLoad(result: D?): Boolean {
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