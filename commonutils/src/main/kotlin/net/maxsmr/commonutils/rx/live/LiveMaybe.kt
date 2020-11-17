package net.maxsmr.commonutils.rx.live

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import io.reactivex.Maybe
import io.reactivex.MaybeOperator
import io.reactivex.ObservableOperator
import io.reactivex.disposables.Disposable


/**
 * Представляет собой lifecycle-aware [Maybe].
 *
 * Цель: иметь возможность подписаться из View на единоразовые события от ViewModel без
 * необходимости отписываться вручную.
 *
 * @param observingState минимальное состояние ЖЦ LifecycleOwner, при котором он должен получать эвенты
 */
class LiveMaybe<T> @JvmOverloads constructor(
        private val maybe: Maybe<T>,
        private val filter: ((T) -> Boolean)? = null,
        observingState: Lifecycle.State? = Lifecycle.State.STARTED
) : BaseLiveWrapper(observingState) {

    fun subscribe(
            owner: LifecycleOwner,
            operator: MaybeOperator<T, T>? = null,
            emitOnce: Boolean = false,
            onSuccess: (T) -> Unit
    ) {
        registerDisposable(owner) {
            createDisposable(owner, operator, emitOnce, onSuccess)
        }
    }

    private fun createDisposable(
            owner: LifecycleOwner,
            operator: MaybeOperator<T, T>? = null,
            emitOnce: Boolean,
            onSuccess: (T) -> Unit
    ): Disposable {
        val maybe =  maybe
                .filter {
                    (observingState == null || owner.lifecycle.currentState.isAtLeast(observingState))
                            && (filter == null || filter.invoke(it))
                }
                .doOnEvent { item, _ ->
                    item?.let(onSuccess)
                    if (emitOnce) {
                        unsubscribe(owner)
                    }
                }
        return if (operator == null) {
            maybe.subscribe()
        } else {
            maybe.lift<Any>(operator).subscribe()
        }
    }
}

fun <T> Maybe<T>.toLive(
        filter: ((T) -> Boolean)? = null,
        observingState: Lifecycle.State? = Lifecycle.State.STARTED
): LiveMaybe<T> =
        LiveMaybe(this, filter, observingState)
