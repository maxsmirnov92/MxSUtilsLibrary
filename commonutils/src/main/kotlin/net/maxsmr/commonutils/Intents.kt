package net.maxsmr.commonutils

import android.content.ComponentName
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.graphics.PointF
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Browser
import android.provider.Settings
import android.text.TextUtils
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import net.maxsmr.commonutils.media.MIME_TYPE_ANY
import net.maxsmr.commonutils.media.getMimeTypeFromExtension
import net.maxsmr.commonutils.media.getMimeTypeFromUrl
import net.maxsmr.commonutils.media.mimeType
import net.maxsmr.commonutils.media.toContentUri
import net.maxsmr.commonutils.media.toFileUri
import net.maxsmr.commonutils.text.EMPTY_STRING
import java.io.File

const val URL_SCHEME_MARKET = "market"
const val URL_SCHEME_MAIL = "mailto"
const val URL_SCHEME_TEL = "tel"
const val URL_SCHEME_SMS = "sms"
const val URL_SCHEME_GEO = "geo"
const val URL_SCHEME_GEO_GOOGLE = "google.navigation"
const val URL_SCHEME_INTENT = "intent"

const val URL_ANY_MARKET_FORMAT = "$URL_SCHEME_MARKET://details?id=%s"
const val URL_PLAY_MARKET_FORMAT = "https://play.google.com/store/apps/details?id=%s"
const val ACTION_HUAWEI_MARKET = "com.huawei.appmarket.intent.action.AppDetail"

const val URL_GOOGLE_PAY_SAVE_FORMAT = "https://pay.google.com/gp/v/save/%s"

@JvmOverloads
fun getAppSettingsIntent(context: Context, packageName: String = context.packageName): Intent =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        .setData("package:$packageName".toUri())

// для MIUI:
// setClassName("com.miui.securitycenter", "com.miui.appmanager.ApplicationsDetailsActivity")
// putExtra("package_name", fragment.requireContext().packageName)

@RequiresApi(Build.VERSION_CODES.M)
@JvmOverloads
fun getManageWriteSettingsIntent(context: Context, packageName: String = context.packageName) =
    Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
        .setData("package:$packageName".toUri())

@RequiresApi(Build.VERSION_CODES.M)
@JvmOverloads
fun getManageOverlayPermissionIntent(context: Context, packageName: String = context.packageName): Intent? {
    if (!Settings.canDrawOverlays(context)) {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
        intent.setData(Uri.fromParts("package", packageName, null))
        return intent
    }
    return null
}

fun getLocationSettingsIntent() = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)

fun getWifiSettingsIntent() = Intent(Settings.ACTION_WIFI_SETTINGS)

@RequiresApi(Build.VERSION_CODES.M)
fun getIgnoreBatteryOptimizationsIntent(context: Context): Intent? {
    val packageName = context.packageName
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager? ?: return null
    if (!pm.isIgnoringBatteryOptimizations(packageName)) {
        return Intent().apply {
            action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
            data = "package:$packageName".toUri()
        }
    }
    return null
}

fun getAnyMarketIntent(uri: Uri): Intent? {
    if (!URL_SCHEME_MARKET.equals(uri.scheme, true)) {
        return null
    }
    return getViewUrlIntent(uri, null)
}

fun getAnyMarketIntent(appId: String): Intent =
    getViewUrlIntent(URL_ANY_MARKET_FORMAT.format(appId), null)

fun getPlayMarketIntent(appId: String): Intent =
    getViewUrlIntent(URL_PLAY_MARKET_FORMAT.format(appId), null)

fun getHuaweiMarketIntent(appId: String): Intent =
    Intent(ACTION_HUAWEI_MARKET).apply {
        setPackage("com.huawei.appmarket")
        putExtra("APP_PACKAGENAME", appId)
    }

fun getGooglePaySaveUri(jwt: String) =
    getViewIntent(URL_GOOGLE_PAY_SAVE_FORMAT.format(jwt).toUri())

/**
 * Интент для открытия SAF (Storage Access Framework) пикера файлов. Открывает дефолтный UI для
 * выбора файлов из любого доступного приложения (предоставляющего контент провайдер).
 * Доступ к полученным таким образом файлам постоянный (можно хранить uri для долговременного использования)
 */
fun getOpenDocumentIntent(mimeType: String?, mimeTypes: List<String>?): Intent =
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
fun getContentIntent(intentType: String?, mimeTypes: List<String>?) =
    Intent(Intent.ACTION_GET_CONTENT).apply {
        applyMimeTypes(intentType, mimeTypes)
    }

