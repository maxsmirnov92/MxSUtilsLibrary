package net.maxsmr.commonutils

import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder.Companion.formatException
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder.Companion.logException
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder.Companion.throwRuntimeException
import net.maxsmr.commonutils.text.EMPTY_STRING
import java.io.BufferedInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset
import java.security.DigestInputStream
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*
import java.util.zip.CRC32

private const val HEX_CHARS = "0123456789ABCDEF"

const val ALGORITHM_SHA1 = "SHA-1"
const val ALGORITHM_MD5 = "MD5"
const val ALGORITHM_CRC32 = "CRC32"

const val REG_EX_ALGORITHM_SHA1 = "^[a-fA-F0-9]{40}$"
const val REG_EX_ALGORITHM_MD5 = "^[a-fA-F0-9]{32}$"
const val REG_EX_ALGORITHM_CRC32 = "^[a-fA-F0-9]{8}$"

private val logger = BaseLoggerHolder.instance.getLogger<BaseLogger>("HashUtils")

fun messageDigest(algorithm: String): MessageDigest? = try {
    messageDigestOrThrow(algorithm)
} catch (e: RuntimeException) {
    logException(logger, e)
    null
}

@Throws(RuntimeException::class)
fun messageDigestOrThrow(algorithm: String): MessageDigest = try {
    MessageDigest.getInstance(algorithm)
} catch (e: NoSuchAlgorithmException) {
    throw RuntimeException(formatException(e, "getInstance"), e)
}

@JvmOverloads
fun InputStream.digest(algorithm: String, closeStream: Boolean = true): ByteArray? = try {
    digestOrThrow(algorithm, closeStream)
} catch (e: RuntimeException) {
    logger.e(e)
    null
}

@Throws(RuntimeException::class)
@JvmOverloads
fun InputStream.digestOrThrow(algorithm: String, closeStream: Boolean = true): ByteArray =
        digestOrThrow(messageDigestOrThrow(algorithm), closeStream)

@JvmOverloads
fun InputStream.digest(algorithm: MessageDigest, closeStream: Boolean = true): ByteArray? = try {
    digestOrThrow(algorithm, closeStream)
} catch (e: RuntimeException) {
    logger.e(e)
    null
}

@Throws(RuntimeException::class)
@JvmOverloads
fun InputStream.digestOrThrow(md: MessageDigest, closeStream: Boolean = true): ByteArray {
    md.reset()
    val bis = BufferedInputStream(this)
//    val dis = DigestInputStream(bis, md)
    try {
        bis.use {
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var bytesRead: Int
            do {
                bytesRead = bis.read(buffer)
                if (bytesRead > 0) {
                    md.update(buffer, 0, bytesRead)
                }
            } while (bytesRead > 0)
        }
    } catch (e: IOException) {
        throwRuntimeException(e, "digest read/update")
    }
    return md.digest()
}

@Throws(RuntimeException::class)
fun String.digest(algorithm: String, charset: Charset = Charsets.UTF_8): ByteArray? = try {
    digestOrThrow(algorithm, charset)
} catch (e: RuntimeException) {
    logger.e(e)
    null
}

@Throws(RuntimeException::class)
@JvmOverloads
fun String.digestOrThrow(algorithm: String, charset: Charset = Charsets.UTF_8): ByteArray =
        digestOrThrow(messageDigestOrThrow(algorithm), charset)

fun String.digest(algorithm: MessageDigest, charset: Charset = Charsets.UTF_8): ByteArray? = try {
    digestOrThrow(algorithm, charset)
} catch (e: RuntimeException) {
    logger.e(e)
    null
}

@Throws(RuntimeException::class)
fun String.digestOrThrow(algorithm: MessageDigest, charset: Charset = Charsets.UTF_8): ByteArray = toByteArray(charset).digestOrThrow(algorithm)

@Throws(RuntimeException::class)
fun ByteArray.digest(algorithm: String): ByteArray? = try {
    digestOrThrow(algorithm)
} catch (e: RuntimeException) {
    logger.e(e)
    null
}

@Throws(RuntimeException::class)
fun ByteArray.digestOrThrow(algorithm: String): ByteArray =
        digestOrThrow(messageDigestOrThrow(algorithm))

fun ByteArray.digest(algorithm: MessageDigest): ByteArray? = try {
    digestOrThrow(algorithm)
} catch (e: RuntimeException) {
    logger.e(e)
    null
}

@Throws(RuntimeException::class)
fun ByteArray.digestOrThrow(algorithm: MessageDigest): ByteArray {
    algorithm.reset()
    try {
        algorithm.update(this, 0, size)
    } catch (e: RuntimeException) {
        throwRuntimeException(e, "update")
    }
    return algorithm.digest()
}

@JvmOverloads
fun String?.getCrc32Hash(charset: Charset = Charsets.UTF_8): Long = this?.let {
    it.toByteArray(charset).getCrc32Hash()
} ?: 0L

fun ByteArray?.getCrc32Hash(): Long = this?.let {
    with(CRC32()) {
        update(it)
        this.value
    }
} ?: 0L

fun ByteArray?.toHexString(): String {
    this ?: return EMPTY_STRING

    val result = StringBuilder(size * 2)
    forEach {
        val i = it.toInt()
        result.append(HEX_CHARS[i shr 4 and 0x0f])
        result.append(HEX_CHARS[i and 0x0f])
    }
    return result.toString().toLowerCase(Locale.getDefault())
}

fun File?.getCrc32Hash(): Long =
        readBytesFromFile(this).getCrc32Hash()