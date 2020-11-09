package net.maxsmr.commonutils.rx.live

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.ReplaySubject
import io.reactivex.subjects.Subject


/**
 * Представляет собой lifecycle-aware PublishSubject.
 *
 * Цель: иметь возможность подписаться из View на единоразовые события от ViewModel без
 * необходимости отписываться вручную.
 */
class LiveSubject<T>(
        private val observingState: Lifecycle.State = Lifecycle.State.STARTED,
        private val filter: ((T) -> Boolean)? = null,
        subjectType: SubjectType = SubjectType.PUBLISH
) {

    private val subject: Subject<T> = subjectType.createSubject()

    private val observers: MutableMap<LifecycleOwner, DisposeObserver> = mutableMapOf()

    fun onNext(event: T) {
        subject.onNext(event)
    }

    @JvmOverloads
    fun subscribe(owner: LifecycleOwner, emitOnce: Boolean = false, onNext: (T) -> Unit) {
        if (observers.containsKey(owner)) return //owner уже подписан
        val disposeObserver = DisposeObserver(owner, subject
                .filter {
                    owner.lifecycle.currentState.isAtLeast(observingState)
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
                .subscribe()
        )
        owner.lifecycle.addObserver(disposeObserver)
        observers[owner] = disposeObserver
    }

    fun unsubscribe(owner: LifecycleOwner) {
        observers[owner]?.dispose()
    }

    enum class SubjectType {
        PUBLISH, BEHAVIOUR, REPLAY;

        fun <T> createSubject(): Subject<T> = when (this) {
            PUBLISH -> PublishSubject.create<T>()
            BEHAVIOUR -> BehaviorSubject.create<T>()
            REPLAY -> ReplaySubject.create<T>()
        }
    }

    private inner class DisposeObserver(
            private val owner: LifecycleOwner,
            private val disposable: Disposable
    ) : LifecycleObserver {

        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        fun dispose() {
            disposable.dispose()
            owner.lifecycle.removeObserver(this)
            observers.remove(owner)
        }
    }
}