package net.maxsmr.networkutils.monitor

import android.net.TrafficStats
import net.maxsmr.commonutils.Observable
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.networkutils.monitor.model.NetworkTrafficStats
import net.maxsmr.networkutils.monitor.model.TrafficDirection
import net.maxsmr.tasksutils.ScheduledThreadPoolExecutorManager
import net.maxsmr.tasksutils.ScheduledThreadPoolExecutorManager.RunOptions
import net.maxsmr.tasksutils.ScheduledThreadPoolExecutorManager.ScheduleMode
import net.maxsmr.tasksutils.runnable.RunnableInfoRunnable
import net.maxsmr.tasksutils.taskexecutor.RunnableInfo
import java.util.concurrent.TimeUnit

val INTERVAL_NETWORK_STATS_MONITOR_DEFAULT: Long = 3000

class NetworkStatsMonitor(interval: Long = INTERVAL_NETWORK_STATS_MONITOR_DEFAULT) {

    private val logger = BaseLoggerHolder.getInstance().getLogger<BaseLogger>(NetworkStatsMonitor::class.java)

    private val manager = ScheduledThreadPoolExecutorManager("NetworkStatsMonitor")

    private val networkStatsListeners = NetworkStatsObservable()

    private var lastNetworkStats: NetworkTrafficStats? = null

    init {
        manager.addRunnableTask(NetworkStatsUpdateRunnable(1),
                RunOptions(0, interval, ScheduleMode.FIXED_DELAY))
    }

    fun addNetworkStatsListener(listener: NetworkStatsListener) {
        networkStatsListeners.registerObserver(listener)
    }

    fun removeNetworkStatsListener(listener: NetworkStatsListener) {
        networkStatsListeners.unregisterObserver(listener)
    }

    fun getLastNetworkStats(): NetworkTrafficStats? = lastNetworkStats

    @Synchronized
    fun start() {
        manager.start(1)
    }

    @Synchronized
    fun restart() {
        manager.restart(1)
    }

    @Synchronized
    fun stop() {
        manager.stop()
    }

    private inner class NetworkStatsUpdateRunnable(id: Int) : RunnableInfoRunnable<RunnableInfo?>(RunnableInfo(id)) {

        private var lastUpdateTime: Long = 0

        override fun run() {
            doNetworkTrafficMonitor()
        }

        private fun doNetworkTrafficMonitor() {
            logger.d("doNetworkTrafficMonitor()")
            val currentTotalBytesRx = TrafficStats.getTotalRxBytes()
            val currentTotalBytesTx = TrafficStats.getTotalTxBytes()
            if (currentTotalBytesRx < 0 && currentTotalBytesTx < 0) {
                logger.e("invalid total bytes: rx=$currentTotalBytesRx, tx=$currentTotalBytesTx")
                return
            }

            var direction = TrafficDirection.NONE
            var uploadSpeed = 0.0
            var downloadSpeed = 0.0
            val currentTime = System.currentTimeMillis()
            val interval = if (lastUpdateTime in 1 until currentTime) currentTime - lastUpdateTime else 0

            val lastNetworkStats = lastNetworkStats
            if (lastNetworkStats != null && lastNetworkStats.totalBytesRx > 0 && lastNetworkStats.totalBytesTx > 0) {
                if (currentTotalBytesRx > 0 && currentTotalBytesRx > lastNetworkStats.totalBytesRx) {
                    downloadSpeed = (currentTotalBytesRx - lastNetworkStats.totalBytesRx).toDouble() / TimeUnit.MILLISECONDS.toSeconds(interval)
                }
                if (currentTotalBytesTx > 0 && currentTotalBytesTx > lastNetworkStats.totalBytesTx) {
                    uploadSpeed = (currentTotalBytesTx - lastNetworkStats.totalBytesTx).toDouble() / TimeUnit.MILLISECONDS.toSeconds(interval)
                }
            } else {
                downloadSpeed = if (currentTotalBytesRx > 0) currentTotalBytesRx.toDouble() / TimeUnit.MILLISECONDS.toSeconds(interval) else 0.0
                uploadSpeed = if (currentTotalBytesRx > 0) currentTotalBytesRx.toDouble() / TimeUnit.MILLISECONDS.toSeconds(interval) else 0.0
            }
            if (downloadSpeed > 0 && uploadSpeed > 0) {
                direction = TrafficDirection.BOTH
            } else if (downloadSpeed > 0 && uploadSpeed == 0.0) {
                direction = TrafficDirection.RECEIVE
            } else if (downloadSpeed == 0.0 && uploadSpeed > 0) {
                direction = TrafficDirection.TRANSMIT
            }
            val highestUploadSpeedKBs = if (lastNetworkStats == null || uploadSpeed > lastNetworkStats.highestUploadSpeedKBs) uploadSpeed else lastNetworkStats.highestUploadSpeedKBs
            val highestDownloadSpeedKBs = if (lastNetworkStats == null || downloadSpeed > lastNetworkStats.highestDownloadSpeedKBs) downloadSpeed else lastNetworkStats.highestDownloadSpeedKBs
            with(NetworkTrafficStats(
                    currentTotalBytesRx,
                    currentTotalBytesTx,
                    TrafficStats.getTotalRxPackets(),
                    TrafficStats.getTotalTxPackets(),
                    uploadSpeed,
                    downloadSpeed,
                    highestUploadSpeedKBs,
                    highestDownloadSpeedKBs,
                    direction
            )) {
                this@NetworkStatsMonitor.lastNetworkStats = this
                lastUpdateTime = System.currentTimeMillis()
                networkStatsListeners.notifyUpdate(this)
            }
        }
    }

    private class NetworkStatsObservable : Observable<NetworkStatsListener?>() {

        fun notifyUpdate(stats: NetworkTrafficStats) {
            synchronized(observers) {
                for (l in observers) {
                    l?.onUpdateNetworkTrafficStats(stats)
                }
            }
        }
    }
}