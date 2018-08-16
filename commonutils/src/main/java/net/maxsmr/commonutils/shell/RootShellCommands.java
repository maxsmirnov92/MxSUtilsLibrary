package net.maxsmr.commonutils.shell;

import android.os.Build;
import android.support.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;

public class RootShellCommands {

    private final static Logger logger = LoggerFactory.getLogger(RootShellCommands.class);

    private static final int MAX_INSTALL_RETRIES = 20;
    private static final int MAX_UNINSTALL_RETRIES = 20;

    /**
     * Execute SU command
     */
    @Nullable
    public static CommandResult executeCommand(String command) {

        try {

            ShellWrapper rootShell = new ShellWrapper();
            CommandResult commandResult = rootShell.executeCommand(command, true);
            rootShell.dispose();
            return commandResult;

        } catch (RuntimeException e) {
            logger.error("Failed to execute command \"" + command + "\": " + Arrays.asList(e.getStackTrace()));
        }

        return null;
    }

    /**
     * Check if root is available
     */
    public static boolean isRootAvailable() {
        CommandResult commandResult = executeCommand("id");
        return commandResult != null && commandResult.isSuccessful() && commandResult.getStdOut().toLowerCase().contains("uid=0");

    }

    /**
     * Remount the /system in RW mode
     *
     * @return Output
     */
    public static boolean remountSystemRw() {
        String command = "mount -o remount,rw /system";
        CommandResult commandResult = executeCommand(command);

        return commandResult != null && commandResult.isSuccessful();
    }

    /**
     * Creates the directory (with parent directories)
     *
     * @param path Path to create
     * @return Output
     */
    public static boolean createDirectory(String path) {
        String command = String.format("mkdir -p %s", path);
        CommandResult commandResult = executeCommand(command);

        return commandResult != null && commandResult.isSuccessful();
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

        CommandResult commandResult = executeCommand(command);

        return commandResult != null && commandResult.isSuccessful();
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

        if (commandResult == null || !commandResult.isSuccessful()) {
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

        CommandResult commandResult = executeCommand(command);

        return commandResult != null && commandResult.isSuccessful();
    }

    /**
     * Reboot the device
     */
    public static boolean reboot() {
        CommandResult commandResult = executeCommand("reboot");

        return commandResult != null && commandResult.isSuccessful();
    }

    /**
     * Install the given APK file via the Package manager
     *
     * @param packageName If we need to update existing apk, you can pass the package name
     */
    public static boolean installApk(File apkFile, @Nullable String packageName) {
        String command;

        CommandResult commandResult;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            command = String.format("pm install -r -d %s", apkFile.getAbsolutePath());
        } else {
            if (packageName != null) {
                command = String.format("pm uninstall -k %s", packageName);
                commandResult = executeCommand(command);
                logger.info( "uninstall result: " + commandResult);
            }

            command = String.format("pm install -r %s", apkFile.getAbsolutePath());
        }

        commandResult = executeCommand(command);

        logger.info( "install result: " + commandResult);

        if (commandResult != null && commandResult.isSuccessful()) {
            return commandResult.getStdOut().toLowerCase().contains("success");
        }

        return false;
    }

    /**
     * Try to install apk until success or max retries reached (workaround for some devices)
     */
    public static boolean installApkUntilSuccess(File apkFile, @Nullable String packageName) {
        for (int i = 0; i < MAX_INSTALL_RETRIES; i++) {
            if (installApk(apkFile, packageName)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Uninstall the given package via the Package manager
     */
    public static boolean uninstallPackage(String packageName) {
        String command = "pm uninstall " + packageName;

        CommandResult commandResult = executeCommand(command);

        logger.info( "uninstall result: " + commandResult);

        if (commandResult != null && commandResult.isSuccessful()) {
            return commandResult.getStdOut().toLowerCase().contains("success");
        }

        return false;
    }


    /**
     * Try to uninstall apk until success or max retries reached (workaround for some devices)
     */
    public static boolean uninstallPackageUntilSuccess(String packageName) {
        for (int i = 0; i < MAX_UNINSTALL_RETRIES; i++) {
            if (uninstallPackage(packageName)) {
                return true;
            }
        }

        return false;
    }

}
