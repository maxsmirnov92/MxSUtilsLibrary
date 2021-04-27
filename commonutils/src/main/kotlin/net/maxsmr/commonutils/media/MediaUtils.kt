package net.maxsmr.commonutils.media

import android.annotation.TargetApi
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.location.Location
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Base64
import android.webkit.MimeTypeMap
import androidx.collection.ArraySet
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import net.maxsmr.commonutils.gui.getCorrectedDisplayRotation
import net.maxsmr.commonutils.isAtLeastKitkat
import net.maxsmr.commonutils.isAtLeastLollipop
import net.maxsmr.commonutils.isAtLeastMarshmallow
import net.maxsmr.commonutils.isAtLeastQ
import net.maxsmr.commonutils.*
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.commonutils.text.isEmpty
import net.maxsmr.commonutils.graphic.canDecodeImage
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder.*
import java.io.*
import java.nio.charset.Charset
import java.util.*
import kotlin.math.abs

const val MIME_TYPE_ANY = "*/*"

const val ENV_SECONDARY_STORAGE = "SECONDARY_STORAGE"
const val ENV_EXTERNAL_STORAGE = "EXTERNAL_STORAGE"

private val logger = BaseLoggerHolder.getInstance().getLogger<BaseLogger>("MediaUtils")

val File.mimeType get() = name.mimeType

val String?.mimeType get() = MimeTypeMap.getSingleton().getMimeTypeFromExtension(this.extension)
        ?: EMPTY_STRING

fun File.isPrivate(context: Context): Boolean {
    val internalFileDir = Environment.getDataDirectory()
    val externalFileDir = context.getExternalFilesDir(null)
    return this.startsWith(internalFileDir) || (externalFileDir != null && this.startsWith(externalFileDir))
}

fun File.toContentUri(context: Context): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.provider", this)

fun File.toFileUri(): Uri = Uri.fromFile(this)

/**
 * @return true, если внешнее хранилище примонтировано
 */
fun isExternalStorageMounted(): Boolean =
        isExternalStorageMountedAndWritable() || isExternalStorageMountedReadOnly()

/**
 * @return true, если внешнее хранилище примонтировано и доступно для записи
 */
fun isExternalStorageMountedAndWritable(): Boolean =
        Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED, ignoreCase = true)

/**
 * @return true, если внешнее хранилище примонтировано и доступно только для чтения
 */
fun isExternalStorageMountedReadOnly(): Boolean =
        Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED_READ_ONLY, ignoreCase = true)

@JvmOverloads
fun getExternalFilesDirs(context: Context, type: String = EMPTY_STRING): Set<File?> {
    var result: Set<File?>? = null
    if (isExternalStorageMounted()) {
        result = if (isAtLeastKitkat()) {
            setOf(*context.getExternalFilesDirs(type))
        } else {
            setOf(context.getExternalFilesDir(type))
        }
    }
    return result ?: emptySet<File>()
}

/**
 * @param type тип для Context.getExternalFilesDirs
 */
