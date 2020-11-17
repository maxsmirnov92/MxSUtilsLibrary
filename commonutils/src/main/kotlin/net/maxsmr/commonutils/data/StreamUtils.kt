package net.maxsmr.commonutils.data

import net.maxsmr.commonutils.data.text.EMPTY_STRING
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder.logException
import java.io.*

// Вспомогательные методы для чтения из {@link InputStream]
// и записи в {@link OutputStream}

private val logger = BaseLoggerHolder.getInstance().getLogger<BaseLogger>("StreamUtils")

const val STREAM_BUF_SIZE_DEFAULT = 256

const val CHARSET_DEFAULT = "UTF-8"

@JvmOverloads
fun revectorStream(
        `in`: InputStream,
        out: OutputStream,
        notifier: IStreamNotifier? = null,
        buffSize: Int = STREAM_BUF_SIZE_DEFAULT,
        closeInput: Boolean = true,
        closeOutput: Boolean = true
): Boolean =
        try {
            revectorStreamOrThrow(`in`, out, notifier, buffSize, closeInput, closeOutput)
            true
        } catch (e: IOException) {
            logException(logger, e, "revectorStream")
            false
        }

@Throws(IOException::class)
@JvmOverloads
fun revectorStreamOrThrow(
        `in`: InputStream,
        out: OutputStream,
        notifier: IStreamNotifier? = null,
        buffSize: Int = STREAM_BUF_SIZE_DEFAULT,
        closeInput: Boolean = true,
        closeOutput: Boolean = true
) {
    require(buffSize > 0) { "buffSize" }

    try {
        val buff = ByteArray(buffSize)
        var bytesWriteCount = 0
        var totalBytesCount = 0
        try {
            totalBytesCount = `in`.available()
        } catch (e: IOException) {
        }
        var len: Int
        var lastNotifyTime: Long = 0
        while (`in`.read(buff).also { len = it } > 0) {
            if (notifier != null) {
                val interval = notifier.notifyInterval
                if (interval >= 0 && (interval == 0L || lastNotifyTime == 0L || System.currentTimeMillis() - lastNotifyTime >= interval)) {
                    if (!notifier.onProcessing(`in`, out, bytesWriteCount.toLong(),
                                    if (totalBytesCount > 0 && bytesWriteCount <= totalBytesCount) (totalBytesCount - bytesWriteCount).toLong() else 0.toLong())) {
                        throw InterruptedIOException("Revector streams interrupted")
                    }
                    lastNotifyTime = System.currentTimeMillis()
                }
            }
            out.write(buff, 0, len)
            bytesWriteCount += len
        }
    } finally {
        if (closeInput) {
            `in`.close()
        }
        if (closeOutput) {
            out.close()
        }
    }
}

@JvmOverloads
fun readBytesFromInputStream(inputStream: InputStream, closeInput: Boolean = true): ByteArray? =
        try {
            readBytesFromInputStreamOrThrow(inputStream, closeInput)
        } catch (e: IOException) {
            logException(logger, e, "readBytesFromInputStream")
            null
        }

@Throws(IOException::class)
@JvmOverloads
fun readBytesFromInputStreamOrThrow(inputStream: InputStream, closeInput: Boolean = true): ByteArray {
    try {
        val data = ByteArray(inputStream.available())
        var readByteCount: Int
        do {
            readByteCount = inputStream.read(data, 0, data.size)
        } while (readByteCount > 0)
        return data
    } finally {
        if (closeInput) {
            inputStream.close()
        }
    }
}

@JvmOverloads
fun readStringsFromInputStream(
        `is`: InputStream,
        count: Int = 0,
        closeInput: Boolean = true,
        charsetName: String = CHARSET_DEFAULT
): List<String> = try {
    readStringsFromInputStreamOrThrow(`is`, count, closeInput, charsetName)
} catch (e: IOException) {
    logException(logger, e, "readStringsFromInputStream")
    emptyList()
}

/**
 * Читает в несколько строк содержимое [inputStream]
 *
 * @param count       количество исходных строк для чтения
 * @param charsetName имя кодировки
 */
@Throws(IOException::class)
@JvmOverloads
fun readStringsFromInputStreamOrThrow(
        `is`: InputStream,
        count: Int = 0,
        closeInput: Boolean = true,
        charsetName: String = CHARSET_DEFAULT
): List<String> {
    val out = mutableListOf<String>()
    var `in`: BufferedReader? = null
    try {
        `in` = BufferedReader(InputStreamReader(`is`, charsetName))
        var line: String = EMPTY_STRING
        while ((count <= 0 || out.size < count) && `in`.readLine().also { line = it } != null) {
            out.add(line)
        }
        return out
    } finally {
        if (closeInput) {
            `in`?.close()
        }
    }
}

@JvmOverloads
fun writeBytesToOutputStream(
        outputStream: OutputStream,
        data: ByteArray,
        closeOutput: Boolean = true
) = try {
    writeBytesToOutputStreamOrThrow(outputStream, data, closeOutput)
    true
} catch (e: IOException) {
    logException(logger, e, "writeBytesToOutputStreamOrThrow")
    false
}

@Throws(IOException::class)
@JvmOverloads
fun writeBytesToOutputStreamOrThrow(
        outputStream: OutputStream,
        data: ByteArray,
        closeOutput: Boolean = true
) {
    try {
        outputStream.write(data)
        outputStream.flush()
    } finally {
        if (closeOutput) {
            outputStream.close()
        }
    }
}

@JvmOverloads
fun convertInputStreamToString(inputStream: InputStream, charsetName: String = CHARSET_DEFAULT): String? {
    val result = ByteArrayOutputStream()
    revectorStreamOrThrow(inputStream, result)
    return try {
        result.toString(charsetName)
    } catch (e: UnsupportedEncodingException) {
        logger.e("An UnsupportedEncodingException occurred", e)
        null
    }
}

interface IStreamNotifier {

    val notifyInterval: Long

    /**
     * @return true if should proceed
     */
    fun onProcessing(
            inputStream: InputStream,
            outputStream: OutputStream,
            bytesWrite: Long,
            bytesLeft: Long
    ): Boolean = true
}