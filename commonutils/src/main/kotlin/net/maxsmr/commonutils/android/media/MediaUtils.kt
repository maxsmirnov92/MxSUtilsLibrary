package net.maxsmr.commonutils.android.media

import android.annotation.TargetApi
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.res.Resources
import android.database.Cursor
import android.location.Location
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.text.TextUtils
import android.util.Base64
import android.webkit.MimeTypeMap
import androidx.collection.ArraySet
import androidx.exifinterface.media.ExifInterface
import net.maxsmr.commonutils.android.gui.getCorrectedDisplayRotation
import net.maxsmr.commonutils.android.isAtLeastKitkat
import net.maxsmr.commonutils.android.isAtLeastLollipop
import net.maxsmr.commonutils.data.*
import net.maxsmr.commonutils.data.text.EMPTY_STRING
import net.maxsmr.commonutils.data.text.isEmpty
import net.maxsmr.commonutils.graphic.GraphicUtils
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder.*
import java.io.*
import java.nio.charset.Charset
import java.util.*
import kotlin.math.abs

const val MIME_TYPE_ANY = "*/*"

private val logger = BaseLoggerHolder.getInstance().getLogger<BaseLogger>("MediaUtils")

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

@JvmOverloads
fun getFilteredExternalFilesDirs(
        context: Context,
        type: String = EMPTY_STRING,
        excludeNotRemovable: Boolean,
        includePrimaryExternalStorage: Boolean,
        onlyRootPaths: Boolean
): Set<File> {
    val result: MutableSet<File> = ArraySet()
    val rawSecondaryStorage = System.getenv("SECONDARY_STORAGE")
    val primaryExternalStorage = Environment.getExternalStorageDirectory()
    if (isAtLeastKitkat()) {
        // can get getExternalFilesDirs by type
        val external = getExternalFilesDirs(context, type)
        for (d in external) {
            if (d != null) {
                val path = d.absolutePath
                if (includePrimaryExternalStorage
                        || primaryExternalStorage == null || !path.startsWith(primaryExternalStorage.absolutePath)) {
                    var secondaryPath = path
                    if (onlyRootPaths) {
                        val index = path.lastIndexOf(File.separator + "Android" /*+ File.separator + "data"*/)
                        if (index > 0) {
                            secondaryPath = path.substring(0, index)
                        }
                    }
                    if (isAtLeastLollipop()
                            && (!excludeNotRemovable || Environment.isExternalStorageRemovable(d))
                            || !isEmpty(rawSecondaryStorage) && rawSecondaryStorage!!.contains(path)) {
                        result.add(File(secondaryPath))
                    }
                }
            }
        }
    } else {
        // if KITKAT or earlier - use splitted "SECONDARY_STORAGE" environment value as external paths
        if (!isEmpty(rawSecondaryStorage)) {
            val rawSecondaryStoragePaths = rawSecondaryStorage!!.split(File.separator).toTypedArray()
            for (secondaryPath in rawSecondaryStoragePaths) {
                if (includePrimaryExternalStorage
                        || primaryExternalStorage == null || !secondaryPath.startsWith(primaryExternalStorage.absolutePath)) {
                    result.add(File(secondaryPath))
                }
            }
        }
    }
    return result
}

fun getMimeTypeFromFile(file: File?): String =
        getMimeTypeFromFile(file?.name ?: EMPTY_STRING)

fun getMimeTypeFromFile(fileName: String?): String =
        MimeTypeMap.getSingleton().getMimeTypeFromExtension(getFileExtension(fileName))
                ?: EMPTY_STRING

@JvmOverloads
fun isResourceExists(
        contentResolver: ContentResolver,
        uri: Uri?,
        checkBySize: Boolean = true
): Boolean = try {
    isResourceExistsOrThrow(contentResolver, uri, checkBySize)
} catch (e: RuntimeException) {
    logger.e(e)
    false
}

