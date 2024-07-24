package net.maxsmr.commonutils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder.Companion.formatException

private val logger = BaseLoggerHolder.instance.getLogger<BaseLogger>("OpenIntentUtils")

@JvmOverloads
fun Context.openEmailIntent(
    address: String?,
    sendAction: SendAction = SendAction.SENDTO,
    chooserTitle: String? = null,
    sendIntentFunc: ((Intent) -> Unit)? = null,
    chooserIntentFunc: ((Intent) -> Unit)? = null,
    flags: Int = Intent.FLAG_ACTIVITY_NEW_TASK,
    options: Bundle? = null,
    errorHandler: ((RuntimeException) -> Unit)? = null,
): Boolean {
    return openEmailIntent(
        getSendEmailUri(address),
        address?.let { listOf(address) },
        sendAction,
        chooserTitle,
        sendIntentFunc,
        chooserIntentFunc,
        flags,
        options,
        errorHandler
    )
}

/**
 * @param uri со схемой [URL_SCHEME_MAIL]
 * @param sendIntentFunc дополнительно можно указать subject, text и т.д.
 * @param chooserIntentFunc настройка chooser [Intent] при указании [chooserTitle]
 */
@JvmOverloads
fun Context.openEmailIntent(
    uri: Uri,
    addresses: List<String>? = null,
    sendAction: SendAction = SendAction.SENDTO,
    chooserTitle: String? = null,
    sendIntentFunc: ((Intent) -> Unit)? = null,
    chooserIntentFunc: ((Intent) -> Unit)? = null,
    flags: Int = Intent.FLAG_ACTIVITY_NEW_TASK,
    options: Bundle? = null,
    errorHandler: ((RuntimeException) -> Unit)? = null,
): Boolean {
    val intent = getSendEmailIntent(uri, sendAction, addresses) ?: return false
    return openSendDataIntent(
        intent,
        chooserTitle,
        sendIntentFunc,
        chooserIntentFunc,
        flags,
        options,
        errorHandler
    )
}

@JvmOverloads
fun Context.openSendDataIntent(
    sendAction: SendAction = SendAction.SEND,
    chooserTitle: String? = null,
    sendIntentFunc: (Intent) -> Unit,
    chooserIntentFunc: ((Intent) -> Unit)? = null,
    flags: Int = Intent.FLAG_ACTIVITY_NEW_TASK,
    options: Bundle? = null,
    errorHandler: ((RuntimeException) -> Unit)? = null,
): Boolean {
    return openSendDataIntent(
        getSendIntent(sendAction),
        chooserTitle,
        sendIntentFunc,
        chooserIntentFunc,
        flags,
        options,
        errorHandler
    )
}

private fun Context.openSendDataIntent(
    intent: Intent,
    chooserTitle: String?,
    sendIntentFunc: ((Intent) -> Unit)?,
    chooserIntentFunc: ((Intent) -> Unit)?,
    flags: Int = Intent.FLAG_ACTIVITY_NEW_TASK,
    options: Bundle? = null,
    errorHandler: ((RuntimeException) -> Unit)? = null,
): Boolean {
    return startActivitySafe(
        intent.apply {
            sendIntentFunc?.invoke(this)
            addFlags(flags)
        }.wrapChooser(chooserTitle).apply {
            chooserIntentFunc?.invoke(this)
        },
        options = options,
        errorHandler = errorHandler
    )
}

@JvmOverloads
fun Context.openDocument(
    type: String?,
    mimeTypes: List<String>,
    flags: Int = Intent.FLAG_ACTIVITY_NEW_TASK,
    options: Bundle? = null,
    errorHandler: ((RuntimeException) -> Unit)? = null,
): Boolean =
    startActivitySafe(
        getOpenDocumentIntent(type, mimeTypes).addFlags(flags),
        options = options,
        errorHandler = errorHandler
    )

/**
 * Для открытия в конкретном аппе
 * пользовать setPackage для [getViewUrlIntent]
 */
@JvmOverloads
fun Context.openViewUrl(
    url: String,
    mimeType: String? = null,
    flags: Int = Intent.FLAG_ACTIVITY_NEW_TASK,
    options: Bundle? = null,
    errorHandler: ((RuntimeException) -> Unit)? = null,
): Boolean {
    return if (url.isEmpty()) {
        false
    } else {
        openViewUrl(Uri.parse(url), mimeType, flags, options, errorHandler)
    }
}

@JvmOverloads
fun Context.openViewUrl(
    uri: Uri,
    mimeType: String? = null,
    flags: Int = Intent.FLAG_ACTIVITY_NEW_TASK,
    options: Bundle? = null,
    errorHandler: ((RuntimeException) -> Unit)? = null,
): Boolean {
    return startActivitySafe(
        getViewUrlIntent(uri, mimeType, context = this).addFlags(flags),
        options = options,
        errorHandler = errorHandler
    )
}

@RequiresApi(Build.VERSION_CODES.S)
fun Context.openViewUrlNonBrowser(
    url: String,
    mimeType: String? = null,
    flags: Int = Intent.FLAG_ACTIVITY_NEW_TASK,
    options: Bundle? = null,
    errorHandler: ((RuntimeException) -> Unit)? = null,
): Boolean {
    return if (url.isEmpty()) {
        false
    } else {
        openViewUrlNonBrowser(Uri.parse(url), mimeType, flags, options, errorHandler)
    }
}

