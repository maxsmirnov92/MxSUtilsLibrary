package net.maxsmr.commonutils.media

import android.content.ContentResolver
import android.content.Context
import android.content.res.Resources
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import android.text.TextUtils
import android.util.Base64
import net.maxsmr.commonutils.*
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder.formatException
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder.throwRuntimeException
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.commonutils.text.isEmpty
import java.io.*
import java.nio.charset.Charset

private val logger = BaseLoggerHolder.getInstance().getLogger<BaseLogger>("UriExt")

fun Uri.readBytes(
        contentResolver: ContentResolver,
        offset: Int = 0,
        length: Int = 0
): ByteArray? = try {
    readBytesOrThrow(contentResolver, offset, length)
} catch (e: RuntimeException) {
    logger.e(e)
    null
}

@Throws(RuntimeException::class)
fun Uri.readBytesOrThrow(
        contentResolver: ContentResolver,
        offset: Int = 0,
        length: Int = 0
): ByteArray =try {
        openInputStreamOrThrow(contentResolver).readBytesOrThrow(
                offset,
                length
        )
    } catch (e: IOException) {
        throw RuntimeException(formatException(e, "readBytesOrThrow"), e)
    }

fun Uri.readStrings(
        contentResolver: ContentResolver,
        count: Int = 0,
        charsetName: String = CHARSET_DEFAULT
): List<String> = try {
    readStringsOrThrow(contentResolver, count, charsetName)
} catch (e: RuntimeException) {
    logger.e(e)
    emptyList()
}

@Throws(RuntimeException::class)
@JvmOverloads
fun Uri.readStringsOrThrow(
        contentResolver: ContentResolver,
        count: Int = 0,
        charsetName: String = CHARSET_DEFAULT
): List<String> {
    return try {
        openInputStreamOrThrow(contentResolver).readStringsOrThrow(count, charsetName = charsetName)
    } catch (e: IOException) {
        throw RuntimeException(formatException(e, "readStringsOrThrow"), e)
    }
}

fun Uri.writeBytes(contentResolver: ContentResolver, data: ByteArray?) = try {
    writeBytesOrThrow(contentResolver, data)
    true
} catch (e: RuntimeException) {
    logger.e(e)
    false
}

@Throws(RuntimeException::class)
fun Uri.writeBytesOrThrow(contentResolver: ContentResolver, data: ByteArray?) {
    if (data == null) {
        throw NullPointerException("data is null")
    }
    return try {
        openOutputStreamOrThrow(contentResolver).writeBytesOrThrow(data)
    } catch (e: IOException) {
        throw RuntimeException(formatException(e, "writeBytesToOutputStream"), e)
    }
}

fun Uri.writeStrings(contentResolver: ContentResolver, data: Collection<String>?) = try {
    writeStringsOrThrow(contentResolver, data)
    true
} catch (e: RuntimeException) {
    logger.e(e)
    false
}

@Throws(RuntimeException::class)
fun Uri.writeStringsOrThrow(contentResolver: ContentResolver, data: Collection<String>?) {
    val outStreamWriter =
            try {
                OutputStreamWriter(openOutputStreamOrThrow(contentResolver))
            } catch (e: Throwable) {
                throw RuntimeException(formatException(e, "create OutputStreamWriter"))
            }
    try {
        outStreamWriter.writeStringOrThrow(data)
    } catch (e: IOException) {
        throwRuntimeException(e, "writeStringToOutputStreamWriter")
    }
}

@JvmOverloads
fun Uri.copyTo(
        contentResolver: ContentResolver,
        fileTo: File,
        append: Boolean = false,
        deleteOnFinish: Boolean = false,
): Uri? = try {
    copyToOrThrow(contentResolver, fileTo, append, deleteOnFinish)
} catch (e: RuntimeException) {
    logger.e(e)
    null
}

@Throws(RuntimeException::class)
@JvmOverloads
fun Uri.copyToOrThrow(
        contentResolver: ContentResolver,
        fileTo: File,
        append: Boolean = false,
        deleteOnFinish: Boolean = false,
): Uri {
    if (!isEmptyOrThrow(contentResolver)) {
        val input = openInputStreamOrThrow(contentResolver)
        createFileOrThrow(fileTo.name, fileTo.parent, true)
        val output = fileTo.openOutputStreamOrThrow(append)
        try {
            input.copyStreamOrThrow(output)
        } catch (e: IOException) {
            throwRuntimeException(e, "copyStream")
        }
    }
    if (deleteOnFinish) {
        deleteOrThrow(contentResolver)
    }
    return Uri.fromFile(fileTo)
}

@JvmOverloads
fun Uri.getBase64(
        contentResolver: ContentResolver,
        charset: Charset = Charset.defaultCharset(),
        flags: Int = Base64.DEFAULT
): String = getBase64(
        TextUtils.join(System.getProperty("line.separator") ?: "\\n",
                readStrings(contentResolver)),
        charset,
        flags
)