@Suppress("DEPRECATION")
@JvmOverloads
fun getFilteredExternalFilesDirs(
        context: Context,
        excludeNotRemovable: Boolean,
        includePrimaryExternalStorage: Boolean,
        onlyRootPaths: Boolean,
        type: String = EMPTY_STRING
): Set<File> {
    val result: MutableSet<File> = ArraySet()

    val rawSecondaryStoragePath: String = System.getenv(ENV_SECONDARY_STORAGE) ?: EMPTY_STRING
    val rawExternalStoragePath: String = System.getenv(ENV_EXTERNAL_STORAGE) ?: EMPTY_STRING
    val primaryExternalStorage: File = Environment.getExternalStorageDirectory()

    fun includeRemovable(path: File) = !isAtLeastLollipop()
            || !excludeNotRemovable || Environment.isExternalStorageRemovable(path)

    /**
     * @return true если данный путь из env не начинается с getExternalStorageDirectory
     */
    fun includePrimary(path: String) = path.isNotEmpty() &&
            (includePrimaryExternalStorage || !path.startsWith(primaryExternalStorage.absolutePath))

    if (isAtLeastKitkat()) {
        val external = getExternalFilesDirs(context, type)
        for (d in external) {
            if (d != null) {
                val path = d.absolutePath
                if (includePrimary(path)) {
                    var secondaryPath = path
                    if (onlyRootPaths) {
                        val index = path.lastIndexOf(File.separator + "Android" /*+ File.separator + "data"*/)
                        if (index > 0) {
                            secondaryPath = path.substring(0, index)
                        }
                    }
                    if (includeRemovable(d)) {
                        result.add(File(secondaryPath))
                    }
                }
            }
        }
    } else {
        // if KITKAT or earlier - use splitted "SECONDARY_STORAGE" environment value as external paths
        // (because of getExternalFilesDir is only possible, not getExternalFilesDirs)
        if (includePrimary(rawSecondaryStoragePath)) {
            result.add(File(rawSecondaryStoragePath))
        }
        if (includePrimary(rawExternalStoragePath)) {
            result.add(File(rawExternalStoragePath))
        }
    }
    return result
}

@TargetApi(Build.VERSION_CODES.Q)
@JvmOverloads
fun File.copyToExternal(
        contentResolver: ContentResolver,
        mimeType: String,
        useRelativePath: Boolean = false
) = try {
    copyToExternalOrThrow(contentResolver, mimeType, useRelativePath)
    true
} catch (e: RuntimeException) {
    logger.e(e)
    false
}

@TargetApi(Build.VERSION_CODES.Q)
@Throws(RuntimeException::class)
@JvmOverloads
fun File.copyToExternalOrThrow(
        contentResolver: ContentResolver,
        mimeType: String? = null,
        useRelativePath: Boolean = false,
        notifier: IStreamNotifier? = null,
        buffSize: Int = DEFAULT_BUFFER_SIZE
) {
    val values = ContentValues().apply {
        if (useRelativePath) {
            parentFile?.name?.let {
                put(MediaStore.Images.Media.RELATIVE_PATH, it)
            }
        }
        put(MediaStore.Images.Media.DISPLAY_NAME, name)
        put(MediaStore.Images.Media.MIME_TYPE,
                if (!mimeType.isNullOrEmpty()) mimeType else this@copyToExternalOrThrow.mimeType)
    }

    val targetUri = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

    contentResolver.insert(targetUri, values)?.let { item ->
        val out = item.openOutputStreamOrThrow(contentResolver)
        try {
            openInputStreamOrThrow().copyStreamOrThrow(out, notifier, buffSize)
        } catch (e: IOException) {
            throwRuntimeException(e, "copyStream")
        }
    } ?: throw RuntimeException("Insert to $targetUri failed")
}

@JvmOverloads
fun scanFiles(
        context: Context,
        files: Collection<File>?,
        onScanCompletedListener: ((String, Uri) -> Unit)? = null
) {
    val filesMap = mutableMapOf<String, String>()
    files?.forEach {
        filesMap[it.absolutePath] = it.mimeType.takeIf { type -> type.isNotEmpty() }
                ?: MIME_TYPE_ANY
    }
    MediaScannerConnection.scanFile(context, filesMap.keys.toTypedArray(), filesMap.values.toTypedArray()) { path, uri ->
        uri?.let {
            onScanCompletedListener?.invoke(path ?: EMPTY_STRING, it)
        }
    }
}

/**
 * Get a file path from a Uri. This will get the the path for Storage Access
 * Framework Documents, as well as the _data field for the MediaStore and
 * other file-based ContentProviders.
 *
 * @param context The context.
 * @author paulburke
 */
