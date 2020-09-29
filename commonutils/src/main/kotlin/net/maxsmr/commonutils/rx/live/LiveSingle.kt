package net.maxsmr.commonutils.rx.live

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
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
class LiveSingle<T>(
        private val single: Single<T>,
        observingState: Lifecycle.State = Lifecycle.State.STARTED
): BaseLiveWrapper(observingState) {

    fun subscribe(owner: LifecycleOwner, onSuccess: (T) -> Unit) {
        registerDisposable(owner) {
            createDisposable(owner, onSuccess)
        }
    }

    private fun createDisposable(owner: LifecycleOwner, onSuccess: (T) -> Unit): Disposable {
        return single
                .filter { owner.lifecycle.currentState.isAtLeast(observingState) }
                .doOnEvent { item, _ ->
                    unsubscribe(owner)
                    item?.let(onSuccess)
                }
                .subscribe()
    }
}

fun <T> Single<T>.toLive(observingState: Lifecycle.State = Lifecycle.State.STARTED): LiveSingle<T> =
        LiveSingle(this, observingState)