/**
 * @param shouldUseFileProvider true, if intended to use FileProvider (content://) instead of file://
 * (must be declared in manifest)
 */
@JvmOverloads
fun getShareFileIntent(
    context: Context,
    shouldUseFileProvider: Boolean = true,
    vararg files: File,
): Intent {
    return getShareIntent(
        context.contentResolver,
        emptyList(),
        *files.map { getFileUri(context, it, shouldUseFileProvider) }.toTypedArray(),
    ).apply {
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}

@JvmOverloads
fun getShareIntent(
    contentResolver: ContentResolver,
    mimeTypes: List<String> = emptyList(),
    vararg uris: Uri,
): Intent = getSendIntent(uris.size > 1).apply {
    if (uris.isNotEmpty()) {
        if (uris.size > 1) {
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris.toList()))
            val types = uris.map { it.mimeType(contentResolver) }
            if (types.all { it == types[0] }) {
                applyDataAndMimeTypes(uris[0], contentResolver, null, mimeTypes)
            } else {
                applyMimeTypes("*/*", mimeTypes)
            }
        } else {
            val uri = uris[0]
            putExtra(Intent.EXTRA_STREAM, uri)
            applyDataAndMimeTypes(uri, contentResolver, null, mimeTypes)
        }
    }
}

fun getSendEmailUri(email: String?): Uri {
    return if (!email.isNullOrEmpty()) {
        Uri.fromParts(URL_SCHEME_MAIL, email, null)
    } else {
        "$URL_SCHEME_MAIL:".toUri()
    }
}

@JvmOverloads
fun getSendEmailIntent(
    email: String?,
    isSendTo: Boolean = !email.isNullOrEmpty(),
    subject: String? = null,
    text: String? = null,
    addresses: List<String>? = null
): Intent? {
    return getSendEmailIntent(getSendEmailUri(email), isSendTo, subject, text, addresses)
}

@JvmOverloads
fun getSendEmailIntent(
    uri: Uri,
    isSendTo: Boolean = true,
    subject: String? = null,
    text: String? = null,
    addresses: List<String>? = null
): Intent? {
    if (!URL_SCHEME_MAIL.equals(uri.scheme, true)) {
        return null
    }
    return Intent(
        if (isSendTo) {
            Intent.ACTION_SEND
        } else {
            Intent.ACTION_SENDTO
        }
    ).apply {
        data = uri
        subject?.takeIf { it.isNotEmpty() }?.let {
            putExtra(Intent.EXTRA_SUBJECT, subject)
        }
        text?.let {
            putExtra(Intent.EXTRA_TEXT, text)
        }
        addresses?.let {
            putExtra(Intent.EXTRA_EMAIL, it.toTypedArray())
        }
    }
}

fun getSendIntent(isMultiple: Boolean) = Intent(
    if (isMultiple) {
        Intent.ACTION_SEND_MULTIPLE
    } else {
        Intent.ACTION_SEND
    }
)

@JvmOverloads
fun getViewUrlIntent(
    url: String,
    mimeType: String? = getMimeTypeFromUrl(url),
    context: Context? = null
) = getViewUrlIntent(url.toUri(), mimeType, context)

@JvmOverloads
fun getViewUrlIntent(
    uri: Uri,
    mimeType: String? = getMimeTypeFromUrl(uri.toString()),
    context: Context? = null
) = getViewIntent().apply {
    setDataAndType(uri, getIntentType(mimeType, null))
    context?.let {
        putExtra(Browser.EXTRA_APPLICATION_ID, context.packageName)
    }
}

/**
 * @param shouldUseFileProvider true, if intended to use FileProvider instead of file://
 * (must be declared in manifest)
 */
