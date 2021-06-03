package net.maxsmr.networkutils.monitor

import android.annotation.TargetApi
import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.ConnectivityManager.TYPE_MOBILE
import android.os.Build
import net.maxsmr.commonutils.getApplicationUid
import net.maxsmr.commonutils.hardware.DeviceUtils
import net.maxsmr.commonutils.isAtLeastMarshmallow
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder.Companion.logException
import java.lang.RuntimeException

/**
 * Only for apps which has allowed sharing stats to other apps
 */
@TargetApi(Build.VERSION_CODES.M)
class NetworkStatsWrapper @JvmOverloads constructor(
        private val context: Context,
        private val packageUid: Int = getApplicationUid(context, context.packageName) ?: -1
) {

    private val logger = BaseLoggerHolder.instance.getLogger<BaseLogger>(NetworkStatsWrapper::class.java)


    private val networkStatsManager: NetworkStatsManager = context.getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager?
            ?: throw RuntimeException("NetworkStatsManager is null")


    @JvmOverloads
    fun getTotalRxBytes(type: Int = TYPE_MOBILE): Long = querySummaryForDevice(type)?.rxBytes ?: -1

    @JvmOverloads
    fun getTotalTxBytes(type: Int = TYPE_MOBILE): Long = querySummaryForDevice(type)?.txBytes ?: -1

    @JvmOverloads
    fun getPackageRxBytes(type: Int = TYPE_MOBILE): Long = queryRxBytesForUid(type)

    @JvmOverloads
    fun getPackageTxBytes(type: Int = TYPE_MOBILE) : Long= queryTxBytesForUid(type)

    private fun querySummaryForDevice(type: Int): NetworkStats.Bucket? = try {
                networkStatsManager.querySummaryForDevice(type,
                        getSubscriberId(context, type),
                        0,
                        System.currentTimeMillis())
        } catch (e: Exception) {
            logException(logger, e, "querySummaryForDevice")
            null
        }

    private fun queryRxBytesForUid(type: Int): Long = queryDetailsForUidAndCalc(type) {
        it.rxBytes
    }

    private fun queryTxBytesForUid(type: Int): Long = queryDetailsForUidAndCalc(type) {
        it.txBytes
    }

    private fun queryDetailsForUidAndCalc(type: Int, value: (NetworkStats.Bucket) -> Long): Long {
        val networkStats = queryDetailsForUid(type) ?: return -1
        var result = 0L
        val bucket = NetworkStats.Bucket()
        while (networkStats.hasNextBucket()) {
            networkStats.getNextBucket(bucket)
            result += value(bucket)
        }
        networkStats.close()
        return result
    }

    private fun queryDetailsForUid(type: Int) : NetworkStats? = try {
        networkStatsManager.queryDetailsForUid(type,
                getSubscriberId(context, type),
                0,
                System.currentTimeMillis(),
                packageUid)
    } catch (e: Exception) {
        logException(logger, e, "queryDetailsForUid")
        null
    }

    private fun getSubscriberId(context: Context, networkType: Int): String? {
        return if (TYPE_MOBILE == networkType) {
            DeviceUtils.getIMSI(context)
        } else {
            EMPTY_STRING
        }
    }

    companion object {

        @JvmStatic
        val isNetworkStatsManagerSupported: Boolean = isAtLeastMarshmallow()
    }
}