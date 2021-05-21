package net.maxsmr.commonutils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

abstract class BaseBroadcastWrapper @JvmOverloads constructor(
        val context: Context,
        val intentFilter: IntentFilter,
        registerFirst: Boolean = true
) {

    private var receiver: ActionBroadcastReceiver? = null

    init {
        if (registerFirst) {
            registerReceiver()
        }
    }

    protected abstract fun onReceive(intent: Intent)

    protected open fun Intent.matches(): Boolean = true

    fun registerReceiver() {
        if (receiver == null) {
            receiver = ActionBroadcastReceiver()
            context.registerReceiver(receiver, intentFilter)
        }
    }

    fun unregisterReceiver() {
        receiver?.let {
            context.unregisterReceiver(it)
            receiver = null
        }
    }

    private inner class ActionBroadcastReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent?) {
            if (intent != null && intent.matches())
                onReceive(intent)
        }
    }
}