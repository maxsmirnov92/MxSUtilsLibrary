package net.maxsmr.commonutils.android;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import net.maxsmr.commonutils.android.processmanager.AbstractProcessManager;
import net.maxsmr.commonutils.android.processmanager.ProcessInfo;
import net.maxsmr.commonutils.data.FileHelper;
import net.maxsmr.commonutils.logger.BaseLogger;
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder;
import net.maxsmr.commonutils.shell.CommandResult;
import net.maxsmr.commonutils.shell.RootShellCommands;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static net.maxsmr.commonutils.shell.RootShellCommands.killProcessByPid;

public final class AppUtils {

    private static final BaseLogger logger = BaseLoggerHolder.getInstance().getLogger(AppUtils.class);

    private AppUtils() {
        throw new AssertionError("no instances");
    }

    public static boolean isPackageInstalled(@NotNull Context context, String packageName) {
        return getApplicationInfo(context, packageName) != null;
    }

    @Nullable
    public static ApplicationInfo getApplicationInfo(@NotNull Context context, String packageName) {
        return getApplicationInfo(context, packageName, 0);
    }

    @Nullable
    public static ApplicationInfo getApplicationInfo(@NotNull Context context, String packageName, int flags) {
        PackageManager packageManager = context.getPackageManager();
        try {
            return packageManager.getApplicationInfo(packageName, flags);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    @Nullable
    public static PackageInfo getPackageInfo(@NotNull Context context, String packageName) {
        return getPackageInfo(context, packageName, 0);
    }

    @Nullable
    public static PackageInfo getPackageInfo(@NotNull Context context, String packageName, int flags) {
        PackageManager packageManager = context.getPackageManager();
        if (!TextUtils.isEmpty(packageName)) {
            try {
                return packageManager.getPackageInfo(packageName, flags);
            } catch (PackageManager.NameNotFoundException e) {
            }
        }
        return null;
    }

    @Nullable
    public static PackageInfo getArchivePackageInfo(@NotNull Context context, File apkFile) {
        return getArchivePackageInfo(context, apkFile, 0);
    }

    @Nullable
    public static PackageInfo getArchivePackageInfo(@NotNull Context context, File apkFile, int flags) {
        if (FileHelper.isFileCorrect(apkFile)) {
            PackageManager packageManager = context.getPackageManager();
            return packageManager.getPackageArchiveInfo(apkFile.getAbsolutePath(), flags);
        }
        return null;
    }

    @Nullable
    public static Intent getLaunchIntentForPackage(@NotNull Context context, String packageName) {
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

    /**
     * @return {@linkplain Intent} only if given apk file is installed
     * */
    @Nullable
    public static Intent getArchiveLaunchIntentForPackage(@NotNull Context context, File apkFile) {
        PackageInfo packageInfo = getArchivePackageInfo(context, apkFile);
        return packageInfo != null? getLaunchIntentForPackage(context, packageInfo.packageName) : null;
    }

    @Nullable
    public static Intent getSelfLaunchIntentForPackage(@NotNull Context context) {
        return getLaunchIntentForPackage(context, context.getPackageName());
    }

    @Nullable
    public static Intent getViewFileIntent(@NotNull Context context, @Nullable File file) {
        Intent result = null;
        if (file != null) {
            result = new Intent(Intent.ACTION_VIEW);
            String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(FileHelper.getFileExtension(file));
            result.setDataAndType(Uri.fromFile(file), mimeType);
            result.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (!canHandleActivityIntent(context, result)) {
                result = null;
            }
        }
        return result;
    }

    public static boolean canHandleActivityIntent(@NotNull Context context, @Nullable Intent intent) {
        List<ResolveInfo> resolveInfos = intent != null ? context.getPackageManager().queryIntentActivities(intent, 0) : null;
        return resolveInfos != null && !resolveInfos.isEmpty();
    }

    public static int getPackageActivitiesCount(@NotNull PackageInfo packageInfo) {
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
    public static int getApkVersionCode(@NotNull Context context, File apkFile) {
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
    public static int getAppVersionCode(@NotNull Context context, String packageName) {
        PackageInfo packageInfo = getPackageInfo(context, packageName);

        if (packageInfo != null) {
            return packageInfo.versionCode;
        }
        return 0;
    }

    /**
     * Check if app requires update
     */
    public static boolean requiresUpdate(@NotNull Context context, String packageName, int build) {
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
    public static String getAppTitle(@NotNull Context context, String packageName) {
        PackageManager packageManager = context.getPackageManager();
        ApplicationInfo info = getApplicationInfo(context, packageName);
        return info != null ? info.loadLabel(packageManager).toString() : null;
    }

    /**
     * @return uid for specified name or -1 if not found
     */
    public static int getApplicationUid(@NotNull Context context, String packageName) {
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

    public static boolean isAppInBackground(@NotNull Context context, String packageName) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (am == null) {
            throw new RuntimeException(ActivityManager.class.getSimpleName() + " is null");
        }
        boolean isInBackground = true;
        if (!TextUtils.isEmpty(packageName)) {
            List<ActivityManager.RunningAppProcessInfo> runningProcesses = am.getRunningAppProcesses();
            for (ActivityManager.RunningAppProcessInfo processInfo : runningProcesses) {
                if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                    if (packageName.equals(processInfo.processName)) {
                        isInBackground = false;
                        break;
                    }
                }
            }
        }
        return isInBackground;
    }

    public static boolean isAppInBackground(String packageName,
                                            @NotNull AbstractProcessManager manager, boolean includeSystemPackages) {
        boolean isInBackground = true;
        if (!TextUtils.isEmpty(packageName)) {
            for (ProcessInfo info : manager.getProcesses(includeSystemPackages)) {
                if (info.isForeground) {
                    isInBackground = false;
                    break;
                }
            }
        }
        return isInBackground;
    }

    /**
     * @return -1 if not found
     */
    public static int getPidByName(@Nullable String packageName,
                                   @NotNull AbstractProcessManager manager, boolean includeSystemPackages) {

        int pid = -1;

        if (!TextUtils.isEmpty(packageName)) {
            for (ProcessInfo process : manager.getProcesses(includeSystemPackages)) {
                if (packageName.equals(process.packageName)) {
                    pid = process.pid;
                    break;
                }
            }
        }

        return pid;
    }

    /**
     * @return false if at least one kill was failed
     */
    public static boolean killApps(@Nullable List<String> apps,
                                   @NotNull AbstractProcessManager manager, boolean includeSystemPackages) {
        boolean result = true;
        if (apps != null && !apps.isEmpty()) {
            for (ProcessInfo process : manager.getProcesses(includeSystemPackages)) {
                if (apps.contains(process.packageName)) {
                    if (!killProcessByPid(process.pid)) {
                        result = false;
                    }
                }
            }
        }
        return result;
    }

}
