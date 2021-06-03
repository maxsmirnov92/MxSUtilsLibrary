package net.maxsmr.commonutils.media

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder

private val logger = BaseLoggerHolder.instance.getLogger<BaseLogger>("CursorUtils")

fun Cursor?.isValid() = this != null && !this.isClosed

fun Cursor?.isNonEmpty() = this != null && !this.isClosed && this.count > 0

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
fun <T> Cursor.getColumnValueOrThrow(columnType: Class<T>, columnIndexFunc: ((Cursor) -> Int)? = null): T {
    val index = columnIndexFunc?.invoke(this) ?: 0
    return getColumnValueOrThrow(columnType, index)
}

fun <T : Any> Cursor.mapToList(predicate: (Cursor) -> T?): List<T> =
        generateSequence { if (moveToNext()) predicate(this) else null }
                .toList()

fun getColumnNames(contentResolver: ContentResolver, uri: Uri?): List<String> {
    if (uri != null && uri.isContentScheme()) {
        val c = contentResolver.query(uri, null, null, null, null)
        if (c != null && !c.isClosed) {
            return c.use {
                listOf(*c.columnNames)
            }
        }
    }
    return listOf()
}