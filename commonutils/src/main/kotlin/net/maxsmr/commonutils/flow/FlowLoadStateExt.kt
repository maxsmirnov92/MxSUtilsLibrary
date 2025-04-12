package net.maxsmr.commonutils.flow

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import net.maxsmr.commonutils.states.ILoadState
import net.maxsmr.commonutils.states.LoadState
import net.maxsmr.commonutils.states.PgnLoadState

fun <D> MutableStateFlow<LoadState<D>>.loading(): LoadState<D> {
    val initial = value
    return initial.preLoad().also {
        value = it
    }
}

fun <D> MutableStateFlow<LoadState<D>>.successLoad(data: D): LoadState<D> {
    val initial = value
    return initial.successLoad(data).also {
        value = it
    }
}

fun <D> MutableStateFlow<LoadState<D>>.errorLoad(error: Exception): LoadState<D> {
    return errorLoad(ILoadState.ErrorData(error))
}

fun <D> MutableStateFlow<LoadState<D>>.errorLoad(error: ILoadState.ErrorData): LoadState<D> {
    val initial = value
    return initial.errorLoad(error).also {
        value = it
    }
}

fun <D> MutableStateFlow<PgnLoadState<D>>.pgnLoading(isFromStart: Boolean): PgnLoadState<D> {
    val initial = value
    return if (isFromStart) {
        initial.preLoad()
    } else {
        initial.prePgnLoading()
    }.also {
        value = it
    }
}

fun <D> MutableStateFlow<PgnLoadState<D>>.pgnSuccessLoad(
    data: D,
    isComplete: Boolean,
): PgnLoadState<D> {
    val initial = value
    return initial.successLoad(data).copy(
        loadingState = PgnLoadState.PgnLoading.StandBy(isComplete)
    ).also {
        value = it
    }
}

fun <D> MutableStateFlow<PgnLoadState<D>>.pgnErrorLoad(
    error: ILoadState.ErrorData,
    isComplete: Boolean
): PgnLoadState<D> {
    val initial = value
    return initial.errorLoad(error).copy(
        loadingState = PgnLoadState.PgnLoading.StandBy(isComplete)
    ).also {
        value = it
    }
}

fun <D, S: ILoadState<D>> Flow<S>.transformLoadStateIfLoading(): Flow<S> {
    return takeWhileInclusive { it.isLoading }
}