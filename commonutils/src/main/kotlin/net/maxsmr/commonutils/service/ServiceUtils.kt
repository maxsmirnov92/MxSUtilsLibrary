package net.maxsmr.commonutils.service

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.text.TextUtils
import net.maxsmr.commonutils.*
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder

import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder.Companion.logException

private val logger = BaseLoggerHolder.instance.getLogger<BaseLogger>("ServiceUtils")

@JvmOverloads
fun <S : Service> isServiceRunning(
        context: Context,
        serviceClass: Class<S>,
        isForeground: Boolean = false
): Boolean {
    val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager?
            ?: throw RuntimeException(ActivityManager::class.java.simpleName + " is null")
    return manager.getRunningServices(Integer.MAX_VALUE).find { service -> serviceClass.name == service.service.className &&
            (!isForeground || service.foreground)} != null
}

@JvmOverloads
fun <S : Service> start(
        context: Context,
        serviceClass: Class<S>,
        args: Bundle? = null,
        action: String? = null,
        startForeground: Boolean = false
): StartResult {
    if (!isServiceRunning(context, serviceClass)) {
        return  if (startNoCheck(context, serviceClass, args, action, startForeground)) {
            StartResult.STARTED
        } else {
            StartResult.NOT_STARTED_FAILED
        }
    }
    return StartResult.NOT_STARTED_ALREADY_RUNNING
}

@JvmOverloads
fun <S : Service> restart(
        context: Context,
        serviceClass: Class<S>,
        args: Bundle? = null,
        action: String? = null,
        startForeground: Boolean = false
): Boolean {
    stop(context, serviceClass)
    return startNoCheck(context, serviceClass, args, action, startForeground)
}

fun <S : Service> stop(context: Context, serviceClass: Class<S>) {
    logger.d("stop, serviceClass=$serviceClass")
    if (isServiceRunning(context, serviceClass)) {
        stopNoCheck(context, serviceClass)
    }
}

fun <S : Service> restartDelay(
        context: Context,
        serviceClass: Class<S>,
        delay: Long,
        args: Bundle? = null,
        action: String? = null,
        requestCode: Int = 0,
        flags: Int = PendingIntent.FLAG_CANCEL_CURRENT,
        wakeUp: Boolean = true
): Boolean {
    logger.d("restartDelay, serviceClass=$serviceClass, delay=$delay")
    require(delay >= 0) { "Incorrect delay: $delay" }
    stop(context, serviceClass)
    return startDelayNoCheck(context, serviceClass, delay, args, action, requestCode, flags, wakeUp)
}


fun <S : Service> startDelay(
        context: Context,
        serviceClass: Class<S>,
        delay: Long,
        args: Bundle? = null,
        action: String? = null,
        requestCode: Int = 0,
        flags: Int = PendingIntent.FLAG_CANCEL_CURRENT,
        wakeUp: Boolean = true
): Boolean {
    logger.d("startDelay, serviceClass=$serviceClass, delay=$delay")
    if (!isServiceRunning(context, serviceClass)) {
        return restartDelayNoCheck(context, serviceClass, delay, args, action, requestCode, flags, wakeUp)
    }
    return false
}

fun <S : Service> cancelDelay(
        context: Context,
        serviceClass: Class<S>,
        requestCode: Int = 0,
        flags: Int = 0
) {
    logger.d("cancelDelay, serviceClass=$serviceClass")
    val intent = Intent(context, serviceClass)
    val pendingIntent = PendingIntent.getService(context, if (requestCode >= 0) requestCode else 0, intent, flags)
    cancelAlarm(context, pendingIntent)
}

fun <C : ServiceConnection, S : Service> bindService(
        context: Context,
        serviceClass: Class<S>,
        serviceConnection: C,
        args: Bundle? = null,
        action: String? = null,
        flags: Int = 0,
        restartIfFailed: Boolean = false
): Boolean {
    logger.d("bindService, serviceClass=$serviceClass, serviceConnection=$serviceConnection, flags=$flags")
    var bounded = false
    try {
        bounded = context.bindService(createServiceIntent(context.packageName, serviceClass, args, action), serviceConnection, flags)
    } catch (e: Exception) {
        logException(logger, e, "bindService")
    }

    if (!bounded) {
        logger.e("Binding to service $serviceClass failed")
        if (restartIfFailed) {
            restart(context, serviceClass)
        }
        return false
    }
    return true
}