@JvmOverloads
fun getViewFileIntent(
    file: File,
    context: Context,
    shouldUseFileProvider: Boolean = true
): Intent? {
    val uriAndType = getFileUriWithType(context, file, shouldUseFileProvider) ?: return null
    return getViewIntent(uriAndType.first, context.contentResolver, uriAndType.second).apply {
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}

fun getViewIntent(
    uri: Uri,
    contentResolver: ContentResolver? = null,
    intentType: String = EMPTY_STRING,
    mimeTypes: List<String> = emptyList()
): Intent = getViewIntent().apply {
    applyDataAndMimeTypes(uri, contentResolver, intentType, mimeTypes)
}

fun getViewIntent() = Intent(Intent.ACTION_VIEW)

fun getDialIntent(uri: Uri): Intent? {
    if (!URL_SCHEME_TEL.equals(uri.scheme, true)) {
        return null
    }
    return Intent(Intent.ACTION_DIAL, uri)
}

@JvmOverloads
fun getViewLocationIntent(
    location: PointF?,
    query: String? = null,
    isGoogle: Boolean = false,
): Intent? {
    if (location == null && !query.isNullOrEmpty()) return null
    val uri = StringBuilder()
    uri.append("${if (isGoogle) URL_SCHEME_GEO_GOOGLE else URL_SCHEME_GEO}:")
    if (location != null) {
        uri.append("${location.x},${location.y}")
    }
    if (!query.isNullOrEmpty()) {
        if (uri.isNotEmpty()) {
            uri.append("?")
        }
        uri.append("q=$query")
    }
    return getViewUrlIntent(uri.toString().toUri(), null).apply {
        if (isGoogle) {
            setPackage("com.google.android.apps.maps")
        }
    }
}

@JvmOverloads
fun Intent.wrapChooserWithInitial(
    context: Context,
    title: String?,
    intentSender: IntentSender? = null,
): Intent = wrapChooser(
    title,
    intentSender,
    *flatten(context).toTypedArray()
)

@JvmOverloads
fun Intent.wrapChooser(
    title: String?,
    intentSender: IntentSender? = null,
    vararg initialIntents: Intent
): Intent {
    return if (!title.isNullOrEmpty()) {
        (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1 && intentSender != null) {
            Intent.createChooser(this, title, intentSender)
        } else {
            Intent.createChooser(this, title)
        }).apply {
            initialIntents.takeIf { it.isNotEmpty() }?.let {
                putExtra(Intent.EXTRA_INITIAL_INTENTS, initialIntents)
            }
        }
    } else {
        this
    }
}

/**
 * Разделяет интент this, под который могут подходить несколько приложений, на список отдельных
 * интентов под каждое приложение;
 * Может быть использован в [Intent.EXTRA_INITIAL_INTENTS];
 * Требует <queries> в манифесте при targetSdkVersion = 30
 */
@JvmOverloads
fun Intent.flatten(
    context: Context,
    flags: Int = 0,
    shouldFilterCaller: Boolean = true
): List<Intent> {
    return context.queryIntentActivitiesCompat(this, flags, shouldFilterCaller)
        .map {
            Intent(this).apply {
                `package` = it.activityInfo.packageName
                component = ComponentName(it.activityInfo.packageName, it.activityInfo.name)
            }
        }
}

private fun getIntentType(intentType: String?, mimeTypes: List<String>?) = when {
    // при заполнении несколькими основной тип не должен оставаться нульным
    intentType == null -> if (!mimeTypes.isNullOrEmpty()) mimeTypes[0] else null
    !TextUtils.isEmpty(intentType) -> intentType
    else -> MIME_TYPE_ANY
}

private fun Intent.applyMimeTypes(intentType: String?, mimeTypes: List<String>?) {
    this.type = getIntentType(intentType, mimeTypes)
    if (!mimeTypes.isNullOrEmpty()) {
        if (isAtLeastKitkat()) {
            putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes.toTypedArray())
        } else {
            this.type = TextUtils.join("|", mimeTypes)
        }
    }
}

/**
 * @param contentResolver при наличии и отсутствии известных [mimeType]/[mimeTypes]
 * будет использован для определения
 */
private fun Intent.applyDataAndMimeTypes(
    uri: Uri,
    contentResolver: ContentResolver?,
    intentType: String?,
    mimeTypes: List<String>
) {
    if (contentResolver != null && intentType.isNullOrEmpty() && mimeTypes.isEmpty()) {
        applyMimeTypes(uri.mimeType(contentResolver), emptyList())
    } else {
        applyMimeTypes(intentType, mimeTypes)
    }
    data = uri
}

private fun getFileUri(
    context: Context,
    file: File,
    shouldUseFileProvider: Boolean
): Uri {
    return if (shouldUseFileProvider) {
        file.toContentUri(context)
    } else {
        file.toFileUri()
    }
}

private fun getFileUriWithType(
    context: Context,
    file: File,
    shouldUseFileProvider: Boolean
): Pair<Uri, String>? {
    return Pair(
        getFileUri(context, file, shouldUseFileProvider),
        getMimeTypeFromExtension(file.extension)
    )
}
