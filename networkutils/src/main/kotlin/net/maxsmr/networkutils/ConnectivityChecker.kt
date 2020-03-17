package net.maxsmr.networkutils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.annotation.CallSuper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import net.maxsmr.commonutils.android.livedata.setValueIfNew
import net.maxsmr.networkutils.NetworkHelper.isOnline

private const val ACTION_CONNECTIVITY_CHANGE = "android.net.conn.CONNECTIVITY_CHANGE"

open class ConnectivityChecker(val context: Context) {

    private val connectedValue = MutableLiveData<Boolean>()

    val isConnected: LiveData<Boolean>
        get() = connectedValue

    var isDisposed: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                if (field) {
                    unregisterNetworkReceiver()
                }
            }
        }

    var connectedAction: (() -> Unit)? = null
    var notConnectedAction: (() -> Unit)? = null

    private var networkReceiver: NetworkStateChangedReceiver? = null

    init {
        registerNetworkReceiver()
    }

    @CallSuper
    protected open fun doActionWhenConnected() {
        checkDisposed()
        connectedAction?.invoke()
    }

    @CallSuper
    protected open fun doActionWhenNotConnected() {
        checkDisposed()
        notConnectedAction?.invoke()
    }

    private fun registerNetworkReceiver() {
        checkDisposed()
        if (networkReceiver == null) {
            with(IntentFilter()) {
                addAction(ACTION_CONNECTIVITY_CHANGE)
                networkReceiver = NetworkStateChangedReceiver()
                context.registerReceiver(networkReceiver, this)
            }
        }
    }

    private fun unregisterNetworkReceiver() {
        networkReceiver?.let {
            context.unregisterReceiver(it)
        }
    }

    private fun checkDisposed() {
        require(!isDisposed) {
            throw IllegalStateException("ConnectivityChecker was disposed")
        }
    }

    protected inner class NetworkStateChangedReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent?) {
            if (isDisposed) {
                return
            }
            intent?.let {
                if (it.action == ACTION_CONNECTIVITY_CHANGE) {
                    val isConnected = isOnline(context)
                    if (isConnected) {
                        doActionWhenConnected()
                    } else {
                        doActionWhenNotConnected()
                    }
                    connectedValue.setValueIfNew(isConnected)
                }
            }
        }
    }
}