@Throws(RuntimeException::class)
@JvmOverloads
fun isResourceExistsOrThrow(
        contentResolver: ContentResolver,
        uri: Uri?,
        checkBySize: Boolean = true
): Boolean {
    if (checkBySize) {
        return getResourceSizeOrThrow(contentResolver, uri) > 0
    }
    if (uri == null) {
        throw NullPointerException("uri is null")
    }
    val path = uri.path ?: throw RuntimeException("uri path is null")
    when {
        uri.isFileScheme() -> {
            return isFileExistsOrThrow(path)
        }
        uri.isContentScheme() -> {
            queryUriOrThrow(contentResolver, uri, checkCursorEmpty = false).count > 0
        }
        else -> {
            throw RuntimeException("Incorrect uri scheme: ${uri.path}")
        }
    }
    return false
}

fun getResourceSize(contentResolver: ContentResolver, uri: Uri?): Long = try {
    getResourceSizeOrThrow(contentResolver, uri)
} catch (e: RuntimeException) {
    logger.e(e)
    0
}

@Throws(RuntimeException::class)
fun getResourceSizeOrThrow(contentResolver: ContentResolver, uri: Uri?): Long {
    if (uri == null) {
        throw NullPointerException("uri is null")
    }
    val path = uri.path ?: throw RuntimeException("uri path is null")
    return when {
        uri.isFileScheme() -> {
            getFileLengthOrThrow(path)
        }
        uri.isContentScheme() -> {
            queryUriFirstOrThrow(
                    contentResolver,
                    uri,
                    Long::class.java,
                    listOf(OpenableColumns.SIZE)
            )
        }
        else -> {
            throw RuntimeException("Incorrect uri scheme: ${uri.path}")
        }
    }
}

fun getResourceName(contentResolver: ContentResolver, uri: Uri?): String = try {
    getResourceNameOrThrow(contentResolver, uri)
} catch (e: RuntimeException) {
    logger.e(e)
    EMPTY_STRING
}

@Throws(RuntimeException::class)
fun getResourceNameOrThrow(contentResolver: ContentResolver, uri: Uri?): String {
    if (uri == null) {
        throw NullPointerException("uri is null")
    }
    val path = uri.path ?: throw RuntimeException("uri path is null")
    return when {
        uri.isFileScheme() -> {
            File(path).name
        }
        uri.isContentScheme() -> {
            queryUriFirstOrThrow(
                    contentResolver,
                    uri,
                    String::class.java,
                    listOf(OpenableColumns.DISPLAY_NAME)
            )
        }
        else -> {
            throw RuntimeException("Incorrect uri scheme: ${uri.path}")
        }
    }
}

fun deleteResource(
        contentResolver: ContentResolver,
        uri: Uri?,
        throwIfNotExists: Boolean = false
) = try {
    deleteResourceOrThrow(contentResolver, uri, throwIfNotExists)
    true
} catch (e: RuntimeException) {
    logger.e(e)
    false
}

@Throws(RuntimeException::class)
@JvmOverloads
fun deleteResourceOrThrow(
        contentResolver: ContentResolver,
        uri: Uri?,
        throwIfNotExists: Boolean = false
) {
    if (uri == null) {
        throw NullPointerException("uri is null")
    }
    if (!isResourceExistsOrThrow(contentResolver, uri, false)) {
        if (throwIfNotExists) {
            throw Resources.NotFoundException("Resource $uri not found")
        }
        return
    }
    when {
        uri.isFileScheme() -> {
            deleteFileOrThrow(File(uri.path as String))
        }
        uri.isContentScheme() -> {
            contentResolver.delete(uri, null, null)
        }
        else -> {
            throw RuntimeException("Incorrect uri scheme: ${uri.path}")
        }
    }
}

fun getInputStreamFromResource(context: Context, uri: Uri?): InputStream? = try {
    getInputStreamFromResourceOrThrow(context, uri)
} catch (e: RuntimeException) {
    logger.e(e)
    null
}

