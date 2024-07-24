package net.maxsmr.commonutils

import android.app.Activity
import android.app.ActivityManager
import android.content.*
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.StrictMode
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder.Companion.logException
import net.maxsmr.commonutils.processmanager.AbstractProcessManager
import net.maxsmr.commonutils.text.isEmpty
import java.io.File
import java.lang.reflect.Method
import java.util.*

private val logger = BaseLoggerHolder.instance.getLogger<BaseLogger>("AppUtils")

fun PackageManager.isPackageInstalled(packageName: String): Boolean =
    getApplicationInfoOrNull(packageName) != null

fun Context.getSelfVersionName(): String =
    getSelfPackageInfo(PackageManager.GET_META_DATA)?.versionName.orEmpty()

fun Context.getSelfVersionCode(): Long? =
    getSelfPackageInfo(PackageManager.GET_META_DATA).getVersionCode()

fun PackageManager.getVersionName(packageName: String): String =
    getPackageInfoOrNull(packageName, PackageManager.GET_META_DATA)?.versionName.orEmpty()

fun PackageManager.getVersionCode(packageName: String): Long? =
    getPackageInfoOrNull(packageName, PackageManager.GET_META_DATA).getVersionCode()

fun PackageManager.getArchiveVersionName(apkFile: File?): String =
    getArchivePackageInfo(apkFile, PackageManager.GET_META_DATA)?.versionName.orEmpty()

fun PackageManager.getArchiveVersionCode(apkFile: File?): Long? =
    getArchivePackageInfo(apkFile, PackageManager.GET_META_DATA).getVersionCode()

@JvmOverloads
fun Context.getSelfApplicationInfo(flags: Int = 0): ApplicationInfo? =
    packageManager.getApplicationInfoOrNull(packageName, flags)

fun PackageManager.getApplicationInfoOrNull(
    packageName: String,
    flags: Int = 0
): ApplicationInfo? {
    return try {
        getApplicationInfo(packageName, flags)
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }
}

@JvmOverloads
fun Context.getSelfPackageInfo(flags: Int = 0): PackageInfo? =
    packageManager.getPackageInfoOrNull(this.packageName, flags)

@JvmOverloads
fun PackageManager.getPackageInfoOrNull(packageName: String, flags: Int = 0): PackageInfo? {
    if (!isEmpty(packageName)) {
        try {
            return getPackageInfo(packageName, flags)
        } catch (e: PackageManager.NameNotFoundException) {
            // ignored
        }

    }
    return null
}

@JvmOverloads
fun PackageManager.getArchivePackageInfo(
    apkFile: File?,
    flags: Int = 0
): PackageInfo? {
    if (apkFile != null && isFileValid(apkFile)) {
        return getPackageArchiveInfo(apkFile.absolutePath, flags)
    }
    return null
}

fun PackageManager.getLaunchIntentForPackageOrNull(packageName: String): Intent? {
    if (!isEmpty(packageName)) {
        try {
            return getLaunchIntentForPackage(packageName)
        } catch (e: Exception) {
            logException(logger, e, "getLaunchIntentForPackage")
        }
    }
    return null
}

/**
 * @return [Intent] only if given apk file is installed
 */
fun PackageManager.getArchiveLaunchIntentForPackage(apkFile: File): Intent? {
    val packageInfo = getArchivePackageInfo(apkFile)
    return if (packageInfo != null) {
        getLaunchIntentForPackageOrNull(packageInfo.packageName)
    } else {
        null
    }
}

fun Context.getSelfLaunchIntentForPackage(): Intent? =
    packageManager.getLaunchIntentForPackageOrNull(packageName)

@JvmOverloads
fun Context.launchSelf(flags: Int = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK) {
    val activityIntent = getSelfLaunchIntentForPackage()
        ?: throw RuntimeException("Cannot launch: self package $packageName intent not found")
    if (flags != 0) {
        activityIntent.flags = flags
    }
    startActivity(activityIntent)
}

/**
 * try to disable throwing exception when sharing file:// scheme instead of [androidx.core.content.FileProvider]
 */
