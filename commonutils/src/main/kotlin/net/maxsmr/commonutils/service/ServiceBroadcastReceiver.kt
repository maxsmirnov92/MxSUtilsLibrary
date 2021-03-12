package net.maxsmr.commonutils.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.annotation.CallSuper
import androidx.annotation.MainThread
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import net.maxsmr.commonutils.Predicate
import net.maxsmr.commonutils.text.isEmpty
import java.util.*

/**
 * @param serviceClasses null or empty if subscribe on all
 */

open class ServiceBroadcastReceiver(serviceClasses: Collection<Class<out Service>>?): BroadcastReceiver() {

    val serviceClasses = mutableSetOf<Class<out Service>>()

    init {
        serviceClasses?.let {
            this.serviceClasses.addAll(it)
        }
    }

    @CallSuper
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val name = intent.getStringExtra(EXTRA_SERVICE_NAME)
        if (name != null && !isEmpty(name)) {
            val serviceClass = Predicate.Methods.find(serviceClasses) { element: Class<out Service?> -> element.name.equals(name, ignoreCase = true) }
            if (serviceClass != null) {
                val action = intent.action
                if (action != null) {
                    when (action) {
                        ACTION_SERVICE_CREATED -> doActionOnCreate(name)
                        ACTION_SERVICE_STARTED -> doActionOnStart(name)
                        ACTION_SERVICE_DESTROYED -> doActionOnDestroyed(name)
                    }
                }
            }
        }
    }

    @MainThread
    protected fun doActionOnCreate(className: String) {
    }

    @MainThread
    protected fun doActionOnStart(className: String) {
    }

    @MainThread
    protected fun doActionOnDestroyed(className: String) {
    }

    companion object {

        const val ACTION_SERVICE_CREATED = "service_created"
        const val ACTION_SERVICE_STARTED = "service_started"
        const val ACTION_SERVICE_DESTROYED = "service_destroyed"

        const val EXTRA_SERVICE_NAME = "service_name"

        fun <S : Service> sendBroadcastCreated(service: S) {
            sendBroadcast(service, ACTION_SERVICE_CREATED)
        }

        fun <S : Service> sendBroadcastStarted(service: S) {
            sendBroadcast(service, ACTION_SERVICE_STARTED)
        }

        fun <S : Service> sendBroadcastDestroyed(service: S) {
            sendBroadcast(service, ACTION_SERVICE_DESTROYED)
        }

        private fun <S : Service> sendBroadcast(service: S, action: String) {
            require(!isEmpty(action)) { "empty action" }
            LocalBroadcastManager.getInstance(service).sendBroadcast(Intent(action).putExtra(EXTRA_SERVICE_NAME, service.javaClass.getName()))
        }

        fun register(context: Context, receiver: ServiceBroadcastReceiver) {
            val filter = IntentFilter()
            filter.addAction(ACTION_SERVICE_CREATED)
            filter.addAction(ACTION_SERVICE_STARTED)
            filter.addAction(ACTION_SERVICE_DESTROYED)
            LocalBroadcastManager.getInstance(context).registerReceiver(receiver, filter)
        }

        fun unregister(context: Context, receiver: ServiceBroadcastReceiver) {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver)
        }
    }

}