package net.maxsmr.networkutils.monitor;

import net.maxsmr.networkutils.monitor.model.NetworkTrafficStats;

public interface NetworkStatsListener {

    void onUpdateNetworkTrafficStats(NetworkTrafficStats networkStats);
}
