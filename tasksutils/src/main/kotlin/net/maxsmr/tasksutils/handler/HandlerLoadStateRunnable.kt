package net.maxsmr.tasksutils.handler

import android.os.Handler
import android.os.Looper
import androidx.annotation.CallSuper
import androidx.lifecycle.MutableLiveData
import net.maxsmr.commonutils.android.live.errorLoad
import net.maxsmr.commonutils.android.live.preLoad
import net.maxsmr.commonutils.android.live.successLoad
import net.maxsmr.commonutils.data.states.ILoadState
import java.lang.NullPointerException

abstract class HandlerLoadStateRunnable<T>(
        private val loadState: MutableLiveData<ILoadState<T>>,
        handler: Handler = Handler(Looper.getMainLooper())
) : HandlerRunnable<T>(handler) {

    @CallSuper
    override fun preExecute() {
        super.preExecute()
        loadState.preLoad()
    }

    @CallSuper
    override fun postExecute(result: T?) {
        super.postExecute(result)
        if (result != null) {
            loadState.successLoad(result)
        } else {
            loadState.errorLoad(NullPointerException("result is null"))
        }
    }
}