package net.maxsmr.commonutils.android

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.text.TextUtils
import android.webkit.MimeTypeMap
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import net.maxsmr.commonutils.data.FileHelper
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

fun getBrowseLinkIntent(url: String = EMPTY_STRING): Intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))

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

    var result: Intent? = null

    if (file != null && FileHelper.isFileExists(file)) {
        result = Intent(Intent.ACTION_VIEW)

        val fileUri = if (shouldUseFileProvider) {
            FileProvider.getUriForFile(context,
                    String.format(PROVIDER_AUTHORITY_FORMAT, context.packageName),
                    file)
        } else {
            Uri.fromFile(file)
        }

        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(FileHelper.getFileExtension(file))
        result.setDataAndType(fileUri, mimeType)

        if (flags != 0) {
            result.flags = flags
        }

        if (shouldUseFileProvider) {
            result.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
    return result
}
