package net.maxsmr.commonutils

import android.annotation.TargetApi
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder.Companion.formatException

private val logger = BaseLoggerHolder.instance.getLogger<BaseLogger>("OpenIntentUtils")

@JvmOverloads
fun Context.openSystemBrowser(
    url: String?,
    flags: Int = Intent.FLAG_ACTIVITY_NEW_TASK,
    options: Bundle? = null,
    errorHandler: ((ActivityNotFoundException?) -> Unit)? = null,
): Boolean {
    return if (url.isNullOrEmpty()) {
        false
    } else {
        openSystemBrowser(Uri.parse(url), flags, options, errorHandler)
    }
}

@JvmOverloads
fun Context.openSystemBrowser(
    uri: Uri,
    flags: Int = Intent.FLAG_ACTIVITY_NEW_TASK,
    options: Bundle? = null,
    errorHandler: ((ActivityNotFoundException?) -> Unit)? = null,
): Boolean {
    return startActivitySafe(getViewUrlIntent(uri, this).addFlags(flags), options, errorHandler)
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
    } catch (e: ActivityNotFoundException) {
        // при оборачивании в chooser, не будет брошено
        logger.e(formatException(e, "startActivity/startActivityForResult"))
        errorHandler?.invoke(e)
        false
    }
}