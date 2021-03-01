package net.maxsmr.commonutils

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import net.maxsmr.commonutils.media.openInputStreamFromResourceOrThrow
import net.maxsmr.commonutils.media.readBytesFromUri
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import java.io.File
import java.security.MessageDigest

private val logger = BaseLoggerHolder.getInstance().getLogger<BaseLogger>("AndroidHashUtils")

@JvmOverloads
fun digest(
        context: Context,
        uri: Uri,
        algorithm: String = ALGORITHM_DEFAULT
): ByteArray? = try {
    digestOrThrow(context, uri, algorithm)
} catch (e: RuntimeException) {
    logger.e(e)
    null
}

@Throws(RuntimeException::class)
@JvmOverloads
fun digestOrThrow(
        context: Context,
        uri: Uri,
        algorithm: String = ALGORITHM_DEFAULT
): ByteArray =
        digestOrThrow(context, uri, digestSafe(algorithm))

fun digest(
        context: Context,
        uri: Uri?,
        algorithm: MessageDigest
): ByteArray? = try {
    digestOrThrow(context, uri, algorithm)
} catch (e: RuntimeException) {
    logger.e(e)
    null
}

@Throws(RuntimeException::class)
fun digestOrThrow(
        context: Context,
        uri: Uri?,
        algorithm: MessageDigest
): ByteArray =
        digestOrThrow(uri.openInputStreamFromResourceOrThrow(context), algorithm, true)


fun getCrc32Hash(contentResolver: ContentResolver, uri: Uri): Long =
        getCrc32Hash(readBytesFromUri(contentResolver, uri))

fun getCrc32Hash(file: File): Long =
        getCrc32Hash(readBytesFromFile(file))

