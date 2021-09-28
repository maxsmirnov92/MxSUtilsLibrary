package net.maxsmr.commonutils

import android.annotation.TargetApi
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import androidx.annotation.RequiresApi
import net.maxsmr.commonutils.media.*
import net.maxsmr.commonutils.text.EMPTY_STRING
import java.io.File
import java.util.*

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
fun getManageSettingsIntent(context: Context, packageName: String = context.packageName) =
    Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
        .setData(Uri.parse("package:$packageName"))

fun getLocationSettingsIntent() = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)

fun getBrowseLinkIntent(url: String): Intent =
    getViewIntent(Uri.parse(url), mimeType = getMimeTypeFromUrl(url))

fun getBrowseLocationIntent(latitude: Double, longitude: Double) = getViewIntent(
    Uri.parse("geo:%1f,%2f".format(latitude, longitude))
).apply {
    setPackage("com.google.android.apps.maps")
}

fun getGooglePaySaveUri(jwt: String) =
    getViewIntent(Uri.parse("https://pay.google.com/gp/v/save/$jwt"))

/**
 * Интент для открытия SAF (Storage Access Framework) пикера файлов. Открывает дефолтный UI для
 * выбора файлов из любого доступного приложения (предоставляющего контент провайдер).
 * Доступ к полученным таким образом файлам постоянный (можно хранить uri для долговременного использования)
 */
@RequiresApi(Build.VERSION_CODES.KITKAT)
fun getOpenDocumentIntent(mimeType: String, mimeTypes: List<String>) =
    Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        applyMimeTypes(mimeType, mimeTypes)
    }

/**
 * Интент для получения файла из стороннего приложения. Как правило открывает экран стороннего
 * приложения для выбора файла (каждое стороннее приложение предоставляет UI для выбора файла).
 * Доступ к полученным таким образом файлам может быть временным (т.е. например сохранять uri полученного
 * таким образом файла для использования в дальнейшем - не лучшая идея)
 */
@RequiresApi(Build.VERSION_CODES.KITKAT)
fun getContentIntent(type: String, mimeTypes: List<String>) =
    Intent(Intent.ACTION_GET_CONTENT).apply {
        applyMimeTypes(type, mimeTypes)
    }

/**
 * @param shouldUseFileProvider true, if intended to use FileProvider instead of file://
 * (must be declared in manifest)
 */
@JvmOverloads
fun getViewFileIntent(
    context: Context,
    file: File,
    shouldUseFileProvider: Boolean = true
): Intent? {
    val uriAndType = getFileUriAndType(context, file, shouldUseFileProvider) ?: return null
    return getViewIntent(uriAndType.first, context.contentResolver, uriAndType.second).apply {
        if (!shouldUseFileProvider) {
            wrapIntent(this, flags = Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}

fun getViewIntent(
    uri: Uri,
    contentResolver: ContentResolver? = null,
    mimeType: String = EMPTY_STRING,
    mimeTypes: List<String> = emptyList()
): Intent = Intent(Intent.ACTION_VIEW).apply {
    applyDataAndMimeTypes(uri, contentResolver, mimeType, mimeTypes)
}

/**
 * @param shouldUseFileProvider true, if intended to use FileProvider (content://) instead of file://
 * (must be declared in manifest)
 */
@JvmOverloads
fun getShareFileIntent(
    context: Context,
    file: File?,
    subject: String,
    text: String,
    recipients: List<String> = emptyList(),
    shouldUseFileProvider: Boolean = true
): Intent {
    val uriAndType = getFileUriAndType(context, file, shouldUseFileProvider)
    return getShareIntent(
        uriAndType?.first,
        subject,
        text,
        context.contentResolver,
        uriAndType?.second ?: EMPTY_STRING,
        recipients = recipients,
        isMultiple = false
    ).apply {
        if (!shouldUseFileProvider) {
            wrapIntent(this, flags = Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}

@JvmOverloads
fun getShareIntent(
    uri: Uri?,
    subject: String,
    text: String,
    contentResolver: ContentResolver? = null,
    mimeType: String = EMPTY_STRING,
    mimeTypes: List<String> = emptyList(),
    recipients: List<String> = emptyList(),
    isMultiple: Boolean = uri == null
): Intent = Intent(
    if (isMultiple) Intent.ACTION_SEND_MULTIPLE else Intent.ACTION_SEND
).apply {
    putExtra(Intent.EXTRA_SUBJECT, subject)
    putExtra(Intent.EXTRA_TEXT, text)
    uri?.let {
        putExtra(Intent.EXTRA_STREAM, uri)
    }
    if (recipients.isNotEmpty()) {
        putExtra(Intent.EXTRA_EMAIL, recipients.toTypedArray())
    }
    applyDataAndMimeTypes(uri, contentResolver, mimeType, mimeTypes)
}

private fun Intent.applyMimeTypes(type: String, types: List<String>) {
    this.type = if (!TextUtils.isEmpty(type)) {
        type
    } else {
        MIME_TYPE_ANY
    }
    if (isAtLeastKitkat() && types.isNotEmpty()) {
        putExtra(Intent.EXTRA_MIME_TYPES, types.toTypedArray())
    }
}

/**
 * @param contentResolver при наличии и отсутствии известных [mimeType]/[mimeTypes]
 * будет использован для определения
 */
private fun Intent.applyDataAndMimeTypes(
    uri: Uri?,
    contentResolver: ContentResolver?,
    type: String,
    types: List<String>
) {
    if (uri != null && contentResolver != null && type.isEmpty() && types.isEmpty()) {
        applyMimeTypes(uri.mimeType(contentResolver), emptyList())
    } else {
        applyMimeTypes(type, types)
    }
    data = uri
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
    return Pair(fileUri, getMimeTypeFromExtension(file.extension))
}