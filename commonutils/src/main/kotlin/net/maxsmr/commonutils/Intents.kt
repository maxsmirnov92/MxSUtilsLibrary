package net.maxsmr.commonutils

import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import android.webkit.MimeTypeMap
import androidx.annotation.RequiresApi
import net.maxsmr.commonutils.media.toContentUri
import net.maxsmr.commonutils.media.toFileUri
import net.maxsmr.commonutils.text.EMPTY_STRING
import java.io.File

@JvmOverloads
fun wrapIntent(
        intent: Intent,
        title: String = EMPTY_STRING,
        flags: Int = 0
): Intent {
    with(intent) {
        if (flags != 0) {
            this.addFlags(flags)
        }
        return if (title.isNotEmpty()) {
            Intent.createChooser(this, title)
        } else {
            this
        }
    }
}

@JvmOverloads
fun getAppSettingsIntent(context: Context, packageName: String = context.packageName): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.parse("package:$packageName"))

@TargetApi(Build.VERSION_CODES.M)
@JvmOverloads
fun getAppManageSettingsIntent(context: Context, packageName: String = context.packageName) =
        Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                .setData(Uri.parse("package:$packageName"))


@JvmOverloads
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

/**
 * Интент для открытия SAF (Storage Access Framework) пикера файлов. Открывает дефолтный UI для
 * выбора файлов из любого доступного приложения (предоставляющего контент провайдер).
 * Доступ к полученным таким образом файлам постоянный (можно хранить uri для долговременного использования)
 */
@RequiresApi(Build.VERSION_CODES.KITKAT)
fun getOpenDocumentIntent(type: String?, mimeTypes: List<String>?) = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
    addCategory(Intent.CATEGORY_OPENABLE)
    applyMimeTypes(type, mimeTypes)
}

/**
 * Интент для получения файла из стороннего приложения. Как правило открывает экран стороннего
 * приложения для выбора файла (каждое стороннее приложение предоставляет UI для выбора файла).
 * Доступ к полученным таким образом файлам может быть временным (т.е. например сохранять uri полученного
 * таким образом файла для использования в дальнейшем - не лучшая идея)
 */
@RequiresApi(Build.VERSION_CODES.KITKAT)
fun getContentIntent(type: String?, mimeTypes: List<String>?)= Intent(Intent.ACTION_GET_CONTENT).apply {
    applyMimeTypes(type, mimeTypes)
}

@RequiresApi(Build.VERSION_CODES.KITKAT)
private fun Intent.applyMimeTypes(type: String?, mimeTypes: List<String>?) {
    this.type = if (!TextUtils.isEmpty(type)) {
        type
    } else {
        "*/*"
    }
    if (mimeTypes != null && mimeTypes.isNotEmpty()) {
        putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes.toTypedArray())
    }
}

/**
 * @param shouldUseFileProvider true, if intended to use FileProvider instead of file://
 */
@JvmOverloads
fun getViewFileIntent(
        context: Context,
        file: File?,
        shouldUseFileProvider: Boolean = true
): Intent? {
    val uriAndType = getFileUriAndType(context, file, shouldUseFileProvider) ?: return null
    return getViewIntent(uriAndType.first, uriAndType.second).apply {
        if (!shouldUseFileProvider) {
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
    }
}

fun getViewIntent(uri: Uri, mimeType: String): Intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(uri, mimeType)

/**
 * @param shouldUseFileProvider true, if intended to use FileProvider (content://) instead of file://
 */
@JvmOverloads
fun getShareFileIntent(
        context: Context,
        file: File?,
        subject: String,
        text: String,
        shouldUseFileProvider: Boolean = true
): Intent? {
    val uriAndType = getFileUriAndType(context, file, shouldUseFileProvider) ?: return null
    return getShareIntent(subject, text, uriAndType.second, uriAndType.first, isMultiple = false).apply {
        if (!shouldUseFileProvider) {
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
    }
}

@JvmOverloads
fun getShareIntent(
        subject: String,
        text: String,
        mimeType: String,
        uri: Uri? = null,
        recipients: List<String>? = null,
        isMultiple: Boolean = uri == null
): Intent = Intent(
        if (isMultiple) Intent.ACTION_SEND_MULTIPLE else Intent.ACTION_SEND
).apply {
    putExtra(Intent.EXTRA_SUBJECT, subject)
    putExtra(Intent.EXTRA_TEXT, text)
    uri?.let {
        putExtra(Intent.EXTRA_STREAM, uri)
    }
    if (recipients != null && recipients.isNotEmpty()) {
        putExtra(Intent.EXTRA_EMAIL, recipients.toTypedArray())
    }
    type = mimeType
}

private fun getFileUriAndType(
        context: Context,
        file: File?,
        shouldUseFileProvider: Boolean
): Pair<Uri, String>? {
    if (file == null || !isFileExists(file)) {
        return null
    }
    val fileUri = if (shouldUseFileProvider) {
        file.toContentUri(context)
    } else {
        file.toFileUri()
    }
    val ext = file.extension
    val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
    return Pair(fileUri, mimeType ?: EMPTY_STRING)
}