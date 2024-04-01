package net.maxsmr.commonutils.live.wrappers

import androidx.annotation.CallSuper
import androidx.annotation.MainThread
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.atomic.AtomicBoolean

/**
 * [MutableLiveData] с оповещением observer'ов по желанию
 */
open class NotifyCheckMutableLiveData<T>(
        initialValue: T?
) : MutableLiveData<T>(initialValue) {

    // копия списка, поскольку фактический является private
    private val observers = CopyOnWriteArraySet<ObserverWrapper<in T>>()

    override fun observe(owner: LifecycleOwner, observer: Observer<in T>) {
        val wrapper = ObserverWrapper(observer)
        observers.add(wrapper)
        super.observe(owner, wrapper)
    }

    override fun removeObservers(owner: LifecycleOwner) {
        observers.clear()
        super.removeObservers(owner)
    }

    override fun removeObserver(observer: Observer<in T>) {
        observers.remove(observer as ObserverWrapper<in T>)
        super.removeObserver(observer)
    }

    @MainThread
    @CallSuper
    open fun setValue(t: T?, shouldNotify: Boolean) {
        setShouldNotify(shouldNotify)
        value = t
    }

    @MainThread
    @CallSuper
    open fun postValue(t: T?, shouldNotify: Boolean) {
        setShouldNotify(shouldNotify)
        postValue(t)
    }

    fun setShouldNotify(shouldNotify: Boolean) {
        observers.forEach { it.shouldNotify.set(shouldNotify) }
    }

    private class ObserverWrapper<T>(private val observer: Observer<T>) : Observer<T> {

        val shouldNotify = AtomicBoolean(true)

        override fun onChanged(value: T) {
            if (shouldNotify.get()) {
                observer.onChanged(value)
            }
        }
    }
}