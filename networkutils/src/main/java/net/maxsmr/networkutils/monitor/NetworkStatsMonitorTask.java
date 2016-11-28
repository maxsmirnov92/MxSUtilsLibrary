package net.maxsmr.networkutils.monitor;

import android.net.TrafficStats;

import net.maxsmr.commonutils.data.MathUtils;
import net.maxsmr.networkutils.NetworkHelper;
import net.maxsmr.networkutils.monitor.stats.NetworkTrafficStats;
import net.maxsmr.networkutils.monitor.stats.TRAFFIC_DIRECTION;
import net.maxsmr.networkutils.watcher.NetworkWatcher;
import net.maxsmr.tasksutils.ScheduledThreadPoolExecutorManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;

public class NetworkStatsMonitorTask extends ScheduledThreadPoolExecutorManager {

    private static final Logger logger = LoggerFactory.getLogger(NetworkStatsMonitorTask.class);

    // private final int appUid;

    public NetworkStatsMonitorTask(/* int appUid */) {
        super(ScheduledThreadPoolExecutorManager.ScheduleMode.FIXED_DELAY, NetworkStatsMonitorTask.class.getSimpleName());
        addRunnableTask(networkStatsUpdateRunnable);
        // this.appUid = appUid;
    }

    private final LinkedList<NetworkStatsListener> networkStatsListeners = new LinkedList<NetworkStatsListener>();

    public void addNetworkStatsListener(NetworkStatsListener listener) throws NullPointerException {

        if (listener == null) {
            throw new NullPointerException();
        }

        synchronized (networkStatsListeners) {
            if (!networkStatsListeners.contains(listener)) {
                networkStatsListeners.add(listener);
            }
        }
    }

    public void removeNetworkStatsListener(NetworkStatsListener listener) {
        synchronized (networkStatsListeners) {
            if (networkStatsListeners.contains(listener)) {
                networkStatsListeners.remove(listener);
            }
        }
    }

    public final static long DEFAULT_NETWORK_STATS_MONITOR_INTERVAL = 3000;

    private NetworkTrafficStats networkStats;

    public NetworkTrafficStats getLastNetworkStats() {
        return networkStats;
    }

    private final NetworkStatsUpdateRunnable networkStatsUpdateRunnable = new NetworkStatsUpdateRunnable();

    private class NetworkStatsUpdateRunnable implements Runnable {

        @Override
        public void run() {
            doNetworkTrafficMonitor();
        }

        private void doNetworkTrafficMonitor() {
            logger.debug("doNetworkTrafficMonitor()");

            if (networkStats == null)
                networkStats = new NetworkTrafficStats();

            final long currentTotalBytesRx = TrafficStats.getTotalRxBytes(); // TrafficStats.getUidRxBytes(appUid);
            final long currentTotalBytesTx = TrafficStats.getTotalTxBytes(); // TrafficStats.getUidTxBytes(appUid);

            // logger.debug("previousTotalBytesRx={}", networkStats.getTotalBytesRx());
            // logger.debug("previousTotalBytesTx={}", networkStats.getTotalBytesTx());
            // logger.debug("currentTotalBytesRx={}", currentTotalBytesRx);
            // logger.debug("currentTotalBytesTx={}", currentTotalBytesTx);

            TRAFFIC_DIRECTION direction = TRAFFIC_DIRECTION.NONE;

            double uploadSpeedKBs = 0;
            double downloadSpeedKBs = 0;

            if (networkStats.totalBytesRx > 0 && networkStats.totalBytesTx > 0) {

                if (currentTotalBytesRx > networkStats.totalBytesRx && currentTotalBytesTx > networkStats.totalBytesTx) {
                    direction = TRAFFIC_DIRECTION.BOTH;
                    uploadSpeedKBs = (currentTotalBytesTx - networkStats.totalBytesTx) / (getIntervalMs() / 1000d) / 1024d;
                    downloadSpeedKBs = (currentTotalBytesRx - networkStats.totalBytesRx) / (getIntervalMs() / 1000d) / 1024d;
                } else if (currentTotalBytesRx > networkStats.totalBytesRx && currentTotalBytesTx == networkStats.totalBytesTx) {
                    direction = TRAFFIC_DIRECTION.RECEIVE;
                    downloadSpeedKBs = (currentTotalBytesRx - networkStats.totalBytesRx) / (getIntervalMs() / 1000d) / 1024d;
                } else if (currentTotalBytesRx == networkStats.totalBytesRx && currentTotalBytesTx > networkStats.totalBytesTx) {
                    direction = TRAFFIC_DIRECTION.TRANSMIT;
                    uploadSpeedKBs = (currentTotalBytesTx - networkStats.totalBytesTx) / (getIntervalMs() / 1000d) / 1024d;
                } else if (currentTotalBytesRx == networkStats.totalBytesRx && currentTotalBytesTx == networkStats.totalBytesTx) {
                    direction = TRAFFIC_DIRECTION.NONE;
                }
            }

            final double highestUploadSpeedKBs = uploadSpeedKBs > networkStats.highestUploadSpeedKBs ? uploadSpeedKBs
                    : networkStats.highestUploadSpeedKBs;
            final double highestDownloadSpeedKBs = downloadSpeedKBs > networkStats.highestDownloadSpeedKBs ? downloadSpeedKBs
                    : networkStats.highestDownloadSpeedKBs;

            networkStats = new NetworkTrafficStats(currentTotalBytesRx, currentTotalBytesTx, TrafficStats.getTotalRxPackets(),
                    TrafficStats.getTotalTxPackets(), uploadSpeedKBs, downloadSpeedKBs, highestUploadSpeedKBs, highestDownloadSpeedKBs,
                    direction, NetworkHelper.isReachable(NetworkHelper.getInetAddressByIp(NetworkWatcher.HostPingTask.DEFAULT_PING_IP_ADDRESS), 1,
                    MathUtils.safeLongToInt(NetworkWatcher.HostPingTask.DEFAULT_TIMEOUT / 1000)));
            // TrafficStats.getUidRxPackets(appUid), TrafficStats.getUidTxPackets(appUid);

            synchronized (networkStatsListeners) {
                if (networkStatsListeners.size() > 0) {
                    for (NetworkStatsListener l : networkStatsListeners) {
                        l.onUpdateNetworkTrafficStats(networkStats);
                    }
                }
            }
        }

    }

    @Override
    public synchronized void stop(boolean await, long timeoutMs) {
        super.stop(await, timeoutMs);

        networkStats = new NetworkTrafficStats();

        synchronized (networkStatsListeners) {
            if (networkStatsListeners.size() > 0) {
                for (NetworkStatsListener l : networkStatsListeners) {
                    l.onUpdateNetworkTrafficStats(getLastNetworkStats());
                }
            }
        }
    }

}