/**
 * @return false if:
 * 1. Only browser apps can handle the intent.
 * 2. The user has set a browser app as the default app.
 * 3. The user hasn't set any app as the default for handling this URL.
 */
@RequiresApi(Build.VERSION_CODES.S)
fun Context.openViewUrlNonBrowser(
    uri: Uri,
    mimeType: String? = null,
    flags: Int = Intent.FLAG_ACTIVITY_NEW_TASK,
    options: Bundle? = null,
    errorHandler: ((RuntimeException) -> Unit)? = null,
): Boolean {
    return startActivitySafe(
        getViewUrlIntent(uri, mimeType).addFlags(
            flags
                    or Intent.FLAG_ACTIVITY_REQUIRE_NON_BROWSER
                    or Intent.FLAG_ACTIVITY_REQUIRE_DEFAULT
        ).addCategory(Intent.CATEGORY_BROWSABLE),
        options = options,
        errorHandler = errorHandler
    )
}

@RequiresApi(Build.VERSION_CODES.M)
fun Context.openBatteryOptimizationSettings() {
    startActivitySafe(
        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            .setData(Uri.parse("package:$packageName"))
    ) {
        startActivitySafe(Intent().apply {
            setClassName(
                "com.miui.powerkeeper",
                "com.miui.powerkeeper.ui.HiddenAppsConfigActivity"
                // HiddenAppsContainerManagementActivity
                // PowerHideModeActivity
            )
            putExtra("package_name", packageName);
        }) {
            startActivitySafe(getAppSettingsIntent(this))
        }
    }
}

@SuppressLint("BatteryLife")
@RequiresApi(Build.VERSION_CODES.M)
fun Context.openRequestIgnoreBatteryOptimizations() {
    val powerService = getSystemService(Context.POWER_SERVICE) as PowerManager? ?: return
    val packageName: String = packageName
    if (!powerService.isIgnoringBatteryOptimizations(packageName)) {
        startActivitySafe(
            Intent().apply {
                action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                data = Uri.parse("package:$packageName")
            }
        )
    }
}

/**
 * Текущий package установлен в системе в кач-ве дефолтного браузера
 */
fun Context.isSystemBrowserByDefault(url: String): Boolean {
    val viewUrlResults = queryIntentActivitiesCompat(
        getViewUrlIntent(url, null),
        // CATEGORY_BROWSABLE не добавляем
        PackageManager.MATCH_DEFAULT_ONLY,
        false
    )
    return viewUrlResults.singleOrNull { it.activityInfo.packageName == packageName } != null
}

@SuppressLint("QueryPermissionsNeeded")
@JvmOverloads
fun Context.queryIntentActivitiesCompat(
    intent: Intent,
    flags: Int = 0,
    shouldFilterCaller: Boolean = true,
): List<ResolveInfo> {
    if (isAtLeastTiramisu()) {
        packageManager.queryIntentActivities(
            intent,
            PackageManager.ResolveInfoFlags.of(flags.toLong())
        )
    } else {
        packageManager.queryIntentActivities(intent, flags)
    }.also { results ->
        return if (shouldFilterCaller) {
            results.filter {
                it.activityInfo.packageName != packageName
            }
        } else {
            results
        }
    }
}

@JvmOverloads
fun Context.startActivitySafe(
    intent: Intent,
    requestCode: Int? = null,
    options: Bundle? = null,
    errorHandler: ((RuntimeException) -> Unit)? = null,
): Boolean = startActivitySafeForAny(intent, requestCode, options = options, errorHandler = errorHandler)

@JvmOverloads
fun Activity.startActivitySafe(
    intent: Intent,
    requestCode: Int? = null,
    options: Bundle? = null,
    errorHandler: ((RuntimeException) -> Unit)? = null,
): Boolean {
    return startActivitySafeForAny(intent, requestCode, options, errorHandler)
}

@JvmOverloads
fun Fragment.startActivitySafe(
    intent: Intent,
    requestCode: Int? = null,
    options: Bundle? = null,
    errorHandler: ((RuntimeException) -> Unit)? = null,
): Boolean {
    return startActivitySafeForAny(intent, requestCode, options, errorHandler)
}

private fun Any.startActivitySafeForAny(
    intent: Intent,
    requestCode: Int? = null,
    options: Bundle? = null,
    errorHandler: ((RuntimeException) -> Unit)? = null,
): Boolean {
    val context: Context = when (this) {
        is Context -> this
        is Fragment -> requireContext()
        else -> return false
    }
    return try {
        if (requestCode != null) {
            when (this) {
                is Activity -> startActivityForResult(intent, requestCode, options)
                is Fragment -> startActivityForResult(intent, requestCode, options)
                is Context -> startActivity(intent, options)
                else -> return false
            }
        } else {
            context.startActivity(intent, options)
        }
        true
    } catch (e: RuntimeException) {
        // при оборачивании в chooser ActivityNotFoundException не будет брошено
        logger.e(formatException(e, "startActivity/startActivityForResult"))
        errorHandler?.invoke(e)
        false
    }
}