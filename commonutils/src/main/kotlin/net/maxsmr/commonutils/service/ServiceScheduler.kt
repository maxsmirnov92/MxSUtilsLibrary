package net.maxsmr.commonutils.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Bundle

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

    var lastFailedMode: ServiceLaunchMode? = null

    fun start(args: Bundle? = null) {
        start(context, serviceClass, args).let {
            if (it == StartResult.STARTED) {
                isServiceScheduled = false
                lastFailedMode = null
            } else if (it == StartResult.NOT_STARTED_FAILED) {
                lastFailedMode = ServiceLaunchMode.START.apply { this.args = args }
            }
        }
    }

    fun restart(args: Bundle? = null) {
        if (restart(context, serviceClass, args)) {
            isServiceScheduled = false
            lastFailedMode = null
        } else {
            lastFailedMode = ServiceLaunchMode.RESTART.apply { this.args = args }
        }
    }

    fun stop() {
        stop(context, serviceClass)
    }

    fun startDelay(
            delay: Long,
            args: Bundle? = null,
            action: String? = null,
    ) {
        if (startDelay(context, serviceClass, delay, 0, args = args, action = action)) {
            isServiceScheduled = true
        }
    }


    fun restartDelay(
            delay: Long,
            args: Bundle? = null,
            action: String? = null,
    ) {
        if (restartDelay(context, serviceClass, delay, 0, args = args, action = action)) {
            isServiceScheduled = true
        }
    }

    fun cancelDelay() {
        cancelDelay(context, serviceClass, 0)
    }

    // вызвать вручную из того места, где отслеживается жизненный цикл
    fun notifyAppStateChanged(state: AppState) {
        lastFailedMode?.let {
            if (state == AppState.FOREGROUND) {
                when (it) {
                    ServiceLaunchMode.START -> start(it.args)
                    ServiceLaunchMode.RESTART -> restart(it.args)
                }
            }
        }
    }

    enum class ServiceLaunchMode {
        START, RESTART;

        var args: Bundle? = null
    }

    enum class AppState {

        FOREGROUND, BACKGROUND, NOT_RUNNING
    }
}