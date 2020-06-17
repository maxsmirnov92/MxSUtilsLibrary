package net.maxsmr.commonutils.rx

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import io.reactivex.Completable
import io.reactivex.disposables.Disposable


/**
 * Представляет собой lifecycle-aware [Completable].
 *
 * Цель: иметь возможность подписаться из View на единоразовые события от ViewModel без
 * необходимости отписываться вручную.
 *
 * @param observingState минимальное состояние ЖЦ LifecycleOwner, при котором он должен получать эвенты
 */
class LiveCompletable(
        private val completable: Completable,
        observingState: Lifecycle.State = Lifecycle.State.STARTED
): BaseLiveWrapper(observingState) {

    fun subscribe(owner: LifecycleOwner, onComplete: () -> Unit) {
        registerDisposable(owner) {
            createDisposable(owner, onComplete)
        }
    }

    private fun createDisposable(owner: LifecycleOwner, onComplete: () -> Unit): Disposable {
        return completable
                .doOnComplete {
                    if (owner.lifecycle.currentState.isAtLeast(observingState)) {
                        onComplete()
                    }
                    unsubscribe(owner)
                }
                .subscribe()
    }
}

fun Completable.toLive(observingState: Lifecycle.State = Lifecycle.State.STARTED): LiveCompletable =
        LiveCompletable(this, observingState)
