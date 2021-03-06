package net.maxsmr.commonutils.processmanager;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import net.maxsmr.commonutils.logger.BaseLogger;
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder;
import net.maxsmr.commonutils.processmanager.model.ProcessInfo;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static net.maxsmr.commonutils.AppUtilsKt.isSystemApp;

public abstract class AbstractProcessManager {

    protected final BaseLogger logger = BaseLoggerHolder.getInstance().getLogger(getLoggerClass());

    @NotNull
    protected final Context context;

    @NotNull
    protected final PackageManager packageManager;

    private final List<String> installedPackages = new ArrayList<>();
    private final List<String> systemPackages = new ArrayList<>();

    protected AbstractProcessManager(@NotNull Context context) {
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
            if (isSystemApp(installedPackage)) {
                systemPackages.add(installedPackage.packageName);
            }
        }
    }

    /**
     * Checks if package is installed
     * Useful for parsing ps output — there are processes that look like valid packages, but not installed
     */
    protected boolean isPackageInstalled(@NotNull String packageName) {
        return installedPackages.contains(packageName);
    }

    /**
     * Checks if package is marked as system
     */
    protected boolean isSystemPackage(@NotNull String packageName) {
        return systemPackages.contains(packageName);
    }

    @NotNull
    public abstract List<ProcessInfo> getProcesses(boolean includeSystemPackages);

    @NotNull
    private Class<?> getLoggerClass() {
        return getClass();
    }
}
