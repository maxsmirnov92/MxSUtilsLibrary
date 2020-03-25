package net.maxsmr.commonutils.android

import android.app.ActivityManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.StrictMode
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.WindowManager
import net.maxsmr.commonutils.android.processmanager.AbstractProcessManager
import net.maxsmr.commonutils.data.*
import net.maxsmr.commonutils.data.text.EMPTY_STRING
import net.maxsmr.commonutils.data.text.isEmpty
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import java.io.File
import java.lang.reflect.Method
import java.util.*

private val logger = BaseLoggerHolder.getInstance().getLogger<BaseLogger>("AppUtils")

fun isPackageInstalled(context: Context, packageName: String): Boolean {
    return getApplicationInfo(context, packageName) != null
}

fun getSelfVersionName(context: Context): String {
    return getVersionName(getSelfPackageInfo(context, PackageManager.GET_META_DATA))
}

fun getSelfVersionCode(context: Context): Long? {
    return getVersionCode(getSelfPackageInfo(context, PackageManager.GET_META_DATA))
}

fun getVersionName(context: Context, packageName: String): String {
    return getVersionName(getPackageInfo(context, packageName, PackageManager.GET_META_DATA))
}

fun getVersionCode(context: Context, packageName: String): Long? {
    return getVersionCode(getPackageInfo(context, packageName, PackageManager.GET_META_DATA))
}

fun getArchiveVersionName(context: Context, apkFile: File?): String {
    return getVersionName(getArchivePackageInfo(context, apkFile, PackageManager.GET_META_DATA))
}

fun getArchiveVersionCode(context: Context, apkFile: File?): Long? {
    return getVersionCode(getArchivePackageInfo(context, apkFile, PackageManager.GET_META_DATA))
}

fun getSelfApplicationInfo(context: Context, flags: Int = 0): ApplicationInfo? {
    return getApplicationInfo(context, context.packageName, flags)
}

fun getApplicationInfo(
        context: Context,
        packageName: String,
        flags: Int = 0
): ApplicationInfo? {
    val packageManager = context.packageManager
    return try {
        packageManager.getApplicationInfo(packageName, flags)
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }

}

fun getSelfPackageInfo(context: Context, flags: Int = 0): PackageInfo? {
    return getPackageInfo(context, context.packageName)
}

fun getPackageInfo(
        context: Context,
        packageName: String,
        flags: Int = 0
): PackageInfo? {
    val packageManager = context.packageManager
    if (!isEmpty(packageName)) {
        try {
            return packageManager.getPackageInfo(packageName, flags)
        } catch (e: PackageManager.NameNotFoundException) {
            // ignored
        }

    }
    return null
}

fun getArchivePackageInfo(
        context: Context,
        apkFile: File?,
        flags: Int = 0
): PackageInfo? {
    if (apkFile != null && FileHelper.isFileCorrect(apkFile)) {
        val packageManager = context.packageManager
        return packageManager.getPackageArchiveInfo(apkFile.absolutePath, flags)
    }
    return null
}

fun getLaunchIntentForPackage(context: Context, packageName: String): Intent? {
    val packageManager = context.packageManager
    if (!isEmpty(packageName)) {
        try {
            return packageManager.getLaunchIntentForPackage(packageName)
        } catch (e: Exception) {
            logger.e("an Exception occurred during getLaunchIntentForPackage(): " + e.message, e)
        }
    }
    return null
}

/**
 * @return [Intent] only if given apk file is installed
 */
fun getArchiveLaunchIntentForPackage(context: Context, apkFile: File): Intent? {
    val packageInfo = getArchivePackageInfo(context, apkFile)
    return if (packageInfo != null) getLaunchIntentForPackage(context, packageInfo.packageName) else null
}

fun getSelfLaunchIntentForPackage(context: Context): Intent? {
    return getLaunchIntentForPackage(context, context.packageName)
}

fun launchSelf(context: Context, flags: Int = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK) {
    val activityIntent = getSelfLaunchIntentForPackage(context)
            ?: throw RuntimeException("Cannot launch: self package ${context.packageName} intent not found")
    if (flags != 0) {
        activityIntent.flags = flags
    }
    context.startActivity(activityIntent)
}

/**
 * try to disable throwing exception when sharing file:// scheme instead of [androidx.core.content.FileProvider]
 */
