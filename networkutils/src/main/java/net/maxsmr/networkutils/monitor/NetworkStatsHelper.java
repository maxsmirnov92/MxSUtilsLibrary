package net.maxsmr.networkutils.monitor;

import android.annotation.TargetApi;
import android.app.usage.NetworkStats;
import android.app.usage.NetworkStatsManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Build;

import net.maxsmr.commonutils.android.hardware.DeviceUtils;
import net.maxsmr.commonutils.logger.BaseLogger;
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder;

import org.jetbrains.annotations.NotNull;

import static net.maxsmr.commonutils.android.AppUtilsKt.getApplicationUid;


/**
 * only for apps which has allowed sharing stats to other apps
 */
@TargetApi(Build.VERSION_CODES.M)
public final class NetworkStatsHelper {

    private static final BaseLogger logger = BaseLoggerHolder.getInstance().getLogger(NetworkStatsHelper.class);

    @NotNull
    private NetworkStatsManager networkStatsManager;

    private final int packageUid;

    public NetworkStatsHelper(@NotNull Context context) {
        this(context, getApplicationUid(context, context.getPackageName()));
    }

    public NetworkStatsHelper(@NotNull Context context, Integer packageUid) {
        networkStatsManager = (NetworkStatsManager) context.getSystemService(Context.NETWORK_STATS_SERVICE);
        if (networkStatsManager == null) {
            throw new IllegalArgumentException(NetworkStatsManager.class.getSimpleName() + " is null");
        }
        if (packageUid != null) {
            this.packageUid = packageUid;
        } else {
            this.packageUid = -1;
        }
    }

    public long getAllRxBytesMobile(Context context) {
        NetworkStats.Bucket bucket;
        try {
            bucket = networkStatsManager.querySummaryForDevice(ConnectivityManager.TYPE_MOBILE,
                    getSubscriberId(context, ConnectivityManager.TYPE_MOBILE),
                    0,
                    System.currentTimeMillis());
        } catch (Exception e) {
            logger.e("an " + e.getClass().getSimpleName() + " occurred: " + e.getMessage(), e);
            return -1;
        }
        return bucket.getRxBytes();
    }

    public long getAllTxBytesMobile(Context context) {
        NetworkStats.Bucket bucket;
        try {
            bucket = networkStatsManager.querySummaryForDevice(ConnectivityManager.TYPE_MOBILE,
                    getSubscriberId(context, ConnectivityManager.TYPE_MOBILE),
                    0,
                    System.currentTimeMillis());
        } catch (Exception e) {
            logger.e("an " + e.getClass().getSimpleName() + " occurred: " + e.getMessage(), e);
            return -1;
        }
        return bucket.getTxBytes();
    }

    public long getAllRxBytesWifi() {
        NetworkStats.Bucket bucket;
        try {
            bucket = networkStatsManager.querySummaryForDevice(ConnectivityManager.TYPE_WIFI,
                    "",
                    0,
                    System.currentTimeMillis());
        } catch (Exception e) {
            logger.e("an " + e.getClass().getSimpleName() + " occurred: " + e.getMessage(), e);
            return -1;
        }
        return bucket.getRxBytes();
    }

    public long getAllTxBytesWifi() {
        NetworkStats.Bucket bucket;
        try {
            bucket = networkStatsManager.querySummaryForDevice(ConnectivityManager.TYPE_WIFI,
                    "",
                    0,
                    System.currentTimeMillis());
        } catch (Exception e) {
            logger.e("an " + e.getClass().getSimpleName() + " occurred: " + e.getMessage(), e);
            return -1;
        }
        return bucket.getTxBytes();
    }

    public long getPackageRxBytesMobile(Context context) {
        NetworkStats networkStats = null;
        try {
            networkStats = networkStatsManager.queryDetailsForUid(
                    ConnectivityManager.TYPE_MOBILE,
                    getSubscriberId(context, ConnectivityManager.TYPE_MOBILE),
                    0,
                    System.currentTimeMillis(),
                    packageUid);
        } catch (Exception e) {
            logger.e("an " + e.getClass().getSimpleName() + " occurred: " + e.getMessage(), e);
            return -1;
        }

        long rxBytes = 0L;
        NetworkStats.Bucket bucket = new NetworkStats.Bucket();
        while (networkStats.hasNextBucket()) {
            networkStats.getNextBucket(bucket);
            rxBytes += bucket.getRxBytes();
        }
        networkStats.close();
        return rxBytes;
    }

