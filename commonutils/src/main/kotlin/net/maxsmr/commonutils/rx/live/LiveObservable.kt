package net.maxsmr.commonutils.rx.live

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import io.reactivex.Observable
import io.reactivex.ObservableOperator
import io.reactivex.disposables.Disposable

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