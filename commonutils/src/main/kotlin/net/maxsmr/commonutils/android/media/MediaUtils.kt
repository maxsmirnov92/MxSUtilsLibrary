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
import android.text.TextUtils
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
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder.throwRuntimeException
import java.io.File
import java.io.IOException
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

fun isResourceExists(context: Context, uri: Uri?): Boolean {
    if (uri == null) return false
    val path = uri.path ?: throw RuntimeException("uri path is null")
    when {
        uri.isFileScheme() -> {
            return isFileExists(path)
        }
        uri.isContentScheme() -> {
            queryUri(context, uri)?.use {
                return it.count > 0
            }
        }
    }
    return false
}

fun deleteResource(
        context: Context,
        uri: Uri?,
        throwIfNotExists: Boolean = false
) = try {
    deleteResourceOrThrow(context, uri, throwIfNotExists)
    true
} catch (e: RuntimeException) {
    logger.e(e)
    false
}

@Throws(RuntimeException::class)
@JvmOverloads
fun deleteResourceOrThrow(
        context: Context,
        uri: Uri?,
        throwIfNotExists: Boolean = false
) {
    if (uri == null) {
        throw NullPointerException("uri is null")
    }
    if (!isResourceExists(context, uri)) {
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
            context.contentResolver.delete(uri, null, null)
        }
    }
}

@TargetApi(Build.VERSION_CODES.Q)
@JvmOverloads
fun copyToExternal(
        context: Context,
        file: File,
        mimeType: String,
        useRelativePath: Boolean = false
) = try {
    copyToExternalOrThrow(context, file, mimeType, useRelativePath)
    true
} catch (e: RuntimeException) {
    logger.e(e)
    false
}

@TargetApi(Build.VERSION_CODES.Q)
@JvmOverloads
@Throws(RuntimeException::class)
fun copyToExternalOrThrow(
        context: Context,
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
    val resolver = context.contentResolver

    val targetUri = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

    resolver.insert(targetUri, values)?.let { item ->
        try {
            val out = resolver.openOutputStream(item)
                    ?: throw RuntimeException("Cannot open stream on $item")
            copyStreamOrThrow(file.toFisOrThrow(), out, notifier, buffSize)
        } catch (e: IOException) {
            throwRuntimeException(e)
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
            val contentUri = ContentUris.withAppendedId(
                    Uri.parse("content://downloads/public_downloads"), java.lang.Long.valueOf(id))
            return getDataColumn(context, contentUri)
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
            return getDataColumn(context, contentUri, "_id=?", listOf(split[1]))
        }
    } else if (uri.isContentScheme()) {

        // Return the remote address
        if (isGooglePhotosDocument(uri)) {
            val data = uri.lastPathSegment
            return data ?: EMPTY_STRING
        }
        return getDataColumn(context, uri)
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
        context: Context,
        uri: Uri?,
        selection: String? = null,
        selectionArgs: List<String>? = null
): String = queryUriFirst(context, uri, String::class.java, listOf(MediaStore.Images.ImageColumns.DATA), selection, selectionArgs)
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
    return try {
        val exif = ExifInterface(imageFile.absolutePath)
        val provider = exif.getAttribute(ExifInterface.TAG_GPS_PROCESSING_METHOD)
        val result = Location(provider)
        var value: String?
        var latitude = 0.0
        try {
            value = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE)
            if (value != null) {
                latitude = Location.convert(value)
            }
        } catch (e: IllegalArgumentException) {
            throw RuntimeException("An IllegalArgumentException occurred", e)
        }
        var longitude = 0.0
        try {
            value = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE)
            if (value != null) {
                longitude = Location.convert(value)
            }
        } catch (e: NumberFormatException) {
            throw RuntimeException("An IllegalArgumentException occurred", e)
        }
        var altitude = 0.0
        try {
            value = exif.getAttribute(ExifInterface.TAG_GPS_ALTITUDE)
            if (value != null) {
                altitude = Location.convert(value)
            }
        } catch (e: NumberFormatException) {
            throw RuntimeException("An IllegalArgumentException occurred", e)
        }
        var timestamp = 0L
        try {
            value = exif.getAttribute(ExifInterface.TAG_GPS_ALTITUDE)
            if (value != null) {
                timestamp = value.toLong()
            }
        } catch (e: NumberFormatException) {
            throw RuntimeException("A NumberFormatException occurred", e)
        }
        result.latitude = latitude
        result.longitude = longitude
        result.altitude = altitude
        result.time = timestamp
        result
    } catch (e: IOException) {
        throw RuntimeException("An IOException occurred", e)
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
    if (imageFile == null || !GraphicUtils.canDecodeImage(imageFile)) {
        throw RuntimeException("Incorrect image file: $imageFile")
    }
    if (location == null) {
        throw NullPointerException("location is null")
    }
    try {
        val exif = ExifInterface(imageFile.absolutePath)
        val latitude = location.latitude
        exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, convertLocationDoubleToString(latitude))
        exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, if (latitude > 0) "N" else "S")
        val longitude = location.longitude
        exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, convertLocationDoubleToString(longitude))
        exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, if (longitude > 0) "E" else "W")
        exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE, convertLocationDoubleToString(location.altitude))
        exif.setAttribute(ExifInterface.TAG_GPS_TIMESTAMP, location.time.toString())
        location.provider?.let {
            exif.setAttribute(ExifInterface.TAG_GPS_PROCESSING_METHOD, it.toString())
        }
        exif.saveAttributes()
    } catch (e: IOException) {
        throw RuntimeException("an IOException occurred", e)
    }
}

