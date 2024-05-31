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
import net.maxsmr.commonutils.cancelAlarm
import net.maxsmr.commonutils.isAtLeastNougat
import net.maxsmr.commonutils.isAtLeastOreo
import net.maxsmr.commonutils.isAtLeastS
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder.Companion.logException
import net.maxsmr.commonutils.setAlarm

private val logger = BaseLoggerHolder.instance.getLogger<BaseLogger>("ServiceUtils")

@JvmOverloads
fun <S : Service> isServiceRunning(
    context: Context,
    serviceClass: Class<S>,
    isForeground: Boolean? = null
): Boolean {
    val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager?
        ?: throw RuntimeException("ActivityManager is null")
    return manager.getRunningServices(Integer.MAX_VALUE).find { service ->
        serviceClass.name == service.service.className &&
                (isForeground == null || service.foreground == isForeground)
    } != null
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
        return if (startNoCheck(context, serviceClass, args, action, startForeground)) {
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

fun <S : Service> stop(context: Context, serviceClass: Class<S>): Boolean {
    logger.d("stop, serviceClass=$serviceClass")
    if (isServiceRunning(context, serviceClass)) {
        return stopNoCheck(context, serviceClass)
    }
    return true
}

fun <S : Service> restartDelay(
    context: Context,
    serviceClass: Class<S>,
    delay: Long,
    requestCode: Int = 0,
    isForeground: Boolean = false,
    packageName: String = context.packageName,
    args: Bundle? = null,
    action: String? = null,
    flags: Int = PendingIntent.FLAG_CANCEL_CURRENT,
    shouldWakeUp: Boolean = true
): Boolean {
    logger.d("restartDelay, serviceClass=$serviceClass, delay=$delay")
    require(delay >= 0) { "Incorrect delay: $delay" }
    stop(context, serviceClass)
    return startDelayNoCheck(
        context,
        serviceClass,
        delay,
        requestCode,
        flags,
        isForeground,
        packageName,
        args,
        action,
        shouldWakeUp
    )
}


fun <S : Service> startDelay(
    context: Context,
    serviceClass: Class<S>,
    delay: Long,
    requestCode: Int = 0,
    isForeground: Boolean = false,
    packageName: String = context.packageName,
    args: Bundle? = null,
    action: String? = null,
    flags: Int = PendingIntent.FLAG_CANCEL_CURRENT,
    shouldWakeUp: Boolean = true
): Boolean {
    logger.d("startDelay, serviceClass=$serviceClass, delay=$delay")
    if (!isServiceRunning(context, serviceClass)) {
        return restartDelayNoCheck(
            context,
            serviceClass,
            delay,
            requestCode,
            flags,
            isForeground,
            packageName,
            args,
            action,
            shouldWakeUp
        )
    }
    return false
}

@JvmOverloads
fun <S : Service> cancelDelay(
    context: Context,
    serviceClass: Class<S>,
    requestCode: Int = 0,
    flags: Int = PendingIntent.FLAG_CANCEL_CURRENT,
    isForeground: Boolean = false,
    packageName: String = context.packageName,
    args: Bundle? = null,
    action: String? = null
) {
    logger.d("cancelDelay, serviceClass=$serviceClass")
    val pendingIntent = createServicePendingIntent(
        context,
        serviceClass,
        requestCode,
        flags,
        isForeground,
        packageName,
        args,
        action
    )
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
        bounded = context.bindService(
            createServiceIntent(
                context.packageName,
                serviceClass,
                args,
                action
            ), serviceConnection, flags
        )
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
    args: Bundle? = null,
    action: String? = null
): Intent {
    require(!TextUtils.isEmpty(packageName)) { "Empty package name: $packageName" }
    val componentName = ComponentName(packageName, serviceClass.name)
    val serviceIntent = if (!action.isNullOrEmpty()) {
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

@JvmOverloads
fun createServicePendingIntent(
    context: Context,
    serviceClass: Class<out Service>,
    requestCode: Int = 0,
    flags: Int,
    isForeground: Boolean = false,
    packageName: String = context.packageName,
    args: Bundle? = null,
    action: String? = null
): PendingIntent {
    val intent = createServiceIntent(
        packageName,
        serviceClass,
        args,
        action
    )
    val modifiedFlags = withMutabilityFlag(flags, false)
    return if (isForeground && isAtLeastOreo()) {
        PendingIntent.getForegroundService(
            context,
            requestCode,
            intent,
            modifiedFlags
        )
    } else {
        PendingIntent.getService(
            context,
            requestCode,
            intent,
            modifiedFlags
        )
    }
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
    startForeground: Boolean = false
): Boolean {
    val i = createServiceIntent(context.packageName, serviceClass, args, action)
    return try {
        if (startForeground && isAtLeastOreo()) {
            context.startForegroundService(i) != null
        } else {
            context.startService(i) != null
        }
    } catch (e: Exception) {
        // на >=O выбросится если вызов был с опозданием после определённого интервала при уходе аппа в bg
        logException(logger, e, "Cannot start service with intent $i")
        false
    }
}

fun withMutabilityFlag(flags: Int, mutable: Boolean): Int {
    val mutableFlag = if (mutable) {
        if (isAtLeastS()) PendingIntent.FLAG_MUTABLE else 0
    } else {
        PendingIntent.FLAG_IMMUTABLE
    }
    return flags or mutableFlag
}

fun Service.stopForegroundCompat(removeNotification: Boolean) {
    if (removeNotification) {
        stopForeground(true)
    } else {
        if (isAtLeastNougat()) {
            stopForeground(Service.STOP_FOREGROUND_DETACH)
        } else {
            stopForeground(false)
        }
    }
}

private fun <S : Service> stopNoCheck(context: Context, serviceClass: Class<S>): Boolean {
    val service = Intent(context, serviceClass)
    return context.stopService(service)
}

private fun <S : Service> restartDelayNoCheck(
    context: Context,
    serviceClass: Class<S>,
    delay: Long,
    requestCode: Int = 0,
    flags: Int = PendingIntent.FLAG_CANCEL_CURRENT,
    isForeground: Boolean = false,
    packageName: String = context.packageName,
    args: Bundle? = null,
    action: String? = null,
    shouldWakeUp: Boolean = true
): Boolean {

    require(delay >= 0) { "Incorrect delay: $delay" }

    stopNoCheck(context, serviceClass)
    return startDelayNoCheck(
        context,
        serviceClass,
        delay,
        requestCode,
        flags,
        isForeground,
        packageName,
        args,
        action,
        shouldWakeUp
    )
}

private fun <S : Service> startDelayNoCheck(
    context: Context,
    serviceClass: Class<S>,
    delay: Long,
    requestCode: Int = 0,
    flags: Int = PendingIntent.FLAG_CANCEL_CURRENT,
    isForeground: Boolean = false,
    packageName: String = context.packageName,
    args: Bundle? = null,
    action: String? = null,
    shouldWakeUp: Boolean = true
): Boolean {
    cancelDelay(context, serviceClass, requestCode)
    return setAlarm(
        context,
        createServicePendingIntent(
            context,
            serviceClass,
            requestCode,
            flags,
            isForeground,
            packageName,
            args,
            action
        ),
        delay,
        shouldWakeUp
    )
}

enum class StartResult {
    NOT_STARTED_ALREADY_RUNNING,
    NOT_STARTED_FAILED,
    STARTED
}