package net.maxsmr.networkutils.monitor.model

import java.io.Serializable

data class NetworkTrafficStats(
        val totalBytesRx: Long = 0,

        val totalBytesTx: Long = 0,

        val totalPacketsRx: Long = 0,

        val totalPacketsTx: Long = 0,

        val uploadSpeedKBs: Double = 0.0,

        val downloadSpeedKBs: Double = 0.0,

        val highestUploadSpeedKBs: Double = 0.0,

        val highestDownloadSpeedKBs: Double = 0.0,

        val trafficDirection: TrafficDirection? = null
): Serializable {

    override fun toString(): String {
        return "NetworkTrafficStats(totalBytesRx=$totalBytesRx, totalBytesTx=$totalBytesTx, totalPacketsRx=$totalPacketsRx, totalPacketsTx=$totalPacketsTx, uploadSpeedKBs=$uploadSpeedKBs, downloadSpeedKBs=$downloadSpeedKBs, highestUploadSpeedKBs=$highestUploadSpeedKBs, highestDownloadSpeedKBs=$highestDownloadSpeedKBs, trafficDirection=$trafficDirection)"
    }
}