@Throws(RuntimeException::class)
fun getInputStreamFromResourceOrThrow(context: Context, uri: Uri?): InputStream {
    if (uri == null) {
        throw NullPointerException("uri is null")
    }
    val path = uri.path ?: throw RuntimeException("uri path is null")
    return when {
        uri.isFileScheme() -> {
            File(path).toFisOrThrow()
        }
        uri.isContentScheme() -> {
            uri.openInputStreamOrThrow(context.contentResolver)
        }
        else -> throw RuntimeException("Incorrect uri scheme: ${uri.scheme}")
    }
}

@TargetApi(Build.VERSION_CODES.Q)
@JvmOverloads
fun copyToExternal(
        contentResolver: ContentResolver,
        file: File,
        mimeType: String,
        useRelativePath: Boolean = false
) = try {
    copyToExternalOrThrow(contentResolver, file, mimeType, useRelativePath)
    true
} catch (e: RuntimeException) {
    logger.e(e)
    false
}

@TargetApi(Build.VERSION_CODES.Q)
@JvmOverloads
@Throws(RuntimeException::class)
fun copyToExternalOrThrow(
        contentResolver: ContentResolver,
        file: File?,
        mimeType: String? = null,
        useRelativePath: Boolean = false,
        notifier: IStreamNotifier? = null,
        buffSize: Int = DEFAULT_BUFFER_SIZE
) {
    if (file == null) {
        throw NullPointerException("file is null")
    }
    val values = ContentValues().apply {
        if (useRelativePath) {
            file.parentFile?.name?.let {
                put(MediaStore.Images.Media.RELATIVE_PATH, it)
            }
        }
        put(MediaStore.Images.Media.DISPLAY_NAME, file.name)
        put(MediaStore.Images.Media.MIME_TYPE,
                if (!mimeType.isNullOrEmpty()) mimeType else getMimeTypeFromFile(file))
    }

    val targetUri = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

    contentResolver.insert(targetUri, values)?.let { item ->
        val out = item.openOutputStreamOrThrow(contentResolver)
        try {
            copyStreamOrThrow(file.toFisOrThrow(), out, notifier, buffSize)
        } catch (e: IOException) {
            throwRuntimeException(e, "copyStream")
        }
    } ?: throw RuntimeException("Insert to $targetUri failed")
}

fun scanFile(
        context: Context,
        file: File?,
        onScanCompletedListener: ((String, Uri) -> Unit)? = null
) = scanFiles(context, if (file != null) listOf(file) else null, onScanCompletedListener)