@JvmOverloads
fun <T> Uri.queryFirst(
        contentResolver: ContentResolver,
        propertyType: Class<T>,
        projection: List<String>? = null,
        selection: String? = null,
        selectionArgs: List<String>? = null,
        columnIndexFunc: ((Cursor) -> Int)? = null
): T? = try {
    queryFirstOrThrow(contentResolver, propertyType, projection, selection, selectionArgs, columnIndexFunc)
} catch (e: RuntimeException) {
    logger.e(e)
    null
}

@Throws(RuntimeException::class)
@JvmOverloads
fun <T> Uri.queryFirstOrThrow(
        contentResolver: ContentResolver,
        columnType: Class<T>,
        projection: List<String>? = null,
        selection: String? = null,
        selectionArgs: List<String>? = null,
        columnIndexFunc: ((Cursor) -> Int)? = null
): T {
    queryOrThrow(contentResolver, projection, selection, selectionArgs).use { cursor ->
        if (cursor.position == -1) {
            cursor.moveToFirst()
        }
        return cursor.getColumnValueOrThrow(columnType, columnIndexFunc)
    }
}

@JvmOverloads
fun <T : Any> Uri.query(
        contentResolver: ContentResolver,
        columnType: Class<T>,
        projection: List<String>? = null,
        selection: String? = null,
        selectionArgs: List<String>? = null,
        sortOrder: String? = null,
        columnIndexFunc: ((Cursor) -> Int)? = null
): List<T> = try {
    queryOrThrow(contentResolver, columnType, projection, selection, selectionArgs, sortOrder, columnIndexFunc)
} catch (e: RuntimeException) {
    logger.e(e)
    listOf()
}

@Throws(RuntimeException::class)
@JvmOverloads
fun <T : Any> Uri.queryOrThrow(
        contentResolver: ContentResolver,
        columnType: Class<T>,
        projection: List<String>? = null,
        selection: String? = null,
        selectionArgs: List<String>? = null,
        sortOrder: String? = null,
        columnIndexFunc: ((Cursor) -> Int)? = null
): List<T> {
    queryOrThrow(contentResolver, projection, selection, selectionArgs, sortOrder).use { cursor ->
        return cursor.mapToList {
            cursor.getColumnValueOrThrow(columnType, columnIndexFunc)
        }
    }
}

@JvmOverloads
fun Uri.query(
        contentResolver: ContentResolver,
        projection: List<String>? = null,
        selection: String? = null,
        selectionArgs: List<String>? = null,
        sortOrder: String? = null,
        checkCursorEmpty: Boolean = true
): Cursor? = try {
    queryOrThrow(contentResolver, projection, selection, selectionArgs, sortOrder, checkCursorEmpty)
} catch (e: RuntimeException) {
    logger.e(e)
    null
}

