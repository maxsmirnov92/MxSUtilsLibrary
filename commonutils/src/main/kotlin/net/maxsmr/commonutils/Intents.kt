package net.maxsmr.commonutils

import android.annotation.TargetApi
import android.content.ComponentName
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Browser
import android.provider.Settings
import android.text.TextUtils
import androidx.annotation.RequiresApi
import net.maxsmr.commonutils.SendAction.*
import net.maxsmr.commonutils.media.*
import net.maxsmr.commonutils.text.EMPTY_STRING
import java.io.File
import java.io.Serializable

const val URL_ANY_MARKET_FORMAT = "market://details?id=%s"
const val URL_PLAY_MARKET_FORMAT = "https://play.google.com/store/apps/details?id=%s"
const val ACTION_HUAWEI_MARKET = "com.huawei.appmarket.intent.action.AppDetail"

const val URL_GOOGLE_PAY_SAVE_FORMAT = "https://pay.google.com/gp/v/save/%s"

const val URL_SCHEME_MAIL = "mailto"

@JvmOverloads
fun getAppSettingsIntent(context: Context, packageName: String = context.packageName): Intent =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        .setData(Uri.parse("package:$packageName"))
// для MIUI:
// setClassName("com.miui.securitycenter", "com.miui.appmanager.ApplicationsDetailsActivity")
// putExtra("package_name", fragment.requireContext().packageName)

@TargetApi(Build.VERSION_CODES.M)
@JvmOverloads
fun getManageSettingsIntent(context: Context, packageName: String = context.packageName) =
    Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
        .setData(Uri.parse("package:$packageName"))

fun getLocationSettingsIntent() = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)

fun getWifiSettingsIntent() = Intent(Settings.ACTION_WIFI_SETTINGS)

@TargetApi(Build.VERSION_CODES.M)
fun getIgnoreBatteryOptimizationsIntent(context: Context): Intent? {
    val packageName = context.packageName
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager? ?: return null
    if (!pm.isIgnoringBatteryOptimizations(packageName)) {
        return Intent().apply {
            action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
            data = Uri.parse("package:$packageName")
        }
    }
    return null
}

fun getBrowseLocationIntent(latitude: Double, longitude: Double) = getViewIntent(
    Uri.parse("geo:%1f,%2f".format(latitude, longitude))
).apply {
    setPackage("com.google.android.apps.maps")
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
    getViewIntent(Uri.parse(URL_GOOGLE_PAY_SAVE_FORMAT.format(jwt)))

/**
 * Интент для открытия SAF (Storage Access Framework) пикера файлов. Открывает дефолтный UI для
 * выбора файлов из любого доступного приложения (предоставляющего контент провайдер).
 * Доступ к полученным таким образом файлам постоянный (можно хранить uri для долговременного использования)
 */
@RequiresApi(Build.VERSION_CODES.KITKAT)
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
@RequiresApi(Build.VERSION_CODES.KITKAT)
fun getContentIntent(intentType: String?, mimeTypes: List<String>?) =
    Intent(Intent.ACTION_GET_CONTENT).apply {
        applyMimeTypes(intentType, mimeTypes)
    }

@JvmOverloads
fun getShareIntent(
    uri: Uri?,
    subject: String?,
    text: String?,
    contentResolver: ContentResolver? = null,
    intentType: String? = EMPTY_STRING,
    mimeTypes: List<String> = emptyList(),
    recipients: List<String> = emptyList(),
    sendAction: SendAction = SEND
): Intent = getSendIntent(sendAction).apply {
    subject?.let {
        putExtra(Intent.EXTRA_SUBJECT, subject)
    }
    text?.let {
        putExtra(Intent.EXTRA_TEXT, text)
    }
    uri?.let {
        putExtra(Intent.EXTRA_STREAM, uri)
    }
    if (recipients.isNotEmpty()) {
        putExtra(Intent.EXTRA_EMAIL, recipients.toTypedArray())
    }
    applyDataAndMimeTypes(uri, contentResolver, intentType, mimeTypes)
}

@JvmOverloads
fun getViewUrlIntent(
    url: String,
    mimeType: String? = getMimeTypeFromUrl(url),
    context: Context? = null
) = getViewUrlIntent(Uri.parse(url), mimeType, context)

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
    context: Context,
    file: File,
    shouldUseFileProvider: Boolean = true
): Intent? {
    val uriAndType = getFileUriAndType(context, file, shouldUseFileProvider) ?: return null
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
        sendAction = SEND
    ).apply {
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}