@Suppress("DEPRECATION")
@TargetApi(Build.VERSION_CODES.KITKAT)
fun Uri?.getPath(context: Context): String {
    if (this == null) return EMPTY_STRING

    if (isAtLeastKitkat() && DocumentsContract.isDocumentUri(context, this)) {

        // content: and DocumentProvider
        when {
            isExternalStorageDocument(this) -> {
                // ExternalStorageProvider
                val docId = DocumentsContract.getDocumentId(this)
                val split = docId.split(":").toTypedArray()
                val fullPath: String = getPathFromExtSD(split)
                return if (fullPath.isNotEmpty()) {
                    fullPath
                } else {
                    EMPTY_STRING
                }
            }
            isDownloadsDocument(this) -> {
                // DownloadsProvider

                /**
                 * @return убранная подстрока "raw", извлечённая из path
                 * или null при отсутствии изменений
                 */
                fun String.replaceRawInId(): String? {
                    if (startsWith("raw:")) {
                        return replaceFirst("raw:".toRegex(), EMPTY_STRING)
                    }
                    return null
                }

                fun Uri.replaceRawInPath(): String {
                    return path?.replaceFirst("^/document/raw:".toRegex(), EMPTY_STRING)?.replaceFirst("^raw:".toRegex(), EMPTY_STRING)
                            ?: EMPTY_STRING
                }

                if (isAtLeastMarshmallow()) {

                    val fileName = this.name(context.contentResolver)
                    val path = Environment.getExternalStorageDirectory().toString() + "/Download/" + fileName
                    if (!TextUtils.isEmpty(path)) {
                        return path
                    }

                    val documentTextId = DocumentsContract.getDocumentId(this)
                    if (TextUtils.isEmpty(documentTextId)) {
                        return EMPTY_STRING
                    }
                    documentTextId.replaceRawInId()?.let {
                        return it
                    }

                    val documentId: Long = documentTextId.toLongOrNull()
                    //In Android 8 and later the id is not a number
                    // "msf:" == ContentResolver.open
                            ?: return this.replaceRawInPath()

                    val contentUriPrefixesToTry = arrayOf(
                            "content://downloads/public_downloads",
                            "content://downloads/my_downloads"
                    )

                    var result: String = EMPTY_STRING

                    contentUriPrefixesToTry.forEach { contentUriPrefix ->
                        val contentUri = ContentUris.withAppendedId(Uri.parse(contentUriPrefix), documentId)
                        result = contentUri.getDataColumn(context.contentResolver, null, null)
                        if (result.isNotEmpty()) {
                            return@forEach
                        }
                    }
                    return result

                } else {

                    val documentTextId = DocumentsContract.getDocumentId(this)
                    if (TextUtils.isEmpty(documentTextId)) {
                        return EMPTY_STRING
                    }

                    documentTextId.replaceRawInId()?.let {
                        return it
                    }

                    val documentId: Long = documentTextId.toLongOrNull()
                    //In Android 8 and later the id is not a number
                    // "msf:" == ContentResolver.open
                            ?: return this.replaceRawInPath()

                    val contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), documentId)
                    return contentUri.getDataColumn(context.contentResolver)
                }
            }
            isMediaDocument(this) -> {
                // MediaProvider
                val docId = DocumentsContract.getDocumentId(this)
                val split = docId.split(":").toTypedArray()
                val type = split[0]
                var contentUri: Uri? = null
                if ("image" == type) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                } else if ("video" == type) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                } else if ("audio" == type) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }
                return contentUri?.getDataColumn(context.contentResolver, "_id=?", listOf(split[1]))
                        ?: EMPTY_STRING
            }
        }
    } else if (this.isContentScheme()) {
        // Return the remote address
        return if (isGooglePhotosUri(this)) {
            val data = this.lastPathSegment
            data ?: EMPTY_STRING
        } else {
            if (isAtLeastQ()) {
                logger.e("Can't retrieve path because of version >= Q!")
                EMPTY_STRING
            } else {
                this.getDataColumn(context.contentResolver)
            }
        }
    } else if (this.isFileScheme()) {
        val data = this.path
        return data ?: EMPTY_STRING
    }
    return EMPTY_STRING
}

