package net.maxsmr.commonutils.android.gui.fragments.actions

import androidx.annotation.CallSuper
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

/**
 * Базовый тип для взаимодействия VM -> View
 */
abstract class BaseViewModelAction<Actor> {

    protected val actorSubject = PublishSubject.create<Actor>()

    fun actorObservable(): Observable<Actor> = actorSubject.hide()

    @CallSuper
    open fun doAction(actor: Actor) {
        actorSubject.onNext(actor)
    }
}