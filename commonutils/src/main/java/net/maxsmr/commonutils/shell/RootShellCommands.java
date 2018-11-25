package net.maxsmr.commonutils.shell;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;

import net.maxsmr.commonutils.android.AppUtils;
import net.maxsmr.commonutils.android.processmanager.AbstractProcessManager;
import net.maxsmr.commonutils.android.processmanager.model.ProcessInfo;
import net.maxsmr.commonutils.data.CompareUtils;
import net.maxsmr.commonutils.data.FileHelper;
import net.maxsmr.commonutils.data.Predicate;
import net.maxsmr.commonutils.logger.BaseLogger;
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class RootShellCommands {

    private final static BaseLogger logger = BaseLoggerHolder.getInstance().getLogger(RootShellCommands.class);

    public RootShellCommands() {
        throw new AssertionError("no instances.");
    }

    /**
     * Execute SU command
     */
    @NotNull
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
    @Nullable
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
     * Gets installed packages via pm list command
     * THIS IS WORKAROUND for PackageManager bug — it can suddenly crash if there is too much apps installed
     * There is no exact info about apps count limitations :(
     *
     * @return List of packages
     */
    public static List<String> getInstalledPackages() {
        CommandResult commandResult = RootShellCommands.executeCommand("pm list packages");
        if (!commandResult.isSuccessful()) {
            return new ArrayList<>(0);
        }
        List<String> packages = new ArrayList<>();
        for (String out : commandResult.getStdOutLines()) {
            if (!TextUtils.isEmpty(out)) {
                packages.add(out.replaceFirst("package:", ""));
            }
        }
        return packages;
    }

    /**
     * Check if the given package is installed
     *
     * @param packageName Package to check
     */
    public static boolean isPackageInstalled(String packageName) {
        return getInstalledPackages().contains(packageName);
    }

    /**
     * Install the given APK file via the Package manager
     *
     * @param packageName If we need to update existing apk, you can pass the package name
     */
    public static boolean installApk(File apkFile, @Nullable String packageName, @NotNull Context context) {

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
                    && isPackageInstalled(packageName)) {
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
        return isInstallOrUninstallSuccess(commandResult);

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
        return isInstallOrUninstallSuccess(commandResult);
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
        return killProcessesByName(processName, manager, includeSystemPackages, CompareUtils.MatchStringOption.EQUALS.flag);
    }

    public static boolean killProcessesByName(@Nullable String processName,
                                              @NotNull AbstractProcessManager manager, boolean includeSystemPackages, int matchFlags) {
        final Map<Integer, Boolean> statusMap = killProcessesByNameWithStatus(processName, manager, includeSystemPackages, matchFlags);
        boolean result = true;
        if (statusMap != null) {
            for (Boolean status : statusMap.values()) {
                if (status == null || !status) {
                    result = false;
                    break;
                }
            }
        }
        return result;
    }

    /** @return null if not found, map with PID - kill result for PID otherwise */
    @Nullable
    public static Map<Integer, Boolean> killProcessesByNameWithStatus(@Nullable String processName,
                                                                      @NotNull AbstractProcessManager manager, boolean includeSystemPackages) {
        return killProcessesByNameWithStatus(processName, manager, includeSystemPackages, CompareUtils.MatchStringOption.EQUALS.flag);
    }

    /** @return null if not found, map with PID - kill result for PID otherwise */
    @Nullable
    public static Map<Integer, Boolean> killProcessesByNameWithStatus(@Nullable String processName,
                                                                      @NotNull AbstractProcessManager manager, boolean includeSystemPackages, int matchFlags) {
        final Map<Integer, Boolean> result = new LinkedHashMap<>();
        final Set<Integer> pids = AppUtils.getPidsByName(processName, manager, includeSystemPackages, matchFlags);
        for (Integer pid : pids) {
            if (pid > 0) {
                result.put(pid, RootShellCommands.killProcessByPid(pid));
            }
        }
        if (result.isEmpty()) {
            return null;
        }
        return result;
    }

    /**
     * @return false if at least one kill was failed
     */
    public static boolean killProcessesByNames(@Nullable List<String> processNames,
                                               @NotNull AbstractProcessManager manager, boolean includeSystemPackages) {
        return killProcessesByNames(processNames, manager, includeSystemPackages, CompareUtils.MatchStringOption.EQUALS.flag);
    }

    /**
     * @return false if at least one kill was failed
     */
    public static boolean killProcessesByNames(@Nullable List<String> processNames,
                                               @NotNull AbstractProcessManager manager, boolean includeSystemPackages, int matchFlags) {
        boolean result = true;
        if (processNames != null && !processNames.isEmpty()) {
            final List<ProcessInfo> runningProcesses = manager.getProcesses(includeSystemPackages);
            for (ProcessInfo process : runningProcesses) {
                if (Predicate.Methods.contains(processNames,
                        element -> CompareUtils.stringsMatch(process.packageName, element, matchFlags))) {
                    if (!killProcessByPid(process.pid)) {
                        result = false;
                    }
                }
            }
        }
        return result;
    }

    public static boolean isInstallOrUninstallSuccess(CommandResult commandResult) {
        boolean isSuccess = false;
        if (commandResult.isSuccessful()) {
            isSuccess = isInstallOrUninstallSuccess(commandResult.getStdOutLines());
            if (!isSuccess) {
                isSuccess = isInstallOrUninstallSuccess(commandResult.getStdErrLines());
            }
        }
        return isSuccess;

    }

    private static boolean isInstallOrUninstallSuccess(List<String> std) {
        return Predicate.Methods.contains(std, s -> !TextUtils.isEmpty(s) && s.toLowerCase().startsWith("success"));
    }

    @Nullable
    public static String getInstallFailErrString(CommandResult commandResult) {
        String failure = getInstallFailErrString(commandResult.getStdErrLines());
        if (TextUtils.isEmpty(failure)) {
            failure = getInstallFailErrString(commandResult.getStdOutLines());
        }
        return failure;
    }

    @Nullable
    private static String getInstallFailErrString(List<String> std) {
        return Predicate.Methods.find(std, s -> !TextUtils.isEmpty(s) && s.toLowerCase().startsWith("failure"));
    }
}
