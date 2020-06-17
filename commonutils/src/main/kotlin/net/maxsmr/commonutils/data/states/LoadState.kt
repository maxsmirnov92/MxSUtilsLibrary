package net.maxsmr.commonutils.data.states

import java.io.Serializable

/**
 * Базовый контейнер для состояния загрузки
 */
interface ILoadState<D> : Serializable {

    /**
     * Флаг о том, что загрузка завершилась хотя бы один раз с любым результатом
     */
    var wasLoaded: Boolean

    var data: D?

    var error: Throwable?

    fun _isLoading(): Boolean
    fun preLoad(): Boolean
    fun successLoad(result: D): Boolean
    fun errorLoad(error: Throwable): Boolean

    fun isSuccessLoad(dataValidator: ((D?) -> Boolean)? = {it != null}) = !_isLoading() && error == null
            && (dataValidator == null || dataValidator(data))
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
        var isLoading: Boolean = false,
        override var data: D? = null,
        override var error: Throwable? = null
): ILoadState<D> {

    override var wasLoaded: Boolean = false

    override fun _isLoading(): Boolean = isLoading

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

    companion object {

        fun <D> createEmpty() = getOrCreate<D>(null).first

        /**
         * @return новый или существующий изменённый [LoadState] + флаг факта изменения
         */
        fun <D> getOrCreate(
                state: LoadState<D>?,
                data: D? = null,
                isLoading: Boolean? = null,
                error: Throwable? = null
        ): Pair<LoadState<D>, Boolean> {
            var result = state
            var hasChanged = false
            if (state == null) {
                result = LoadState()
                hasChanged = true
            }
            result as LoadState<D>
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
    }
}


/**
 * Контейнер для состояния загрузки с [PgnLoading]
 */
data class PgnLoadState<D>(
        var loading: PgnLoading = PgnLoading(),
        override var data: D? = null,
        override var error: Throwable? = null
) : IPgnLoadState<D> {

    override var wasLoaded: Boolean = false

    override fun _isLoading(): Boolean = loading.state.isLoading()

    override fun preLoad(): Boolean {
        var hasChanged = false
        val newLoadingState = loading.copy(state = PgnState.MAIN_LOAD)
        if (loading != newLoadingState) {
            loading = newLoadingState
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
        val newLoadingState = loading.copy(state = PgnState.PGN_LOAD)
        if (loading != newLoadingState) {
            loading = newLoadingState
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
        val newLoadingState = loading.copy(state = PgnState.STANDBY)
        if (loading != newLoadingState) {
            loading = newLoadingState
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
        val newLoadingState = loading.copy(state = PgnState.STANDBY)
        if (loading != newLoadingState) {
            loading = newLoadingState
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