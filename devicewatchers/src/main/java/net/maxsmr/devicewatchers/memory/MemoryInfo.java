package net.maxsmr.devicewatchers.memory;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;

public final class MemoryInfo {

    public long totalJvmHeapKb;

    public long maxToExpandJvmHeapKb;

    public long availableJvmHeapKb;

    public long usedJvmHeapKb;

    public long totalSysRamKb;

    public long availableSysRamKb;

    public long thresholdSysRamKb;

    public long usedSysRamKb;

    public boolean isLow;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MemoryInfo that = (MemoryInfo) o;

        if (totalJvmHeapKb != that.totalJvmHeapKb) return false;
        if (maxToExpandJvmHeapKb != that.maxToExpandJvmHeapKb) return false;
        if (availableJvmHeapKb != that.availableJvmHeapKb) return false;
        if (usedJvmHeapKb != that.usedJvmHeapKb) return false;
        if (totalSysRamKb != that.totalSysRamKb) return false;
        if (availableSysRamKb != that.availableSysRamKb) return false;
        if (thresholdSysRamKb != that.thresholdSysRamKb) return false;
        if (usedSysRamKb != that.usedSysRamKb) return false;
        return isLow == that.isLow;
    }

    @Override
    public int hashCode() {
        int result = (int) (totalJvmHeapKb ^ (totalJvmHeapKb >>> 32));
        result = 31 * result + (int) (maxToExpandJvmHeapKb ^ (maxToExpandJvmHeapKb >>> 32));
        result = 31 * result + (int) (availableJvmHeapKb ^ (availableJvmHeapKb >>> 32));
        result = 31 * result + (int) (usedJvmHeapKb ^ (usedJvmHeapKb >>> 32));
        result = 31 * result + (int) (totalSysRamKb ^ (totalSysRamKb >>> 32));
        result = 31 * result + (int) (availableSysRamKb ^ (availableSysRamKb >>> 32));
        result = 31 * result + (int) (thresholdSysRamKb ^ (thresholdSysRamKb >>> 32));
        result = 31 * result + (int) (usedSysRamKb ^ (usedSysRamKb >>> 32));
        result = 31 * result + (isLow ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("MemoryInfo [ JVM heap | ");
        sb.append(" total: ").append(totalJvmHeapKb).append(" kB");
        sb.append(", available: ").append(availableJvmHeapKb).append(" kB");
        sb.append(", used: ").append(usedJvmHeapKb).append(" kB");
        sb.append(", max to expand: ").append(maxToExpandJvmHeapKb).append(" kB ], ");
        sb.append("[ System RAM | ");
        sb.append(" total: ").append(totalSysRamKb).append(" kB");
        sb.append(", available: ").append(availableSysRamKb).append(" kB");
        sb.append(", threshold: ").append(thresholdSysRamKb).append(" kB");
        sb.append(", used: ").append(usedSysRamKb).append(" kB");
        sb.append(", isLow: ").append(isLow).append(" kB ]");
        return sb.toString();
    }


    public static long getTotalSystemMemory(Context ctx) {

        ActivityManager activityManager = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            return memoryInfo.totalMem;
        } else {
            return 0;
        }
    }

    public static long getAvailableSystemMemory(Context ctx) {

        ActivityManager activityManager = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        android.app.ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);

        return memoryInfo.availMem;
    }

    public static MemoryInfo makeMemoryInfo(Context ctx) {
        MemoryInfo memInfo = new MemoryInfo();

        memInfo.totalJvmHeapKb = (Runtime.getRuntime().totalMemory() / 1024);
        memInfo.maxToExpandJvmHeapKb = (Runtime.getRuntime().maxMemory() / 1024);
        memInfo.availableJvmHeapKb = (Runtime.getRuntime().freeMemory() / 1024);
        memInfo.usedJvmHeapKb = ((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024);

        ActivityManager activityManager = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        android.app.ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            memInfo.totalSysRamKb = (memoryInfo.totalMem / 1024);
        }
        memInfo.availableSysRamKb = (memoryInfo.availMem / 1024);
        memInfo.thresholdSysRamKb = (memoryInfo.threshold / 1024);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            memInfo.usedSysRamKb = ((memoryInfo.totalMem - memoryInfo.availMem) / 1024);
        }
        memInfo.isLow = (memoryInfo.lowMemory);

        return memInfo;
    }

}
