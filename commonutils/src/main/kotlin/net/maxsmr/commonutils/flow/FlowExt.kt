package net.maxsmr.commonutils.flow

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * @return [MutableStateFlow] из исходного [Flow] по принципу Eagerly
 */
fun <T> Flow<T>.mutableStateIn(
    scope: CoroutineScope,
    initialValue: T,
    context: CoroutineContext = EmptyCoroutineContext
): MutableStateFlow<T> {
    val flow = MutableStateFlow(initialValue)
    scope.launch(context) {
        this@mutableStateIn.collect(flow)
    }
    return flow
}

/**
 * @return [MutableSharedFlow] из исходного [Flow] по принципу Eagerly
 */
fun <T> Flow<T>.mutableSharedStateIn(
    scope: CoroutineScope,
    replay: Int = 0,
    extraBufferCapacity: Int = 0,
    onBufferOverflow: BufferOverflow = BufferOverflow.SUSPEND,
    context: CoroutineContext = EmptyCoroutineContext,
): MutableSharedFlow<T> {
    val flow = MutableSharedFlow<T>(
        replay,
        extraBufferCapacity,
        onBufferOverflow
    )
    scope.launch(context) {
        collect { flow.emit(it) }
    }
    return flow
}

/**
 * Собирать значения пока выполняется [predicate] с проверкой ПОСЛЕ очередного emit;
 * Если нужно просто собирать все значения, удовлетворяющие [predicate] - использовать стандартный [takeWhile]
 * @return конечный [Flow] с прерыванием collect по AbortFlowException, в отличие от обычного [transform]
 */
fun <T> Flow<T>.takeWhileInclusive(predicate: suspend (T) -> Boolean): Flow<T> {
    return transformWhile {
        emit(it)
        predicate(it)
    }
}

/**
 * Взять единственное значение, удовлетворящее [predicate]
 * @return холодный [Flow], состоящий из одного элемента
 */
fun <T> Flow<T>.takeOnceIf(predicate: suspend (T) -> Boolean): Flow<T> {
    return filter(predicate).take(1)
}

/**
 * Собирать очередное значение пока выполняется [predicate]
 * @return холодный [Flow], конечность которого зависит от исходного
 */
fun <T> Flow<T>.transformIf(predicate: suspend (T) -> Boolean): Flow<T> {
    return transform {
        if (predicate(it)) {
            emit(it)
        }
    }
}