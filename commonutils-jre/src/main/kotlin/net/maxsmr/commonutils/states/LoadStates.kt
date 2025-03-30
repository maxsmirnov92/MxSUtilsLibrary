package net.maxsmr.commonutils.states

import net.maxsmr.commonutils.states.ILoadState.ErrorData
import net.maxsmr.commonutils.states.PgnLoadState.PgnLoading
import net.maxsmr.commonutils.text.ITextMessage

/**
 * Базовый контейнер для состояния загрузки
 */
interface ILoadState<D> {

    /**
     * Флаг о том, что загрузка завершилась с любым результатом
     */
    val wasLoaded: Boolean

    val isLoading: Boolean

    val data: D?

    val error: ErrorData?

    /**
     * Статус успеха с данными или без
     */
    val isSuccess get() = !isLoading && error == null

    val isError get() = getStatus() == Status.ERROR

    fun preLoad(): ILoadState<D>

    /**
     * Выставление успешного состояния с данными или без
     */
    fun successLoad(result: D?): ILoadState<D>

    fun errorLoad(error: ErrorData): ILoadState<D>

    fun hasData(
        checkSuccess: Boolean = true,
        dataValidator: ((D?) -> Boolean)? = null
    ) = (!checkSuccess || isSuccess)
                && data != null
                && (dataValidator == null || dataValidator(data))

    fun getData(
        checkSuccess: Boolean = true,
        dataValidator: ((D?) -> Boolean)? = null
    ) = if (hasData(checkSuccess, dataValidator)) {
            data
        } else {
            null
        }

    fun getStatus(): Status = when {
        isLoading -> Status.LOADING
        isSuccess -> Status.SUCCESS
        else -> Status.ERROR
    }

    enum class Status {
        SUCCESS,
        ERROR,
        LOADING
    }

    data class ErrorData(
        val error: Exception,
        val message: ITextMessage<*>? = null
    ) {

        override fun toString(): String {
            return "ErrorData(error=$error, message=$message)"
        }
    }
}

/**
 * Базовый контейнер для состояния загрузки с пагинацией
 */
interface IPgnLoadState<D> : ILoadState<D> {

    fun prePgnLoading(): PgnLoadState<D>
}

/**
 * Контейнер для состояния загрузки с флагом загрузки
 */
data class LoadState<D>(
    override val wasLoaded: Boolean = false,
    override val isLoading: Boolean = false,
    override val data: D? = null,
    override val error: ErrorData? = null,
) : ILoadState<D> {

    override fun preLoad(): LoadState<D> {
        return stateOf(this, isLoading = true)
    }

    override fun successLoad(result: D?): LoadState<D> {
        return stateOf(this, wasLoaded = true, isLoading = false, data = result, error = null)
    }

    override fun errorLoad(error: ErrorData): LoadState<D> {
        return stateOf(this, wasLoaded = true, isLoading = false, data = null, error = error)
    }

    override fun toString(): String {
        return "LoadState(wasLoaded=$wasLoaded," +
                "isLoading=$isLoading," +
                "data=$data," +
                "error=$error)"
    }

    companion object {

        @JvmStatic
        @JvmOverloads
        fun <D> ILoadState<*>.copyOf(data: D? = null): LoadState<D> {
            return LoadState(
                this.wasLoaded,
                this.isLoading,
                data,
                this.error
            )
        }

        @JvmOverloads
        fun <D> initial(data: D? = null) = success(data, false)

        @JvmStatic
        @JvmOverloads
        fun <D> loading(data: D? = null): LoadState<D> = stateOf(
            null,
            wasLoaded = false,
            isLoading = true,
            data = data
        )

        @JvmStatic
        fun <D> error(
            error: Exception,
            data: D? = null,
        ): LoadState<D> = error(ErrorData(error), data)

        @JvmStatic
        fun <D> error(
            error: ErrorData,
            data: D? = null,
        ): LoadState<D> = stateOf(
            null,
            wasLoaded = true,
            isLoading = false,
            data = data,
            error = error
        )

        fun <D> success(data: D?) = success(data, true)

        @JvmStatic
        private fun <D> success(data: D?, wasLoaded: Boolean): LoadState<D> =
            stateOf(
                null,
                wasLoaded = wasLoaded,
                isLoading = false,
                data = data
            )

        @JvmStatic
        private fun <D> stateOf(
            current: LoadState<D>?,
            data: D? = current?.data,
            wasLoaded: Boolean? = current?.wasLoaded,
            isLoading: Boolean? = current?.isLoading,
            error: ErrorData? = current?.error,
        ): LoadState<D> {
            val initial: LoadState<D> = current ?: LoadState()
            return initial.copy(
                wasLoaded = wasLoaded ?: initial.wasLoaded,
                isLoading = isLoading ?: initial.isLoading,
                data = data,
                error = error
            )
        }
    }
}

