package net.maxsmr.commonutils

import android.app.Activity
import android.app.ActivityManager
import android.content.*
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder.Companion.logException
import net.maxsmr.commonutils.processmanager.AbstractProcessManager
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.commonutils.text.isEmpty
import java.io.File
import java.lang.reflect.Method
import java.util.*

private val logger = BaseLoggerHolder.instance.getLogger<BaseLogger>("AppUtils")

fun isPackageInstalled(context: Context, packageName: String): Boolean =
        getApplicationInfo(context, packageName) != null

fun getSelfVersionName(context: Context): String =
        getVersionName(getSelfPackageInfo(context, PackageManager.GET_META_DATA))

fun getSelfVersionCode(context: Context): Long? =
        getVersionCode(getSelfPackageInfo(context, PackageManager.GET_META_DATA))

fun getVersionName(context: Context, packageName: String): String =
        getVersionName(getPackageInfo(context, packageName, PackageManager.GET_META_DATA))

fun getVersionCode(context: Context, packageName: String): Long? =
        getVersionCode(getPackageInfo(context, packageName, PackageManager.GET_META_DATA))

fun getArchiveVersionName(context: Context, apkFile: File?): String =
        getVersionName(getArchivePackageInfo(context, apkFile, PackageManager.GET_META_DATA))

fun getArchiveVersionCode(context: Context, apkFile: File?): Long? =
        getVersionCode(getArchivePackageInfo(context, apkFile, PackageManager.GET_META_DATA))

@JvmOverloads
fun getSelfApplicationInfo(context: Context, flags: Int = 0): ApplicationInfo? =
        getApplicationInfo(context, context.packageName, flags)

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

@JvmOverloads
fun getSelfPackageInfo(context: Context, flags: Int = 0): PackageInfo? =
        getPackageInfo(context, context.packageName)

@JvmOverloads
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

