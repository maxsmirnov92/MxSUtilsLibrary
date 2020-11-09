package net.maxsmr.commonutils.rx.live

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import io.reactivex.Maybe
import io.reactivex.disposables.Disposable


/**
 * Представляет собой lifecycle-aware [Maybe].
 *
 * Цель: иметь возможность подписаться из View на единоразовые события от ViewModel без
 * необходимости отписываться вручную.
 *
 * @param observingState минимальное состояние ЖЦ LifecycleOwner, при котором он должен получать эвенты
 */
class LiveMaybe<T>(
        private val maybe: Maybe<T>,
        private val filter: ((T) -> Boolean)? = null,
        observingState: Lifecycle.State = Lifecycle.State.STARTED
) : BaseLiveWrapper(observingState) {

    fun subscribe(owner: LifecycleOwner, onSuccess: (T) -> Unit) {
        registerDisposable(owner) {
            createDisposable(owner, onSuccess)
        }
    }

    private fun createDisposable(owner: LifecycleOwner, onSuccess: (T) -> Unit): Disposable {
        return maybe
                .filter {
                    owner.lifecycle.currentState.isAtLeast(observingState)
                            && (filter == null || filter.invoke(it))
                }
                .doOnEvent { item, _ ->
                    unsubscribe(owner)
                    item?.let(onSuccess)
                }
                .subscribe()
    }
}

fun <T> Maybe<T>.toLive(filter: ((T) -> Boolean)? = null, observingState: Lifecycle.State = Lifecycle.State.STARTED): LiveMaybe<T> =
        LiveMaybe(this, filter, observingState)