@Suppress("DEPRECATION")
private fun getPathFromExtSD(pathData: Array<String>): String {
    if (pathData.isEmpty()) return EMPTY_STRING
    val type = pathData[0]
    val relativePath = "/" + pathData[1]
    var fullPath = EMPTY_STRING

    // on my Sony devices (4.4.4 & 5.1.1), `type` is a dynamic string
    // something like "71F8-2C0A", some kind of unique id per storage
    // don't know any API that can get the root path of that storage based on its id.
    //
    // so no "primary" type, but let the check here for other devices
    if ("primary".equals(type, ignoreCase = true)) {
        fullPath = Environment.getExternalStorageDirectory().toString() + relativePath
        if (isFileExists(fullPath)) {
            return fullPath
        }
    }

    // Environment.isExternalStorageRemovable() is `true` for external and internal storage
    // so we cannot relay on it.
    //
    // instead, for each possible path, check if file exists
    // we'll start with secondary storage as this could be our (physically) removable sd card
    System.getenv(ENV_SECONDARY_STORAGE)?.let { secondaryPath ->
        fullPath = secondaryPath + relativePath
        if (isFileExists(fullPath)) {
            return fullPath
        }
    }
    System.getenv(ENV_EXTERNAL_STORAGE)?.let { externalPath ->
        fullPath = externalPath + relativePath
    }
    return fullPath
}

/**
 * Get the value of the data column for this Uri. This is useful for
 * MediaStore Uris, and other file-based ContentProviders.
 *
 * @param context       The context.
 * @param uri           The Uri to query.
 * @param selection     (Optional) Filter used in the query.
 * @param selectionArgs (Optional) Selection arguments used in the query.
 * @return The value of the _data column, which is typically a file path.
 */
@JvmOverloads
fun Uri.getDataColumn(
        contentResolver: ContentResolver,
        selection: String? = null,
        selectionArgs: List<String>? = null
): String = queryFirst(contentResolver, String::class.java, MediaStore.Images.ImageColumns.DATA, selection, selectionArgs)
        ?: EMPTY_STRING

/**
 * @param uri The Uri to check.
 * @return Whether the Uri authority is ExternalStorageProvider.
 */
fun isExternalStorageDocument(uri: Uri): Boolean = "com.android.externalstorage.documents" == uri.authority

/**
 * @param uri The Uri to check.
 * @return Whether the Uri authority is DownloadsProvider.
 */
fun isDownloadsDocument(uri: Uri): Boolean = "com.android.providers.downloads.documents" == uri.authority

/**
 * @param uri The Uri to check.
 * @return Whether the Uri authority is MediaProvider.
 */
fun isMediaDocument(uri: Uri): Boolean = "com.android.providers.media.documents" == uri.authority

/**
 * @param uri The Uri to check.
 * @return Whether the Uri authority is Google Photos.
 */
fun isGooglePhotosUri(uri: Uri): Boolean = "com.google.android.apps.photos.content" == uri.authority

fun isWhatsAppFile(uri: Uri): Boolean = "com.whatsapp.provider.media" == uri.authority

fun isGoogleDriveUri(uri: Uri): Boolean = "com.google.android.apps.docs.storage" == uri.authority || "com.google.android.apps.docs.storage.legacy" == uri.authority

fun readExifLocation(imageFile: File?): Location? = try {
    readExifLocationOrThrow(imageFile)
} catch (e: RuntimeException) {
    logger.e(e)
    null
}

