package net.maxsmr.commonutils.data

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import net.maxsmr.commonutils.android.media.getInputStreamFromResourceOrThrow
import net.maxsmr.commonutils.android.media.readBytesFromUri
import net.maxsmr.commonutils.data.text.appendSubstringWhileLess
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder.*
import java.io.BufferedInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.math.BigInteger
import java.security.DigestInputStream
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.zip.CRC32

const val ALGORITHM_DEFAULT = "SHA-1"

const val MD5_HASH_CHARS_COUNT_DEFAULT = 32

private val logger = BaseLoggerHolder.getInstance().getLogger<BaseLogger>("HashUtils")

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
        uri: Uri,
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
        uri: Uri,
        algorithm: MessageDigest
): ByteArray =
        digestOrThrow(getInputStreamFromResourceOrThrow(context, uri), algorithm, true)

@JvmOverloads
fun digest(
        `is`: InputStream,
        algorithm: String = ALGORITHM_DEFAULT,
        closeStream: Boolean = true
): ByteArray? = try {
    digestOrThrow(`is`, algorithm, closeStream)
} catch (e: RuntimeException) {
    logger.e(e)
    null
}

@Throws(RuntimeException::class)
@JvmOverloads
fun digestOrThrow(
        `is`: InputStream,
        algorithm: String = ALGORITHM_DEFAULT,
        closeStream: Boolean = true
): ByteArray =
        digestOrThrow(`is`, digestSafe(algorithm), closeStream)

@JvmOverloads
fun digest(
        `is`: InputStream,
        algorithm: MessageDigest,
        closeStream: Boolean = true
): ByteArray? = try {
    digestOrThrow(`is`, algorithm, closeStream)
} catch (e: RuntimeException) {
    logger.e(e)
    null
}

@Throws(RuntimeException::class)
@JvmOverloads
fun digestOrThrow(
        `is`: InputStream,
        algorithm: MessageDigest,
        closeStream: Boolean = true
): ByteArray {
    algorithm.reset()
    val bis = BufferedInputStream(`is`)
    val dis = DigestInputStream(bis, algorithm)
    try {
        while (dis.read() != -1) {
        }
    } catch (e: IOException) {
        throwRuntimeException(e, "read")
    } finally {
        if (closeStream) {
            try {
                `is`.close()
            } catch (e: IOException) {
                logException(logger, e, "close")
            }
        }
    }
    return algorithm.digest()
}

@Throws(RuntimeException::class)
fun digest(data: ByteArray, algorithm: String = ALGORITHM_DEFAULT): ByteArray? = try {
    digestOrThrow(data, algorithm)
} catch (e: RuntimeException) {
    logger.e(e)
    null
}

@Throws(RuntimeException::class)
@JvmOverloads
fun digestOrThrow(data: ByteArray, algorithm: String = ALGORITHM_DEFAULT): ByteArray =
        digestOrThrow(data, digestSafe(algorithm))

fun digest(data: ByteArray, algorithm: MessageDigest): ByteArray? = try {
    digestOrThrow(data, algorithm)
} catch (e: RuntimeException) {
    logger.e(e)
    null
}

@Throws(RuntimeException::class)
fun digestOrThrow(data: ByteArray, algorithm: MessageDigest): ByteArray {
    algorithm.reset()
    try {
        algorithm.update(data, 0, data.size)
    } catch (e: RuntimeException) {
        throwRuntimeException(e, "update")
    }
    return algorithm.digest()
}

@JvmOverloads
fun stringMd5Digest(
        digest: ByteArray,
        minCharsCount: Int = MD5_HASH_CHARS_COUNT_DEFAULT
): String = appendSubstringWhileLess(
        BigInteger(1, digest).toString(16),
        minCharsCount,
        true,
        "0"
).toString()


fun getCrc32Hash(contentResolver: ContentResolver, uri: Uri): Long =
        getCrc32Hash(readBytesFromUri(contentResolver, uri))

fun getCrc32Hash(file: File): Long =
        getCrc32Hash(readBytesFromFile(file))

fun getCrc32Hash(array: ByteArray?): Long = array?.let {
    with(CRC32()) {
        update(array)
        return@let this.value
    }
} ?: 0L

private fun digestSafe(algorithm: String) = try {
    MessageDigest.getInstance(algorithm)
} catch (e: NoSuchAlgorithmException) {
    throw RuntimeException(formatException(e, "getInstance"), e)
}