/**
 * Контейнер для состояния загрузки с [PgnLoading]
 */
data class PgnLoadState<D>(
    val loadingState: PgnLoading = PgnLoading.StandBy(true),
    override val wasLoaded: Boolean = false,
    override val data: D? = null,
    override val error: ErrorData? = null,
) : IPgnLoadState<D> {

    override val isLoading: Boolean = loadingState.isLoading

    override fun preLoad(): PgnLoadState<D> {
        return pgnStateOf(this, loadingState = PgnLoading.MainLoad)
    }

    override fun prePgnLoading(): PgnLoadState<D> {
        return pgnStateOf(this, loadingState = PgnLoading.PageLoad)
    }

    override fun successLoad(result: D?): PgnLoadState<D> {
        return pgnStateOf(
            this,
            wasLoaded = true,
            loadingState = if (loadingState is PgnLoading.StandBy) {
                loadingState
            } else {
                PgnLoading.StandBy(true)
            },
            data = result,
            error = null
        )
    }

    override fun errorLoad(error: ErrorData): PgnLoadState<D> {
        return pgnStateOf(
            this,
            wasLoaded = true,
            loadingState = if (loadingState is PgnLoading.StandBy) {
                loadingState
            } else {
                PgnLoading.StandBy(true)
            },
            data = null,
            error = error
        )
    }

    override fun toString(): String {
        return "PgnLoadState(loadingState=$loadingState," +
                "wasLoaded=$wasLoaded," +
                "data=$data," +
                "error=$error," +
                "isLoading=$isLoading)"
    }

    companion object {

        @JvmStatic
        @JvmOverloads
        fun <D> ILoadState<*>.pgnCopyOf(data: D? = null): PgnLoadState<D> {
            return if (this is PgnLoadState) {
                PgnLoadState(
                    this.loadingState,
                    this.wasLoaded,
                    data,
                    this.error
                )
            } else {
                PgnLoadState(
                    if (isLoading) {
                        PgnLoading.MainLoad
                    } else {
                        PgnLoading.StandBy(true)
                    },
                    this.wasLoaded,
                    data,
                    this.error
                )
            }
        }

        @JvmOverloads
        fun <D> pgnInitial(data: D? = null) = pgnSuccess(data, wasLoaded = false, isComplete = false)

        @JvmStatic
        @JvmOverloads
        fun <D> pgnLoading(loadingState: PgnLoading, data: D? = null): PgnLoadState<D> = pgnStateOf(
            null,
            wasLoaded = false,
            loadingState = loadingState,
            data = data
        )

        @JvmStatic
        fun <D> pgnError(
            error: Exception,
            data: D? = null,
            isComplete: Boolean = true
        ): PgnLoadState<D> = pgnError(
            ErrorData(error),
            data,
            isComplete
        )

        @JvmStatic
        fun <D> pgnError(
            error: ErrorData,
            data: D? = null,
            isComplete: Boolean = true
        ): PgnLoadState<D> = pgnStateOf(
            null,
            wasLoaded = true,
            loadingState = PgnLoading.StandBy(isComplete),
            data = data,
            error = error
        )

        fun <D> pgnSuccess(
            data: D?,
            isComplete: Boolean
        ) = pgnSuccess(data, wasLoaded = true, isComplete = isComplete)

        @JvmStatic
        private fun <D> pgnSuccess(
            data: D?,
            wasLoaded: Boolean,
            isComplete: Boolean
        ): PgnLoadState<D> =
            pgnStateOf(
                null,
                wasLoaded = wasLoaded,
                loadingState = PgnLoading.StandBy(isComplete),
                data = data
            )

        @JvmStatic
        private fun <D> pgnStateOf(
            current: PgnLoadState<D>?,
            data: D? = current?.data,
            wasLoaded: Boolean? = current?.wasLoaded,
            loadingState: PgnLoading? = current?.loadingState,
            error: ErrorData? = current?.error,
        ): PgnLoadState<D> {
            val initial: PgnLoadState<D> = current ?: PgnLoadState()
            return initial.copy(
                wasLoaded = wasLoaded ?: initial.wasLoaded,
                loadingState = loadingState ?: initial.loadingState,
                data = data,
                error = error
            )
        }
    }

    sealed interface PgnLoading {

        val isLoading: Boolean

        class StandBy(val isCompleted: Boolean) : PgnLoading {

            override val isLoading: Boolean = false
        }

        data object MainLoad : PgnLoading {

            override val isLoading: Boolean = true
        }

        data object PageLoad : PgnLoading {

            override val isLoading: Boolean = true
        }
    }
}
