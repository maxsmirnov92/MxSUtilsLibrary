package net.maxsmr.commonutils.rx

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import io.reactivex.Observable
import io.reactivex.disposables.Disposable


/**
 * Представляет собой lifecycle-aware [Observable].
 *
 * Цель: иметь возможность подписаться из View на единоразовые события от ViewModel без
 * необходимости отписываться вручную.
 *
 * @param observingState минимальное состояние ЖЦ LifecycleOwner, при котором он должен получать эвенты
 */
class LiveObservable<T>(
        private val observable: Observable<T>,
        observingState: Lifecycle.State = Lifecycle.State.STARTED
): BaseLiveWrapper(observingState) {

    @JvmOverloads
    fun subscribe(owner: LifecycleOwner, emitOnce: Boolean = false, onNext: (T) -> Unit) {
        registerDisposable(owner) {
            createDisposable(owner, emitOnce, onNext)
        }
    }

    private fun createDisposable(owner: LifecycleOwner, emitOnce: Boolean, onNext: (T) -> Unit): Disposable {
        return observable
                .filter { owner.lifecycle.currentState.isAtLeast(observingState) }
                .doOnNext {
                    onNext(it)
                }
                .doAfterNext {
                    if (emitOnce) {
                        unsubscribe(owner)
                    }
                }
                .subscribe()
    }
}

/**
 * Конвертирует Rx Observable в [LiveObservable] с автоотпиской
 *
 * @param observingState минимальное состояние ЖЦ LifecycleOwner, при котором он должен получать эвенты
 */
fun <T> Observable<T>.toLive(observingState: Lifecycle.State = Lifecycle.State.STARTED): LiveObservable<T> =
        LiveObservable(this, observingState)