@RequiresApi(Build.VERSION_CODES.N)
fun disableFileUriStrictMode(): Boolean {
    var result = true
    if (isAtLeastNougat()) {
        result = false
        var disableMethod: Method? = null
        try {
            disableMethod = ReflectionUtils.findMethod(StrictMode::class.java, "disableDeathOnFileUriExposure", false, true)
        } catch (e: NoSuchMethodException) {
            logger.e("Cannot disable \"death on file uri exposure\": " + e.message, e)
        }

        if (disableMethod != null) {
            try {
                disableMethod.invoke(null)
                result = true
            } catch (e: Exception) {
                logger.e("Cannot disable \"death on file uri exposure\": " + e.message, e)
            }

        }
    }
    return result
}

fun getPackageActivitiesCount(packageInfo: PackageInfo): Int {
    return if (packageInfo.activities == null || packageInfo.activities.isEmpty()) {
        0
    } else {
        packageInfo.activities.size
    }
}

/**
 * Check if app requires update
 */
fun PackageManager.packageRequiresUpdate(
    packageName: String,
    targetVersionCode: Long
): Boolean {
    val currentVersionCode = getVersionCode(packageName)
    if (currentVersionCode == null) {
        logger.e("Version code for $packageName not found")
        return true
    }
    logger.i("Package: $packageName, current: $currentVersionCode, new: $targetVersionCode")
    return currentVersionCode > 0 && targetVersionCode > 0 && currentVersionCode < targetVersionCode
            || currentVersionCode <= 0 || targetVersionCode <= 0
}

fun Context.getSelfApplicationLabel(): String? =
    packageManager.getApplicationLabel(packageName)

/**
 * Get application title by package name
 *
 * @return null if not found
 */
fun PackageManager.getApplicationLabel(packageName: String): String? {
    val info = getApplicationInfoOrNull(packageName)
    return info?.loadLabel(this)?.toString()
}

fun Context.getSelfApplicationUid(): Int? =
    packageManager.getApplicationUid(packageName)

/**
 * @return uid for specified name or null if not found
 */
fun PackageManager.getApplicationUid(packageName: String): Int? {
    val info = getApplicationInfoOrNull(packageName)
    return info?.uid
}

fun Context.isSelfAppInBackground(
    manager: AbstractProcessManager,
    includeSystemPackages: Boolean
): Boolean? = try {
    isSelfAppInBackgroundOrThrow(manager, includeSystemPackages)
} catch (e: RuntimeException) {
    logger.e(e)
    null
}

@Throws(RuntimeException::class)
fun Context.isSelfAppInBackgroundOrThrow(
    manager: AbstractProcessManager,
    includeSystemPackages: Boolean
): Boolean = isAppInBackgroundOrThrow(packageName, manager, includeSystemPackages)

fun isAppInBackground(
    packageName: String,
    manager: AbstractProcessManager,
    includeSystemPackages: Boolean
): Boolean? = try {
    isAppInBackgroundOrThrow(packageName, manager, includeSystemPackages)
} catch (e: RuntimeException) {
    logger.e(e)
    null
}

@Throws(RuntimeException::class)
fun isAppInBackgroundOrThrow(
    packageName: String,
    manager: AbstractProcessManager,
    includeSystemPackages: Boolean
): Boolean {
    if (!isEmpty(packageName)) {
        for (info in manager.getProcesses(includeSystemPackages)) {
            if (stringsEqual(info.packageName, packageName, false)) {
                return info.isForeground != null && !info.isForeground
            }
        }
    }
    throw RuntimeException("Process with name '$packageName' not found")
}

fun Context.isSelfAppInBackground(): Boolean? = isAppInBackground(packageName)

@Throws(RuntimeException::class)
fun Context.isSelfAppInBackgroundOrThrow(): Boolean = isAppInBackgroundOrThrow(packageName)

fun Context.isAppInBackground(packageName: String): Boolean? {
    return try {
        isAppInBackgroundOrThrow(packageName)
    } catch (e: RuntimeException) {
        logger.e(e)
        null
    }
}

/**
 * @return null if not found
 */
@Throws(RuntimeException::class)
fun Context.isAppInBackgroundOrThrow(packageName: String): Boolean {
    val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager?
        ?: throw RuntimeException(ActivityManager::class.java.simpleName + " is null")
    if (!isEmpty(packageName)) {
        val runningProcesses = am.runningAppProcesses
        if (runningProcesses != null) {
            for (processInfo in runningProcesses) {
                if (packageName == processInfo.processName) {
                    return processInfo.importance != ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                }
            }
        }
    }
    throw RuntimeException("Process with name '$packageName' not found")
}

