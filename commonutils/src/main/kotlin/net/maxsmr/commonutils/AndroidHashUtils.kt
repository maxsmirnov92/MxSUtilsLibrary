package net.maxsmr.commonutils

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import net.maxsmr.commonutils.media.openInputStreamOrThrow
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.commonutils.media.readBytes
import java.io.File
import java.security.MessageDigest

private val logger = BaseLoggerHolder.getInstance().getLogger<BaseLogger>("AndroidHashUtils")

@JvmOverloads
fun Uri.digest(contentResolver: ContentResolver, algorithm: String = ALGORITHM_DEFAULT
): ByteArray? = try {
    digestOrThrow(contentResolver, algorithm)
} catch (e: RuntimeException) {
    logger.e(e)
    null
}

@Throws(RuntimeException::class)
@JvmOverloads
fun Uri.digestOrThrow(contentResolver: ContentResolver, algorithm: String = ALGORITHM_DEFAULT): ByteArray =
        digestOrThrow(contentResolver, digestSafe(algorithm))

fun Uri.digest(contentResolver: ContentResolver, algorithm: MessageDigest): ByteArray? = try {
    digestOrThrow(contentResolver, algorithm)
} catch (e: RuntimeException) {
    logger.e(e)
    null
}

@Throws(RuntimeException::class)
fun Uri.digestOrThrow(contentResolver: ContentResolver, algorithm: MessageDigest): ByteArray =
        digestOrThrow(openInputStreamOrThrow(contentResolver), algorithm, true)


fun Uri.getCrc32Hash(contentResolver: ContentResolver): Long =
        getCrc32Hash(readBytes(contentResolver))

fun File.getCrc32Hash(): Long =
        getCrc32Hash(readBytesFromFile(this))

