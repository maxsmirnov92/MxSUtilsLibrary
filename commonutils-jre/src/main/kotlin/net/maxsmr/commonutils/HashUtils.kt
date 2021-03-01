package net.maxsmr.commonutils

import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.commonutils.text.appendSubstringWhileLess
import java.io.BufferedInputStream
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
        BaseLoggerHolder.throwRuntimeException(e, "read")
    } finally {
        if (closeStream) {
            try {
                `is`.close()
            } catch (e: IOException) {
                BaseLoggerHolder.logException(logger, e, "close")
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
        BaseLoggerHolder.throwRuntimeException(e, "update")
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

fun getCrc32Hash(array: ByteArray?): Long = array?.let {
    with(CRC32()) {
        update(array)
        return@let this.value
    }
} ?: 0L

fun digestSafe(algorithm: String) = try {
    MessageDigest.getInstance(algorithm)
} catch (e: NoSuchAlgorithmException) {
    throw RuntimeException(BaseLoggerHolder.formatException(e, "getInstance"), e)
}