@JvmOverloads
fun getArchivePackageInfo(
        context: Context,
        apkFile: File?,
        flags: Int = 0
): PackageInfo? {
    if (apkFile != null && isFileValid(apkFile)) {
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

fun getSelfLaunchIntentForPackage(context: Context): Intent? =
        getLaunchIntentForPackage(context, context.packageName)

@JvmOverloads
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

@JvmOverloads
fun canHandleActivityIntent(
        context: Context,
        intent: Intent?,
        flags: Int = 0
): Boolean {
    var result = false
    if (intent != null) {
        val pm = context.packageManager
        if (pm.resolveActivity(intent, flags) != null) {
            result = true
        }
    }
    return result
}

@JvmOverloads
fun canHandleActivityIntentQuery(
        context: Context,
        intent: Intent?,
        flags: Int = 0
): Boolean {
    val resolveInfos =
            if (intent != null) context.packageManager.queryIntentActivities(intent, flags) else null
    return resolveInfos != null && resolveInfos.isNotEmpty()
}

@JvmOverloads
fun startActivitySafe(
        context: Context,
        intent: Intent?,
        options: Bundle? = null
): Boolean {
    if (canHandleActivityIntent(context, intent)) {
        context.startActivity(intent, options)
        return true
    }
    return false
}

@JvmOverloads
fun startActivityForResultSafe(
        activity: Activity?,
        fragment: Fragment?,
        intent: Intent?,
        requestCode: Int,
        options: Bundle? = null
): Boolean {
    val context = activity ?: fragment?.requireContext() ?: throw NullPointerException("activity and fragment is null")
    if (canHandleActivityIntent(context, intent)) {
        if (fragment != null) {
            fragment.startActivityForResult(intent, requestCode, options)
        } else {
            activity?.startActivityForResult(intent, requestCode, options)
        }
        return true
    }
    return false
}

@JvmOverloads
fun browseLink(url: String = EMPTY_STRING, context: Context, options: Bundle? = null): Boolean {
    if (!startActivitySafe(context, getBrowseLinkIntent(url), options)) {
        return startActivitySafe(context, getBrowseLinkIntent(url, false), options)
    }
    return false
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

fun getSelfApplicationLabel(context: Context): String? =
        getApplicationLabel(context, context.packageName)

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

fun getApplicationUid(context: Context): Int? =
        getApplicationUid(context, context.packageName)

/**
 * @return uid for specified name or null if not found
 */
fun getApplicationUid(context: Context, packageName: String): Int? {
    val info = getApplicationInfo(context, packageName)
    return info?.uid
}

fun isSelfAppInBackground(
        context: Context,
        manager: AbstractProcessManager,
        includeSystemPackages: Boolean
): Boolean = try {
    isSelfAppInBackgroundOrThrow(context, manager, includeSystemPackages)
} catch (e: RuntimeException) {
    logger.e(e)
    false
}

@Throws(RuntimeException::class)
fun isSelfAppInBackgroundOrThrow(
        context: Context,
        manager: AbstractProcessManager,
        includeSystemPackages: Boolean
): Boolean = isAppInBackgroundOrThrow(context.packageName, manager, includeSystemPackages)

fun isAppInBackground(
        packageName: String,
        manager: AbstractProcessManager,
        includeSystemPackages: Boolean
): Boolean = try {
    isAppInBackgroundOrThrow(packageName, manager, includeSystemPackages)
} catch (e: RuntimeException) {
    logger.e(e)
    false
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

fun isSelfAppInBackground(context: Context): Boolean? {
    return try {
        isSelfAppInBackgroundOrThrow(context)
    } catch (e: RuntimeException) {
        logger.e(e)
        null
    }
}

@Throws(RuntimeException::class)
fun isSelfAppInBackgroundOrThrow(context: Context): Boolean = isAppInBackgroundOrThrow(context, context.packageName)

fun isAppInBackground(context: Context, packageName: String): Boolean? {
    return try {
        isAppInBackgroundOrThrow(context, packageName)
    } catch (e: RuntimeException) {
        logger.e(e)
        null
    }
}

/**
 * @return null if not found
 */
@Throws(RuntimeException::class)
fun isAppInBackgroundOrThrow(context: Context, packageName: String): Boolean {
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager?
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

fun getVersionName(info: PackageInfo?): String {
    return if (info != null) {
        info.versionName
    } else {
        EMPTY_STRING
    }
}

fun getVersionCode(info: PackageInfo?): Long? {
    return if (info != null) {
        if (isAtLeastPie()) {
            info.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            info.versionCode.toLong()
        }
    } else {
        null
    }
}

fun isSystemApp(packageInfo: PackageInfo?): Boolean =
        isSystemApp(packageInfo?.applicationInfo)

/**
 * @return null if not found
 */
fun isSystemApp(applicationInfo: ApplicationInfo?): Boolean {
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
fun forceLocaleInApp(context: Context, locale: Locale) {
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

fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
            ?: throw NullPointerException(ClipboardManager::class.java.simpleName + " is null")
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
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

@JvmOverloads
fun Context.fragmentActivity(maxDepth: Int = 20): FragmentActivity? = try {
    fragmentActivityOrThrow(maxDepth)
} catch (e: RuntimeException) {
    null
}

@JvmOverloads
@Throws(RuntimeException::class)
fun Context.fragmentActivityOrThrow(maxDepth: Int = 20): FragmentActivity {
    require(maxDepth > 0) { "Incorrect maxDepth: $maxDepth"}
    var curContext = this
    var depth = maxDepth
    while (--depth > 0 && curContext !is FragmentActivity) {
        curContext = (curContext as ContextWrapper).baseContext
    }
    return if(curContext is FragmentActivity) {
        curContext
    }
    else {
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
    require(maxDepth > 0) { "Incorrect maxDepth: $maxDepth"}
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

enum class DeviceType {
    PHONE, HYBRID, TABLET
}