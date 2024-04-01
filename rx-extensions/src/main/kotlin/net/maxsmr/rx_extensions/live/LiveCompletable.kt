package net.maxsmr.rx_extensions.live

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import io.reactivex.Completable
import io.reactivex.CompletableOperator
import io.reactivex.disposables.Disposable

/**
 * Представляет собой lifecycle-aware [Completable].
 *
 * Цель: иметь возможность подписаться из View на единоразовые события от ViewModel без
 * необходимости отписываться вручную.
 *
 * @param observingState минимальное состояние ЖЦ LifecycleOwner, при котором он должен получать эвенты
 */
class LiveCompletable @JvmOverloads constructor(
        private val completable: Completable,
        observingState: Lifecycle.State? = Lifecycle.State.STARTED
): BaseLiveWrapper(observingState) {

    fun subscribe(owner: LifecycleOwner, operator: CompletableOperator? = null, onComplete: () -> Unit) {
        registerDisposable(owner) {
            createDisposable(owner, operator, onComplete)
        }
    }

    private fun createDisposable(owner: LifecycleOwner, operator: CompletableOperator? = null, onComplete: () -> Unit): Disposable {
        val completable = completable
                .doOnComplete {
                    if (observingState == null || owner.lifecycle.currentState.isAtLeast(observingState)) {
                        onComplete()
                    }
                    unsubscribe(owner)
                }
        return if (operator == null) {
            completable.subscribe()
        } else {
            completable.lift(operator).subscribe()
        }
    }
}

fun Completable.toLive(observingState: Lifecycle.State? = Lifecycle.State.STARTED): LiveCompletable =
        LiveCompletable(this, observingState)
