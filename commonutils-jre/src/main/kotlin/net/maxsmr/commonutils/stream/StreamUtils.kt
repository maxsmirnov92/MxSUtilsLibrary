package net.maxsmr.commonutils.stream

import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.commonutils.text.NEXT_LINE
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder.Companion.logException
import java.io.*
import java.nio.charset.Charset
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

// Вспомогательные методы для чтения из {@link InputStream]
// и записи в {@link OutputStream}

private val logger = BaseLoggerHolder.instance.getLogger<BaseLogger>("StreamUtils")

/**
 * @return amount of bytes copied
 */
@JvmOverloads
fun InputStream.copyStream(
    out: OutputStream,
    notifier: IStreamNotifier? = null,
    buffSize: Int = DEFAULT_BUFFER_SIZE,
    closeInput: Boolean = true,
    closeOutput: Boolean = true
): Long? = try {
    copyStreamOrThrow(out, notifier, buffSize, closeInput, closeOutput)
} catch (e: IOException) {
    logException(logger, e, "copyStream")
    null
}

/**
 * @return amount of bytes copied
 */
@Throws(IOException::class)
@JvmOverloads
fun InputStream.copyStreamOrThrow(
    out: OutputStream,
    notifier: IStreamNotifier? = null,
    buffSize: Int = DEFAULT_BUFFER_SIZE,
    closeInput: Boolean = true,
    closeOutput: Boolean = true
): Long {
    require(buffSize > 0) { "buffSize" }

    var bytesWriteCount = 0L

    try {
        val buff = ByteArray(buffSize)
        val totalBytesCount = this.available().toLong()

        var len: Int
        var lastNotifyTime: Long = 0
        while (this.read(buff).also { len = it } > 0) {
            if (notifier != null) {
                val interval = notifier.notifyInterval
                val current = System.currentTimeMillis()
                if (interval >= 0 && (interval == 0L || lastNotifyTime == 0L || current - lastNotifyTime >= interval)) {
                    if (!notifier.onProcessing(
                            this, out, bytesWriteCount,
                            if (totalBytesCount >= bytesWriteCount) {
                                totalBytesCount
                            } else {
                                0L
                            }
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
        notifier?.onStreamEnd(this, out, bytesWriteCount)
    } finally {
        if (closeInput) {
            this.close()
        }
        if (closeOutput) {
            out.close()
        }
    }
    return bytesWriteCount
}

@JvmOverloads
fun InputStream.readBytes(
    offset: Int = 0,
    length: Int = 0,
    closeInput: Boolean = true
): ByteArray? = try {
    readBytesOrThrow(offset, length, closeInput)
} catch (e: IOException) {
    logException(logger, e, "readBytes")
    null
}

@Throws(IOException::class)
@JvmOverloads
fun InputStream.readBytesOrThrow(
    offset: Int = 0,
    length: Int = 0,
    closeInput: Boolean = true
): ByteArray {
    try {
        val available = available()
        val data = ByteArray(if (length in 1..available) length else available)
        var readByteCount: Int
        do {
            readByteCount = read(
                data,
                if (offset >= 0) offset else 0,
                data.size
            )
        } while (readByteCount > 0 && length <= 0)
        return data
    } finally {
        if (closeInput) {
            close()
        }
    }
}

@JvmOverloads
fun InputStream.readStrings(
    count: Int = 0,
    closeInput: Boolean = true,
    charsetName: String = Charset.defaultCharset().name()
): List<String> = try {
    readStringsOrThrow(count, closeInput, charsetName)
} catch (e: IOException) {
    logException(logger, e, "readStrings")
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
fun InputStream.readStringsOrThrow(
    count: Int = 0,
    closeInput: Boolean = true,
    charsetName: String = Charset.defaultCharset().name()
): List<String> {
    val out = mutableListOf<String>()
    var `in`: BufferedReader? = null
    try {
        `in` = BufferedReader(InputStreamReader(this, charsetName))
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

@JvmOverloads
fun InputStream.readString(
    closeInput: Boolean = true,
    charsetName: String = Charset.defaultCharset().name()
): String? = try {
    readStringOrThrow(closeInput, charsetName)
} catch (e: IOException) {
    logException(logger, e, "readString")
    null
}


@Throws(IOException::class)
@JvmOverloads
fun InputStream.readStringOrThrow(
    closeInput: Boolean = true,
    charsetName: String = Charset.defaultCharset().name()
): String {
    val result = ByteArrayOutputStream()
    copyStreamOrThrow(result, closeInput = closeInput)
    return result.toString(charsetName)
}

@JvmOverloads
fun OutputStream.writeBytes(
    data: ByteArray,
    closeOutput: Boolean = true
) = try {
    writeBytesOrThrow(data, closeOutput)
    true
} catch (e: IOException) {
    logException(logger, e, "writeBytes")
    false
}

@Throws(IOException::class)
@JvmOverloads
fun OutputStream.writeBytesOrThrow(data: ByteArray, closeOutput: Boolean = true) {
    try {
        write(data)
        flush()
    } finally {
        if (closeOutput) {
            close()
        }
    }
}

@JvmOverloads
fun Writer.writeString(data: Collection<String>?, closeOutput: Boolean = true) = try {
    writeStringOrThrow(data, closeOutput)
    true
} catch (e: IOException) {
    logException(logger, e, "writeString")
    false
}

@Throws(IOException::class)
@JvmOverloads
fun Writer.writeStringOrThrow(data: Collection<String>?, closeOutput: Boolean = true) {
    val bw = BufferedWriter(this)
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
            stream.copyStreamOrThrow(zos, notifier, buffSize, closeInput = closeInput, closeOutput = false)
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

@JvmOverloads
fun InputStream.unzipStream(
    saveDirHierarchy: Boolean = true,
    buffSize: Int = DEFAULT_BUFFER_SIZE,
    notifier: IStreamNotifier? = null,
    closeInput: Boolean = true,
    closeOutput: Boolean = true,
    createDirFunc: (String) -> Unit,
    createOutputStream: (String) -> OutputStream
) = try {
    unzipStreamOrThrow(
        saveDirHierarchy,
        buffSize,
        notifier,
        closeInput,
        closeOutput,
        createDirFunc,
        createOutputStream
    )
} catch (e: IOException) {
    logException(logger, e, "unzipStreamOrThrow")
}


@Throws(IOException::class)
@JvmOverloads
fun InputStream.unzipStreamOrThrow(
    saveDirHierarchy: Boolean = true,
    buffSize: Int = DEFAULT_BUFFER_SIZE,
    notifier: IStreamNotifier? = null,
    closeInput: Boolean = true,
    closeOutput: Boolean = true,
    createDirFunc: (String) -> Unit,
    createOutputStream: (String) -> OutputStream
) {
    val zis = ZipInputStream(this)
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
                zis.copyStreamOrThrow(createOutputStream(entryName), notifier, buffSize, false, closeOutput)
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

    val notifyInterval: Long get() = 0L

    /**
     * @return true if should proceed
     */
    fun onProcessing(
        inputStream: InputStream,
        outputStream: OutputStream,
        bytesWrite: Long,
        bytesTotal: Long
    ): Boolean = true

    fun onStreamEnd(
        inputStream: InputStream,
        outputStream: OutputStream,
        bytesWrite: Long
    ) {
        // do nothing
    }
}