@Throws(RuntimeException::class)
fun readExifLocationOrThrow(imageFile: File?): Location {
    if (imageFile == null || !canDecodeImage(imageFile)) {
        throw RuntimeException("Incorrect image file: $imageFile")
    }

    fun getLocationAttr(exif: ExifInterface, attrName: String): Double {
        val value = exif.getAttribute(attrName)
        if (value != null) {
            try {
                return Location.convert(value)
            } catch (e: IllegalArgumentException) {
                throw RuntimeException(formatException(e, "convert"), e)
            }
        }
        return 0.0
    }

    val exif = createExifOrThrow(imageFile)
    try {
        val provider = exif.getAttribute(ExifInterface.TAG_GPS_PROCESSING_METHOD)
        val result = Location(provider)
        val latitude = getLocationAttr(exif, ExifInterface.TAG_GPS_LATITUDE)
        val longitude = getLocationAttr(exif, ExifInterface.TAG_GPS_LONGITUDE)
        val altitude = getLocationAttr(exif, ExifInterface.TAG_GPS_ALTITUDE)
        var timestamp = 0L
        exif.getAttribute(ExifInterface.TAG_GPS_TIMESTAMP)?.let { value ->
            value.toLongOrNull()?.let {
                timestamp = it
            }
        }
        result.latitude = latitude
        result.longitude = longitude
        result.altitude = altitude
        result.time = timestamp
        return result
    } catch (e: Exception) {
        throw RuntimeException(formatException(e))
    }
}

fun writeExifLocation(imageFile: File?, location: Location?) = try {
    writeExifLocationOrThrow(imageFile, location)
    true
} catch (e: RuntimeException) {
    logger.e(e)
    false
}

@Throws(RuntimeException::class)
fun writeExifLocationOrThrow(imageFile: File?, location: Location?) {
    if (imageFile == null || !canDecodeImage(imageFile)) {
        throw RuntimeException("Incorrect image file: $imageFile")
    }
    if (location == null) {
        throw NullPointerException("location is null")
    }
    val exif = createExifOrThrow(imageFile)
    try {
        val latitude = location.latitude
        exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, convertLocationDoubleToString(latitude))
        exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, if (latitude > 0) "N" else "S")
        val longitude = location.longitude
        exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, convertLocationDoubleToString(longitude))
        exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, if (longitude > 0) "E" else "W")
        exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE, convertLocationDoubleToString(location.altitude))
        exif.setAttribute(ExifInterface.TAG_GPS_TIMESTAMP, location.time.toString())
        location.provider?.let {
            exif.setAttribute(ExifInterface.TAG_GPS_PROCESSING_METHOD, it)
        }
        exif.saveAttributes()
    } catch (e: Exception) {
        throwRuntimeException(e)
    }
}

fun convertLocationDoubleToString(value: Double): String {
    var result: String = EMPTY_STRING
    val aValue = abs(value)
    val dms = Location.convert(aValue, Location.FORMAT_SECONDS)
    val splits = dms.split(":").toTypedArray()
    if (splits.size >= 3) {
        val seconds = splits[2].split("\\.").toTypedArray()
        val secondsStr: String
        secondsStr = if (seconds.isEmpty()) {
            splits[2]
        } else {
            seconds[0]
        }
        result = splits[0] + "/1," + splits[1] + "/1," + secondsStr + "/1"
    }
    return result
}

fun File.getImageRotationAngle(): Int = try {
    getImageRotationAngleOrThrow()
} catch (e: RuntimeException) {
    logger.e(e)
    ExifInterface.ORIENTATION_UNDEFINED
}

@Throws(RuntimeException::class)
fun File.getImageRotationAngleOrThrow(): Int =
        createExifOrThrow(this).getRotationAngleOrThrow()

fun Uri.getImageRotationAngle(contentResolver: ContentResolver): Int? =
        try {
            getImageRotationAngleOrThrow(contentResolver)
        } catch (e: RuntimeException) {
            logger.e(e)
            null
        }

@Throws(RuntimeException::class)
fun Uri.getImageRotationAngleOrThrow(contentResolver: ContentResolver): Int =
        if (isAtLeastQ()) {
            getOrientationFromMediaStoreOrThrow(contentResolver)
        } else {
            val inputStream = openInputStreamOrThrow(contentResolver)
            createExifOrThrow(inputStream).getRotationAngleOrThrow()
        }

/**
 * Определяет поворот картинки
 */
@TargetApi(Build.VERSION_CODES.Q)
fun Uri.getOrientationFromMediaStore(contentResolver: ContentResolver): Int? = try {
    getOrientationFromMediaStoreOrThrow(contentResolver)
} catch (e: RuntimeException) {
    logger.e(e)
    null
}

