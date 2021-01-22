package net.maxsmr.commonutils.data

import net.maxsmr.commonutils.data.text.EMPTY_STRING
import net.maxsmr.commonutils.data.text.NEXT_LINE
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder.logException
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.Pair

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
    logException(logger, e, "copyStream")
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
fun readBytesFromInputStream(
        inputStream: InputStream,
        offset: Int = 0,
        length: Int = 0,
        closeInput: Boolean = true
): Pair<ByteArray, Int>? = try {
            readBytesFromInputStreamOrThrow(inputStream, offset, length, closeInput)
        } catch (e: IOException) {
            logException(logger, e, "readBytesFromInputStream")
            null
        }

@Throws(IOException::class)
@JvmOverloads
fun readBytesFromInputStreamOrThrow(
        inputStream: InputStream,
        offset: Int = 0,
        length: Int = 0,
        closeInput: Boolean = true
): Pair<ByteArray, Int> {
    try {
        val available = inputStream.available()
        val data = ByteArray(if (length in 1..available) length else available)
        var readByteCount: Int
        do {
            readByteCount = inputStream.read(
                    data,
                    if (offset >= 0) offset else 0,
                    data.size
            )
        } while (readByteCount > 0 && length <= 0)
        return Pair(data, inputStream.available())
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
        while ((count <= 0 || out.size < count) && `in`.readLine().also { line = it ?: EMPTY_STRING } != null) {
            out.add(line)
        }
        return out
    } finally {
        if (closeInput) {
            `in`?.close()
        }
    }
}

fun readStringFromInputStream(inputStream: InputStream, charsetName: String = CHARSET_DEFAULT): String? = try {
    readStringFromInputStreamOrThrow(inputStream, charsetName)
} catch (e: IOException) {
    logException(logger, e, "readStringFromInputStream")
    null
}


@Throws(IOException::class)
@JvmOverloads
fun readStringFromInputStreamOrThrow(inputStream: InputStream, charsetName: String = CHARSET_DEFAULT): String? {
    val result = ByteArrayOutputStream()
    copyStreamOrThrow(inputStream, result)
    return result.toString(charsetName)
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
    logException(logger, e, "writeBytesToOutputStream")
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
fun writeStringToOutputStreamWriter(
        writer: OutputStreamWriter,
        data: Collection<String>?,
        closeOutput: Boolean = true
) = try {
    writeStringToOutputStreamWriterOrThrow(writer, data, closeOutput)
    true
} catch (e: IOException) {
    logException(logger, e, "writeStringToOutputStreamWriter")
    false
}

@Throws(IOException::class)
@JvmOverloads
fun writeStringToOutputStreamWriterOrThrow(
        writer: OutputStreamWriter,
        data: Collection<String>?,
        closeOutput: Boolean = true
) {
    val bw = BufferedWriter(writer)
    try {
        for (line in (data ?: emptyList())) {
            bw.append(line)
            bw.append(NEXT_LINE)
            bw.flush()
        }
    } finally {
        if (closeOutput) {
            bw.close()
        }
    }
}

@JvmOverloads
fun compressStreamsToZip(
        inputStreams: Map<String, InputStream>,
        outputStream: OutputStream,
        buffSize: Int = DEFAULT_BUFFER_SIZE,
        notifier: IStreamNotifier? = null,
        closeInput: Boolean = true,
        closeOutput: Boolean = true
): Int = try {
    compressStreamsToZipOrThrow(inputStreams, outputStream, buffSize, notifier, closeInput, closeOutput)
} catch (e: IOException) {
    logException(logger, e, "compressStreamsToZip")
    0
}

@Throws(IOException::class)
@JvmOverloads
fun compressStreamsToZipOrThrow(
        inputStreams: Map<String, InputStream>,
        outputStream: OutputStream,
        buffSize: Int = DEFAULT_BUFFER_SIZE,
        notifier: IStreamNotifier? = null,
        closeInput: Boolean = true,
        closeOutput: Boolean = true
): Int {
    val zos = ZipOutputStream(BufferedOutputStream(outputStream))

    var count = 0

    val copyStreamsMap = inputStreams.toMutableMap()

    try {
        for (name in copyStreamsMap.keys.toMutableList()) {
            val stream = copyStreamsMap[name] ?: continue
            val entry = ZipEntry(name)
            zos.putNextEntry(entry)
            copyStreamOrThrow(stream, zos, notifier, buffSize, closeInput = closeInput, closeOutput = false)
            zos.closeEntry()
            copyStreamsMap.remove(name)
            count++
        }
    } finally {
        zos.close()
        if (closeOutput) {
            outputStream.close()
        }
        if (closeInput) {
            copyStreamsMap.values.forEach {
                it.close()
            }
        }
    }
    return count
}

@Throws(IOException::class)
@JvmOverloads
fun unzipStreamOrThrow(
        inputStream: InputStream,
        saveDirHierarchy: Boolean = true,
        buffSize: Int = DEFAULT_BUFFER_SIZE,
        notifier: IStreamNotifier? = null,
        closeInput: Boolean = true,
        closeOutput: Boolean = true,
        createDirFunc: (String) -> Unit,
        createOutputStream: (String) -> OutputStream
) {
    val zis = ZipInputStream(inputStream)
    val zipEntry = zis.nextEntry
    try {
        while (zipEntry != null) {
            val isDirectory = zipEntry.isDirectory
            if (isDirectory && !saveDirHierarchy) {
                continue
            }
            val parts = zipEntry.name.split(File.separator).toTypedArray()
            val entryName = if (!saveDirHierarchy && parts.isNotEmpty()) parts[parts.size - 1] else zipEntry.name
            if (isDirectory) {
                createDirFunc(entryName)
            } else {
                copyStreamOrThrow(zis, createOutputStream(entryName), notifier, buffSize, false, closeOutput)
            }
            zis.closeEntry()
        }
    } finally {
        if (closeInput) {
            zis.close()
        }
    }
}

interface IStreamNotifier {

    @JvmDefault
    val notifyInterval: Long
        get() = 0

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