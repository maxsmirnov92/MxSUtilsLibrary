package net.maxsmr.commonutils.processmanager;

import android.app.ActivityManager;
import android.content.Context;

import net.maxsmr.commonutils.processmanager.model.ProcessInfo;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static net.maxsmr.commonutils.AppUtilsKt.getApplicationLabel;

/**
 *
 * ProcessManager using default Android API:
 * {@linkplain ActivityManager#getRunningAppProcesses()}
 *
 * Working on all devices, but almost useless on >= Lollipop
 * */
public class DefaultProcessManager extends AbstractProcessManager {

    private final ActivityManager activityManager;

    public DefaultProcessManager(@NotNull Context context) {
        super(context);
        activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
    }

    @NotNull
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

        return new ProcessInfo(
                runningAppProcessInfo.processName,
                getApplicationLabel(context, runningAppProcessInfo.processName),
                runningAppProcessInfo.pid,
                0,
                String.valueOf(runningAppProcessInfo.uid),
                runningAppProcessInfo.uid,
                0,
                0,
                null,
                runningAppProcessInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND,
                isSystemPackage(runningAppProcessInfo.processName)
        );

    }
}
