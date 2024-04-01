package net.maxsmr.rx_extensions

import android.app.Activity
import io.reactivex.subjects.PublishSubject

/**
 * Содержит активную (отображаемую) активити
 */
class ActiveActivityHolder {

    private val activitySubject = PublishSubject.create<Activity>()
    val activityObservable = activitySubject.hide()

    var activity: Activity? = null
        set(value) {
            field = value
            activitySubject.onNext(value ?: return)
        }

    val isExist: Boolean
        get() = activity != null

    fun clearActivity() {
        this.activity = null
    }
}