fun <C : ServiceConnection> unbindService(context: Context, serviceConnection: C) {
    logger.d("unbindService, serviceConnection=$serviceConnection")
    try {
        context.unbindService(serviceConnection)
    } catch (e: Exception) {
        logException(logger, e, "unbindService")
    }
}

@JvmOverloads
fun createServiceIntent(
        packageName: String,
        serviceClass: Class<out Service>,
        args: Bundle?  = null,
        action: String? = null
): Intent {
    require(!TextUtils.isEmpty(packageName)) { "Empty package name: $packageName" }
    val componentName = ComponentName(packageName, serviceClass.name)
    val serviceIntent = if (action != null && action.isNotEmpty()) {
        Intent(action)
    } else {
        Intent()
    }.apply {
        args?.let {
            putExtras(it)
        }
    }
    serviceIntent.component = componentName
    return serviceIntent
}

fun createServicePendingIntent(
        context: Context,
        requestCode: Int,
        serviceClass: Class<out Service>,
        isForeground: Boolean,
        args: Bundle,
        action: String? = null
): PendingIntent {
    val intent = createServiceIntent(
            context.packageName,
            serviceClass,
            args,
            action
    )
    return if (isForeground && isAtLeastOreo()) {
        PendingIntent.getForegroundService(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_CANCEL_CURRENT
        )
    } else {
        PendingIntent.getService(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_CANCEL_CURRENT
        )
    }
}

private fun <S : Service> restartNoCheck(
        context: Context,
        serviceClass: Class<S>,
        args: Bundle? = null,
        action: String? = null,
        startForeground: Boolean = false
) {
    stopNoCheck(context, serviceClass)
    startNoCheck(context, serviceClass, args, action, startForeground)
}

private fun <S : Service> stopNoCheck(context: Context, serviceClass: Class<S>) {
    val service = Intent(context, serviceClass)
    context.stopService(service)
}

/**
 * @param startForeground true if intended to start in foreground for 8+
 */
@SuppressLint("NewApi")
@JvmOverloads
fun <S : Service> startNoCheck(
        context: Context,
        serviceClass: Class<S>,
        args: Bundle? = null,
        action: String? = null,
        startForeground: Boolean
): Boolean {
    val i = createServiceIntent(context.packageName, serviceClass, args, action)
    if (startForeground) {
        context.startForegroundService(i)
    } else {
        if (!isAtLeastOreo() || !isSelfAppInBackgroundOrThrow(context)) {
            context.startService(i)
        } else {
            logger.e("Cannot start service with intent $i: app is not in foreground")
            return false
        }
    }
    return true
}

private fun <S : Service> restartDelayNoCheck(
        context: Context,
        serviceClass: Class<S>,
        delay: Long,
        args: Bundle? = null,
        action: String? = null,
        requestCode: Int = 0,
        flags: Int = PendingIntent.FLAG_CANCEL_CURRENT,
        wakeUp: Boolean = true
): Boolean {

    require(delay >= 0) { "Incorrect delay: $delay" }

    stopNoCheck(context, serviceClass)
    return startDelayNoCheck(context, serviceClass, delay, args, action, requestCode, flags, wakeUp)
}

private fun <S : Service> startDelayNoCheck(
        context: Context,
        serviceClass: Class<S>,
        delay: Long,
        args: Bundle? = null,
        action: String? = null,
        requestCode: Int = 0,
        flags: Int = PendingIntent.FLAG_CANCEL_CURRENT,
        wakeUp: Boolean = true
): Boolean {
    cancelDelay(context, serviceClass)
    val pendingIntent = PendingIntent.getService(
            context,
            requestCode,
            createServiceIntent(context.packageName, serviceClass, args, action),
            flags
    )
    return setAlarm(context, pendingIntent, delay, wakeUp)
}

enum class StartResult {
    NOT_STARTED_ALREADY_RUNNING,
    NOT_STARTED_FAILED,
    STARTED
}