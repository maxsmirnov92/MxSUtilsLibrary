package net.maxsmr.commonutils.data

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import net.maxsmr.commonutils.android.media.getInputStreamFromResourceOrThrow
import net.maxsmr.commonutils.android.media.readBytesFromUri
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder.logException
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder.throwRuntimeException
import java.io.BufferedInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.security.DigestInputStream
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.zip.CRC32

const val DEFAULT_ALGORITHM = "SHA-1"

private val logger = BaseLoggerHolder.getInstance().getLogger<BaseLogger>("HashUtils")

@JvmOverloads
fun digest(
        context: Context,
        uri: Uri,
        algorithm: String = DEFAULT_ALGORITHM
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
        algorithm: String = DEFAULT_ALGORITHM
): ByteArray {
    val messageDigest = try {
        MessageDigest.getInstance(algorithm)
    } catch (e: NoSuchAlgorithmException) {
        throw RuntimeException("A NoSuchAlgorithmException occurred during getInstance")
    }
    return digestOrThrow(context, uri, messageDigest)
}

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
        algorithm: String = DEFAULT_ALGORITHM,
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
        algorithm: String = DEFAULT_ALGORITHM,
        closeStream: Boolean = true
): ByteArray? {
    val messageDigest = try {
        MessageDigest.getInstance(algorithm)
    } catch (e: NoSuchAlgorithmException) {
        throw RuntimeException("A NoSuchAlgorithmException occurred during getInstance")
    }
    return digestOrThrow(`is`, messageDigest, closeStream)
}

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