fun getPidsByName(
    packageName: String?,
    manager: AbstractProcessManager,
    includeSystemPackages: Boolean,
    matchFlags: Int = MatchStringOption.EQUALS.flag
): Set<Int> {
    val pids = LinkedHashSet<Int>()
    if (!isEmpty(packageName)) {
        val runningProcesses = manager.getProcesses(includeSystemPackages)
        for (process in runningProcesses) {
            if (stringsMatch(packageName, process.packageName, matchFlags)) {
                pids.add(process.pid)
            }
        }
    }
    return pids
}

fun PackageInfo?.getVersionCode(): Long? {
    return if (this != null) {
        if (isAtLeastPie()) {
            longVersionCode
        } else {
            @Suppress("DEPRECATION")
            versionCode.toLong()
        }
    } else {
        null
    }
}

fun PackageInfo.isSystemApp(): Boolean =
    applicationInfo.isSystemApp()

/**
 * @return null if not found
 */
fun ApplicationInfo.isSystemApp(): Boolean {
    return this.flags and (ApplicationInfo.FLAG_UPDATED_SYSTEM_APP or ApplicationInfo.FLAG_SYSTEM) > 0
}

fun Context.copyToClipboard(label: String, text: String) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
        ?: throw NullPointerException(ClipboardManager::class.java.simpleName + " is null")
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
}

fun Context.getDisplayMetrics(): DisplayMetrics {
    val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val outMetrics = DisplayMetrics()
    wm.defaultDisplay.getMetrics(outMetrics)
    return outMetrics
}

@JvmOverloads
fun Context.fragmentActivity(maxDepth: Int = 20): FragmentActivity? = try {
    fragmentActivityOrThrow(maxDepth)
} catch (e: RuntimeException) {
    null
}

@JvmOverloads
@Throws(RuntimeException::class)
fun Context.fragmentActivityOrThrow(maxDepth: Int = 20): FragmentActivity {
    require(maxDepth > 0) { "Incorrect maxDepth: $maxDepth" }
    var curContext = this
    var depth = maxDepth
    while (--depth > 0 && curContext !is FragmentActivity) {
        curContext = (curContext as ContextWrapper).baseContext
    }
    return if (curContext is FragmentActivity) {
        curContext
    } else {
        throw RuntimeException("FragmentActivity not found")
    }
}

@JvmOverloads
fun Context.lifecycleOwner(maxDepth: Int = 20): LifecycleOwner? = try {
    lifecycleOwnerOrThrow(maxDepth)
} catch (e: RuntimeException) {
    null
}

@JvmOverloads
@Throws(RuntimeException::class)
fun Context.lifecycleOwnerOrThrow(maxDepth: Int = 20): LifecycleOwner {
    require(maxDepth > 0) { "Incorrect maxDepth: $maxDepth" }
    var curContext = this
    var depth = maxDepth
    while (--depth > 0 && curContext !is LifecycleOwner) {
        curContext = (curContext as ContextWrapper).baseContext
    }
    return if (curContext is LifecycleOwner) {
        curContext
    } else {
        throw RuntimeException("LifecycleOwner not found")
    }
}

fun Any.asContext(): Context? = try {
    asContextOrThrow()
} catch (e: ClassCastException) {
    null
}

@Throws(ClassCastException::class)
fun Any.asContextOrThrow(): Context {
    return when (this) {
        is Context -> {
            this
        }

        is Fragment -> {
            this.requireContext()
        }

        else -> {
            throw ClassCastException("Cannot cast to Context")
        }
    }
}

fun Any.asActivity(): Activity? = try {
    asActivityOrThrow()
} catch (e: ClassCastException) {
    null
}

@Throws(ClassCastException::class)
fun Any.asActivityOrThrow(): Activity {
    return when (this) {
        is Activity -> {
            this
        }

        is Fragment -> {
            this.requireActivity()
        }

        else -> {
            throw ClassCastException("Cannot cast to Activity")
        }
    }
}