fun disableFileUriStrictMode(): Boolean {
    var result = true
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
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

fun canLaunchIntent(context: Context, intent: Intent?): Boolean {
    var result = false
    if (intent != null) {
        val pm = context.packageManager
        if (pm.resolveActivity(intent, 0) != null) {
            result = true
        }
    }
    return result
}

fun canHandleActivityIntent(context: Context, intent: Intent?): Boolean {
    val resolveInfos = if (intent != null) context.packageManager.queryIntentActivities(intent, 0) else null
    return resolveInfos != null && resolveInfos.isNotEmpty()
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
fun isPackageRequireUpdate(
        context: Context,
        packageName: String,
        targetVersionCode: Long
): Boolean {
    val currentVersionCode = getVersionCode(context, packageName)
    if (currentVersionCode == null) {
        logger.e("Version code for $packageName not found")
        return true
    }
    logger.i("Package: $packageName, current: $currentVersionCode, new: $targetVersionCode")
    return currentVersionCode > 0 && targetVersionCode > 0 && currentVersionCode != targetVersionCode
            || currentVersionCode <= 0 || targetVersionCode <= 0
}

fun getSelfApplicationLabel(context: Context): String? {
    return getApplicationLabel(context, context.packageName)
}

/**
 * Get application title by package name
 *
 * @return null if not found
 */
fun getApplicationLabel(context: Context, packageName: String): String? {
    val packageManager = context.packageManager
    val info = getApplicationInfo(context, packageName)
    return info?.loadLabel(packageManager)?.toString()
}

fun getApplicationUid(context: Context): Int? {
    return getApplicationUid(context, context.packageName)
}

/**
 * @return uid for specified name or null if not found
 */
fun getApplicationUid(context: Context, packageName: String): Int? {
    val info = getApplicationInfo(context, packageName)
    return info?.uid
}

fun isSelfAppInBackground(context: Context): Boolean {
    val result = isAppInBackground(context, context.packageName)
    return result
            ?: throw RuntimeException("Self package ${context.packageName} not found")
}

/**
 * @return null if not found
 */
fun isAppInBackground(context: Context, packageName: String): Boolean? {
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager?
            ?: throw RuntimeException(ActivityManager::class.java.simpleName + " is null")
    var isInBackground: Boolean? = null
    if (!isEmpty(packageName)) {
        val runningProcesses = am.runningAppProcesses
        if (runningProcesses != null) {
            for (processInfo in runningProcesses) {
                if (packageName == processInfo.processName) {
                    isInBackground = processInfo.importance != ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                }
            }
        }
    }
    return isInBackground
}

fun isSelfAppInBackground(
        context: Context,
        manager: AbstractProcessManager,
        includeSystemPackages: Boolean
): Boolean? {
    return isAppInBackground(context.packageName, manager, includeSystemPackages)
}

/**
 * @return null if not found
 */
fun isAppInBackground(
        packageName: String,
        manager: AbstractProcessManager,
        includeSystemPackages: Boolean
): Boolean? {
    var isInBackground: Boolean? = null
    if (!isEmpty(packageName)) {
        for (info in manager.getProcesses(includeSystemPackages)) {
            if (stringsEqual(info.packageName, packageName, false)) {
                isInBackground = info.isForeground != null && !info.isForeground
                break
            }
        }
    }
    return isInBackground
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

fun getVersionName(info: PackageInfo?): String {
    return if (info != null) {
        info.versionName
    } else {
        EMPTY_STRING
    }
}

fun getVersionCode(info: PackageInfo?): Long? {
    return if (info != null) {
        if (SdkUtils.isAtLeastPie()) {
            info.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            info.versionCode.toLong()
        }
    } else {
        null
    }
}

fun isSystemApp(packageInfo: PackageInfo?): Boolean? {
    return isSystemApp(packageInfo?.applicationInfo)
}

/**
 * @return null if not found
 */
fun isSystemApp(applicationInfo: ApplicationInfo?): Boolean? {
    if (applicationInfo == null) {
        return false;
    }
    return applicationInfo.flags and (ApplicationInfo.FLAG_UPDATED_SYSTEM_APP or ApplicationInfo.FLAG_SYSTEM) > 0
}

/**
 * Задает локаль по умолчанию в рамках приложения.
 *
 * @param locale объект локали
 */
fun forceLocaleInApp(context: Context, locale: Locale?) {
    Locale.setDefault(locale)
    context.resources?.let {
        val configuration = it.configuration
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            configuration.setLocale(locale)
            context.createConfigurationContext(configuration)
        } else {
            configuration.locale = locale
            it.updateConfiguration(configuration, it.displayMetrics)
        }
    }
}

fun copyToClipboard(context: Context, label: String?, text: String?) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
            ?: throw NullPointerException(ClipboardManager::class.java.simpleName + " is null")
    val clip = ClipData.newPlainText(label, text)
    clipboard.primaryClip = clip
}

fun getDisplayMetrics(context: Context): DisplayMetrics {
    val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            ?: throw java.lang.NullPointerException(WindowManager::class.java.simpleName + "")
    val outMetrics = DisplayMetrics()
    wm.defaultDisplay.getMetrics(outMetrics)
    return outMetrics
}

/**
 * Converts pixel value to dp value
 */
fun convertPxToDp(px: Float, context: Context): Float {
    return px / context.resources.displayMetrics.density
}

@JvmOverloads
fun convertAnyToPx(
        value: Float,
        unit: Int = TypedValue.COMPLEX_UNIT_DIP,
        context: Context
): Float {
    // OR simply px = value * density (if DIP)
    return if (value <= 0) {
        0f
    } else {
        TypedValue.applyDimension(unit, value, context.resources.displayMetrics)
    }
}

fun getScreenType(context: Context): DeviceType {
    val outMetrics = getDisplayMetrics(context)
    val deviceType: DeviceType
    val shortSize = Math.min(outMetrics.heightPixels, outMetrics.widthPixels)
    val shortSizeDp = shortSize * DisplayMetrics.DENSITY_DEFAULT / outMetrics.densityDpi
    deviceType = if (shortSizeDp < 600) { // 0-599dp: "phone" UI with a separate status & navigation bar
        DeviceType.PHONE
    } else if (shortSizeDp < 720) { // 600-719dp: "phone" UI with modifications for larger screens
        DeviceType.HYBRID
    } else { // 720dp: "tablet" UI with a single combined status & navigation bar
        DeviceType.TABLET
    }
    return deviceType
}

fun isTabletUI(con: Context): Boolean {
    return getScreenType(con) == DeviceType.TABLET
}

enum class DeviceType {
    PHONE, HYBRID, TABLET
}
