package net.maxsmr.rx_extensions.live

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import io.reactivex.MaybeOperator
import io.reactivex.Single
import io.reactivex.disposables.Disposable

/**
 * Представляет собой lifecycle-aware [Single].
 *
 * Цель: иметь возможность подписаться из View на единоразовые события от ViewModel без
 * необходимости отписываться вручную.
 *
 * @param observingState минимальное состояние ЖЦ LifecycleOwner, при котором он должен получать эвенты
 */
class LiveSingle<T> @JvmOverloads constructor(
        private val single: Single<T>,
        private val filter: ((T) -> Boolean)? = null,
        observingState: Lifecycle.State? = Lifecycle.State.STARTED
) : BaseLiveWrapper(observingState) {

    fun subscribe(owner: LifecycleOwner, operator: MaybeOperator<T, T>? = null, onSuccess: (T) -> Unit) {
        registerDisposable(owner) {
            createDisposable(owner, operator, onSuccess)
        }
    }

    private fun createDisposable(owner: LifecycleOwner, operator: MaybeOperator<T, T>? = null, onSuccess: (T) -> Unit): Disposable {
        val single = single
                .filter {
                    (observingState == null || owner.lifecycle.currentState.isAtLeast(observingState))
                            && (filter == null || filter.invoke(it))
                }
                .doOnEvent { item, _ ->
                    unsubscribe(owner)
                    item?.let(onSuccess)
                }
        return if (operator == null) {
            single.subscribe()
        } else {
            single.lift<Any>(operator).subscribe()
        }
    }
}

fun <T> Single<T>.toLive(
        filter: ((T) -> Boolean)? = null,
        observingState: Lifecycle.State? = Lifecycle.State.STARTED
): LiveSingle<T> =
        LiveSingle(this, filter, observingState)
