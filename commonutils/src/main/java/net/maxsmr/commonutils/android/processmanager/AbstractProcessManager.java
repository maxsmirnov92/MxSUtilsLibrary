package net.maxsmr.commonutils.android.processmanager;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;

import net.maxsmr.commonutils.logger.BaseLogger;
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractProcessManager {

    protected final BaseLogger logger = BaseLoggerHolder.getInstance().getLogger(getLoggerClass());

    @NonNull
    protected final Context context;

    @NonNull
    protected final PackageManager packageManager;

    private final List<String> installedPackages = new ArrayList<>();
    private final List<String> systemPackages = new ArrayList<>();

    AbstractProcessManager(@NonNull Context context) {
        this.context = context;
        this.packageManager = context.getPackageManager();
        refreshPackages();
    }

    /**
     * Refresh packages list
     * It's recommended to call this method on every processes list refresh
     */
    protected void refreshPackages() {
        installedPackages.clear();
        systemPackages.clear();

        List<PackageInfo> installedPackagesInfo = packageManager.getInstalledPackages(0);
        for (PackageInfo installedPackage : installedPackagesInfo) {
            installedPackages.add(installedPackage.packageName);

            if ((installedPackage.applicationInfo.flags & (ApplicationInfo.FLAG_UPDATED_SYSTEM_APP | ApplicationInfo.FLAG_SYSTEM)) > 0) {
                systemPackages.add(installedPackage.packageName);
            }
        }
    }

    /**
     * Checks if package is installed
     * Useful for parsing ps output — there are processes that look like valid packages, but not installed
     */
    protected boolean isPackageInstalled(@NonNull String packageName) {
        return installedPackages.contains(packageName);
    }

    /**
     * Checks if package is marked as system
     */
    protected boolean isSystemPackage(@NonNull String packageName) {
        return systemPackages.contains(packageName);
    }

    @NonNull
    public abstract List<ProcessInfo> getProcesses(boolean includeSystemPackages);

    @NonNull
    private Class<?> getLoggerClass() {
        return getClass();
    }
}