@TargetApi(Build.VERSION_CODES.Q)
@Throws(RuntimeException::class)
fun Uri.getOrientationFromMediaStoreOrThrow(contentResolver: ContentResolver): Int =
        queryFirstOrThrow(contentResolver,
                Int::class.java,
                MediaStore.Images.ImageColumns.ORIENTATION)

fun getRotationAngleFromExif(imageFile: File?): Int = try {
    getRotationAngleFromExifOrThrow(imageFile)
} catch (e: RuntimeException) {
    logger.e(e)
    0
}

@Throws(RuntimeException::class)
fun getRotationAngleFromExifOrThrow(imageFile: File?): Int {
    if (imageFile == null || !canDecodeImage(imageFile)) {
        throw RuntimeException("Incorrect image file: $imageFile")
    }
    try {
        val exif = createExifOrThrow(imageFile)
        return exif.getRotationAngleOrThrow()
    } catch (e: Exception) {
        throw RuntimeException(formatException(e))
    }
}

fun getRotationAngleFromExif(contentResolver: ContentResolver, imageUri: Uri?): Int? = try {
    getRotationAngleFromExifOrThrow(contentResolver, imageUri)
} catch (e: RuntimeException) {
    logger.e(e)
    null
}

@Throws(RuntimeException::class)
fun getRotationAngleFromExifOrThrow(contentResolver: ContentResolver, imageUri: Uri?): Int {
    if (imageUri == null || !canDecodeImage(contentResolver, imageUri)) {
        throw RuntimeException("Incorrect image uri: $imageUri")
    }
    try {
        val exif = createExifOrThrow(imageUri.openInputStreamOrThrow(contentResolver))
        return exif.getRotationAngleOrThrow()
    } catch (e: Exception) {
        throw RuntimeException(formatException(e))
    }
}

@Throws(RuntimeException::class)
private fun ExifInterface.getRotationAngleOrThrow(): Int {
    val orientation = getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    if (orientation == ExifInterface.ORIENTATION_UNDEFINED)
        throw RuntimeException("Orientation undefined")
    return getRotationAngleByExifOrientation(orientation)
}

fun writeRotationAngleToExif(imageFile: File, degrees: Int) = try {
    writeRotationAngleToExifOrThrow(imageFile, degrees)
    true
} catch (e: RuntimeException) {
    logger.e(e)
    false
}

@Throws(RuntimeException::class)
fun writeRotationAngleToExifOrThrow(imageFile: File, degrees: Int) {
    if (!canDecodeImage(imageFile)) {
        throw RuntimeException("Incorrect image file: $imageFile")
    }
    if (degrees < 0) {
        throw RuntimeException("Incorrect angle: $degrees")
    }
    val exif = createExifOrThrow(imageFile)
    try {
        exif.setAttribute(ExifInterface.TAG_ORIENTATION, getExifOrientationByRotationAngle(degrees).toString())
        exif.saveAttributes()
    } catch (e: Exception) {
        throwRuntimeException(e)
    }
}

fun copyDefaultExifInfo(sourceFile: File?, targetFile: File?) = try {
    copyDefaultExifInfoOrThrow(sourceFile, targetFile)
    true
} catch (e: RuntimeException) {
    logger.e(e)
    false
}

/**
 * Копирование exif-информации из одного файла в другой
 *
 * @param sourcePath исходный файл с exif-информацией
 * @param targetPath файл-получатель
 */
