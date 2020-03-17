package net.maxsmr.commonutils.android.livedata.wrappers

import net.maxsmr.commonutils.android.livedata.setValueIfNewNotify

data class LiveDataCanError<T>(
        private val initialDataValue: T?
) : NotifyCheckMutableLiveData<T>(initialDataValue) {

    val error: NotifyCheckMutableLiveData<Throwable?> = NotifyCheckMutableLiveData(null)

    override fun setValue(t: T?, shouldNotify: Boolean) {
        super.setValue(t, shouldNotify)
        error.setValue(null, shouldNotify)
    }

    fun setError(
            throwable: Throwable,
            eagerNotify: Boolean,
            shouldNotify: Boolean
    ) {
        error.setValueIfNewNotify(throwable, eagerNotify, shouldNotify)
    }
}