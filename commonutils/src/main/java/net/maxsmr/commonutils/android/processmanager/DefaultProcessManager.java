package net.maxsmr.commonutils.android.processmanager;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;


import net.maxsmr.commonutils.android.AppUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Default (pre-lollipop) process manager
 */
public class DefaultProcessManager extends AbstractProcessManager {

    private final ActivityManager activityManager;

    public DefaultProcessManager(@NonNull Context context) {
        super(context);
        activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager == null) {
            throw new RuntimeException(ActivityManager.class.getSimpleName() + " is null");
        }
    }

    @NonNull
    @Override
    public List<ProcessInfo> getProcesses(boolean includeSystemPackages) {
        List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = activityManager.getRunningAppProcesses();

        List<ProcessInfo> processes = new ArrayList<>();

        refreshPackages();

        for (ActivityManager.RunningAppProcessInfo runningAppProcess : runningAppProcesses) {
            ProcessInfo processInfo = parseProcessInfo(runningAppProcess);

            if (processInfo == null) {
                logger.e("Cannot parse process info for '" + runningAppProcess.processName + "'");
                continue;
            }

            if (!includeSystemPackages && processInfo.isSystemApp) {
                logger.e("Process '" + runningAppProcess.processName + "' is system and should be excluded");
                continue;
            }

            processes.add(processInfo);
        }

        return processes;
    }

    @Nullable
    private ProcessInfo parseProcessInfo(ActivityManager.RunningAppProcessInfo runningAppProcessInfo) {

        if (!isPackageInstalled(runningAppProcessInfo.processName)) {
            logger.e("Package '" + runningAppProcessInfo.processName + "' is not installed");
            return null;
        }

        ApplicationInfo info = AppUtils.getApplicationInfo(context, runningAppProcessInfo.processName);

        if (info == null) {
            logger.e("Cannot acquire " + ApplicationInfo.class.getSimpleName() + " for package '" + runningAppProcessInfo.processName);
            return null;
        }

        return new ProcessInfo(
                runningAppProcessInfo.processName,
                info.loadLabel(context.getPackageManager()),
                runningAppProcessInfo.pid,
                String.valueOf(runningAppProcessInfo.uid),
                runningAppProcessInfo.uid,
                0,
                0,
                runningAppProcessInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND,
                isSystemPackage(runningAppProcessInfo.processName)
        );

    }
}