@Throws(RuntimeException::class)
fun copyDefaultExifInfoOrThrow(sourceFile: File?, targetFile: File?) {
    val attributes = arrayOf(
            ExifInterface.TAG_DATETIME,
            ExifInterface.TAG_EXPOSURE_TIME,
            ExifInterface.TAG_FLASH,
            ExifInterface.TAG_FOCAL_LENGTH,
            ExifInterface.TAG_GPS_ALTITUDE,
            ExifInterface.TAG_GPS_ALTITUDE_REF,
            ExifInterface.TAG_GPS_DATESTAMP,
            ExifInterface.TAG_GPS_LATITUDE,
            ExifInterface.TAG_GPS_LATITUDE_REF,
            ExifInterface.TAG_GPS_LONGITUDE,
            ExifInterface.TAG_GPS_LONGITUDE_REF,
            ExifInterface.TAG_GPS_PROCESSING_METHOD,
            ExifInterface.TAG_GPS_TIMESTAMP,
            ExifInterface.TAG_MAKE,
            ExifInterface.TAG_MODEL,
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.TAG_WHITE_BALANCE
    )
    copyExifInfoOrThrow(sourceFile, targetFile, Arrays.asList(*attributes))
}

fun copyExifInfo(
        sourceFile: File?,
        targetFile: File?,
        attributeNames: Collection<String>?
) = try {
    copyExifInfoOrThrow(sourceFile, targetFile, attributeNames)
    true
} catch (e: RuntimeException) {
    logger.e(e)
    false
}

@Throws(RuntimeException::class)
fun copyExifInfoOrThrow(
        sourceFile: File?,
        targetFile: File?,
        attributeNames: Collection<String>?
) {
    val oldExif: ExifInterface = createExifOrThrow(sourceFile)
    val newExif: ExifInterface = createExifOrThrow(targetFile)
    try {
        if (attributeNames != null) {
            for (attr in attributeNames) {
                if (!isEmpty(attr)) {
                    val value = oldExif.getAttribute(attr)
                    if (value != null) {
                        newExif.setAttribute(attr, value)
                    }
                }
            }
        }
        newExif.saveAttributes()
    } catch (e: IOException) {
        throwRuntimeException(e)
    }
}

/**
 * Определяем угол для поворота http://sylvana.net/jpegcrop/exif_orientation.html
 */
fun getRotationAngleByExifOrientation(orientation: Int): Int {
    return when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90
        ExifInterface.ORIENTATION_ROTATE_180 -> 180
        ExifInterface.ORIENTATION_ROTATE_270 -> 270
        else -> ExifInterface.ORIENTATION_UNDEFINED
    }
}

fun getExifOrientationByRotationAngle(degrees: Int): Int {
    var orientation = getCorrectedDisplayRotation(degrees)
    orientation = when (orientation) {
        90 -> ExifInterface.ORIENTATION_ROTATE_90
        180 -> ExifInterface.ORIENTATION_ROTATE_180
        270 -> ExifInterface.ORIENTATION_ROTATE_270
        else -> ExifInterface.ORIENTATION_NORMAL
    }
    return orientation
}

@JvmOverloads
fun String?.toBase64(charset: Charset = Charset.defaultCharset(), flags: Int = Base64.DEFAULT): String = this?.toByteArray(charset).toBase64(flags)

/**
 * Получение бинартых данных файла в формате Base64
 *
 * @param media - массив байт
 * @return - строка в формате base64
 */
@JvmOverloads
fun ByteArray?.toBase64(flags: Int = Base64.DEFAULT): String =
        this?.let { Base64.encodeToString(this, flags) } ?: EMPTY_STRING

@Throws(RuntimeException::class)
private fun createExifOrThrow(file: File?) =
        createExifOrThrow(file?.absolutePath)

@Throws(RuntimeException::class)
private fun createExifOrThrow(path: String?): ExifInterface {
    if (path == null || path.isEmpty()) {
        throw NullPointerException("path is null or empty")
    }
    return try {
        ExifInterface(path)
    } catch (e: Exception) {
        throw RuntimeException(formatException(e, "create ExifInterface"), e)
    }
}

@Throws(RuntimeException::class)
private fun createExifOrThrow(stream: InputStream?): ExifInterface {
    if (stream == null) {
        throw NullPointerException("path is null")
    }
    return try {
        ExifInterface(stream)
    } catch (e: Exception) {
        throw RuntimeException(formatException(e, "create ExifInterface"), e)
    }
}