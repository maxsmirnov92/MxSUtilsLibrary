package net.maxsmr.networkutils.monitor

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager.TYPE_MOBILE
import android.net.TrafficStats
import android.os.Process
import net.maxsmr.commonutils.getApplicationUid
import net.maxsmr.networkutils.monitor.NetworkStatsWrapper.Companion.isNetworkStatsManagerSupported

fun shouldUseTrafficStats(uid: Int): Boolean {
    val callingUid = Process.myUid()
    return !isNetworkStatsManagerSupported || callingUid == Process.SYSTEM_UID || uid > 0 && callingUid == uid
}

@JvmOverloads
fun getUidRxBytes(packageName: String, context: Context, type: Int = TYPE_MOBILE): Long =
        getUidRxBytes(getApplicationUid(context, packageName) ?: -1, context, type)

@JvmOverloads
@SuppressLint("InlinedApi")
fun getUidRxBytes(uid: Int, context: Context, type: Int = TYPE_MOBILE): Long {
    return if (shouldUseTrafficStats(uid)) {
        TrafficStats.getUidRxBytes(uid)
    } else {
        NetworkStatsWrapper(context, uid).getPackageRxBytes(type)
    }
}

@JvmOverloads
fun getUidTxBytes(packageName: String, context: Context, type: Int = TYPE_MOBILE): Long =
        getUidTxBytes(getApplicationUid(context, packageName) ?: -1, context, type)

@JvmOverloads
@SuppressLint("InlinedApi")
fun getUidTxBytes(uid: Int, context: Context, type: Int = TYPE_MOBILE): Long {
    return if (shouldUseTrafficStats(uid)) {
        TrafficStats.getUidTxBytes(uid)
    } else {
        NetworkStatsWrapper(context, uid).getPackageTxBytes(type)
    }
}
