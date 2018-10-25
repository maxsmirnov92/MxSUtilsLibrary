package net.maxsmr.networkutils.monitor;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.TrafficStats;
import org.jetbrains.annotations.NotNull;

import static net.maxsmr.networkutils.monitor.NetworkStatsHelper.isNetworkStatsManagerSupported;


public final class TrafficStatsDelegate {

    public TrafficStatsDelegate() {
        throw new AssertionError("no instances");
    }

    public static boolean shouldUseTrafficStats(int uid) {
        final int callingUid = android.os.Process.myUid();
        return !isNetworkStatsManagerSupported() || callingUid == android.os.Process.SYSTEM_UID || uid > 0 && callingUid == uid;
    }

    @SuppressLint("InlinedApi")
    @SuppressWarnings("ConstantConditions")
    public static long getUidRxBytes(int uid, @NotNull Context context) {
        if (shouldUseTrafficStats(uid)) {
            return TrafficStats.getUidRxBytes(uid);
        } else {
            return new NetworkStatsHelper(context, uid).getPackageRxBytes(context);
        }
    }

    @SuppressLint("InlinedApi")
    @SuppressWarnings("ConstantConditions")
    public static long getUidTxBytes(int uid, @NotNull Context context) {
        if (shouldUseTrafficStats(uid)) {
            return TrafficStats.getUidTxBytes(uid);
        } else {
            return new NetworkStatsHelper(context, uid).getPackageTxBytes(context);
        }
    }


}
