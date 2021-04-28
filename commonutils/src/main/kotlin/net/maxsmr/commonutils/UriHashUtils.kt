package net.maxsmr.commonutils

import android.content.ContentResolver
import android.net.Uri
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.commonutils.media.openInputStreamOrThrow
import net.maxsmr.commonutils.media.readBytes
import java.security.MessageDigest

private val logger = BaseLoggerHolder.getInstance().getLogger<BaseLogger>("AndroidHashUtils")

fun Uri.digest(contentResolver: ContentResolver, algorithm: String): ByteArray? = try {
    digestOrThrow(contentResolver, algorithm)
} catch (e: RuntimeException) {
    logger.e(e)
    null
}

@Throws(RuntimeException::class)
fun Uri.digestOrThrow(contentResolver: ContentResolver, algorithm: String): ByteArray =
        digestOrThrow(contentResolver, messageDigestOrThrow(algorithm))

fun Uri.digest(contentResolver: ContentResolver, algorithm: MessageDigest): ByteArray? = try {
    digestOrThrow(contentResolver, algorithm)
} catch (e: RuntimeException) {
    logger.e(e)
    null
}

@Throws(RuntimeException::class)
fun Uri.digestOrThrow(contentResolver: ContentResolver, algorithm: MessageDigest): ByteArray =
        openInputStreamOrThrow(contentResolver).digestOrThrow(algorithm, true)

fun Uri.getCrc32Hash(contentResolver: ContentResolver): Long =
        readBytes(contentResolver).getCrc32Hash()