package net.maxsmr.commonutils.android

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.text.TextUtils
import android.webkit.MimeTypeMap
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import net.maxsmr.commonutils.data.getFileExtension
import net.maxsmr.commonutils.data.isFileExists
import net.maxsmr.commonutils.data.text.EMPTY_STRING
import java.io.File

const val PROVIDER_AUTHORITY_FORMAT = "%s.provider"

fun wrapIntent(
        intent: Intent,
        title: String = EMPTY_STRING,
        flags: Int = 0
): Intent {
    with(intent) {
        if (flags != 0) {
            this.flags = flags
        }
        return if (title.isNotEmpty()) {
            Intent.createChooser(this, title)
        } else {
            this
        }
    }
}

fun getBrowseLinkIntent(url: String = EMPTY_STRING, withType: Boolean = true): Intent {
    val result = Intent(Intent.ACTION_VIEW)
    val uri = Uri.parse(url)
    if (withType) {
        val ext = MimeTypeMap.getFileExtensionFromUrl(url)
        var type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
        if (type == null) type = "*/*"
        result.setDataAndType(uri, type)
    } else {
        result.data = uri
    }
    return result
}

@RequiresApi(Build.VERSION_CODES.KITKAT)
fun getOpenDocumentIntent(type: String?, mimeTypes: Array<String?>?): Intent {
    with(Intent(Intent.ACTION_OPEN_DOCUMENT)) {
        addCategory(Intent.CATEGORY_OPENABLE)
        this.type = if (!TextUtils.isEmpty(type)) {
            type
        } else {
            "*/*"
        }
        if (mimeTypes != null && mimeTypes.isNotEmpty()) {
            putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
        }
        return this
    }
}

/**
 * @param shouldUseFileProvider true, if intended to use FileProvider instead of file://
 */
fun getViewFileIntent(
        context: Context,
        file: File?,
        shouldUseFileProvider: Boolean,
        flags: Int = Intent.FLAG_ACTIVITY_NEW_TASK
): Intent? {
    val uriAndType = getFileUriAndType(context, file, shouldUseFileProvider) ?: return null
    return getViewIntent(uriAndType.first, uriAndType.second,
            if (shouldUseFileProvider) flags or Intent.FLAG_GRANT_READ_URI_PERMISSION else flags)

}

/**
 * @param shouldUseFileProvider true, if intended to use FileProvider instead of file://
 */
fun getShareFileIntent(
        context: Context,
        file: File?,
        subject: String,
        text: String,
        shouldUseFileProvider: Boolean,
        flags: Int = Intent.FLAG_ACTIVITY_NEW_TASK
): Intent? {
    val uriAndType = getFileUriAndType(context, file, shouldUseFileProvider) ?: return null
    return getShareIntent(uriAndType.first, uriAndType.second, subject, text,
            if (shouldUseFileProvider) flags or Intent.FLAG_GRANT_READ_URI_PERMISSION else flags)
}

fun getViewIntent(
        uri: Uri,
        mimeType: String,
        flags: Int = Intent.FLAG_ACTIVITY_NEW_TASK
): Intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(uri, mimeType)
        .setFlags(flags)

fun getShareIntent(
        uri: Uri,
        subject: String,
        text: String,
        mimeType: String,
        flags: Int = Intent.FLAG_ACTIVITY_NEW_TASK
): Intent = Intent(Intent.ACTION_SEND)
        .putExtra(Intent.EXTRA_STREAM, uri)
        .putExtra(Intent.EXTRA_SUBJECT, subject)
        .putExtra(Intent.EXTRA_TEXT, text)
        .setType(mimeType)
        .setFlags(flags)

private fun getFileUriAndType(
        context: Context,
        file: File?,
        shouldUseFileProvider: Boolean
): Pair<Uri, String>? {
    if (file == null || !isFileExists(file)) {
        return null
    }

    val fileUri = if (shouldUseFileProvider) {
        FileProvider.getUriForFile(context,
                String.format(PROVIDER_AUTHORITY_FORMAT, context.packageName),
                file)
    } else {
        Uri.fromFile(file)
    }

    val ext = getFileExtension(file)
    val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)

    return Pair(fileUri, mimeType ?: EMPTY_STRING)
}