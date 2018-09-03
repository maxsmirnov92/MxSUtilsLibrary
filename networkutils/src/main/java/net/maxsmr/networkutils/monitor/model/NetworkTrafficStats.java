package net.maxsmr.networkutils.monitor.model;

import java.io.Serializable;

public class NetworkTrafficStats implements Serializable {

    private static final long serialVersionUID = 280509419386754690L;

    public final long totalBytesRx;

    public final long totalBytesTx;

    public final long totalPacketsRx;

    public final long totalPacketsTx;

    public final double uploadSpeedKBs;

    public final double downloadSpeedKBs;

    public final double highestUploadSpeedKBs;

    public final double highestDownloadSpeedKBs;

    public final TrafficDirection trafficDirection;

    public NetworkTrafficStats(long totalBytesRx, long totalBytesTx, long totalPacketRx, long totalPacketsTx, double uploadSpeedKBs,
                               double downloadSpeedKBs, double highestUploadSpeedKBs, double highestDownloadSpeedKBs, TrafficDirection trafficDirection) {
        this.totalBytesRx = totalBytesRx;
        this.totalBytesTx = totalBytesTx;
        this.totalPacketsRx = totalPacketRx;
        this.totalPacketsTx = totalPacketsTx;
        this.uploadSpeedKBs = uploadSpeedKBs;
        this.downloadSpeedKBs = downloadSpeedKBs;
        this.highestUploadSpeedKBs = highestUploadSpeedKBs;
        this.highestDownloadSpeedKBs = highestDownloadSpeedKBs;
        this.trafficDirection = trafficDirection;
    }

    public NetworkTrafficStats() {
        this(0, 0, 0, 0, 0, 0, 0, 0, TrafficDirection.NONE);
    }

    @Override
    public String toString() {
        return "NetworkTrafficStats [totalBytesRx=" + totalBytesRx + ", totalBytesTx=" + totalBytesTx + ", totalPacketsRx="
                + totalPacketsRx + ", totalPacketsTx=" + totalPacketsTx + ", uploadSpeedKBs=" + uploadSpeedKBs + ", downloadSpeedKBs="
                + downloadSpeedKBs + ", highestUploadSpeedKBs=" + highestUploadSpeedKBs + ", highestDownloadSpeedKBs="
                + highestDownloadSpeedKBs + ", trafficDirection=" + trafficDirection + "]";
    }

}