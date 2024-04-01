package net.maxsmr.rx_extensions.live

import androidx.lifecycle.Lifecycle
import io.reactivex.Observable

/**
 * Представляет собой lifecycle-aware [Observable].
 *
 * Цель: иметь возможность подписаться из View на единоразовые события от ViewModel без
 * необходимости отписываться вручную.
 *
 * @param observingState минимальное состояние ЖЦ LifecycleOwner, при котором он должен получать эвенты
 */
class LiveObservable<T> @JvmOverloads constructor(
        override val observable: Observable<T>,
        filter: ((T) -> Boolean)? = null,
        observingState: Lifecycle.State? = Lifecycle.State.STARTED
) : BaseLiveObservable<T>(filter, observingState)

/**
 * Конвертирует Rx Observable в [LiveObservable] с автоотпиской
 *
 * @param observingState минимальное состояние ЖЦ LifecycleOwner, при котором он должен получать эвенты
 */
fun <T> Observable<T>.toLive(
        filter: ((T) -> Boolean)? = null,
        observingState: Lifecycle.State? = Lifecycle.State.STARTED
): LiveObservable<T> =
        LiveObservable(this, filter, observingState)