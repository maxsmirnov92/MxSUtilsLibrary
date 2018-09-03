package net.maxsmr.networkutils.monitor;

import android.net.TrafficStats;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import net.maxsmr.commonutils.data.Observable;
import net.maxsmr.commonutils.logger.BaseLogger;
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder;
import net.maxsmr.networkutils.monitor.model.NetworkTrafficStats;
import net.maxsmr.networkutils.monitor.model.TrafficDirection;
import net.maxsmr.tasksutils.ScheduledThreadPoolExecutorManager;

import java.util.concurrent.TimeUnit;

public final class NetworkStatsMonitor {

    private static final BaseLogger logger = BaseLoggerHolder.getInstance().getLogger(NetworkStatsMonitor.class);

    public final static long DEFAULT_NETWORK_STATS_MONITOR_INTERVAL = 3000;

    private final ScheduledThreadPoolExecutorManager manager = new ScheduledThreadPoolExecutorManager(ScheduledThreadPoolExecutorManager.ScheduleMode.FIXED_DELAY, NetworkStatsMonitor.class.getSimpleName());

    private final NetworkStatsObservable networkStatsListeners = new NetworkStatsObservable();

    @Nullable
    private NetworkTrafficStats lastNetworkStats;

    public NetworkStatsMonitor() {
        manager.addRunnableTask(new NetworkStatsUpdateRunnable());
    }

    public void addNetworkStatsListener(@NonNull NetworkStatsListener listener) {
        networkStatsListeners.registerObserver(listener);
    }

    public void removeNetworkStatsListener(NetworkStatsListener listener) {
        networkStatsListeners.unregisterObserver(listener);
    }

    @Nullable
    public NetworkTrafficStats getLastNetworkStats() {
        return lastNetworkStats;
    }

    public synchronized void start(long interval) {
        manager.start(interval);
    }

    public synchronized void stop() {
        manager.stop();
    }

    private class NetworkStatsUpdateRunnable implements Runnable {

        private long lastUpdateTime;

        @Override
        public void run() {
            doNetworkTrafficMonitor();
        }

        private void doNetworkTrafficMonitor() {
            logger.d("doNetworkTrafficMonitor()");

            final long currentTotalBytesRx = TrafficStats.getTotalRxBytes();
            final long currentTotalBytesTx = TrafficStats.getTotalTxBytes();

            if (currentTotalBytesRx < 0 && currentTotalBytesTx < 0) {
                logger.e("invalid total bytes: rx=" + currentTotalBytesRx + ", tx=" + currentTotalBytesTx);
                return;
            }

            TrafficDirection direction = TrafficDirection.NONE;

            double uploadSpeed = 0;
            double downloadSpeed = 0;

            final long currentTime = System.currentTimeMillis();
            final long interval = lastUpdateTime > 0 && currentTime > lastUpdateTime? currentTime - lastUpdateTime : 0;

            if (lastNetworkStats != null && lastNetworkStats.totalBytesRx > 0 && lastNetworkStats.totalBytesTx > 0) {

                if (currentTotalBytesRx > 0 && currentTotalBytesRx > lastNetworkStats.totalBytesRx) {
                    downloadSpeed = (double) (currentTotalBytesRx - lastNetworkStats.totalBytesRx) / TimeUnit.MILLISECONDS.toSeconds(interval);
                }

                if (currentTotalBytesTx > 0 && currentTotalBytesTx > lastNetworkStats.totalBytesTx) {
                    uploadSpeed = (double) (currentTotalBytesTx - lastNetworkStats.totalBytesTx) / TimeUnit.MILLISECONDS.toSeconds(interval);
                }

            } else {
                downloadSpeed = currentTotalBytesRx > 0? (double) currentTotalBytesRx / TimeUnit.MILLISECONDS.toSeconds(interval) : 0;
                uploadSpeed = currentTotalBytesRx > 0? (double) currentTotalBytesRx / TimeUnit.MILLISECONDS.toSeconds(interval) : 0;
            }

            if (downloadSpeed > 0 && uploadSpeed > 0) {
                direction = TrafficDirection.BOTH;
            } else if (downloadSpeed > 0 && uploadSpeed == 0) {
                direction = TrafficDirection.RECEIVE;
            } else if (downloadSpeed == 0 && uploadSpeed > 0) {
                direction = TrafficDirection.TRANSMIT;
            }

            final double highestUploadSpeedKBs = lastNetworkStats == null || uploadSpeed > lastNetworkStats.highestUploadSpeedKBs ? uploadSpeed
                    : lastNetworkStats.highestUploadSpeedKBs;
            final double highestDownloadSpeedKBs = lastNetworkStats == null || downloadSpeed > lastNetworkStats.highestDownloadSpeedKBs ? downloadSpeed
                    : lastNetworkStats.highestDownloadSpeedKBs;

            lastNetworkStats = new NetworkTrafficStats(currentTotalBytesRx, currentTotalBytesTx, TrafficStats.getTotalRxPackets(),
                    TrafficStats.getTotalTxPackets(), uploadSpeed, downloadSpeed, highestUploadSpeedKBs, highestDownloadSpeedKBs, direction);
            lastUpdateTime = System.currentTimeMillis();

            networkStatsListeners.notifyUpdate(lastNetworkStats);
        }

    }

    private static class NetworkStatsObservable extends Observable<NetworkStatsListener> {

        private void notifyUpdate(@NonNull NetworkTrafficStats stats) {
            synchronized (observers) {
                for (NetworkStatsListener l : observers) {
                    l.onUpdateNetworkTrafficStats(stats);
                }
            }
        }

    }
}