@Throws(RuntimeException::class)
@JvmOverloads
fun Uri.queryOrThrow(
        contentResolver: ContentResolver,
        projection: List<String>? = null,
        selection: String? = null,
        selectionArgs: List<String>? = null,
        sortOrder: String? = null,
        checkCursorEmpty: Boolean = true
): Cursor {
    if (!isContentScheme()) throw IllegalArgumentException("uri is not content://")
    val cursor = contentResolver.query(
            this,
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

fun Uri.toContentUri(context: Context): Uri? = try {
    toContentUriOrThrow(context)
} catch (e: RuntimeException) {
    logger.e(e)
    null
}

@Throws(RuntimeException::class)
fun Uri.toContentUriOrThrow(context: Context): Uri {
    val path = path ?: throw RuntimeException("uri path is null")
    return when {
        isFileScheme() -> File(path).toContentUri(context)
        isContentScheme() -> this
        else -> throw RuntimeException("Incorrect uri scheme: $scheme")
    }
}

fun Uri.exists(contentResolver: ContentResolver): Boolean = try {
    existsOrThrow(contentResolver)
} catch (e: RuntimeException) {
    logger.e(e)
    false
}

@Throws(RuntimeException::class)
fun Uri.existsOrThrow(contentResolver: ContentResolver): Boolean {
    val path = path ?: throw RuntimeException("uri path is null")
    return when {
        isFileScheme() -> {
            return isFileExistsOrThrow(path)
        }
        isContentScheme() -> {
            queryOrThrow(contentResolver, checkCursorEmpty = false).count > 0
        }
        else -> {
            throw RuntimeException("Incorrect uri scheme: $scheme")
        }
    }
}

fun Uri.isEmpty(contentResolver: ContentResolver): Boolean = try {
    isEmptyOrThrow(contentResolver)
} catch (e: RuntimeException) {
    logger.e(e)
    true
}

@Throws(RuntimeException::class)
fun Uri.isEmptyOrThrow(contentResolver: ContentResolver): Boolean =
        lengthOrThrow(contentResolver) == 0L

fun Uri.length(contentResolver: ContentResolver): Long = try {
    lengthOrThrow(contentResolver)
} catch (e: RuntimeException) {
    logger.e(e)
    0L
}

@Throws(RuntimeException::class)
fun Uri.lengthOrThrow(contentResolver: ContentResolver): Long {
    val path = this.path ?: throw RuntimeException("uri path is null")
    return when {
        isFileScheme() -> {
            getFileLengthOrThrow(path)
        }
        isContentScheme() -> {
            queryFirstOrThrow(
                    contentResolver,
                    Long::class.java,
                    listOf(OpenableColumns.SIZE)
            )
        }
        else -> {
            throw RuntimeException("Incorrect uri scheme: $scheme")
        }
    }
}

fun Uri.name(contentResolver: ContentResolver): String = try {
    nameOrThrow(contentResolver)
} catch (e: RuntimeException) {
    logger.e(e)
    EMPTY_STRING
}

@Throws(RuntimeException::class)
fun Uri.nameOrThrow(contentResolver: ContentResolver): String {
    val path = this.path ?: throw RuntimeException("uri path is null")
    return when {
        isFileScheme() -> {
            File(path).name
        }
        isContentScheme() -> {
            queryFirstOrThrow(
                    contentResolver,
                    String::class.java,
                    listOf(OpenableColumns.DISPLAY_NAME)
            )
        }
        else -> {
            throw RuntimeException("Incorrect uri scheme: $scheme")
        }
    }
}

fun Uri.mimeType(contentResolver: ContentResolver): String = try {
    mimeTypeOrThrow(contentResolver)
} catch (e: RuntimeException) {
    EMPTY_STRING
}

@Throws(RuntimeException::class)
fun Uri.mimeTypeOrThrow(contentResolver: ContentResolver): String = when {
    isFileScheme() -> getMimeType(path)
    isContentScheme() -> {
        try {
            contentResolver.getType(this)
        } catch (e: Exception) {
            throw RuntimeException(formatException(e))
        } ?: EMPTY_STRING
    }
    else -> throw RuntimeException("Incorrect uri scheme: $scheme")
}

@JvmOverloads
fun Uri.delete(contentResolver: ContentResolver, throwIfNotExists: Boolean = false) = try {
    deleteOrThrow(contentResolver, throwIfNotExists)
    true
} catch (e: RuntimeException) {
    logger.e(e)
    false
}

@Throws(RuntimeException::class)
@JvmOverloads
fun Uri.deleteOrThrow(contentResolver: ContentResolver, throwIfNotExists: Boolean = false) {
    if (!existsOrThrow(contentResolver)) {
        if (throwIfNotExists) {
            throw Resources.NotFoundException("Resource $this not found")
        }
        return
    }
    when {
        isFileScheme() -> {
            deleteFileOrThrow(File(path as String))
        }
        isContentScheme() -> {
            contentResolver.delete(this, null, null)
        }
        else -> {
            throw RuntimeException("Incorrect uri scheme: $scheme")
        }
    }
}

fun Uri.openInputStream(contentResolver: ContentResolver): InputStream? = try {
    openInputStreamOrThrow(contentResolver)
} catch (e: RuntimeException) {
    logger.e(e)
    null
}

@Throws(RuntimeException::class)
fun Uri.openInputStreamOrThrow(contentResolver: ContentResolver): InputStream {
    val path = this.path ?: throw RuntimeException("uri path is null")
    return when {
        this.isFileScheme() -> File(path).openInputStreamOrThrow()
        this.isContentScheme() -> openResolverInputStreamOrThrow(contentResolver)
        else -> throw RuntimeException("Incorrect uri scheme: $scheme")
    }
}

fun Uri.openOutputStream(contentResolver: ContentResolver): OutputStream? = try {
    openOutputStreamOrThrow(contentResolver)
} catch (e: RuntimeException) {
    logger.e(e)
    null
}

@Throws(RuntimeException::class)
fun Uri.openOutputStreamOrThrow(contentResolver: ContentResolver): OutputStream {
    val path = this.path ?: throw RuntimeException("uri path is null")
    return when {
        this.isFileScheme() -> {
            File(path).openOutputStreamOrThrow()
        }
        this.isContentScheme() -> {
            openResolverOutputStreamOrThrow(contentResolver)
        }
        else -> throw RuntimeException("Incorrect uri scheme: $scheme")
    }
}

private fun Uri.openResolverInputStream(resolver: ContentResolver): InputStream? = try {
    openResolverInputStreamOrThrow(resolver)
} catch (e: RuntimeException) {
    logger.e(e)
    null
}

@Throws(RuntimeException::class)
private fun Uri.openResolverInputStreamOrThrow(resolver: ContentResolver): InputStream {
    return try {
        resolver.openInputStream(this)
    } catch (e: IOException) {
        throw RuntimeException(formatException(e, "openInputStream"), e)
    } ?: throw NullPointerException("Cannot open InputStream on $this")
}

private fun Uri.openResolverOutputStream(resolver: ContentResolver): OutputStream? = try {
    openResolverOutputStreamOrThrow(resolver)
} catch (e: RuntimeException) {
    logger.e(e)
    null
}


@Throws(RuntimeException::class)
private fun Uri.openResolverOutputStreamOrThrow(resolver: ContentResolver): OutputStream {
    return try {
        resolver.openOutputStream(this)
    } catch (e: IOException) {
        throw RuntimeException(formatException(e, "openOutputStream"), e)
    } ?: throw NullPointerException("Cannot open OutputStream on $this")
}