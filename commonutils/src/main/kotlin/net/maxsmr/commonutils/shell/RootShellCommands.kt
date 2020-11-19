package net.maxsmr.commonutils.shell

import android.content.Context
import net.maxsmr.commonutils.android.getPidsByName
import net.maxsmr.commonutils.android.isAtLeastKitkat
import net.maxsmr.commonutils.android.processmanager.AbstractProcessManager
import net.maxsmr.commonutils.data.MatchStringOption
import net.maxsmr.commonutils.data.Predicate
import net.maxsmr.commonutils.data.isFileValid
import net.maxsmr.commonutils.data.stringsMatch
import net.maxsmr.commonutils.data.text.EMPTY_STRING
import net.maxsmr.commonutils.data.text.isEmpty
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import java.io.File
import java.util.*

private val logger = BaseLoggerHolder.getInstance().getLogger<BaseLogger>("RootShellCommands")

/**
 * Execute SU command
 */
fun executeCommand(command: String): CommandResult {

    try {

        val rootShell = ShellWrapper(false)
        val commandResult = rootShell.executeCommand(command, true)
        rootShell.dispose()
        return commandResult

    } catch (e: RuntimeException) {
        logger.e("Failed to execute command \"" + command + "\": " + e.message, e)
    }

    val commandResult = CommandResult()
    commandResult.exitCode = DEFAULT_FAIL_CODE
    return commandResult
}

/**
 * Check if root is available
 */
val isRootAvailable: Boolean
    get() {
        val commandResult = executeCommand("id")
        return commandResult.isSuccessful
                && commandResult.getStdOut().toLowerCase(Locale.getDefault()).contains("uid=0")
    }

/**
 * Remount the /system in RW mode
 *
 * @return Output
 */
fun remountSystemRw(): Boolean {
    val command = "mount -o remount,rw /system"
    return executeCommand(command).isSuccessful
}

/**
 * Creates the directory (with parent directories)
 *
 * @param path Path to create
 * @return Output
 */
fun createDirectory(path: String): Boolean {
    val command = String.format("mkdir -p %s", path)
    return executeCommand(command).isSuccessful
}

/**
 * Chmod the file
 *
 * @param file File to chmod
 * @param mode Mode to set
 * @return Output
 */
fun chmod(file: String, mode: String): Boolean {
    val command = String.format("chmod %s %s", mode, file)
    return executeCommand(command).isSuccessful
}

/**
 * Read the given file as root
 *
 * @param file File to read from
 * @return File's contents
 */
fun readFileAsRoot(file: File): String? {
    val command = String.format("cat %s", file.absolutePath)
    val commandResult = executeCommand(command)
    return if (!commandResult.isSuccessful) null else commandResult.getStdOut()
}

/**
 * Write the given data to file as root
 *
 * @param data Data to write
 * @param file File to write to
 */
fun writeToFileAsRoot(data: String, file: File): Boolean {
    val command = String.format("echo '%s' > %s", data, file.absolutePath)
    return executeCommand(command).isSuccessful
}

fun reboot() = executeCommand("reboot").isSuccessful

/**
 * Gets installed packages via pm list command
 * THIS IS WORKAROUND for PackageManager bug — it can suddenly crash if there is too much apps installed
 * There is no exact info about apps count limitations :(
 *
 * @return List of packages
 */
val installedPackages: List<String>
    get() {
        val commandResult = executeCommand("pm list packages")
        if (!commandResult.isSuccessful) {
            return ArrayList(0)
        }
        val packages = ArrayList<String>()
        for (out in commandResult.getStdOutLines()) {
            if (!isEmpty(out)) {
                packages.add(out.replaceFirst("package:".toRegex(), ""))
            }
        }
        return packages
    }

/**
 * Check if the given package is installed
 *
 * @param packageName Package to check
 */
fun isPackageInstalled(packageName: String): Boolean =
        installedPackages.contains(packageName)


/**
 * Install the given APK file via the Package manager
 *
 * @param packageName If we need to update existing apk, you can pass the package name
 */
fun installApk(
        apkFile: File,
        packageName: String,
        context: Context
): Boolean {

    if (!isFileValid(apkFile)) {
        logger.e("Cannot install: incorrect apk file: $apkFile")
        return false
    }

    var command: String
    var commandResult: CommandResult

    if (isAtLeastKitkat()) {
        command = String.format("pm install -r -d %s", apkFile.absolutePath)
    } else {
        if (packageName.isNotEmpty()
                && packageName != context.packageName
                && isPackageInstalled(packageName)) {
            command = String.format("pm uninstall -k %s", packageName)
            commandResult = executeCommand(command)
            logger.i("Uninstall result: $commandResult")
            if (!commandResult.isSuccessful) {
                return false
            }
        }
        command = String.format("pm install -r %s", apkFile.absolutePath)
    }

    commandResult = executeCommand(command)

    logger.i("Install result: $commandResult")
    return isInstallOrUninstallSuccess(commandResult)

}

