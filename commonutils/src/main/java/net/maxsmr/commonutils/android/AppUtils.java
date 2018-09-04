package net.maxsmr.commonutils.android;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import net.maxsmr.commonutils.data.FileHelper;
import net.maxsmr.commonutils.logger.BaseLogger;
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder;
import net.maxsmr.commonutils.shell.CommandResult;
import net.maxsmr.commonutils.shell.RootShellCommands;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static net.maxsmr.commonutils.shell.RootShellCommands.killProcess;

public final class AppUtils {

    private static final BaseLogger logger = BaseLoggerHolder.getInstance().getLogger(AppUtils.class);

    private AppUtils() {
        throw new AssertionError("no instances");
    }

    @Nullable
    public static ApplicationInfo getApplicationInfo(@NonNull Context context, String packageName) {
        return getApplicationInfo(context, packageName, 0);
    }


    @Nullable
    public static ApplicationInfo getApplicationInfo(@NonNull Context context, String packageName, int flags) {
        PackageManager packageManager = context.getPackageManager();
        try {
            return packageManager.getApplicationInfo(packageName, flags);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    @Nullable
    public static PackageInfo getPackageInfo(@NonNull Context context, String packageName) {
        return getPackageInfo(context, packageName, 0);
    }

    @Nullable
    public static PackageInfo getPackageInfo(@NonNull Context context, String packageName, int flags) {
        PackageManager packageManager = context.getPackageManager();
        if (!TextUtils.isEmpty(packageName)) {
            try {
                return packageManager.getPackageInfo(packageName, flags);
            } catch (PackageManager.NameNotFoundException e) {
                logger.e("a NameNotFoundException occurred during getPackageInfo(): " + e.getMessage(), e);
            }
        }
        return null;
    }

    @Nullable
    public static PackageInfo getArchivePackageInfo(@NonNull Context context, File apkFile) {
        return getArchivePackageInfo(context, apkFile, 0);
    }

    @Nullable
    public static PackageInfo getArchivePackageInfo(@NonNull Context context, File apkFile, int flags) {
        if (FileHelper.isFileCorrect(apkFile)) {
            PackageManager packageManager = context.getPackageManager();
            return packageManager.getPackageArchiveInfo(apkFile.getAbsolutePath(), 0);
        }
        return null;
    }

    /**
     * @return {@linkplain Intent} only if given apk file is installed
     */
    @Nullable
    public static Intent getArchiveLaunchIntentForPackage(@NonNull Context context, File apkFile) {
        PackageInfo packageInfo = getArchivePackageInfo(context, apkFile);
        return packageInfo != null ? getLaunchIntentForPackage(context, packageInfo.packageName) : null;
    }

    @Nullable
    public static Intent getLaunchIntentForPackage(@NonNull Context context, String packageName) {
        PackageManager packageManager = context.getPackageManager();
        if (!TextUtils.isEmpty(packageName)) {
            try {
                return packageManager.getLaunchIntentForPackage(packageName);
            } catch (Exception e) {
                logger.e("an Exception occurred during getLaunchIntentForPackage(): " + e.getMessage(), e);
            }
        }
        return null;
    }

    public static boolean canHandleActivityIntent(@NonNull Context context, @Nullable Intent intent) {
        List<ResolveInfo> resolveInfos = intent != null ? context.getPackageManager().queryIntentActivities(intent, 0) : null;
        return resolveInfos != null && !resolveInfos.isEmpty();
    }

    public static int getPackageActivitiesCount(@NonNull PackageInfo packageInfo) {
        if (packageInfo.activities == null || packageInfo.activities.length == 0) {
            return 0;
        }
        return packageInfo.activities.length;
    }


    /**
     * Get APK build code
     *
     * @param context Context to use
     * @return Build version or 0 if app is not installed
     */
    public static int getApkVersionCode(@NonNull Context context, File apkFile) {
        PackageInfo packageInfo = getArchivePackageInfo(context, apkFile);
        if (packageInfo != null) {
            return packageInfo.versionCode;
        }
        return 0;
    }

    /**
     * Get installed package build code
     *
     * @param context Context to use
     * @return Build version or 0 if app is not installed
     */
    public static int getAppVersionCode(@NonNull Context context, String packageName) {
        PackageInfo packageInfo = getPackageInfo(context, packageName);

        if (packageInfo != null) {
            return packageInfo.versionCode;
        }
        return 0;
    }

    /**
     * Check if app requires update
     */
    public static boolean requiresUpdate(@NonNull Context context, String packageName, int build) {
        int currentVersionCode = getAppVersionCode(context, packageName);
        logger.i("Package: " + packageName + ", current: " + currentVersionCode + ", new: " + build);
        return currentVersionCode > 0 && build > 0 && currentVersionCode != build || (currentVersionCode <= 0 || build <= 0);
    }


    /**
     * Get application title by package name
     *
     * @return null if not found
     */
    @Nullable
    public static String getAppTitle(@NonNull Context context, String packageName) {
        PackageManager packageManager = context.getPackageManager();
        ApplicationInfo info = getApplicationInfo(context, packageName);
        return info != null ? info.loadLabel(packageManager).toString() : null;
    }

    /**
     * @return uid for specified name or -1 if not found
     */
    public static int getApplicationUid(@NonNull Context context, String packageName) {
        ApplicationInfo info = getApplicationInfo(context, packageName);
        return info != null ? info.uid : -1;
    }

    public static boolean isSystemPackage(ApplicationInfo applicationInfo) {
        return (applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
    }

    /**
     * Gets installed packages via pm list command
     * THIS IS WORKAROUND for PackageManager bug — it can suddenly crash if there is too much apps installed
     * There is no exact info about apps count limitations :(
     *
     * @return List of packages
     */
    public static List<String> getInstalledPackagesFromShell() {
        CommandResult commandResult = RootShellCommands.executeCommand("pm list packages");
        if (!commandResult.isSuccessful()) {
            return new ArrayList<>(0);
        }

        String output = commandResult.getStdOut();
        output = output.replace("package:", "");
        String[] packagesArray = output.split("\n");

        List<String> packages = new ArrayList<>(packagesArray.length);
        Collections.addAll(packages, packagesArray);

        return packages;
    }

    /**
     * Check if the given package is installed
     *
     * @param packageName Package to check
     */
    public static boolean isPackageInstalledFromShell(String packageName) {
        return getInstalledPackagesFromShell().contains(packageName);
    }

    public static void killApps(@NonNull Context context, List<String> apps) {
        if (apps == null || apps.isEmpty()) {
            return;
        }

        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

        if (activityManager == null) {
            logger.e("ActivityManager is null!");
            return;
        }

        List<ActivityManager.RunningAppProcessInfo> processes = activityManager.getRunningAppProcesses();

        for (ActivityManager.RunningAppProcessInfo process : processes) {
            if (apps.contains(process.processName)) {
                killProcess(process.pid);
            }
        }
    }
}