@JvmOverloads
fun scanFiles(
        context: Context,
        files: Collection<File>?,
        onScanCompletedListener: ((String, Uri) -> Unit)? = null
) {
    val filesMap = mutableMapOf<String, String>()
    files?.forEach {
        filesMap[it.absolutePath] = getMimeTypeFromFile(it).takeIf { type -> type.isNotEmpty() }
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
 * @param uri     The Uri to query.
 * @author paulburke
 */
@TargetApi(Build.VERSION_CODES.KITKAT)
fun getPath(context: Context, uri: Uri?): String {
    if (uri == null) return EMPTY_STRING
    // DocumentProvider
    if (isAtLeastKitkat() && DocumentsContract.isDocumentUri(context, uri)) {
        // ExternalStorageProvider
        if (isExternalStorageDocument(uri)) {
            val docId = DocumentsContract.getDocumentId(uri)
            val split = docId.split(":").toTypedArray()
            val type = split[0]
            if ("primary".equals(type, true)) {
                return Environment.getExternalStorageDirectory().toString() + "/" + split[1]
            }

            // TODO handle non-primary volumes
        } else if (isDownloadsDocument(uri)) {
            val id = DocumentsContract.getDocumentId(uri)
            val documentId: Long
            try {
                documentId = id.toLong()
            } catch (e: NumberFormatException) {
                //In Android 8 and later the id is not a number
                val path = uri.path
                if (path != null) {
                    if (path.contains("/document/raw")) {
                        return path.replaceFirst("^/document/raw:".toRegex(), "").replaceFirst("^raw:".toRegex(), "")
                    }
                    // "msf:" == ContentResolver.open
                }
                return EMPTY_STRING
            }
            val contentUri = ContentUris.withAppendedId(
                    Uri.parse("content://downloads/public_downloads"), documentId
            )
            return getDataColumn(context.contentResolver, contentUri)
        } else if (isMediaDocument(uri)) {
            val docId = DocumentsContract.getDocumentId(uri)
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
            return getDataColumn(context.contentResolver, contentUri, "_id=?", listOf(split[1]))
        }
    } else if (uri.isContentScheme()) {

        // Return the remote address
        if (isGooglePhotosDocument(uri)) {
            val data = uri.lastPathSegment
            return data ?: EMPTY_STRING
        }
        return getDataColumn(context.contentResolver, uri)
    } else if (TextUtils.isEmpty(uri.scheme) || ContentResolver.SCHEME_FILE.equals(uri.scheme, ignoreCase = true)) {
        val data = uri.path
        return data ?: EMPTY_STRING
    }
    return EMPTY_STRING
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
fun getDataColumn(
        contentResolver: ContentResolver,
        uri: Uri?,
        selection: String? = null,
        selectionArgs: List<String>? = null
): String = queryUriFirst(contentResolver, uri, String::class.java, listOf(MediaStore.Images.ImageColumns.DATA), selection, selectionArgs)
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
fun isGooglePhotosDocument(uri: Uri): Boolean = "com.google.android.apps.photos.content" == uri.authority

fun readExifLocation(imageFile: File?): Location? = try {
    readExifLocationOrThrow(imageFile)
} catch (e: RuntimeException) {
    logger.e(e)
    null
}

@Throws(RuntimeException::class)
fun readExifLocationOrThrow(imageFile: File?): Location {
    if (imageFile == null || !GraphicUtils.canDecodeImage(imageFile)) {
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

    val exif = createExitOrThrow(imageFile.absolutePath)
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
    if (imageFile == null || !GraphicUtils.canDecodeImage(imageFile)) {
        throw RuntimeException("Incorrect image file: $imageFile")
    }
    if (location == null) {
        throw NullPointerException("location is null")
    }
    val exif = createExitOrThrow(imageFile.absolutePath)

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

@Throws(RuntimeException::class)
private fun createExitOrThrow(path: String) = try {
    ExifInterface(path)
} catch (e: IOException) {
    throw RuntimeException(formatException(e, "create ExifInterface"), e)
}


/**
 * Определяет поворот картинки
 */
// TODO нужно ли getRotationAngleByExifOrientation??
@TargetApi(Build.VERSION_CODES.Q)
fun getOrientationFromMediaStore(contentResolver: ContentResolver, photoUri: Uri?): Int? =
        queryUriFirst(contentResolver,
                photoUri,
                Int::class.java,
                listOf(MediaStore.Images.ImageColumns.ORIENTATION))

fun getRotationAngleFromExif(imageFile: File): Int {
    if (GraphicUtils.canDecodeImage(imageFile)) {
        try {
            val exif = ExifInterface(imageFile.absolutePath)
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1)
            getRotationAngleByExifOrientation(orientation)
        } catch (e: Exception) {
            logger.e(e)
        }
    }
    return -1
}

/**
 * Определяем угол для поворота http://sylvana.net/jpegcrop/exif_orientation.html
 */
fun getRotationAngleByExifOrientation(orientation: Int?): Int? {
    return when (orientation) {
        null -> null
        ExifInterface.ORIENTATION_ROTATE_90 -> 90
        ExifInterface.ORIENTATION_ROTATE_180 -> 180
        ExifInterface.ORIENTATION_ROTATE_270 -> 270
        else -> 0
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
fun <T> queryUriFirst(
        contentResolver: ContentResolver,
        uri: Uri?,
        propertyType: Class<T>,
        projection: List<String>? = null,
        selection: String? = null,
        selectionArgs: List<String>? = null,
        columnIndexFunc: ((Cursor) -> Int)? = null
): T? = try {
    queryUriFirstOrThrow(contentResolver, uri, propertyType, projection, selection, selectionArgs, columnIndexFunc)
} catch (e: RuntimeException) {
    logger.e(e)
    null
}

@Throws(RuntimeException::class)
@JvmOverloads
fun <T> queryUriFirstOrThrow(
        contentResolver: ContentResolver,
        uri: Uri?,
        columnType: Class<T>,
        projection: List<String>? = null,
        selection: String? = null,
        selectionArgs: List<String>? = null,
        columnIndexFunc: ((Cursor) -> Int)? = null
): T {
    if (uri == null) {
        throw NullPointerException("uri is null")
    }
    queryUriOrThrow(contentResolver, uri, projection, selection, selectionArgs).use { cursor ->
        if (cursor.position == -1) {
            cursor.moveToFirst()
        }
        return cursor.getColumnValueOrThrow(columnType, columnIndexFunc)
    }
}

@JvmOverloads
fun <T : Any> queryUri(
        contentResolver: ContentResolver,
        uri: Uri?,
        columnType: Class<T>,
        projection: List<String>? = null,
        selection: String? = null,
        selectionArgs: List<String>? = null,
        sortOrder: String? = null,
        columnIndexFunc: ((Cursor) -> Int)? = null
): List<T> = try {
    queryUriOrThrow(contentResolver, uri, columnType, projection, selection, selectionArgs, sortOrder, columnIndexFunc)
} catch (e: RuntimeException) {
    logger.e(e)
    listOf()
}

@Throws(RuntimeException::class)
@JvmOverloads
fun <T : Any> queryUriOrThrow(
        contentResolver: ContentResolver,
        uri: Uri?,
        columnType: Class<T>,
        projection: List<String>? = null,
        selection: String? = null,
        selectionArgs: List<String>? = null,
        sortOrder: String? = null,
        columnIndexFunc: ((Cursor) -> Int)? = null
): List<T> {
    queryUriOrThrow(contentResolver, uri, projection, selection, selectionArgs, sortOrder).use { cursor ->
        return cursor.mapToList {
            cursor.getColumnValueOrThrow(columnType, columnIndexFunc)
        }
    }
}

@JvmOverloads
fun queryUri(
        contentResolver: ContentResolver,
        uri: Uri?,
        projection: List<String>? = null,
        selection: String? = null,
        selectionArgs: List<String>? = null,
        sortOrder: String? = null,
        checkCursorEmpty: Boolean = true
): Cursor? = try {
    queryUriOrThrow(contentResolver, uri, projection, selection, selectionArgs, sortOrder, checkCursorEmpty)
} catch (e: RuntimeException) {
    logger.e(e)
    null
}

@Throws(RuntimeException::class)
@JvmOverloads
fun queryUriOrThrow(
        contentResolver: ContentResolver,
        uri: Uri?,
        projection: List<String>? = null,
        selection: String? = null,
        selectionArgs: List<String>? = null,
        sortOrder: String? = null,
        checkCursorEmpty: Boolean = true
): Cursor {
    if (uri == null) {
        throw NullPointerException("uri is null")
    }
    if (!uri.isContentScheme()) throw IllegalArgumentException("uri is not content://")
    val cursor = contentResolver.query(
            uri,
            projection?.toTypedArray() ?: arrayOf(),
            selection,
            selectionArgs?.toTypedArray() ?: arrayOf(),
            sortOrder
    )
    if (cursor == null
            || (if (checkCursorEmpty) !cursor.isNonEmpty() else !cursor.isValid())) {
        throw RuntimeException("cursor is null or empty or closed")
    }
    return cursor
}

fun <T> Cursor.getColumnValue(columnType: Class<T>, index: Int): T? = try {
    getColumnValueOrThrow(columnType, index)
} catch (e: RuntimeException) {
    logger.e(e)
    null
}

@Throws(RuntimeException::class)
@Suppress("UNCHECKED_CAST")
fun <T> Cursor.getColumnValueOrThrow(columnType: Class<T>, index: Int): T {
    return when {
        columnType.isAssignableFrom(ByteArray::class.java) -> {
            getBlob(index) as T
        }
        columnType.isAssignableFrom(String::class.java) -> {
            (getString(index) ?: EMPTY_STRING) as T
        }
        columnType.isAssignableFrom(Short::class.java) -> {
            getShort(index) as T
        }
        columnType.isAssignableFrom(Int::class.java) -> {
            getInt(index) as T
        }
        columnType.isAssignableFrom(Long::class.java) -> {
            getLong(index) as T
        }
        columnType.isAssignableFrom(Float::class.java) -> {
            getFloat(index) as T
        }
        columnType.isAssignableFrom(Double::class.java) -> {
            getDouble(index) as T
        }
        else -> {
            throw IllegalArgumentException("Incorrect column type: $columnType")
        }
    }
}

@Throws(RuntimeException::class)
private fun <T> Cursor.getColumnValueOrThrow(columnType: Class<T>, columnIndexFunc: ((Cursor) -> Int)? = null): T {
    val index = columnIndexFunc?.invoke(this) ?: 0
    return getColumnValueOrThrow(columnType, index)
}

private fun <T : Any> Cursor.mapToList(predicate: (Cursor) -> T?): List<T> =
        generateSequence { if (moveToNext()) predicate(this) else null }
                .toList()

fun getColumnNames(context: Context, uri: Uri?): List<String> {
    if (uri != null && uri.isContentScheme()) {
        val c = context.contentResolver.query(uri, null, null, null, null)
        if (c != null && !c.isClosed) {
            return c.use {
                listOf(*c.columnNames)
            }
        }
    }
    return ArrayList()
}

fun Uri?.isFileScheme(): Boolean {
    if (this != null) {
        val scheme = this.scheme
        return isEmpty(scheme) || ContentResolver.SCHEME_FILE.equals(scheme, true)
    }
    return false
}

fun Uri?.isContentScheme(): Boolean {
    if (this != null) {
        return ContentResolver.SCHEME_CONTENT.equals(this.scheme, true)
    }
    return false
}

fun Uri?.getTableName(): String? {
    return if (this != null && this.isContentScheme()) {
        this.pathSegments[0]
    } else null
}

fun Cursor?.isValid() = this != null && !this.isClosed

fun Cursor?.isNonEmpty() = this != null && !this.isClosed && this.count > 0

fun Uri.openInputStream(resolver: ContentResolver): InputStream? = try {
    openInputStreamOrThrow(resolver)
} catch (e: RuntimeException) {
    logger.e(e)
    null
}

@Throws(RuntimeException::class)
fun Uri.openInputStreamOrThrow(resolver: ContentResolver): InputStream =
        try {
            resolver.openInputStream(this)
        } catch (e: IOException) {
            throw RuntimeException(formatException(e, "openInputStream"), e)
        } ?: throw NullPointerException("Cannot open InputStream on $this")

fun Uri.openOutputStream(resolver: ContentResolver): OutputStream? = try {
    openOutputStreamOrThrow(resolver)
} catch (e: RuntimeException) {
    logger.e(e)
    null
}

@Throws(RuntimeException::class)
fun Uri.openOutputStreamOrThrow(resolver: ContentResolver): OutputStream =
        try {
            resolver.openOutputStream(this)
        } catch (e: IOException) {
            throw RuntimeException(formatException(e, "openOutputStream"), e)
        } ?: throw NullPointerException("Cannot open OutputStream on $this")


fun readBytesFromUri(
        contentResolver: ContentResolver,
        uri: Uri?,
        offset: Int = 0,
        length: Int = 0
): ByteArray? = try {
    readBytesFromUriOrThrow(contentResolver, uri, offset, length)
} catch (e: RuntimeException) {
    logger.e(e)
    null
}

@Throws(RuntimeException::class)
fun readBytesFromUriOrThrow(
        contentResolver: ContentResolver,
        uri: Uri?,
        offset: Int = 0,
        length: Int = 0
): ByteArray {
    if (uri == null) {
        throw NullPointerException("uri is null")
    }
    return try {
        readBytesFromInputStreamOrThrow(
                uri.openInputStreamOrThrow(contentResolver),
                offset,
                length
        ).first
    } catch (e: IOException) {
        throw RuntimeException(formatException(e, "readBytesFromInputStreamOrThrow"), e)
    }
}

fun readStringsFromUri(
        contentResolver: ContentResolver,
        uri: Uri?,
        count: Int = 0,
        charsetName: String = CHARSET_DEFAULT
): List<String> = try {
    readStringsFromUriOrThrow(contentResolver, uri, count, charsetName)
} catch (e: RuntimeException) {
    logger.e(e)
    emptyList()
}

@Throws(RuntimeException::class)
@JvmOverloads
fun readStringsFromUriOrThrow(
        contentResolver: ContentResolver,
        uri: Uri?,
        count: Int = 0,
        charsetName: String = CHARSET_DEFAULT
): List<String> {
    if (uri == null) {
        throw NullPointerException("Incorrect uri: '$uri'")
    }
    return try {
        readStringsFromInputStreamOrThrow(uri.openInputStreamOrThrow(contentResolver), count, charsetName = charsetName)
    } catch (e: IOException) {
        throw RuntimeException(formatException(e, "readStringsFromInputStream"), e)
    }
}

fun writeBytesToUri(
        contentResolver: ContentResolver,
        uri: Uri?,
        data: ByteArray?
) = try {
    writeBytesToUriOrThrow(contentResolver, uri, data)
    true
} catch (e: RuntimeException) {
    logger.e(e)
    false
}

@Throws(RuntimeException::class)
fun writeBytesToUriOrThrow(
        contentResolver: ContentResolver,
        uri: Uri?,
        data: ByteArray?
) {
    if (uri == null) {
        throw NullPointerException("Incorrect uri: '$uri'")
    }
    if (data == null) {
        throw NullPointerException("data is null")
    }
    return try {
        writeBytesToOutputStreamOrThrow(uri.openOutputStreamOrThrow(contentResolver), data)
    } catch (e: IOException) {
        throw RuntimeException(formatException(e, "writeBytesToOutputStream"), e)
    }
}

fun writeStringsToUri(
        contentResolver: ContentResolver,
        uri: Uri?,
        data: Collection<String>?,
) = try {
    writeStringsToUriOrThrow(contentResolver, uri, data)
    true
} catch (e: RuntimeException) {
    logger.e(e)
    false
}

@Throws(RuntimeException::class)
fun writeStringsToUriOrThrow(
        contentResolver: ContentResolver,
        uri: Uri?,
        data: Collection<String>?,
) {
    if (uri == null) {
        throw NullPointerException("Incorrect uri: '$uri'")
    }
    val outStreamWriter =
            try {
                OutputStreamWriter(uri.openOutputStreamOrThrow(contentResolver))
            } catch (e: Throwable) {
                throw RuntimeException(formatException(e, "create OutputStreamWriter"))
            }
    try {
        writeStringToOutputStreamWriterOrThrow(outStreamWriter, data)
    } catch (e: IOException) {
        throwRuntimeException(e, "writeStringToOutputStreamWriter")
    }
}

@JvmOverloads
fun getBase64(
        contentResolver: ContentResolver,
        uri: Uri?,
        charset: Charset = Charset.defaultCharset(),
        flags: Int = Base64.DEFAULT
): String = getBase64(
        TextUtils.join(System.getProperty("line.separator") ?: "\\n",
                readStringsFromUri(contentResolver, uri)),
        charset,
        flags
)

@JvmOverloads
fun getBase64(
        value: String?,
        charset: Charset = Charset.defaultCharset(),
        flags: Int = Base64.DEFAULT
): String = getBase64(value?.toByteArray(charset), flags)

/**
 * Получение бинартых данных файла в формате Base64
 *
 * @param media - массив байт
 * @return - строка в формате base64
 */
@JvmOverloads
fun getBase64(value: ByteArray?, flags: Int = Base64.DEFAULT): String =
        value?.let { Base64.encodeToString(value, flags) } ?: EMPTY_STRING