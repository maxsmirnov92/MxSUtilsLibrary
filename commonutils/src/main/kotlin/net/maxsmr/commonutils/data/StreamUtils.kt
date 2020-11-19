package net.maxsmr.commonutils.data

import android.util.Log
import net.maxsmr.commonutils.data.text.EMPTY_STRING
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import java.io.*

// Вспомогательные методы для чтения из {@link InputStream]
// и записи в {@link OutputStream}

private val logger = BaseLoggerHolder.getInstance().getLogger<BaseLogger>("StreamUtils")

const val CHARSET_DEFAULT = "UTF-8"

/**
 * @return amount of bytes copied
 */
@JvmOverloads
fun copyStream(
        `in`: InputStream,
        out: OutputStream,
        notifier: IStreamNotifier? = null,
        buffSize: Int = DEFAULT_BUFFER_SIZE,
        closeInput: Boolean = true,
        closeOutput: Boolean = true
): Int? = try {
    copyStreamOrThrow(`in`, out, notifier, buffSize, closeInput, closeOutput)
} catch (e: IOException) {
    logger.e(e)
    null
}

/**
 * @return amount of bytes copied
 */
@Throws(IOException::class)
@JvmOverloads
fun copyStreamOrThrow(
        `in`: InputStream,
        out: OutputStream,
        notifier: IStreamNotifier? = null,
        buffSize: Int = DEFAULT_BUFFER_SIZE,
        closeInput: Boolean = true,
        closeOutput: Boolean = true
): Int {
    require(buffSize > 0) { "buffSize" }

    var bytesWriteCount = 0

    try {
        val buff = ByteArray(buffSize)
        val totalBytesCount = `in`.available()

        var len: Int
        var lastNotifyTime: Long = 0
        while (`in`.read(buff).also { len = it } > 0) {
            if (notifier != null) {
                val interval = notifier.notifyInterval
                if (interval >= 0 && (interval == 0L || lastNotifyTime == 0L || System.currentTimeMillis() - lastNotifyTime >= interval)) {
                    if (!notifier.onProcessing(
                                    `in`, out, bytesWriteCount.toLong(),
                                    if (totalBytesCount > 0 && bytesWriteCount <= totalBytesCount) (totalBytesCount - bytesWriteCount).toLong() else 0.toLong()
                            )
                    ) {
                        throw InterruptedIOException("Copying streams interrupted")
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
    return bytesWriteCount
}

@JvmOverloads
fun readBytesFromInputStream(inputStream: InputStream, closeInput: Boolean = true): ByteArray? =
        try {
            readBytesFromInputStreamOrThrow(inputStream, closeInput)
        } catch (e: IOException) {
            logger.e(e)
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
    logger.e(e)
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
    logger.e(e)
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
    copyStreamOrThrow(inputStream, result)
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
    @JvmDefault
    fun onProcessing(
            inputStream: InputStream,
            outputStream: OutputStream,
            bytesWrite: Long,
            bytesLeft: Long
    ): Boolean = true
}