fun convertLocationDoubleToString(value: Double): String? {
    var result: String? = null
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

/**
 * Определяет поворот картинки
 */
@TargetApi(Build.VERSION_CODES.Q)
fun getOrientation(context: Context, photoUri: Uri): Int? =
        queryUriFirst(context, photoUri, Int::class.java, listOf(MediaStore.Images.ImageColumns.ORIENTATION))

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
        context: Context,
        uri: Uri?,
        propertyType: Class<T>,
        projection: List<String>? = null,
        selection: String? = null,
        selectionArgs: List<String>? = null,
        columnIndexFunc: ((Cursor) -> Int)? = null
): T? = try {
    queryUriFirstOrThrow(context, uri, propertyType, projection, selection, selectionArgs, columnIndexFunc)
} catch (e: RuntimeException) {
    logger.e(e)
    null
}

@Throws(RuntimeException::class)
@JvmOverloads
fun <T> queryUriFirstOrThrow(
        context: Context,
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
    queryUriOrThrow(context, uri, projection, selection, selectionArgs).use { cursor ->
        if (cursor.position == -1) {
            cursor.moveToFirst()
        }
        return cursor.getColumnValueOrThrow(columnType, columnIndexFunc = columnIndexFunc)
    }
}

fun <T : Any> queryUri(
        context: Context,
        uri: Uri?,
        columnType: Class<T>,
        projection: List<String>? = null,
        selection: String? = null,
        selectionArgs: List<String>? = null,
        sortOrder: String? = null,
        columnIndexFunc: ((Cursor) -> Int)? = null
): List<T> = try {
    queryUriOrThrow(context, uri, columnType, projection, selection, selectionArgs, sortOrder, columnIndexFunc)
} catch (e: RuntimeException) {
    logger.e(e)
    listOf()
}

@Throws(RuntimeException::class)
fun <T : Any> queryUriOrThrow(
        context: Context,
        uri: Uri?,
        columnType: Class<T>,
        projection: List<String>? = null,
        selection: String? = null,
        selectionArgs: List<String>? = null,
        sortOrder: String? = null,
        columnIndexFunc: ((Cursor) -> Int)? = null
): List<T> {
    queryUriOrThrow(context, uri, projection, selection, selectionArgs, sortOrder).use { cursor ->
        return cursor.mapToList {
            cursor.getColumnValueOrThrow(columnType, columnIndexFunc = columnIndexFunc)
        }
    }
}

fun queryUri(
        context: Context,
        uri: Uri?,
        projection: List<String>? = null,
        selection: String? = null,
        selectionArgs: List<String>? = null,
        sortOrder: String? = null
): Cursor? = try {
    queryUriOrThrow(context, uri, projection, selection, selectionArgs, sortOrder)
} catch (e: RuntimeException) {
    logger.e(e)
    null
}

@Throws(RuntimeException::class)
fun queryUriOrThrow(
        context: Context,
        uri: Uri?,
        projection: List<String>? = null,
        selection: String? = null,
        selectionArgs: List<String>? = null,
        sortOrder: String? = null
): Cursor {
    if (uri == null) {
        throw NullPointerException("uri is null")
    }
    if (!uri.isContentScheme()) throw IllegalArgumentException("uri is not content://")
    val cursor = context.contentResolver.query(
            uri,
            projection?.toTypedArray() ?: arrayOf(),
            selection,
            selectionArgs?.toTypedArray() ?: arrayOf(),
            sortOrder
    )
    if (cursor == null || !cursor.isValid()) {
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
        ContentResolver.SCHEME_CONTENT.equals(this.scheme, true)
    }
    return false
}

fun Uri?.getTableName(): String? {
    return if (this != null && this.isContentScheme()) {
        this.pathSegments[0]
    } else null
}

fun Cursor?.isValid() = this != null && !this.isClosed && this.count > 0
