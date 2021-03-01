package net.maxsmr.commonutils.service

import android.app.Service
import android.content.Context
import android.content.Intent

class ServiceScheduler<S : Service>(
        private val context: Context,
        private val serviceClass: Class<S>
) {

    init {
        cancelDelay()
    }

    var isServiceScheduled = false
        set(value) {
            if (value != field) {
                field = value
                if (!value) {
                    cancelDelay()
                }
            }
        }

    fun start(args: Intent? = null) {
        if (start(context, serviceClass, args)) {
            isServiceScheduled = false
        }
    }

    fun restart(args: Intent? = null) {
        restart(context, serviceClass, args)
        isServiceScheduled = false
    }

    fun stop() {
        stop(context, serviceClass)
    }

    fun startDelay(delay: Long, args: Intent? = null) {
        if (startDelay(context, serviceClass, delay, args)) {
            isServiceScheduled = true
        }
    }


    fun restartDelay(delay: Long, args: Intent? = null) {
        if (restartDelay(context, serviceClass, delay, args)) {
            isServiceScheduled = true
        }
    }

    fun cancelDelay() {
        cancelDelay(context, serviceClass)
    }
}