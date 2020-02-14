package net.maxsmr.commonutils.android

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import net.maxsmr.commonutils.data.EMPTY_STRING
import net.maxsmr.commonutils.data.FileHelper
import java.io.File

val PROVIDER_AUTHORITY_FORMAT = "%s.provider"

fun getBrowseLinkIntent(
        url: String = EMPTY_STRING,
        title: String = EMPTY_STRING,
        flags: Int = Intent.FLAG_ACTIVITY_NEW_TASK
): Intent {
    val targetIntent = Intent(Intent.ACTION_VIEW)
            .apply {
                data = Uri.parse(url)
                if (flags != 0) {
                    this.flags = flags
                }
            }

    return if (title.isNotEmpty()) {
        Intent.createChooser(targetIntent, title)
    } else {
        targetIntent
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
        if (!canHandleActivityIntent(context, result)) {
            result = null
        }
    }
    return result
}
