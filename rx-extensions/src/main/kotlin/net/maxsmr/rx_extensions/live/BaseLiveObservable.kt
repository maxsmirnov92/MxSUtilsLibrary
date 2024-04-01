package net.maxsmr.rx_extensions.live

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import io.reactivex.Observable
import io.reactivex.ObservableOperator
import io.reactivex.disposables.Disposable

abstract class BaseLiveObservable<T>(
        protected val filter: ((T) -> Boolean)? = null,
        observingState: Lifecycle.State? = Lifecycle.State.STARTED
) : BaseLiveWrapper(observingState) {

    abstract val observable: Observable<T>

    @JvmOverloads
    fun subscribe(
            owner: LifecycleOwner,
            operator: ObservableOperator<T, T>? = null,
            emitOnce: Boolean = false,
            onNext: (T) -> Unit
    ) {
        registerDisposable(owner) {
            createDisposable(owner, operator, emitOnce, onNext)
        }
    }

    private fun createDisposable(
            owner: LifecycleOwner,
            operator: ObservableOperator<T, T>? = null,
            emitOnce: Boolean,
            onNext: (T) -> Unit
    ): Disposable {
        val observable = observable
                .filter {
                    (observingState == null || owner.lifecycle.currentState.isAtLeast(observingState))
                            && (filter == null || filter.invoke(it))
                }
                .doOnNext {
                    onNext(it)
                }
                .doAfterNext {
                    if (emitOnce) {
                        unsubscribe(owner)
                    }
                }
        return if (operator == null) {
            observable.subscribe()
        } else {
            observable.lift<Any>(operator).subscribe()
        }
    }
}