/**
 * Try to install apk until success or max retries reached (workaround for some devices)
 * @param maxRetriesCount 0 if install attempts should be infinite
 */
fun installApkUntilSuccess(
        maxRetriesCount: Int,
        apkFile: File,
        packageName: String,
        context: Context
): Boolean {
    require(maxRetriesCount >= 0) { "Incorrect max retries count: $maxRetriesCount" }
    var result = false
    var tryCount = 0
    while (!result && maxRetriesCount == 0 || tryCount > maxRetriesCount) {
        if (installApk(apkFile, packageName, context)) {
            result = true
        }
        tryCount++
    }
    return result
}

/**
 * Uninstall the given package via the Package manager
 */
fun uninstallPackage(packageName: String): Boolean {

    if (isEmpty(packageName)) {
        logger.e("Cannot uninstall app: package name is empty")
        return false
    }

    val command = "pm uninstall $packageName"

    val commandResult = executeCommand(command)

    logger.i("uninstall result: $commandResult")
    return isInstallOrUninstallSuccess(commandResult)
}

/**
 * Try to uninstall apk until success or max retries reached (workaround for some devices)
 * @param maxRetriesCount 0 if uninstall attempts should be infinite
 */
fun uninstallPackageUntilSuccess(maxRetriesCount: Int, packageName: String): Boolean {
    require(maxRetriesCount >= 0) { "Incorrect max retries count: $maxRetriesCount" }
    var result = false
    var tryCount = 0
    while (!result && maxRetriesCount == 0 || tryCount > maxRetriesCount) {
        if (uninstallPackage(packageName)) {
            result = true
        }
        tryCount++
    }
    return result
}

fun killProcessByPid(pid: Int) =
        executeCommand("kill -9 $pid").isSuccessful


@JvmOverloads
fun killProcessesByName(
        processName: String,
        manager: AbstractProcessManager,
        includeSystemPackages: Boolean,
        matchFlags: Int = MatchStringOption.EQUALS.flag
): Boolean {
    val statusMap = killProcessesByNameWithStatus(processName, manager, includeSystemPackages, matchFlags)
    var result = true
    if (statusMap != null) {
        for (status in statusMap.values) {
            if (!status) {
                result = false
                break
            }
        }
    }
    return result
}

/** @return null if not found, map with PID - kill result for PID otherwise
 */
@JvmOverloads
fun killProcessesByNameWithStatus(
        processName: String,
        manager: AbstractProcessManager,
        includeSystemPackages: Boolean,
        matchFlags: Int = MatchStringOption.EQUALS.flag
): Map<Int, Boolean>? {
    val result = LinkedHashMap<Int, Boolean>()
    val pids = getPidsByName(processName, manager, includeSystemPackages, matchFlags)
    for (pid in pids) {
        if (pid > 0) {
            result[pid] = killProcessByPid(pid)
        }
    }
    return if (result.isEmpty()) null else result
}

/**
 * @return false if at least one kill was failed
 */
@JvmOverloads
fun killProcessesByNames(
        processNames: List<String>?,
        manager: AbstractProcessManager,
        includeSystemPackages: Boolean,
        matchFlags: Int = MatchStringOption.EQUALS.flag
): Boolean {
    var result = true
    if (processNames != null && processNames.isNotEmpty()) {
        val runningProcesses = manager.getProcesses(includeSystemPackages)
        for (process in runningProcesses) {
            if (Predicate.Methods.contains(processNames) { element ->
                        stringsMatch(process.packageName, element, matchFlags)
                    }) {
                if (!killProcessByPid(process.pid)) {
                    result = false
                }
            }
        }
    }
    return result
}

fun isInstallOrUninstallSuccess(commandResult: CommandResult): Boolean {
    var isSuccess = false
    if (commandResult.isSuccessful) {
        isSuccess = isInstallOrUninstallSuccess(commandResult.getStdOutLines())
        if (!isSuccess) {
            isSuccess = isInstallOrUninstallSuccess(commandResult.getStdErrLines())
        }
    }
    return isSuccess

}

private fun isInstallOrUninstallSuccess(std: List<String>): Boolean {
    return Predicate.Methods.contains(std) { s ->
        !isEmpty(s) && s.toLowerCase(Locale.getDefault()).startsWith("success")
    }
}

fun getInstallFailErrString(commandResult: CommandResult): String {
    var failure = getInstallFailErrString(commandResult.getStdErrLines())
    if (isEmpty(failure)) {
        failure = getInstallFailErrString(commandResult.getStdOutLines())
    }
    return failure
}

private fun getInstallFailErrString(std: List<String>): String {
    return Predicate.Methods.find(std) { s ->
        !isEmpty(s) && s.toLowerCase(Locale.getDefault()).startsWith("failure")
    } ?: EMPTY_STRING
}