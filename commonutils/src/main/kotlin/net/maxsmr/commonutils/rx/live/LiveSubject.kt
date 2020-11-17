package net.maxsmr.commonutils.rx.live

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import io.reactivex.Observable
import io.reactivex.ObservableOperator
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.ReplaySubject
import io.reactivex.subjects.Subject

/**
 * Представляет собой lifecycle-aware [Subject].
 *
 * Цель: иметь возможность подписаться из View на единоразовые события от ViewModel без
 * необходимости отписываться вручную.
 */
class LiveSubject<T> @JvmOverloads constructor(
        filter: ((T) -> Boolean)? = null,
        observingState: Lifecycle.State = Lifecycle.State.STARTED,
        subjectType: SubjectType = SubjectType.PUBLISH
) : BaseLiveObservable<T>(filter, observingState) {

    private val subject: Subject<T> = subjectType.createSubject()

    override val observable: Observable<T> = subject.hide()

    fun onNext(event: T) {
        subject.onNext(event)
    }

    enum class SubjectType {
        PUBLISH, BEHAVIOUR, REPLAY;

        fun <T> createSubject(): Subject<T> = when (this) {
            PUBLISH -> PublishSubject.create<T>()
            BEHAVIOUR -> BehaviorSubject.create<T>()
            REPLAY -> ReplaySubject.create<T>()
        }
    }
}