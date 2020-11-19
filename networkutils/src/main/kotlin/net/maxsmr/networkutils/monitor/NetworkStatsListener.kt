package net.maxsmr.networkutils.monitor

import net.maxsmr.networkutils.monitor.model.NetworkTrafficStats

interface NetworkStatsListener {

    fun onUpdateNetworkTrafficStats(networkStats: NetworkTrafficStats)
}
