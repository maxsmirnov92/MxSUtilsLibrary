package net.maxsmr.commonutils.shell;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import net.maxsmr.commonutils.android.AppUtils;
import net.maxsmr.commonutils.android.processmanager.AbstractProcessManager;import net.maxsmr.commonutils.android.processmanager.model.ProcessInfo;
import net.maxsmr.commonutils.data.FileHelper;
import net.maxsmr.commonutils.logger.BaseLogger;
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;
import java.util.Set;

public final class RootShellCommands {

    private final static BaseLogger logger = BaseLoggerHolder.getInstance().getLogger(RootShellCommands.class);

    public RootShellCommands() {
        throw new AssertionError("no instances.");
    }

    /**
     * Execute SU command
     */
    @NonNull
    public static CommandResult executeCommand(String command) {

        try {

            ShellWrapper rootShell = new ShellWrapper(false);
            CommandResult commandResult = rootShell.executeCommand(command, true);
            rootShell.dispose();
            return commandResult;

        } catch (RuntimeException e) {
            logger.e("Failed to execute command \"" + command + "\": " + e.getMessage(), e);
        }

        return new CommandResult().setExitCode(-1);
    }

    /**
     * Check if root is available
     */
    public static boolean isRootAvailable() {
        CommandResult commandResult = executeCommand("id");
        return commandResult.isSuccessful() && commandResult.getStdOut().toLowerCase().contains("uid=0");
    }

    /**
     * Remount the /system in RW mode
     *
     * @return Output
     */
    public static boolean remountSystemRw() {
        String command = "mount -o remount,rw /system";
        return executeCommand(command).isSuccessful();
    }

    /**
     * Creates the directory (with parent directories)
     *
     * @param path Path to create
     * @return Output
     */
    public static boolean createDirectory(String path) {
        String command = String.format("mkdir -p %s", path);
        return executeCommand(command).isSuccessful();
    }

    /**
     * Chmod the file
     *
     * @param file File to chmod
     * @param mode Mode to set
     * @return Output
     */
    public static boolean chmod(String file, String mode) {
        String command = String.format("chmod %s %s", mode, file);
        return executeCommand(command).isSuccessful();
    }

    /**
     * Read the given file as root
     *
     * @param file File to read from
     * @return File's contents
     */
    @android.support.annotation.Nullable
    public static String readFileAsRoot(File file) {
        String command = String.format("cat %s", file.getAbsolutePath());

        CommandResult commandResult = executeCommand(command);

        if (!commandResult.isSuccessful()) {
            return null;
        }

        return commandResult.getStdOut();
    }

    /**
     * Write the given data to file as root
     *
     * @param data Data to write
     * @param file File to write to
     */
    public static boolean writeToFileAsRoot(String data, File file) {
        String command = String.format("echo '%s' > %s", data, file.getAbsolutePath());
        return executeCommand(command).isSuccessful();
    }

    public static boolean reboot() {
        return executeCommand("reboot").isSuccessful();
    }

    /**
     * Install the given APK file via the Package manager
     *
     * @param packageName If we need to update existing apk, you can pass the package name
     */
    public static boolean installApk(File apkFile, @android.support.annotation.Nullable String packageName, @NotNull Context context) {

        if (!FileHelper.isFileCorrect(apkFile)) {
            logger.e("Cannot install: incorrect apk file: " + apkFile);
            return false;
        }

        String command;
        CommandResult commandResult;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            command = String.format("pm install -r -d %s", apkFile.getAbsolutePath());
        } else {
            if (!TextUtils.isEmpty(packageName)
                    && !packageName.equals(context.getPackageName())
                    && AppUtils.isPackageInstalledFromShell(packageName)) {
                command = String.format("pm uninstall -k %s", packageName);
                commandResult = executeCommand(command);
                logger.i("Uninstall result: " + commandResult);
                if (!commandResult.isSuccessful()) {
                    return false;
                }
            }
            command = String.format("pm install -r %s", apkFile.getAbsolutePath());
        }

        commandResult = executeCommand(command);

        logger.i("Install result: " + commandResult);

        return commandResult.isSuccessful() && commandResult.getStdOut().toLowerCase().contains("success");

    }

    /**
     * Try to install apk until success or max retries reached (workaround for some devices)
     * @param maxRetriesCount 0 if install attempts should be infinite
     */
    public static boolean installApkUntilSuccess(int maxRetriesCount, File apkFile, @Nullable String packageName, @NotNull Context context) {
        if (maxRetriesCount < 0) {
            throw new IllegalArgumentException("Incorrect max retries count: " + maxRetriesCount);
        }
        boolean result = false;
        int tryCount = 0;
        while (!result && maxRetriesCount == 0
                || tryCount > maxRetriesCount) {
            if (installApk(apkFile, packageName, context)) {
                result = true;
            }
            tryCount++;
        }
        return result;
    }

    /**
     * Uninstall the given package via the Package manager
     */
    public static boolean uninstallPackage(String packageName) {

        if (TextUtils.isEmpty(packageName)) {
            logger.e("Cannot uninstall app: package name is empty");
            return false;
        }

        String command = "pm uninstall " + packageName;

        CommandResult commandResult = executeCommand(command);

        logger.i("uninstall result: " + commandResult);

        if (commandResult.isSuccessful()) {
            return commandResult.getStdOut().toLowerCase().contains("success");
        }

        return false;
    }


    /**
     * Try to uninstall apk until success or max retries reached (workaround for some devices)
     * @param maxRetriesCount 0 if uninstall attempts should be infinite
     */
    public static boolean uninstallPackageUntilSuccess(int maxRetriesCount, String packageName) {
        if (maxRetriesCount < 0) {
            throw new IllegalArgumentException("Incorrect max retries count: " + maxRetriesCount);
        }
        boolean result = false;
        int tryCount = 0;
        while (!result && maxRetriesCount == 0
                || tryCount > maxRetriesCount) {
            if (uninstallPackage(packageName)) {
                result = true;
            }
            tryCount++;
        }
        return result;
    }

    public static boolean killProcessByPid(int pid) {
        return RootShellCommands.executeCommand("kill -9 " + pid).isSuccessful();
    }

    public static boolean killProcessesByName(@Nullable String processName,
                                              @NotNull AbstractProcessManager manager, boolean includeSystemPackages) {
        boolean result = true;
        final Set<Integer> pids = AppUtils.getPidsByName(processName, manager, includeSystemPackages);
        for (Integer pid : pids) {
            result &= RootShellCommands.killProcessByPid(pid);
        }
        return result;
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
