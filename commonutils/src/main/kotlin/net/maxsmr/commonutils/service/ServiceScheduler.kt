package net.maxsmr.commonutils.service

import android.app.PendingIntent
import android.app.Service
import android.content.Context
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

    fun stop() = stop(context, serviceClass)

    @JvmOverloads
    fun startDelay(
            delay: Long,
            requestCode: Int = 0,
            isForeground: Boolean = false,
            packageName: String = context.packageName,
            args: Bundle? = null,
            action: String? = null,
            flags: Int = PendingIntent.FLAG_CANCEL_CURRENT,
            shouldWakeUp: Boolean = true
    ) {
        if (startDelay(context, serviceClass, delay, requestCode, isForeground, packageName, args, action, flags, shouldWakeUp)) {
            isServiceScheduled = true
        }
    }

    @JvmOverloads
    fun restartDelay(
            delay: Long,
            requestCode: Int = 0,
            isForeground: Boolean = false,
            packageName: String = context.packageName,
            args: Bundle? = null,
            action: String? = null,
            flags: Int = PendingIntent.FLAG_CANCEL_CURRENT,
            shouldWakeUp: Boolean = true
    ) {
        if (restartDelay(context, serviceClass, delay, requestCode, isForeground, packageName, args, action, flags, shouldWakeUp)) {
            isServiceScheduled = true
        }
    }

    @JvmOverloads
    fun cancelDelay(
            requestCode: Int = 0,
            flags: Int = PendingIntent.FLAG_CANCEL_CURRENT,
            isForeground: Boolean = false,
            packageName: String = context.packageName,
            args: Bundle? = null,
            action: String? = null
    ) {
        cancelDelay(context, serviceClass, requestCode, flags, isForeground, packageName, args, action)
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