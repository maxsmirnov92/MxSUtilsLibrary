package net.maxsmr.networkutils.monitor;

import net.maxsmr.networkutils.monitor.stats.NetworkTrafficStats;

public interface NetworkStatsListener {

    void onUpdateNetworkTrafficStats(NetworkTrafficStats networkStats);
}