    public long getPackageTxBytesMobile(Context context) {
        NetworkStats networkStats = null;
        try {
            networkStats = networkStatsManager.queryDetailsForUid(
                    ConnectivityManager.TYPE_MOBILE,
                    getSubscriberId(context, ConnectivityManager.TYPE_MOBILE),
                    0,
                    System.currentTimeMillis(),
                    packageUid);
        } catch (Exception e) {
            logger.e("an " + e.getClass().getSimpleName() + " occurred: " + e.getMessage(), e);
            return -1;
        }

        long txBytes = 0L;
        NetworkStats.Bucket bucket = new NetworkStats.Bucket();
        while (networkStats.hasNextBucket()) {
            networkStats.getNextBucket(bucket);
            txBytes += bucket.getTxBytes();
        }
        networkStats.close();
        return txBytes;
    }

    public long getPackageRxBytesWifi() {
        NetworkStats networkStats = null;
        try {
            networkStats = networkStatsManager.queryDetailsForUid(
                    ConnectivityManager.TYPE_WIFI,
                    "",
                    0,
                    System.currentTimeMillis(),
                    packageUid);
        } catch (Exception e) {
            logger.e("an " + e.getClass().getSimpleName() + " occurred: " + e.getMessage(), e);
            return -1;
        }

        long rxBytes = 0L;
        NetworkStats.Bucket bucket = new NetworkStats.Bucket();
        while (networkStats.hasNextBucket()) {
            networkStats.getNextBucket(bucket);
            rxBytes += bucket.getRxBytes();
        }
        networkStats.close();
        return rxBytes;
    }

    public long getPackageTxBytesWifi() {
        NetworkStats networkStats = null;
        try {
            networkStats = networkStatsManager.queryDetailsForUid(
                    ConnectivityManager.TYPE_WIFI,
                    "",
                    0,
                    System.currentTimeMillis(),
                    packageUid);
        } catch (Exception e) {
            logger.e("an " + e.getClass().getSimpleName() + " occurred: " + e.getMessage(), e);
            return -1;
        }

        long txBytes = 0L;
        NetworkStats.Bucket bucket = new NetworkStats.Bucket();
        while (networkStats.hasNextBucket()) {
            networkStats.getNextBucket(bucket);
            txBytes += bucket.getTxBytes();
        }
        networkStats.close();
        return txBytes;
    }

    public long getAllRxBytes(@NotNull Context context) {
        long rxWifiBytes = getAllRxBytesWifi();
        long rxMobileBytes = getAllRxBytesMobile(context);
        rxWifiBytes = rxWifiBytes > 0 ? rxWifiBytes : 0;
        rxMobileBytes = rxMobileBytes > 0 ? rxMobileBytes : 0;
        return rxWifiBytes + rxMobileBytes;
    }

    public long getAllTxBytes(@NotNull Context context) {
        long txWifiBytes = getAllTxBytesWifi();
        long txMobileBytes = getAllTxBytesMobile(context);
        txWifiBytes = txWifiBytes > 0 ? txWifiBytes : 0;
        txMobileBytes = txMobileBytes > 0 ? txMobileBytes : 0;
        return txWifiBytes + txMobileBytes;
    }

    public long getPackageRxBytes(@NotNull Context context) {
        long rxWifiBytes = getPackageRxBytesWifi();
        long rxMobileBytes = getPackageRxBytesMobile(context);
        rxWifiBytes = rxWifiBytes > 0 ? rxWifiBytes : 0;
        rxMobileBytes = rxMobileBytes > 0 ? rxMobileBytes : 0;
        return rxWifiBytes + rxMobileBytes;
    }

    public long getPackageTxBytes(@NotNull Context context) {
        long txWifiBytes = getPackageTxBytesWifi();
        long txMobileBytes = getPackageTxBytesMobile(context);
        txWifiBytes = txWifiBytes > 0 ? txWifiBytes : 0;
        txMobileBytes = txMobileBytes > 0 ? txMobileBytes : 0;
        return txWifiBytes + txMobileBytes;
    }

    public static boolean isNetworkStatsManagerSupported() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    private static String getSubscriberId(@NotNull Context context, int networkType) {
        if (ConnectivityManager.TYPE_MOBILE == networkType) {
            return DeviceUtils.getIMSI(context);
        }
        return "";
    }
}
