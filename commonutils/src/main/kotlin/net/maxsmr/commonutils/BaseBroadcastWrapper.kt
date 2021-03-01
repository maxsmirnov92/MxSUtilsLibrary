package net.maxsmr.commonutils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

abstract class BaseBroadcastWrapper(val context: Context) {

    abstract val intentFilter: IntentFilter

    var isDisposed: Boolean = false
        private set(value) {
            if (field != value) {
                field = value
                if (field) {
                    unregisterReceiver()
                }
            }
        }

    private var receiver: ActionBroadcastReceiver? = null

    init {
        registerReceiver()
    }

    fun dispose() {
        isDisposed = true
    }

    protected abstract fun doAction(intent: Intent)

    private fun registerReceiver() {
        checkDisposed()
        if (receiver == null) {
            receiver = ActionBroadcastReceiver()
            context.registerReceiver(receiver, intentFilter)
        }
    }

    private fun unregisterReceiver() {
        receiver?.let {
            context.unregisterReceiver(it)
        }
    }

    private fun checkDisposed() {
        require(!isDisposed) {
            throw IllegalStateException("$this was disposed")
        }
    }

    private inner class ActionBroadcastReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent?) {
            if (isDisposed) {
                return
            }
            intent?.let {
                doAction(intent)
            }
        }
    }
}