fun getSendEmailUri(email: String?): Uri {
    return if (!email.isNullOrEmpty()) {
        Uri.fromParts(URL_SCHEME_MAIL, email, null)
    } else {
        Uri.parse("$URL_SCHEME_MAIL:")
    }
}

@JvmOverloads
fun getSendEmailIntent(email: String?, sendAction: SendAction = SENDTO): Intent? {
    return getSendEmailIntent(getSendEmailUri(email), sendAction)
}

@JvmOverloads
fun getSendEmailIntent(
    uri: Uri,
    sendAction: SendAction = SENDTO,
    addresses: List<String>? = null
): Intent? {
    if (URL_SCHEME_MAIL != uri.scheme) {
        return null
    }
    return getSendIntent(sendAction).apply {
        data = uri
        addresses?.let {
            putExtra(Intent.EXTRA_EMAIL, it.toTypedArray())
        }
    }
}

@JvmOverloads
fun getSendTextIntent(text: CharSequence, sendAction: SendAction = SEND) =
    getSendIntent(sendAction).apply {
        putExtra(Intent.EXTRA_TEXT, text)
        type = "text/plain"
    }

fun getSendIntent(sendAction: SendAction) = Intent(
    when (sendAction) {
        SEND_MULTIPLE -> Intent.ACTION_SEND_MULTIPLE
        SENDTO -> Intent.ACTION_SENDTO
        else -> Intent.ACTION_SEND
    }
)

@JvmOverloads
fun Intent.wrapChooserWithInitial(
    context: Context,
    title: String?,
    intentSender: IntentSender? = null,
): Intent = wrapChooser(
    title,
    intentSender,
    flatten(context)
)

@JvmOverloads
fun Intent.wrapChooser(
    title: String?,
    intentSender: IntentSender? = null,
    initialIntents: List<Intent>? = null
): Intent {
    return if (!title.isNullOrEmpty()) {
        (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1 && intentSender != null) {
            Intent.createChooser(this, title, intentSender)
        } else {
            Intent.createChooser(this, title)
        }).apply {
            initialIntents?.takeIf { it.isNotEmpty() }?.let {
                putExtra(Intent.EXTRA_INITIAL_INTENTS, ArrayList(it))
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
        .map { Intent(this).apply {
            `package` = it.activityInfo.packageName
            component = ComponentName(it.activityInfo.packageName, it.activityInfo.name)
        }
    }
}

fun <T : Serializable> Intent.getSerializableExtraCompat(name: String, clazz: Class<T>): T? {
    return if (isAtLeastTiramisu()) {
        getSerializableExtra(name, clazz)
    } else {
        getSerializableExtra(name) as? T
    }
}

fun <T : Serializable> Bundle.getSerializableCompat(name: String, clazz: Class<T>): T? {
    return if (isAtLeastTiramisu()) {
        getSerializable(name, clazz)
    } else {
        getSerializable(name) as? T
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
    uri: Uri?,
    contentResolver: ContentResolver?,
    intentType: String?,
    mimeTypes: List<String>
) {
    if (uri != null && contentResolver != null && intentType.isNullOrEmpty() && mimeTypes.isEmpty()) {
        applyMimeTypes(uri.mimeType(contentResolver), emptyList())
    } else {
        applyMimeTypes(intentType, mimeTypes)
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

enum class SendAction {
    SEND_MULTIPLE, SENDTO, SEND
}