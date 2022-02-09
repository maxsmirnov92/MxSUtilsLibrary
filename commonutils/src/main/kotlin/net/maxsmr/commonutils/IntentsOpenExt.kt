package net.maxsmr.commonutils

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder.Companion.formatException

private val logger = BaseLoggerHolder.instance.getLogger<BaseLogger>("OpenIntentUtils")

@JvmOverloads
fun Context.openSystemBrowser(
    url: String?,
    mimeType: String? = null,
    mimeTypes: List<String>? = null,
    options: Bundle? = null,
    errorHandler: ((ActivityNotFoundException?) -> Unit)? = null,
): Boolean {
    return if (url.isNullOrEmpty()) {
        false
    } else {
        openSystemBrowser(Uri.parse(url), mimeType, mimeTypes, options, errorHandler)
    }
}

@JvmOverloads
fun Context.openSystemBrowser(
    uri: Uri,
    mimeType: String? = null,
    mimeTypes: List<String>? = null,
    options: Bundle? = null,
    errorHandler: ((ActivityNotFoundException?) -> Unit)? = null,
): Boolean {
    return startActivitySafe(getViewUrlIntent(uri, mimeType, mimeTypes), options, errorHandler)
}

@JvmOverloads
fun Context.canHandleActivityIntent(
    intent: Intent,
    flags: Int = 0
): Boolean {
    val pm = packageManager
    if (pm.resolveActivity(intent, flags) != null) {
        return true
    }
    return false
}

@JvmOverloads
fun Context.canHandleActivityIntentQuery(
    intent: Intent,
    flags: Int = 0
): Boolean {
    val resolveInfos = packageManager.queryIntentActivities(intent, flags)
    return resolveInfos.isNotEmpty()
}

@JvmOverloads
fun Context.startActivitySafe(
    intent: Intent,
    options: Bundle? = null,
    errorHandler: ((ActivityNotFoundException?) -> Unit)? = null,
): Boolean {
    return startActivitySafeForAny(intent, options = options, errorHandler = errorHandler)
}

@JvmOverloads
fun Activity.startActivitySafe(
    intent: Intent,
    requestCode: Int? = null,
    options: Bundle? = null,
    errorHandler: ((ActivityNotFoundException?) -> Unit)? = null,
): Boolean {
    return startActivitySafeForAny(intent, requestCode, options, errorHandler)
}

@JvmOverloads
fun Fragment.startActivitySafe(
    intent: Intent,
    requestCode: Int? = null,
    options: Bundle? = null,
    errorHandler: ((ActivityNotFoundException?) -> Unit)? = null,
): Boolean {
    return startActivitySafeForAny(intent, requestCode, options, errorHandler)
}

private fun Any.startActivitySafeForAny(
    intent: Intent,
    requestCode: Int? = null,
    options: Bundle? = null,
    errorHandler: ((ActivityNotFoundException?) -> Unit)? = null,
): Boolean {
    val context: Context = when (this) {
        is Context -> this
        is Activity -> this
        is Fragment -> requireActivity()
        else -> return false
    }
    if (!context.canHandleActivityIntent(intent)) {
        // при оборачивании в chooser вёрнёт true при отсутствии аппов
        errorHandler?.invoke(null)
        return false
    }
    return try {
        if (requestCode != null
            && (context is Activity || context is Fragment)
        ) {
            when (context) {
                is Activity -> context.startActivityForResult(intent, requestCode, options)
                is Fragment -> context.startActivityForResult(intent, requestCode, options)
            }
        } else {
            context.startActivity(intent, options)
        }
        true
    } catch (e: ActivityNotFoundException) {
        // при оборачивании в chooser, не будет брошено
        logger.e(formatException(e, "startActivity/startActivityForResult"))
        errorHandler?.invoke(e)
        false
    }
}