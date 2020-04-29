package net.maxsmr.commonutils.rx

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import io.reactivex.Observable
import io.reactivex.disposables.Disposable


/**
 * Представляет собой lifecycle-aware Observable.
 *
 * Цель: иметь возможность подписаться из View на единоразовые события от ViewModel без
 * необходимости отписываться вручную.
 *
 * @param observable source Rx Observable
 * @param observingState минимальное состояние ЖЦ LifecycleOwner, при котором он должен получать эвенты
 */
class LiveObservable<T>(
        private val observable: Observable<T>,
        private val observingState: Lifecycle.State = Lifecycle.State.STARTED
) {

    private val observers: MutableMap<LifecycleOwner, DisposeObserver> = mutableMapOf()

    fun subscribe(owner: LifecycleOwner, onNext: (T) -> Unit) {
        if (observers.containsKey(owner)) return //owner уже подписан
        val disposeObserver = DisposeObserver(owner, observable
                .filter { owner.lifecycle.currentState.isAtLeast(observingState) }
                .subscribe { onNext(it) })
        owner.lifecycle.addObserver(disposeObserver)
        observers[owner] = disposeObserver
    }


    private inner class DisposeObserver(
            private val owner: LifecycleOwner,
            private val disposable: Disposable
    ) : LifecycleObserver {

        @Suppress("unused")
        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        fun dispose() {
            disposable.dispose()
            owner.lifecycle.removeObserver(this)
            observers.remove(owner)
        }
    }
}

/**
 * Конвертирует Rx Observable в [LiveObservable] с автоотпиской
 *
 * @param observingState минимальное состояние ЖЦ LifecycleOwner, при котором он должен получать эвенты
 */
fun <T> Observable<T>.toLive(observingState: Lifecycle.State = Lifecycle.State.STARTED): LiveObservable<T> =
        LiveObservable(this, observingState)