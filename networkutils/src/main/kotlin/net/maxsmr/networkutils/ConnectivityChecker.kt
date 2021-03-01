package net.maxsmr.networkutils

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.annotation.CallSuper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import net.maxsmr.commonutils.BaseBroadcastWrapper
import net.maxsmr.commonutils.live.setValueIfNew
import net.maxsmr.networkutils.NetworkHelper.isOnline

private const val ACTION_CONNECTIVITY_CHANGE = "android.net.conn.CONNECTIVITY_CHANGE"

open class ConnectivityChecker(context: Context): BaseBroadcastWrapper(context) {

    private val connectedData = MutableLiveData<Boolean>()

    val isConnected: LiveData<Boolean>
        get() = connectedData

    override val intentFilter: IntentFilter
        get() = IntentFilter(ACTION_CONNECTIVITY_CHANGE)

    var connectedAction: (() -> Unit)? = null
    var notConnectedAction: (() -> Unit)? = null

    @CallSuper
    override fun doAction(intent: Intent) {
        val isConnected = isOnline(context)
        if (isConnected) {
            doActionWhenConnected()
        } else {
            doActionWhenNotConnected()
        }
        connectedData.setValueIfNew(isConnected)
    }

    @CallSuper
    protected open fun doActionWhenConnected() {
        connectedAction?.invoke()
    }

    @CallSuper
    protected open fun doActionWhenNotConnected() {
        notConnectedAction?.invoke()
    }
}