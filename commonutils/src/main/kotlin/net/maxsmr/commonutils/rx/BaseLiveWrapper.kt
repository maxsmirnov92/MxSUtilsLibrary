package net.maxsmr.commonutils.rx

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import io.reactivex.disposables.Disposable

abstract class BaseLiveWrapper(
        protected val observingState: Lifecycle.State = Lifecycle.State.STARTED
) {

    protected val observers: MutableMap<LifecycleOwner, DisposeObserver> = mutableMapOf()

    fun unsubscribe(owner: LifecycleOwner) {
        observers[owner]?.dispose()
    }

    protected fun registerDisposable(owner: LifecycleOwner, createDisposableFunc: () -> Disposable) {
        if (observers.containsKey(owner)) return //owner уже подписан
        val disposeObserver = DisposeObserver(owner, createDisposableFunc())
        owner.lifecycle.addObserver(disposeObserver)
        observers[owner] = disposeObserver
    }

    protected inner class DisposeObserver(
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