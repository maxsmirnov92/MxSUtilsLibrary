package net.maxsmr.commonutils.android.service

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.text.TextUtils
import android.util.Log
import net.maxsmr.commonutils.android.*
import net.maxsmr.commonutils.android.analytics.AnalyticsHelper
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder

private val logger = BaseLoggerHolder.getInstance().getLogger<BaseLogger>("ServiceUtils")

fun <S : Service> isServiceRunning(context: Context, serviceClass: Class<S>): Boolean {
    val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager?
            ?: throw RuntimeException(ActivityManager::class.java.simpleName + " is null")
    return manager.getRunningServices(Integer.MAX_VALUE).find { service -> serviceClass.name == service.service.className } != null
}

fun <S : Service> isServiceForeground(context: Context, serviceClass: Class<S>): Boolean {
    val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager?
            ?: throw RuntimeException(ActivityManager::class.java.simpleName + " is null")
    return manager.getRunningServices(Integer.MAX_VALUE).find { service -> serviceClass.name == service.service.className && service.foreground } != null
}

fun <S : Service> start(context: Context, serviceClass: Class<S>, args: Intent? = null): Boolean {
    if (!isServiceRunning(context, serviceClass)) {
        restartNoCheck(context, serviceClass, args)
        return true
    }
    return false
}

fun <S : Service> restart(context: Context, serviceClass: Class<S>, args: Intent? = null) {
    stop(context, serviceClass)
    startNoCheck(context, serviceClass, args)
}

fun <S : Service> stop(context: Context, serviceClass: Class<S>) {
    logger.d( "stop(), serviceClass=$serviceClass")
    if (isServiceRunning(context, serviceClass)) {
        stopNoCheck(context, serviceClass)
    }
}

fun <S : Service> restartDelay(
        context: Context,
        serviceClass: Class<S>,
        delay: Long,
        args: Intent? = null,
        requestCode: Int = 0,
        flags: Int = PendingIntent.FLAG_CANCEL_CURRENT,
        wakeUp: Boolean = true
): Boolean {
    logger.d( "restartDelay(), serviceClass=$serviceClass, delay=$delay")

    require(delay >= 0) { "Incorrect delay: $delay" }
    stop(context, serviceClass)
    return startDelayNoCheck(context, serviceClass, delay, args, requestCode, flags, wakeUp)
}


fun <S : Service> startDelay(
        context: Context,
        serviceClass: Class<S>,
        delay: Long,
        args: Intent? = null,
        requestCode: Int = 0,
        flags: Int = PendingIntent.FLAG_CANCEL_CURRENT,
        wakeUp: Boolean = true
): Boolean {
    logger.d( "startDelay(), serviceClass=$serviceClass, delay=$delay")
    if (!isServiceRunning(context, serviceClass)) {
        return restartDelayNoCheck(context, serviceClass, delay, args, requestCode, flags, wakeUp)
    }
    return false
}

fun <S : Service> cancelDelay(
        context: Context,
        serviceClass: Class<S>,
        requestCode: Int = 0,
        flags: Int = 0
) {
    logger.d( "cancelDelay(), serviceClass=$serviceClass")
    val intent = Intent(context, serviceClass)
    val pendingIntent = PendingIntent.getService(context, if (requestCode >= 0) requestCode else 0, intent, flags)
    cancelAlarm(context, pendingIntent)
}

fun <C : ServiceConnection, S : Service> bindService(
        context: Context,
        serviceClass: Class<S>,
        serviceConnection: C,
        args: Intent? = null,
        flags: Int = 0
): Boolean {
    logger.d( "bindService(), serviceClass=$serviceClass, serviceConnection=$serviceConnection, flags=$flags")
    var bounded = false
    try {
        bounded = context.bindService(createServiceIntent(context.packageName, serviceClass, args), serviceConnection, flags)
    } catch (e: Exception) {
        logger.e("An Exception occurred during bindService()", e)
    }

    if (!bounded) {
        logger.e("Binding to service $serviceClass failed")
        restart(context, serviceClass)
        return false
    }
    return true
}

fun <C : ServiceConnection> unbindService(context: Context, serviceConnection: C) {
    logger.d( "unbindService(), serviceConnection=$serviceConnection")
    try {
        context.unbindService(serviceConnection)
    } catch (e: Exception) {
        logger.e("An Exception occurred during unbindService()", e)
    }

}

private fun <S : Service> restartNoCheck(context: Context, serviceClass: Class<S>, args: Intent? = null) {
    stopNoCheck(context, serviceClass)
    startNoCheck(context, serviceClass, args)
}

private fun <S : Service> stopNoCheck(context: Context, serviceClass: Class<S>) {
    val service = Intent(context, serviceClass)
    context.stopService(service)
}

/**
 * @param shouldCheckSdk true if intended to start in foreground for 8+
 */
@SuppressLint("NewApi")
private fun <S : Service> startNoCheck(context: Context, serviceClass: Class<S>, args: Intent?, shouldCheckSdk: Boolean = false) {
    val i = createServiceIntent(context.packageName, serviceClass, args)
    val isOreo = SdkUtils.isAtLeastOreo()
    if (shouldCheckSdk && isOreo) {
        context.startForegroundService(i)
    } else {
        if (!isOreo || !isSelfAppInBackground(context)) {
            context.startService(i)
        } else {
            logger.e("Cannot start service with intent $i: app is not in background")
        }
    }
}

private fun <S : Service> restartDelayNoCheck(
        context: Context,
        serviceClass: Class<S>,
        delay: Long,
        args: Intent? = null,
        requestCode: Int = 0,
        flags: Int = PendingIntent.FLAG_CANCEL_CURRENT,
        wakeUp: Boolean = true
): Boolean {

    require(delay >= 0) { "Incorrect delay: $delay" }

    stopNoCheck(context, serviceClass)
    return startDelayNoCheck(context, serviceClass, delay, args, requestCode, flags, wakeUp)
}

private fun <S : Service> startDelayNoCheck(
        context: Context,
        serviceClass: Class<S>,
        delay: Long,
        args: Intent? = null,
        requestCode: Int = 0,
        flags: Int = PendingIntent.FLAG_CANCEL_CURRENT,
        wakeUp: Boolean = true
): Boolean {
    cancelDelay(context, serviceClass)
    val pendingIntent = PendingIntent.getService(
            context,
            requestCode,
            createServiceIntent(context.packageName, serviceClass, args),
            flags
    )
    return setAlarm(context, pendingIntent, delay, wakeUp)
}

private fun createServiceIntent(packageName: String, serviceClass: Class<out Service>, args: Intent?): Intent {
    require(!TextUtils.isEmpty(packageName)) { "Empty package name: $packageName" }
    val componentName = ComponentName(packageName, serviceClass.name)
    val serviceIntent: Intent = if (args != null) {
        Intent(args)
    } else {
        Intent()
    }
    serviceIntent.component = componentName
    return serviceIntent
}