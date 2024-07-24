package net.maxsmr.commonutils

import net.maxsmr.commonutils.collection.sort.BaseOptionalComparator
import net.maxsmr.commonutils.collection.sort.ISortOption
import net.maxsmr.commonutils.conversion.SizeUnit
import net.maxsmr.commonutils.conversion.SizeUnit.Companion.convert
import net.maxsmr.commonutils.conversion.toLongNotNull
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder.Companion.formatException
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder.Companion.logException
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder.Companion.throwRuntimeException
import net.maxsmr.commonutils.shell.DEFAULT_TARGET_CODE
import net.maxsmr.commonutils.shell.ShellCallback
import net.maxsmr.commonutils.shell.ShellWrapper
import net.maxsmr.commonutils.text.*
import java.io.*
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.channels.OverlappingFileLockException
import java.nio.charset.Charset
import java.util.*
import java.util.concurrent.TimeUnit

const val REG_EX_FILE_NAME = "^[^<>:?\"/*|\\\\]+\$"

const val DEPTH_UNLIMITED = -1

private val logger = BaseLoggerHolder.instance.getLogger<BaseLogger>("FileUtils")

fun getEnvPathFiles(): Set<File> {
    val result = mutableSetOf<File>()
    val paths = (System.getenv("PATH").orEmpty()).split(":").toTypedArray()
    if (paths.isEmpty()) {
        return emptySet()
    }
    for (path in paths) {
        result.add(File(path))
    }
    return result
}

val File.extension: String get() = this.name.getExtension()

fun File.removeExtension(): String = this.name.removeExtension()

@JvmOverloads
fun File.appendExtension(extension: String?, shouldReplace: Boolean = true): File =
    File(this.parent, this.name.appendExtension(extension, shouldReplace))

fun File.appendPostfix(postfix: String?): File =
    File(this.parent, this.name.appendPostfix(postfix))

@JvmOverloads
fun getPartitionSpace(
    fileName: String,
    parentPath: String? = null,
    unit: SizeUnit = SizeUnit.BYTES,
    totalOrFree: Boolean,
): Double = try {
    getPartitionSpaceOrThrow(fileName, parentPath, unit, totalOrFree)
} catch (e: RuntimeException) {
    0.0
}

fun getPartitionSpace(
    file: File,
    unit: SizeUnit = SizeUnit.BYTES,
    totalOrFree: Boolean,
): Double = try {
    getPartitionSpaceOrThrow(file, unit, totalOrFree)
} catch (e: RuntimeException) {
    0.0
}

@Throws(RuntimeException::class)
@JvmOverloads
fun getPartitionSpaceOrThrow(
    fileName: String,
    parentPath: String? = null,
    unit: SizeUnit = SizeUnit.BYTES,
    totalOrFree: Boolean
): Double = toFile(fileName, parentPath)?.let { getPartitionSpaceOrThrow(it, unit, totalOrFree) } ?: run {
    throw RuntimeException("File is null")
}

@Throws(RuntimeException::class)
fun getPartitionSpaceOrThrow(
    file: File,
    unit: SizeUnit = SizeUnit.BYTES,
    totalOrFree: Boolean,
): Double {
    if (!isDirExistsOrThrow(file) && !isFileExistsOrThrow(file)) {
        throw RuntimeException("file or dir '$file' not exists")
    }
    return try {
        convert(if (totalOrFree) file.totalSpace else file.freeSpace, SizeUnit.BYTES, unit).toDouble()
    } catch (e: SecurityException) {
        throw RuntimeException("totalSpace / freeSpace on file '$file'", e)
    }
}

fun File.getCanonicalOrAbsolutePath(): String {
    return try {
        this.canonicalPath.orEmpty()
    } catch (e: IOException) {
        logException(logger, e, "getCanonicalPath on file '$this'")
        this.absolutePath.orEmpty()
    }
}

fun File.isLocked(): Boolean {
    lock().let {
        return try {
            it == null
        } finally {
            it.releaseSafe()
        }
    }
}

fun File.lock(isStrict: Boolean = false): FileLockInfo? {
    if (!exists()) {
        return null
    }
    var randomAccFile: RandomAccessFile? = null
    var channel: FileChannel? = null
    try {
        randomAccFile = RandomAccessFile(this, "rw")
        channel = randomAccFile.channel
        try {
            return FileLockInfo(
                if (!isStrict) channel.tryLock() else channel.lock(),
                channel,
                randomAccFile
            )
        } catch (e: IOException) {
            logException(logger, e, "tryLock")
        } catch (e: OverlappingFileLockException) {
            logException(logger, e, "tryLock")
        }
    } catch (e: FileNotFoundException) {
        logException(logger, e)
    }
    return null
}

fun FileLockInfo?.releaseSafe(): Boolean {
    this?.let {
        try {
            fileLock.release()
            fileChannel.close()
            randomAccFile.close()
            return true
        } catch (e: IOException) {
            logException(logger, e, "release/close")
        }
    }
    return false
}

data class FileLockInfo(
    val fileLock: FileLock,
    val fileChannel: FileChannel,
    val randomAccFile: RandomAccessFile
)

@JvmOverloads
fun isFileValid(fileName: String?, parentPath: String? = null): Boolean = try {
    isFileValidOrThrow(fileName, parentPath)
} catch (e: RuntimeException) {
    logger.e(e)
    false
}

fun isFileValid(file: File?): Boolean = try {
    isFileValidOrThrow(file)
} catch (e: RuntimeException) {
    logger.e(e)
    false
}

@Throws(RuntimeException::class)
@JvmOverloads
fun isFileValidOrThrow(fileName: String?, parentPath: String? = null): Boolean =
    isFileValidOrThrow(toFile(fileName, parentPath))

@Throws(RuntimeException::class)
fun isFileValidOrThrow(file: File?): Boolean = getFileLengthOrThrow(file) > 0

@JvmOverloads
fun getFileLength(fileName: String?, parentPath: String? = null): Long = try {
    getFileLengthOrThrow(fileName, parentPath)
} catch (e: RuntimeException) {
    logger.e(e)
    0
}

fun getFileLength(file: File?): Long = try {
    getFileLengthOrThrow(file)
} catch (e: RuntimeException) {
    logger.e(e)
    0
}

@Throws(RuntimeException::class)
@JvmOverloads
fun getFileLengthOrThrow(fileName: String?, parentPath: String? = null): Long =
    getFileLengthOrThrow(toFile(fileName, parentPath))

@Throws(RuntimeException::class)
fun getFileLengthOrThrow(file: File?): Long {
    if (file != null && isFileExistsOrThrow(file)) {
        try {
            return file.length()
        } catch (e: SecurityException) {
            throwRuntimeException(e, "length on file '$file'")
        }
    }
    return 0
}

@JvmOverloads
fun File.isFileReadOnlyAccessible(isFile: Boolean = true) =
    isReadAccessible(isFile) && !isWriteAccessible(isFile)

@JvmOverloads
fun File.isReadAccessible(isFile: Boolean = true) =
    isAccessible(isFile, forRead = true, forWrite = false)

@JvmOverloads
fun File.isWriteAccessible(isFile: Boolean = true) =
    isAccessible(isFile, forRead = false, forWrite = true)

@JvmOverloads
fun File.isAccessible(
    isFile: Boolean = true,
    forRead: Boolean = true,
    forWrite: Boolean = true
): Boolean = try {
    isAccessibleOrThrow(isFile, forRead, forWrite)
} catch (e: RuntimeException) {
    logger.e(e)
    false
}

@Throws(RuntimeException::class)
@JvmOverloads
fun File.isAccessibleOrThrow(
    isFile: Boolean = true,
    forRead: Boolean = true,
    forWrite: Boolean = true
): Boolean {
    if (!(if (isFile) {
            isFileExistsOrThrow(this)
        } else {
            isDirExistsOrThrow(this)
        })
    ) {
        throw RuntimeException("file or dir '$this' not exists")
    }
    return try {
        var isAccessible = true
        if (forRead) {
            isAccessible = isAccessible and this.canRead()
        }
        if (forWrite) {
            isAccessible = isAccessible and this.canWrite()
        }
        isAccessible
    } catch (e: SecurityException) {
        throw RuntimeException(formatException(e, "canRead / canWrite on file '$this'"), e)
    }
}

@JvmOverloads
fun isFileExists(fileName: String?, parentPath: String? = null): Boolean = try {
    isFileExistsOrThrow(fileName, parentPath)
} catch (e: RuntimeException) {
    logger.e(e)
    false
}

fun isFileExists(file: File?): Boolean = try {
    isFileExistsOrThrow(file)
} catch (e: RuntimeException) {
    logger.e(e)
    false
}

@Throws(RuntimeException::class)
@JvmOverloads
fun isFileExistsOrThrow(fileName: String?, parentPath: String? = null): Boolean =
    isFileExistsOrThrow(toFile(fileName, parentPath))

@Throws(RuntimeException::class)
fun isFileExistsOrThrow(file: File?): Boolean = isFileOrDirExistsOrThrow(file, true)

@JvmOverloads
fun isDirExists(dirName: String?, parentPath: String? = null): Boolean = try {
    isDirExistsOrThrow(dirName, parentPath)
} catch (e: RuntimeException) {
    logger.e(e)
    false
}

fun isDirExists(dir: File?): Boolean = try {
    isDirExistsOrThrow(dir)
} catch (e: RuntimeException) {
    logger.e(e)
    false
}


@Throws(RuntimeException::class)
@JvmOverloads
fun isDirExistsOrThrow(dirName: String?, parentPath: String? = null): Boolean =
    isDirExistsOrThrow(toFile(dirName, parentPath))

@Throws(RuntimeException::class)
fun isDirExistsOrThrow(dir: File?): Boolean = isFileOrDirExistsOrThrow(dir, false)

@Throws(RuntimeException::class)
private fun isFileOrDirExistsOrThrow(file: File?, isFile: Boolean?): Boolean {
    return file != null && file.exists() && (isFile == null || (if (isFile) file.isFile else file.isDirectory))
}

@JvmOverloads
fun isDirEmpty(dirName: String?, parentPath: String? = null): Boolean = try {
    isDirEmptyOrThrow(dirName, parentPath)
} catch (e: RuntimeException) {
    logger.e(e)
    false
}

fun isDirEmpty(dir: File?): Boolean = try {
    isDirEmptyOrThrow(dir)
} catch (e: RuntimeException) {
    logger.e(e)
    false
}

@Throws(RuntimeException::class)
@JvmOverloads
fun isDirEmptyOrThrow(dirName: String?, parentPath: String? = null): Boolean =
    isDirEmptyOrThrow(toFile(dirName, parentPath))

@Throws(RuntimeException::class)
fun isDirEmptyOrThrow(dir: File?): Boolean {
    if (dir != null && isDirExistsOrThrow(dir)) {
        val files: Array<File>? =
            try {
                dir.listFiles()
            } catch (e: SecurityException) {
                throw RuntimeException(formatException(e, "listFiles on dir '$dir'"), e)
            }
        return files.isNullOrEmpty()
    }
    return false
}

@Throws(IllegalArgumentException::class)
@JvmOverloads
fun checkFileOrThrow(fileName: String, parentPath: String? = null, createIfNotExists: Boolean = true) {
    checkFileOrThrow(toFile(fileName, parentPath), createIfNotExists)
}

@Throws(IllegalArgumentException::class)
@JvmOverloads
fun checkFileOrThrow(file: File?, createIfNotExists: Boolean = true) {
    require(checkFile(file, createIfNotExists)) { "Incorrect file: '$file'" }
}

@JvmOverloads
fun checkFile(fileName: String?, parentPath: String? = null, createIfNotExists: Boolean = true): Boolean =
    checkFile(toFile(fileName, parentPath), createIfNotExists)

@JvmOverloads
fun checkFile(file: File?, createIfNotExists: Boolean = true): Boolean {
    return file != null && (isFileExistsOrThrow(file) || createIfNotExists && createFile(file.name, file.parent) != null)
}

@Throws(IllegalArgumentException::class)
@JvmOverloads
fun checkDirOrThrow(dirName: String?, parentPath: String? = null, createIfNotExists: Boolean = true) {
    checkDirOrThrow(toFile(dirName, parentPath), createIfNotExists)
}

@Throws(IllegalArgumentException::class)
@JvmOverloads
fun checkDirOrThrow(dir: File?, createIfNotExists: Boolean = true) {
    require(checkDir(dir, createIfNotExists)) { "Incorrect file: '$dir'" }
}

@JvmOverloads
fun checkDir(dirName: String?, parentPath: String? = null, createIfNotExists: Boolean = true): Boolean =
    checkDir(toFile(dirName, parentPath), createIfNotExists)

@JvmOverloads
fun checkDir(dir: File?, createIfNotExists: Boolean = true): Boolean {
    return dir != null && (isDirExistsOrThrow(dir) || createIfNotExists && createDir(dir.absolutePath) != null)
}

@Throws(IllegalArgumentException::class)
@JvmOverloads
fun checkPathOrThrow(parent: String?, fileName: String?, createIfNotExists: Boolean = true): Boolean {
    if (!checkPath(parent, fileName, createIfNotExists)) {
        throw IllegalArgumentException("Incorrect path: '" + parent + File.separator + fileName + "'")
    }
    return false
}

@JvmOverloads
fun checkPath(fileName: String?, parentPath: String?, createIfNotExists: Boolean = true): Boolean {
    if (checkDir(parentPath, null, createIfNotExists)) {
        if (fileName != null && !isEmpty(fileName)) {
            if (checkFile(fileName, parentPath, createIfNotExists)) {
                return true
            }
        }
    }
    return false
}

@JvmOverloads
fun createFile(
    fileName: String,
    parentPath: String? = null,
    recreate: Boolean = true
): File? = try {
    createFileOrThrow(fileName, parentPath, recreate)
} catch (e: RuntimeException) {
    logger.e(e)
    null
}

/**
 * Создаёт файл с относительным именем fileName
 * по родительскому абсолютному или относительному пути parentPath
 *
 * @param recreate true, если целевой файл должен быть пересоздан,
 * если уже существует
 * @return null если целевой файл уже существует и не смог быть пересоздан
 */
@Throws(RuntimeException::class)
@JvmOverloads
fun createFileOrThrow(
    fileName: String,
    parentPath: String? = null,
    recreate: Boolean = true
): File {
    if (!parentPath.isNullOrEmpty()) {
        createDirOrThrow(parentPath)
    }
    val newFile = toFile(fileName, parentPath)
        ?: throw RuntimeException("Incorrect fileName ('$fileName') or parentPath ('$parentPath')")
    if (isFileExistsOrThrow(newFile)) {
        if (recreate) {
            deleteFileOrThrow(newFile)
        }
    }
    if (!isFileExistsOrThrow(newFile)) {
        try {
            if (!newFile.createNewFile()) {
                throw RuntimeException("Cannot create new file: '$newFile'")
            }
            if (!isFileExistsOrThrow(newFile)) {
                throw RuntimeException("File still not exists: '$newFile'")
            }
        } catch (e: Exception) {
            throwRuntimeException(e, "createNewFile on '$newFile'")
        }
    }
    return newFile
}

@JvmOverloads
fun createDir(
    dirName: String,
    parentPath: String? = null,
    throwIfExists: Boolean = false
): File? = try {
    createDirOrThrow(dirName, parentPath, throwIfExists)
} catch (e: RuntimeException) {
    logger.e(e)
    null
}

/**
 * @return существующая или созданная пустая директория
 */
@Throws(RuntimeException::class)
@JvmOverloads
fun createDirOrThrow(
    dirName: String,
    parentPath: String? = null,
    throwIfExists: Boolean = false
): File {
    val dir = toFile(dirName, parentPath)
        ?: throw RuntimeException("Incorrect dirName or parentPath")
    if (isDirExistsOrThrow(dir)) {
        if (throwIfExists) {
            throw RuntimeException("Cannot create dir '$dir': already exists")
        }
        return dir
    }
    val created = try {
        dir.mkdirs()
    } catch (e: SecurityException) {
        throw RuntimeException(formatException(e, "mkdirs on '$dir'"), e)
    }
    if (!created || !isDirExistsOrThrow(dir)) {
        throw RuntimeException("Cannot create new dir '$dir'")
    }
    return dir
}

@JvmOverloads
fun deleteEmptyDir(dir: File, throwIfNotExists: Boolean = false) = try {
    deleteEmptyDirOrThrow(dir, throwIfNotExists)
    true
} catch (e: RuntimeException) {
    logger.e(e)
    false
}

@Throws(RuntimeException::class)
@JvmOverloads
fun deleteEmptyDirOrThrow(dir: File, throwIfNotExists: Boolean = false) {
    if (!isDirExists(dir)) {
        if (throwIfNotExists) {
            throw RuntimeException("Cannot delete dir '$dir': already not exists")
        }
        return
    }
    if (!isDirEmptyOrThrow(dir)) {
        throw RuntimeException("Cannot delete non-empty dir")
    }
    try {
        if (!dir.delete()) {
            throw RuntimeException("delete on dir failed (false)")
        }
    } catch (e: SecurityException) {
        throwRuntimeException(e, "delete dir '$dir'")
    }
}

@JvmOverloads
fun deleteFile(file: File, throwIfNotExists: Boolean = false) = try {
    deleteFileOrThrow(file, throwIfNotExists)
    true
} catch (e: RuntimeException) {
    logger.e(e)
    false
}

@Throws(RuntimeException::class)
@JvmOverloads
fun deleteFileOrThrow(file: File, throwIfNotExists: Boolean = false) {
    if (!isFileExistsOrThrow(file)) {
        if (throwIfNotExists) {
            throw RuntimeException("Cannot delete file '$file': already not exists")
        }
        return
    }
    try {
        if (!file.delete()) {
            throw RuntimeException("delete file failed (false)")
        }
    } catch (e: SecurityException) {
        throwRuntimeException(e, "delete file '$file'")
    }
}

fun File.resetFile() = try {
    resetOrThrow()
    true
} catch (e: RuntimeException) {
    logger.e(e)
    false
}

@Throws(RuntimeException::class)
fun File.resetOrThrow() {
    if (!isFileExistsOrThrow(this)) {
        throw RuntimeException("file '$this' not exists")
    }
    if (getFileLengthOrThrow(this) == 0L) {
        return
    }
    var ra: RandomAccessFile? = null
    try {
        ra = RandomAccessFile(this, "rw")
        ra.setLength(0)
    } catch (e: Exception) {
        throwRuntimeException(e)
    } finally {
        if (ra != null) {
            try {
                ra.close()
            } catch (e: IOException) {
                logException(logger, e, "close")
            }
        }
    }
}

fun File.isBinaryFile(): Boolean = try {
    isBinaryOrThrow()
} catch (e: RuntimeException) {
    logger.e(e)
    false
}

@Throws(RuntimeException::class)
fun File.isBinaryOrThrow(): Boolean {
    val data = readBytesOrThrow()
    var ascii = 0
    var other = 0
    for (b in data) {
        if (b < 0x09) return true
        if (b.toInt() == 0x09 || b.toInt() == 0x0A || b.toInt() == 0x0C || b.toInt() == 0x0D) {
            ascii++
        } else if (b in 0x20..0x7E) {
            ascii++
        } else {
            other++
        }
    }
    return other != 0 && 100 * other / (ascii + other) > 95
}

@JvmOverloads
fun File.readBytes(
    offset: Int = 0,
    length: Int = 0
): ByteArray? = try {
    readBytesOrThrow(offset, length)
} catch (e: RuntimeException) {
    logger.e(e)
    null
}

@Throws(RuntimeException::class)
@JvmOverloads
fun File.readBytesOrThrow(
    offset: Int = 0,
    length: Int = 0
): ByteArray {
    if (!isFileValidOrThrow(this)) {
        throw RuntimeException("Invalid file: '$this'")
    }
    return try {
        this.openInputStreamOrThrow().readBytesOrThrow(offset, length)
    } catch (e: IOException) {
        throw RuntimeException(formatException(e, "readBytesFromInputStreamOrThrow"), e)
    }
}

@JvmOverloads
fun File.readStrings(
    count: Int = 0,
    charsetName: String = CHARSET_DEFAULT
): List<String> = try {
    readStringsOrThrow(count, charsetName)
} catch (e: RuntimeException) {
    logger.e(e)
    emptyList()
}

@Throws(RuntimeException::class)
@JvmOverloads
fun File.readStringsOrThrow(
    count: Int = 0,
    charsetName: String = CHARSET_DEFAULT
): List<String> {
    if (!isFileValidOrThrow(this)) {
        throw RuntimeException("Incorrect file: '$this'")
    }
    return try {
        openInputStreamOrThrow().readStringsOrThrow(count, charsetName = charsetName)
    } catch (e: IOException) {
        throw RuntimeException(formatException(e, "readStringsFromInputStream"), e)
    }
}

@JvmOverloads
fun File.writeBytes(
    data: ByteArray,
    append: Boolean = false
) = try {
    writeBytesOrThrow(data, append)
    true
} catch (e: RuntimeException) {
    logger.e(e)
    false
}

@Throws(RuntimeException::class)
@JvmOverloads
fun File.writeBytesOrThrow(
    data: ByteArray,
    append: Boolean = false
) {
    if (!isFileExistsOrThrow(this)) {
        createFileOrThrow(this.name, this.parent, !append)
    }
    return try {
        openOutputStreamOrThrow(append).writeBytesOrThrow(data)
    } catch (e: IOException) {
        throw RuntimeException(formatException(e, "writeBytesToOutputStream"), e)
    }
}

@JvmOverloads
fun File.writeStrings(
    data: Collection<String>?,
    append: Boolean = false,
    charset: Charset = Charset.forName("UTF-8")
) = try {
    writeStringsOrThrow(data, append, charset)
    true
} catch (e: RuntimeException) {
    logger.e(e)
    false
}

@Throws(RuntimeException::class)
@JvmOverloads
fun File.writeStringsOrThrow(
    data: Collection<String>?,
    append: Boolean = false,
    charset: Charset = Charset.forName("UTF-8")
) {
    if (!isFileExistsOrThrow(this)) {
        createFileOrThrow(this.name, this.parent, !append)
    }
    val writer: Writer = try {
        OutputStreamWriter(FileOutputStream(this, append), charset)
    } catch (e: IOException) {
        throw RuntimeException(formatException(e, "create Writer"), e)
    }
    try {
        writer.writeStringOrThrow(data)
    } catch (e: IOException) {
        throwRuntimeException(e, "writeStringToOutputStreamWriter")
    }
}

@JvmOverloads
fun File.writeFromStream(
    inputStream: InputStream,
    append: Boolean = false,
    notifier: IStreamNotifier? = null,
    buffSize: Int = DEFAULT_BUFFER_SIZE,
): Boolean = try {
    writeFromStreamOrThrow(inputStream, append, notifier, buffSize)
    true
} catch (e: RuntimeException) {
    logger.e(e)
    false
}

@Throws(RuntimeException::class)
@JvmOverloads
fun File.writeFromStreamOrThrow(
    inputStream: InputStream,
    append: Boolean = false,
    notifier: IStreamNotifier? = null,
    buffSize: Int = DEFAULT_BUFFER_SIZE,
) {
    if (!isFileExistsOrThrow(this)) {
        throw RuntimeException("File not exists")
    }
    try {
        inputStream.copyStreamOrThrow(this.openOutputStreamOrThrow(append), notifier, buffSize)
    } catch (e: IOException) {
        throwRuntimeException(e, "copyStream")
    }
}

@JvmOverloads
fun File.writeToStream(
    outputStream: OutputStream,
    notifier: IStreamNotifier? = null,
    buffSize: Int = DEFAULT_BUFFER_SIZE
): Boolean = try {
    writeToStreamOrThrow(outputStream, notifier, buffSize)
    true
} catch (e: RuntimeException) {
    logger.e(e)
    false
}

@Throws(RuntimeException::class)
@JvmOverloads
fun File.writeToStreamOrThrow(
    outputStream: OutputStream,
    notifier: IStreamNotifier? = null,
    buffSize: Int = DEFAULT_BUFFER_SIZE
) {
    try {
        openInputStreamOrThrow().copyStreamOrThrow(outputStream, notifier, buffSize)
    } catch (e: IOException) {
        throwRuntimeException(e, "copyStream")
    }
}

@JvmOverloads
fun moveFile(
    sourceFile: File,
    targetFile: File,
    deleteIfExists: Boolean,
    deleteEmptyDirs: Boolean,
    preserveFileDate: Boolean = true,
    copyWithBuffering: Boolean = true,
    notifier: ISingleCopyNotifier? = null
): File? {
    if (sourceFile == targetFile) {
        logger.w("Cannot rename: files match ('$sourceFile')")
        return targetFile
    }
    var targetFile = renameFile(sourceFile, targetFile.parent, targetFile.name, deleteIfExists, deleteEmptyDirs)
    if (targetFile != null) {
        targetFile = if (copyWithBuffering) {
            copyFileWithBuffering(sourceFile, targetFile.name, targetFile.parent, deleteIfExists, preserveFileDate, notifier)
        } else {
            try {
                copyFileOrThrow(sourceFile, targetFile.name, targetFile.parent, deleteIfExists, preserveFileDate)
            } catch (e: RuntimeException) {
                notifier?.onExceptionOccurred(e)
                null
            }
        }
        try {
            deleteFileOrThrow(sourceFile)
        } catch (e: RuntimeException) {
            notifier?.onExceptionOccurred(e)
        }
    }
    return targetFile
}

fun renameFile(
    sourceFile: File,
    targetDir: String,
    newFileName: String?,
    deleteIfExists: Boolean,
    deleteEmptyDirs: Boolean
): File? = try {
    renameFileOrThrow(sourceFile, targetDir, newFileName, deleteIfExists, deleteEmptyDirs)
} catch (e: RuntimeException) {
    logger.e(e)
    null
}

@Throws(RuntimeException::class)
fun renameFileOrThrow(
    sourceFile: File,
    targetDir: String,
    newFileName: String?,
    deleteIfExists: Boolean,
    deleteEmptyDirs: Boolean
): File {
    if (!isFileExistsOrThrow(sourceFile)) {
        throw RuntimeException("Source file not exists: '$sourceFile'")
    }
    if (newFileName.isNullOrEmpty()) {
        throw RuntimeException("File name for new file is not specified")
    }
    val newFile: File
    val newDir = createDirOrThrow(targetDir)
    newFile = File(newDir, newFileName)
    if (newFile != sourceFile) {
        if (isFileOrDirExistsOrThrow(newFile, null)) {
            logger.d("Target file '$newFile' already exists")
            if (deleteIfExists) {
                if (!deleteFile(newFile)) {
                    throw RuntimeException("Delete file '$newFile' failed")
                }
            } else {
                throw RuntimeException("File '$newFile' exists, not deleting")
            }
        }
        val renamed = try {
            sourceFile.renameTo(newFile)
        } catch (e: SecurityException) {
            throw RuntimeException(formatException(e, "rename '$sourceFile' to '$newFile'"), e)
        }
        if (renamed) {
            val sourceParentDir = sourceFile.parentFile
            if (deleteEmptyDirs) {
                deleteEmptyDir(sourceParentDir)
            }
        } else {
            throw RuntimeException("Rename '$sourceFile' to '$newFile' failed")
        }
    } else {
        logger.w("New file '$newFile' is same as source file")
    }

    return newFile
}


@JvmOverloads
fun copyFileWithBuffering(
    sourceFile: File,
    targetFile: File,
    rewrite: Boolean = true,
    preserveFileDate: Boolean = true,
    notifier: ISingleCopyNotifier? = null
): File? = copyFileWithBuffering(sourceFile, targetFile.name, targetFile.parent, rewrite, preserveFileDate, notifier)

/**
 * @return target file
 */
@JvmOverloads
fun copyFileWithBuffering(
    sourceFile: File,
    targetName: String,
    targetDir: String?,
    rewrite: Boolean = true,
    preserveFileDate: Boolean = true,
    notifier: ISingleCopyNotifier? = null
): File? {
    if (try {
            !isFileExistsOrThrow(sourceFile)
        } catch (e: RuntimeException) {
            notifier?.onExceptionOccurred(e)
            return null
        }
    ) {
        notifier?.onExceptionOccurred(RuntimeException("Source file not exists: '$sourceFile'"))
        return null
    }
    val targetName = targetName.ifEmpty { sourceFile.name }
    val targetFile = toFile(targetName, targetDir)
    if (targetFile == null || targetFile == sourceFile) {
        notifier?.onExceptionOccurred(RuntimeException("Incorrect destination file: '$targetDir' (source file: '$sourceFile')"))
        return null
    }
    val fis = try {
        sourceFile.openInputStreamOrThrow()
    } catch (e: RuntimeException) {
        notifier?.onExceptionOccurred(e)
        return null
    }

    val totalBytesCount = try {
        getFileLengthOrThrow(sourceFile)
    } catch (e: RuntimeException) {
        notifier?.onExceptionOccurred(e)
        return null
    }

    try {
        targetFile.writeFromStreamOrThrow(fis, !rewrite, if (notifier != null) object : IStreamNotifier {

            override val notifyInterval: Long
                get() = notifier.notifyInterval

            override fun onProcessing(inputStream: InputStream, outputStream: OutputStream, bytesWrite: Long, bytesTotal: Long): Boolean =
                notifier.shouldProceed(sourceFile, targetFile, bytesWrite, totalBytesCount)

        } else null)

    } catch (e: RuntimeException) {
        notifier?.onExceptionOccurred(e)
        return null
    }

    if (preserveFileDate) {
        setLastModified(targetFile, getLastModified(sourceFile))
    }
    return targetFile
}

@JvmOverloads
fun copyFile(
    sourceFile: File,
    targetName: String,
    targetDir: String?,
    rewrite: Boolean = true,
    preserveFileDate: Boolean = true
): File? = try {
    copyFileOrThrow(sourceFile, targetName, targetDir, rewrite, preserveFileDate)
} catch (e: RuntimeException) {
    logger.e(e)
    null
}

/**
 * Copy with no buffering
 * @return target file
 */
@Throws(RuntimeException::class)
@JvmOverloads
fun copyFileOrThrow(
    sourceFile: File,
    targetName: String,
    targetDir: String?,
    rewrite: Boolean = true,
    preserveFileDate: Boolean = true
): File {
    if (!isFileValidOrThrow(sourceFile)) {
        throw RuntimeException("Source file is not valid: '$sourceFile'")
    }
    val targetName = targetName.ifEmpty { sourceFile.name }
    var targetFile = toFile(targetName, targetDir)
    if (sourceFile == targetFile) {
        throw RuntimeException("Source file is same as target: '$targetFile'")
    }
    targetFile = createFileOrThrow(targetName, targetDir, rewrite)
    targetFile.writeBytesOrThrow(sourceFile.readBytesOrThrow(), !rewrite)
    if (preserveFileDate) {
        setLastModified(targetFile, getLastModified(sourceFile))
    }
    return targetFile
}

fun getLastModified(file: File): Long = try {
    getLastModifiedOrThrow(file)
} catch (e: RuntimeException) {
    logger.e(e)
    0
}

@Throws(RuntimeException::class)
fun getLastModifiedOrThrow(file: File): Long {
    return try {
        file.lastModified()
    } catch (e: SecurityException) {
        throw RuntimeException(formatException(e, "lastModified"), e)
    }
}

fun setLastModified(file: File, timestamp: Long) = try {
    setLastModifiedOrThrow(file, timestamp)
    true
} catch (e: RuntimeException) {
    logger.e(e)
    false
}

@Throws(RuntimeException::class)
fun setLastModifiedOrThrow(file: File, timestamp: Long) {
    try {
        if (!file.setLastModified(timestamp)) {
            throw RuntimeException("setLastModified failed (false) on file '$file'")
        }
    } catch (e: SecurityException) {
        throwRuntimeException(e, "setLastModified")
    }
}

@JvmOverloads
fun getSize(
    fromFiles: List<File>?,
    depth: Int = DEPTH_UNLIMITED,
    notifier: IGetNotifier? = null,
): Long {
    var result: Long = 0
    if (fromFiles != null) {
        for (fromFile in fromFiles) {
            result += getSize(fromFile, depth, notifier = notifier)
        }
    }
    return result
}

@JvmOverloads
fun getSize(
    fromFile: File,
    depth: Int = DEPTH_UNLIMITED,
    notifier: IGetNotifier? = null,
): Long {
    var size: Long = 0
    for (f in getFilesRecursive(fromFile, GetMode.FILES, depth = depth, notifier = notifier)) {
        size = try {
            getFileLengthOrThrow(fromFile)
        } catch (e: RuntimeException) {
            notifier?.onExceptionOccurred(e)
            0
        }
    }
    return size
}

@JvmOverloads
fun getFiles(
    fromFiles: Collection<File>?,
    mode: GetMode = GetMode.ALL,
    comparator: Comparator<in File>? = null,
    depth: Int = DEPTH_UNLIMITED,
    notifier: IGetNotifier?,
): Set<File> {
    val result = mutableSetOf<File>()
    if (fromFiles != null) {
        for (fromFile in fromFiles) {
            result.addAll(getFiles(fromFile, mode, comparator, depth, notifier))
        }
    }
    return result
}

@JvmOverloads
fun getFiles(
    fromFile: File,
    mode: GetMode = GetMode.ALL,
    comparator: Comparator<in File>? = null,
    depth: Int = DEPTH_UNLIMITED,
    notifier: IGetNotifier? = null
): Set<File> = getFilesRecursive(fromFile, mode, comparator, depth, 0, notifier)

/**
 * @param fromFile file or directory
 * @return collected set of files or directories from specified directories without source files
 */
private fun getFilesRecursive(
    fromFile: File,
    mode: GetMode = GetMode.ALL,
    comparator: Comparator<in File>? = null,
    depth: Int = DEPTH_UNLIMITED,
    currentLevel: Int = 0,
    notifier: IGetNotifier? = null
): Set<File> {
    val result = mutableSetOf<File>()
    var depth = if (depth == 0) DEPTH_UNLIMITED else depth
    if (depth != DEPTH_UNLIMITED && currentLevel >= depth) {
        return result
    }
    if (!fromFile.exists()) {
        notifier?.onExceptionOccurred(FileIterationException(FileIterationException.Type.NOT_EXISTS, "File '$fromFile' not exists"))
        return result
    }
    if (!fromFile.isFile && !fromFile.isDirectory) {
        notifier?.onExceptionOccurred(FileIterationException(FileIterationException.Type.NOT_VALID, "Invalid file or folder: '$fromFile'"))
        return result
    }
    var wasAdded = false

    if (mode === GetMode.ALL || if (fromFile.isFile) mode === GetMode.FILES else mode === GetMode.FOLDERS) {
        if (notifier == null || (if (fromFile.isFile) {
                notifier.onGetFile(fromFile, Collections.unmodifiableSet(result), currentLevel)
            } else {
                notifier.onGetFolder(fromFile, Collections.unmodifiableSet(result), currentLevel)
//                            val getFolderResult =
//                            if (!getFolderResult.second) {
//                                shouldProceed = false
//                            }
//                            getFolderResult.first
            })
        ) {
            result.add(fromFile)
            wasAdded = true
        } else {
            notifier.onExceptionOccurred(
                FileIterationException(
                    FileIterationException.Type.NOT_CONFIRMED,
                    "File '$fromFile' collecting is not confirmed"
                )
            )
        }
    }
    if (notifier == null || notifier.shouldProceed(fromFile, Collections.unmodifiableSet(result), currentLevel, wasAdded)) {
        if (fromFile.isDirectory) {
            val files = fromFile.listFiles()?.toList() ?: emptyList()
            for (f in files) {
//                    if (f.isDirectory) {
//                        if (depth == DEPTH_UNLIMITED || depth > currentLevel) {
//                            result.addAll(getFilesRecursive(f, mode, comparator, depth, currentLevel + 1, notifier))
//                        }
//                    } else if (f.isFile) {
//                        result.addAll(getFilesRecursive(f, mode, comparator, depth, currentLevel, notifier))
//                    } else {
//                        logger.w("Invalid file or folder: $f")
//                    }
                result.addAll(
                    getFilesRecursive(
                        f,
                        mode,
                        comparator,
                        depth,
                        if (f.isDirectory) currentLevel + 1 else currentLevel,
                        notifier
                    )
                )
            }
        }
    } else {
        notifier.onExceptionOccurred(
            FileIterationException(
                FileIterationException.Type.INTERRUPTED_BY_USER,
                "Collecting files from $fromFile was interrupted"
            )
        )
    }
    comparator?.let {
        return result.sortedWith(it).toSet()
    }
    return result
}

@JvmOverloads
fun searchByName(
    name: String,
    searchFile: File,
    searchFlags: Int = MatchStringOption.AUTO.flag,
    searchFirst: Boolean = false,
    mode: GetMode = GetMode.ALL,
    comparator: Comparator<in File>? = null,
    depth: Int = DEPTH_UNLIMITED,
    notifier: IGetNotifier? = null,
): Set<File> =
    searchByName(name, listOf(searchFile), searchFlags, searchFirst, mode, comparator, depth, notifier)

/**
 * @param comparator to sort each folders list and result set
 * @return found set of files or directories with matched name
 */
@JvmOverloads
fun searchByName(
    name: String,
    searchFiles: List<File>?,
    searchFlags: Int = MatchStringOption.AUTO.flag,
    searchFirst: Boolean = false,
    mode: GetMode = GetMode.ALL,
    comparator: Comparator<in File>? = null,
    depth: Int = DEPTH_UNLIMITED,
    notifier: IGetNotifier? = null,
): Set<File> = getFiles(searchFiles, mode, comparator, depth, notifier = object : IGetNotifier {

    override fun onGetFile(file: File, collected: Set<File>, currentLevel: Int): Boolean =
        (notifier == null || notifier.onGetFile(file, collected, currentLevel)) && check(file)

    override fun onGetFolder(folder: File, collected: Set<File>, currentLevel: Int): Boolean {
        val isMatch = check(folder)
        if (notifier != null) {
            return isMatch && notifier.onGetFolder(folder, collected, currentLevel)
        }
        return isMatch
    }

    override fun shouldProceed(current: File, collected: Set<File>, currentLevel: Int, wasAdded: Boolean) = !searchFirst || !wasAdded

    private fun check(file: File) = stringsMatch(file.name, name, searchFlags)
})

@JvmOverloads
fun sortFilesByName(filesList: Collection<File>?, ascending: Boolean = true): Collection<File?>? =
    sortFiles(filesList, FileComparator(Collections.singletonMap(FileComparator.SortOption.NAME, ascending)))

@JvmOverloads
fun sortFilesBySize(filesList: Collection<File>?, ascending: Boolean = true): Collection<File?>? =
    sortFiles(filesList, FileComparator(Collections.singletonMap(FileComparator.SortOption.SIZE, ascending)))

@JvmOverloads
fun sortFilesByLastModified(filesList: Collection<File>?, ascending: Boolean = true): Collection<File?>? =
    sortFiles(filesList, FileComparator(Collections.singletonMap(FileComparator.SortOption.LAST_MODIFIED, ascending)))

/**
 * @return same sorted list or created sorted array list
 */
fun sortFiles(files: Collection<File>?, comparator: Comparator<in File>): List<File>? {
    if (files == null) {
        return emptyList()
    }
    if (files.isEmpty()) {
        return files.toList()
    }
    return files.sortedWith(comparator)
}

@JvmOverloads
fun moveFiles(
    sourceFile: File,
    targetDir: File,
    comparator: Comparator<in File>? = null,
    preserveFileDate: Boolean = true,
    copyWithBuffering: Boolean = true,
    depth: Int = DEPTH_UNLIMITED,
    singleCopyNotifier: ISingleCopyNotifier? = null,
    multipleCopyNotifier: IMultipleCopyNotifier? = null
) = moveOrCopyFiles(
    true,
    sourceFile,
    targetDir,
    comparator,
    preserveFileDate,
    copyWithBuffering,
    depth,
    singleCopyNotifier,
    multipleCopyNotifier
)


@JvmOverloads
fun copyFiles(
    sourceFile: File,
    targetDir: File,
    comparator: Comparator<in File>? = null,
    preserveFileDate: Boolean = true,
    copyWithBuffering: Boolean = true,
    depth: Int = DEPTH_UNLIMITED,
    singleCopyNotifier: ISingleCopyNotifier? = null,
    multipleCopyNotifier: IMultipleCopyNotifier? = null
) = moveOrCopyFiles(
    false,
    sourceFile,
    targetDir,
    comparator,
    preserveFileDate,
    copyWithBuffering,
    depth,
    singleCopyNotifier,
    multipleCopyNotifier
)

private fun moveOrCopyFiles(
    moveOrCopy: Boolean,
    sourceFile: File,
    targetDir: File,
    comparator: Comparator<in File>? = null,
    preserveFileDate: Boolean = true,
    copyWithBuffering: Boolean = true,
    depth: Int = DEPTH_UNLIMITED,
    singleCopyNotifier: ISingleCopyNotifier? = null,
    multipleCopyNotifier: IMultipleCopyNotifier? = null
): Set<File> {
    val result = mutableSetOf<File>()
    try {
        createDirOrThrow(targetDir.absolutePath)
    } catch (e: RuntimeException) {
        multipleCopyNotifier?.onExceptionOccurred(e)
        return result
    }
    if (targetDir == sourceFile) {
        multipleCopyNotifier?.onExceptionOccurred(RuntimeException("Destination directory '$targetDir' is same as source directory/file '$sourceFile'"))
        return result
    }
    val files: Set<File> =
        getFiles(sourceFile, GetMode.FILES, comparator, depth, if (multipleCopyNotifier != null) object : IGetNotifier {

            override fun onGetFile(file: File, collected: Set<File>, currentLevel: Int): Boolean {
                return multipleCopyNotifier.onCollecting(file, collected, currentLevel)
            }

            override fun onGetFolder(folder: File, collected: Set<File>, currentLevel: Int): Boolean {
                return multipleCopyNotifier.onCollecting(folder, collected, currentLevel)
            }

            override fun onExceptionOccurred(e: RuntimeException) {
                multipleCopyNotifier.onExceptionOccurred(e)
            }

        } else null)

    val totalFilesCount = files.size.toLong()
    var filesProcessed = 0
    for (f in files) {

        if (!f.isFile) {
            multipleCopyNotifier?.onExceptionOccurred(
                FileIterationException(
                    FileIterationException.Type.NOT_VALID,
                    "File '$f' not file, skipping..."
                )
            )
            continue
        }
        if (!f.exists()) {
            multipleCopyNotifier?.onExceptionOccurred(
                FileIterationException(
                    FileIterationException.Type.NOT_EXISTS,
                    "File '$f' not exists, skipping..."
                )
            )
            continue
        }
        var currentDestDir: File? = null
        if (f != sourceFile) {
            var part = f.parent
            if (part != null && part.startsWith(sourceFile.absolutePath)) {
                part = part.substring(sourceFile.absolutePath.length, part.length)
            }
            if (!part.isNullOrEmpty()) {
                currentDestDir = File(targetDir, part)
            }
        }
        if (currentDestDir == null) {
            currentDestDir = targetDir
        }
        if (multipleCopyNotifier != null) {
            if (!multipleCopyNotifier.shouldProceed(
                    f,
                    currentDestDir,
                    Collections.unmodifiableSet(result),
                    filesProcessed.toLong(),
                    totalFilesCount
                )
            ) {
                multipleCopyNotifier.onExceptionOccurred(
                    FileIterationException(
                        FileIterationException.Type.INTERRUPTED_BY_USER,
                        "Copying from '$sourceFile' to '$targetDir' was interrupted"
                    )
                )
                break
            }
        }
        var targetFile: File? = null
        var confirmCopy = true
        if (multipleCopyNotifier != null) {
            confirmCopy = multipleCopyNotifier.confirmMoveOrCopy(f, currentDestDir)
        }
        if (confirmCopy) {
            if (multipleCopyNotifier != null) {
                targetFile = multipleCopyNotifier.onBeforeMoveOrCopy(f, currentDestDir)
            }
            val isSameFile = targetFile == f
            if (targetFile == null || isSameFile) {
                if (isSameFile) {
                    multipleCopyNotifier?.onExceptionOccurred(
                        FileIterationException(
                            FileIterationException.Type.NAME_INVALID,
                            "Target file cannot be equals to source file ('$targetFile')!"
                        )
                    )
                }
                targetFile = File(currentDestDir, f.name)
            }
            var replaceOptions = IMultipleCopyNotifier.ReplaceOptions()
            if (multipleCopyNotifier != null && isFileExistsOrThrow(targetFile)) {
                replaceOptions = multipleCopyNotifier.confirmReplace(targetFile)
            }
            val shouldReplace = replaceOptions.enableReplace
            if (!shouldReplace) {
                multipleCopyNotifier?.onExceptionOccurred(
                    FileIterationException(
                        FileIterationException.Type.REPLACE_DISABLED,
                        "Replace disabled for file '$targetFile', skipping..."
                    )
                )
                continue
            }
            val resultFile =
                if (moveOrCopy) {
                    moveFile(f, targetFile, !replaceOptions.enableAppend, false, preserveFileDate, copyWithBuffering, singleCopyNotifier)
                } else {
                    if (copyWithBuffering) {
                        copyFileWithBuffering(
                            f, targetFile.name, targetFile.parent, !replaceOptions.enableAppend,
                            preserveFileDate, singleCopyNotifier
                        )
                    } else {
                        try {
                            copyFileOrThrow(f, targetFile.name, targetFile.parent, !replaceOptions.enableAppend, preserveFileDate)
                        } catch (e: RuntimeException) {
                            singleCopyNotifier?.onExceptionOccurred(e)
                            null
                        }
                    }
                }
            if (resultFile != null) {
                multipleCopyNotifier?.onSucceeded(f, resultFile)
                result.add(resultFile)
            } else {
                multipleCopyNotifier?.onFailed(f, currentDestDir)
            }
        } else {
            multipleCopyNotifier?.onExceptionOccurred(
                FileIterationException(
                    FileIterationException.Type.NOT_CONFIRMED,
                    "File '$f' move/copy is not confirmed"
                )
            )
        }
        filesProcessed++
    }
    comparator?.let {
        return result.sortedWith(it).toSet()
    }
    return result
}

@Deprecated("use copyFiles")
fun copyFilesWithBufferingLegacy(
    fromFile: File,
    targetDir: File,
    comparator: Comparator<in File>? = null,
    preserveFileDate: Boolean = true,
    depth: Int = DEPTH_UNLIMITED,
    totalFilesCount: Int = 0,
    copied: MutableSet<File> = mutableSetOf(),
    exclusionList: MutableList<String> = mutableListOf(),
    singleNotifier: ISingleCopyNotifier? = null,
    multipleCopyNotifier: IMultipleCopyNotifierLegacy? = null
): Set<File> {
    return copyFilesWithBufferingLegacyRecursive(
        fromFile,
        targetDir,
        comparator,
        preserveFileDate,
        depth,
        0,
        totalFilesCount,
        copied,
        exclusionList,
        singleNotifier,
        multipleCopyNotifier
    )
}

/**
 * @param fromFile file or directory
 */
@Deprecated("use copyFiles")
private fun copyFilesWithBufferingLegacyRecursive(
    fromFile: File,
    targetDir: File,
    comparator: Comparator<in File>? = null,
    preserveFileDate: Boolean = true,
    depth: Int = DEPTH_UNLIMITED,
    currentLevel: Int = 0,
    totalFilesCount: Int = 0,
    copied: MutableSet<File> = mutableSetOf(),
    exclusionList: MutableList<String> = mutableListOf(),
    singleNotifier: ISingleCopyNotifier? = null,
    multipleCopyNotifier: IMultipleCopyNotifierLegacy? = null
): Set<File> {
    var totalFilesCount = totalFilesCount
    val result = mutableSetOf<File>()
    var isValid = false

    if (fromFile.exists()) {
        isValid = true
        if (currentLevel == 0) {
            totalFilesCount =
                getFilesRecursive(
                    fromFile,
                    GetMode.FILES,
                    comparator,
                    depth,
                    notifier = if (multipleCopyNotifier != null) object : IGetNotifier {

                        override fun onGetFile(file: File, collected: Set<File>, currentLevel: Int): Boolean {
                            multipleCopyNotifier.onCalculatingSize(file, collected, currentLevel)
                            return true
                        }

                        override fun onGetFolder(folder: File, collected: Set<File>, currentLevel: Int): Boolean {
                            multipleCopyNotifier.onCalculatingSize(folder, collected, currentLevel)
                            return false
                        }
                    } else null).size
            if (fromFile.isDirectory && targetDir.absolutePath.startsWith(fromFile.absolutePath)) {
                val srcFiles = fromFile.listFiles()
                if (srcFiles != null && srcFiles.isNotEmpty()) {
                    for (srcFile in srcFiles) {
                        exclusionList.add(File(targetDir, srcFile.name).absolutePath)
                    }
                }
            }
        }
        if (multipleCopyNotifier != null) {
            if (!multipleCopyNotifier.onProcessing(
                    fromFile,
                    targetDir,
                    Collections.unmodifiableSet(copied),
                    totalFilesCount.toLong(),
                    currentLevel
                )
            ) {
                return result
            }
        }
        if (fromFile.isDirectory) {
            var files = fromFile.listFiles()
            if (files != null) {
                if (comparator != null) {
                    val sorted: List<File> = ArrayList(listOf(*files))
                    Collections.sort(sorted, comparator)
                    files = sorted.toTypedArray()
                }
                for (f in files) {

//                            if (currentLevel >= 1) {
//                                String tmpPath = targetDir.getAbsolutePath();
//                                int index = tmpPath.lastIndexOf(File.separator);
//                                if (index > 0 && index < tmpPath.length() - 1) {
//                                    tmpPath = tmpPath.substring(0, index);
//                                }
//                                targetDir = new File(tmpPath);
//                            }
                    if (f.isDirectory) {
                        if (depth == DEPTH_UNLIMITED || depth > currentLevel) {
                            result.addAll(
                                copyFilesWithBufferingLegacyRecursive(
                                    f,  /*new File(targetDir + File.separator + fromFile.getName(), f.getName())*/
                                    targetDir,
                                    comparator,
                                    preserveFileDate,
                                    depth,
                                    currentLevel + 1,
                                    totalFilesCount,
                                    copied,
                                    exclusionList,
                                    singleNotifier,
                                    multipleCopyNotifier
                                )
                            )
                        }
                    } else {
                        result.addAll(
                            copyFilesWithBufferingLegacyRecursive(
                                f,  /*new File(targetDir, fromFile.getName()) */
                                targetDir,
                                comparator,
                                preserveFileDate,
                                depth,
                                currentLevel,
                                totalFilesCount,
                                copied,
                                exclusionList,
                                singleNotifier,
                                multipleCopyNotifier
                            )
                        )
                    }
                }
            }
            if (files == null || files.isEmpty()) {
                val emptyDir = if (currentLevel == 0) targetDir.toString() + File.separator + fromFile.name else targetDir.absolutePath
                if (!isDirExistsOrThrow(emptyDir)) {
                    createDirOrThrow(emptyDir)
                }
            }
        } else if (isFileExistsOrThrow(fromFile)) {
            var targetFile: File? = null
            var confirmCopy = true
            if (multipleCopyNotifier != null) {
                confirmCopy = multipleCopyNotifier.confirmCopy(fromFile, targetDir, currentLevel)
            }
            if (confirmCopy) {
                if (multipleCopyNotifier != null) {
                    targetFile = multipleCopyNotifier.onBeforeCopy(fromFile, targetDir, currentLevel)
                }
                if (targetFile == null || targetFile == fromFile) {
                    targetFile = File(targetDir, fromFile.name)
                }
                var rewrite = false
                if (multipleCopyNotifier != null && isFileExistsOrThrow(targetFile)) {
                    rewrite = multipleCopyNotifier.onExists(targetFile, currentLevel)
                }
                var resultFile: File? = null
                if (!exclusionList.contains(fromFile.absolutePath)) {
                    resultFile = copyFileWithBuffering(
                        fromFile, targetFile.name, targetFile.parent, rewrite,
                        preserveFileDate, singleNotifier
                    )
                }
                if (resultFile != null) {
                    result.add(resultFile)
                    copied.add(resultFile)
                } else {
                    isValid = false
                }
            }
        } else {
            isValid = false
        }
    }
    if (!isValid) {
        logger.w("Invalid file or folder or failed to copy from: '$fromFile'")
        multipleCopyNotifier?.onFailed(fromFile, targetDir, currentLevel)
    }

    comparator?.let {
        return result.sortedWith(it).toSet()
    }
    return result
}

@JvmOverloads
fun deleteFiles(
    fromFiles: Collection<File>?,
    deleteEmptyDirs: Boolean = true,
    comparator: Comparator<in File>? = null,
    depth: Int = DEPTH_UNLIMITED,
    notifier: IDeleteNotifier? = null
): Set<File> {
    val result: MutableSet<File> = LinkedHashSet()
    if (fromFiles != null) {
        for (file in fromFiles) {
            result.addAll(deleteFiles(file, deleteEmptyDirs, comparator, depth, notifier = notifier))
        }
    }
    return result
}

@JvmOverloads
fun deleteFiles(
    fromFile: File,
    deleteEmptyDirs: Boolean = true,
    comparator: Comparator<in File>? = null,
    depth: Int = DEPTH_UNLIMITED,
    notifier: IDeleteNotifier? = null
): Set<File> {
    return deleteFilesRecursive(fromFile, deleteEmptyDirs, comparator, depth, notifier = notifier)
}

/**
 * @param depth кол-во уровней вложенености (номер последней итерации == depth - 1)
 */
private fun deleteFilesRecursive(
    fromFile: File,
    deleteEmptyDirs: Boolean = true,
    comparator: Comparator<in File>? = null,
    depth: Int = DEPTH_UNLIMITED,
    currentLevel: Int = 0,
    notifier: IDeleteNotifier? = null
): Set<File> {

    val result = mutableSetOf<File>()

    if (depth != DEPTH_UNLIMITED && depth < 0) {
        notifier?.onExceptionOccurred(IllegalArgumentException("Incorrect depth: $depth"))
        return result
    }

    if (depth != DEPTH_UNLIMITED && currentLevel >= depth) {
        return result
    }

    fun deleteEmptyDirChecked(dir: File) {
        if (deleteEmptyDirs && isDirEmpty(dir)) {
            if (notifier == null || notifier.confirmDeleteFolder(dir)) {
                if (deleteEmptyDir(dir)) {
                    result.add(dir)
                } else {
                    notifier?.onDeleteFolderFailed(dir)
                }
            }
        }
    }

    if (notifier != null && !notifier.shouldProceed(fromFile, Collections.unmodifiableSet(result), currentLevel)) {
        notifier.onExceptionOccurred(
            FileIterationException(
                FileIterationException.Type.INTERRUPTED_BY_USER,
                "Deleting from '$fromFile' was interrupted"
            )
        )
        return result
    }

    if (!fromFile.exists()) {
        notifier?.onExceptionOccurred(FileIterationException(FileIterationException.Type.NOT_EXISTS, "File '$fromFile' not exists"))
        return result
    }

    if (fromFile.isDirectory) {
        val files = fromFile.listFiles()?.toMutableList() ?: mutableListOf()
        if (comparator != null) {
            val sorted = files.sortedWith(comparator)
            files.clear()
            files.addAll(sorted)
        }
        for (f in files) {
//            if (!exclusionList.contains(f)) {
//                when {
//                    f.isDirectory -> {
//                        if (depth == DEPTH_UNLIMITED || depth > currentLevel) {
//                            result.addAll(delete(f, deleteEmptyDirs, exclusionList, comparator, depth, currentLevel + 1, notifier))
//                        }
//                        deleteEmptyDirChecked(f)
//                    }
//                    f.isFile -> {
//                        result.addAll(delete(f, deleteEmptyDirs, exclusionList, comparator, depth, currentLevel, notifier))
//                    }
//                    else -> {
//                        logger.w("Invalid file or folder: '$f'")
//                    }
//                }
//            }
            result.addAll(
                deleteFilesRecursive(
                    f,
                    deleteEmptyDirs,
                    comparator,
                    depth,
                    if (f.isDirectory) currentLevel + 1 else currentLevel,
                    notifier
                )
            )
        }
        deleteEmptyDirChecked(fromFile)
    } else if (fromFile.isFile) {
        if (notifier == null || notifier.confirmDeleteFile(fromFile)) {
            if (deleteFile(fromFile)) {
                result.add(fromFile)
            } else {
                notifier?.onDeleteFileFailed(fromFile)
            }
        } else {
            notifier.onExceptionOccurred(
                FileIterationException(
                    FileIterationException.Type.NOT_CONFIRMED,
                    "File '$fromFile' deletion is not confirmed"
                )
            )
        }
    } else {
        notifier?.onExceptionOccurred(FileIterationException(FileIterationException.Type.NOT_VALID, "Invalid file or folder: '$fromFile'"))
    }
    return result
}

@JvmOverloads
fun compressFilesToZip(
    srcFiles: Collection<File>?,
    destZipName: String,
    destZipParent: String?,
    recreate: Boolean = true,
    buffSize: Int = DEFAULT_BUFFER_SIZE,
    notifier: IStreamNotifier? = null
): File? = try {
    compressFilesToZipOrThrow(srcFiles, destZipName, destZipParent, recreate, buffSize, notifier)
} catch (e: RuntimeException) {
    logger.e(e)
    null
}

@Throws(RuntimeException::class)
@JvmOverloads
fun compressFilesToZipOrThrow(
    sourceFiles: Collection<File>?,
    destZipName: String,
    destZipParent: String?,
    recreate: Boolean = true,
    buffSize: Int = DEFAULT_BUFFER_SIZE,
    notifier: IStreamNotifier? = null
): File {
    val zipFile = createFileOrThrow(destZipName, destZipParent, recreate)

    val streams = mutableMapOf<String, InputStream>()

    val os = zipFile.openOutputStreamOrThrow()

    for (sourceFile in (sourceFiles ?: emptyList())) {
        streams[sourceFile.name] = sourceFile.openInputStreamOrThrow()
    }

    try {
        compressStreamsToZipOrThrow(streams, os, buffSize, notifier)
    } catch (e: IOException) {
        throwRuntimeException(e, "compressStreamsTo")
    }
    return zipFile
}

@JvmOverloads
fun File.unzip(
    destPath: String?,
    saveDirHierarchy: Boolean = true,
    recreate: Boolean = true,
    buffSize: Int = DEFAULT_BUFFER_SIZE,
    notifier: IStreamNotifier? = null
) = try {
    unzipOrThrow(destPath, saveDirHierarchy, recreate, buffSize, notifier)
    true
} catch (e: RuntimeException) {
    logger.e(e)
    false
}

@Throws(RuntimeException::class)
@JvmOverloads
fun File.unzipOrThrow(
    destPath: String?,
    saveDirHierarchy: Boolean = true,
    recreate: Boolean = true,
    buffSize: Int = DEFAULT_BUFFER_SIZE,
    notifier: IStreamNotifier? = null
) {
    if (!isFileValidOrThrow(this)) {
        throw IllegalArgumentException("Invalid zip file: '$this'")
    }
    if (destPath.isNullOrEmpty()) {
        throw IllegalArgumentException("destPath is null or empty")
    }
    try {
        this.openInputStreamOrThrow().unzipStreamOrThrow(
            saveDirHierarchy,
            buffSize,
            notifier,
            createDirFunc = { createDirOrThrow(it, destPath) },
            createOutputStream = { createFileOrThrow(it, destPath, recreate).openOutputStreamOrThrow(!recreate) }
        )
    } catch (e: IOException) {
        throwRuntimeException(e, "unzipStream")
    }
}

/**
 * @param name  file or folder name part
 * @param paths 'PATH' environment variable by default
 */
@JvmOverloads
fun checkFilesWithStat(
    name: String,
    paths: Collection<File>? = getEnvPathFiles(),
    useSU: Boolean = true,
    execTimeout: Long = 0,
    comparator: Comparator<in File>? = null,
    notifier: IShellGetNotifier? = null
): Set<File> {
    val result = mutableSetOf<File>()
    for (file in paths?.toList() ?: emptyList()) {
        var path = file.absolutePath
        if (!path.endsWith(File.separator)) {
            path += File.separator
        }
        val targetPath = path + name
        ShellWrapper(false).executeCommand(
            listOf("stat", targetPath),
            useSU,
            DEFAULT_TARGET_CODE,
            execTimeout,
            TimeUnit.MILLISECONDS,
            object : ShellCallback {

                override val needToLogCommands: Boolean = true

                override fun shellOut(from: ShellCallback.StreamType, shellLine: String) {
                    if (shellLine.contains("File: ") && shellLine.contains(name)) {
                        val currentFile = File(targetPath)
                        if (notifier == null || notifier.onGetFile(currentFile, result, 0)) {
                            result.add(currentFile)
                        }
                    }
                }

                override fun processStartFailed(t: Throwable?) {
                    notifier?.onStartFailed(t, File(targetPath))
                }

                override fun processComplete(exitValue: Int) {
                    notifier?.onExitCode(exitValue, File(targetPath))
                }
            })
    }
    comparator?.let {
        return result.sortedWith(it).toSet()
    }
    return result
}

@JvmOverloads
fun getFilesWithLs(
    fromDirs: Collection<File>?,
    useSU: Boolean = true,
    execTimeout: Long = 0,
    comparator: Comparator<in File>? = null,
    notifier: IShellGetNotifier? = null
): Map<File, Double> {
    val collectedMap = mutableMapOf<File, Double>()
    val collected = mutableSetOf<File>()
    for (dir in fromDirs ?: emptyList()) {
        ShellWrapper(false).executeCommand(listOf("ls", dir.absolutePath),
            useSU,
            DEFAULT_TARGET_CODE,
            execTimeout,
            TimeUnit.MILLISECONDS,
            object : ShellCallback {

                override val needToLogCommands: Boolean = true

                override fun shellOut(from: ShellCallback.StreamType, shellLine: String) {
                    if (from === ShellCallback.StreamType.OUT && !isEmpty(shellLine)) {
                        val currentFile = File(dir, shellLine)
                        if (notifier == null || notifier.onGetFile(currentFile, collected, 0)) {
                            collected.add(currentFile)
                        }
                    }
                }

                override fun processStartFailed(t: Throwable?) {
                    notifier?.onStartFailed(t, dir)
                }

                override fun processComplete(exitValue: Int) {
                    notifier?.onExitCode(exitValue, dir)
                }
            })
    }
    comparator?.let {
        val sorted = collected.sortedWith(it)
        collected.clear()
        collected.addAll(sorted)
    }
    for (current in collected) {
        // option "-b" is not supported on android version
        ShellWrapper().executeCommand(listOf("du", "-s", current.absolutePath),
            useSU,
            DEFAULT_TARGET_CODE,
            execTimeout,
            TimeUnit.MILLISECONDS,
            object : ShellCallback {

                override val needToLogCommands: Boolean = true

                override fun shellOut(from: ShellCallback.StreamType, shellLine: String) {
                    if (from === ShellCallback.StreamType.OUT && !isEmpty(shellLine)) {
                        var size: Long = 0
                        val parts = shellLine.split("\\t").toTypedArray()
                        if (parts.size > 1) {
                            size = parts[0].toLongNotNull(10) { e: NumberFormatException ->
                                notifier?.onExceptionOccurred(RuntimeException(formatException(e, "parseLong")))
                            }
                        }
                        collectedMap[current] = SizeUnit.KBYTES.toBytes(size.toDouble())
                    }
                }

                override fun processStartFailed(t: Throwable?) {
                    notifier?.onStartFailed(t, current)
                }

                override fun processComplete(exitValue: Int) {
                    notifier?.onExitCode(exitValue, current)
                }
            })
    }
    return Collections.unmodifiableMap(collectedMap)
}

enum class GetMode {
    FILES, FOLDERS, ALL
}

interface IFsNotifier {

    fun onExceptionOccurred(e: RuntimeException) {
        logException(logger, e, isError = false)
    }
}

interface IGetNotifier : IFsNotifier {
    /**
     * @return false if client code doesn't want to append this file to result
     */

    fun onGetFile(file: File, collected: Set<File>, currentLevel: Int): Boolean = true

    /**
     * @return 1st false - if client code doesn't want to append this folder to result, 2nd false - if not intended to go deeper
     */

    fun onGetFolder(folder: File, collected: Set<File>, currentLevel: Int): Boolean = true

    /**
     * @return false to interrupt collecting
     */

    fun shouldProceed(current: File, collected: Set<File>, currentLevel: Int, wasAdded: Boolean): Boolean = true
}

interface ISingleCopyNotifier : IFsNotifier {
    val notifyInterval: Long get() = 0L


    fun shouldProceed(sourceFile: File, targetFile: File, bytesCopied: Long, bytesTotal: Long): Boolean = true
}

interface IMultipleCopyNotifier : IFsNotifier {
    /**
     * @return false if process should be interrupted
     */

    fun onCollecting(current: File, collected: Set<File>, currentLevel: Int): Boolean = true

    /**
     * @return false if process should be interrupted
     */

    fun shouldProceed(currentFile: File, targetDir: File, copied: Set<File>, filesProcessed: Long, filesTotal: Long): Boolean = true

    /**
     * true if copying confirmed by client code, false to cancel
     */

    fun confirmMoveOrCopy(currentFile: File, targetDir: File): Boolean = true

    /**
     * @return target file to copy in or null for default
     */

    fun onBeforeMoveOrCopy(currentFile: File, targetDir: File): File? = null

    /**
     * @return true if specified destination file is should be replaced (it currently exists)
     */

    fun confirmReplace(targetFile: File): ReplaceOptions = ReplaceOptions()


    fun onSucceeded(currentFile: File, resultFile: File) {
    }


    fun onFailed(currentFile: File, targetDir: File) {
    }

    data class ReplaceOptions(
        val enableReplace: Boolean = true,
        val enableAppend: Boolean = false
    )
}

@Deprecated("use IMultipleCopyNotifier")
interface IMultipleCopyNotifierLegacy : IFsNotifier {
    fun onCalculatingSize(current: File, collected: Set<File>, currentLevel: Int): Boolean
    fun onProcessing(currentFile: File, targetDir: File, copied: Set<File>, filesTotal: Long, currentLevel: Int): Boolean
    fun confirmCopy(currentFile: File, targetDir: File, currentLevel: Int): Boolean
    fun onBeforeCopy(currentFile: File, targetDir: File, currentLevel: Int): File
    fun onExists(targetFile: File, currentLevel: Int): Boolean
    fun onFailed(currentFile: File?, targetFile: File, currentLevel: Int)
}

interface IDeleteNotifier : IFsNotifier {
    /**
     * @return false to interrupt collecting
     */

    fun shouldProceed(current: File, deleted: Set<File>, currentLevel: Int): Boolean = true

    /**
     * @return false if client code doesn't want to delete this file
     */

    fun confirmDeleteFile(file: File): Boolean = true

    /**
     * @return false if client code doesn't want to delete this folder
     */

    fun confirmDeleteFolder(folder: File): Boolean = true


    fun onDeleteFileFailed(file: File) {
    }


    fun onDeleteFolderFailed(folder: File) {
    }
}

interface IShellGetNotifier : IGetNotifier {

    fun onStartFailed(t: Throwable?, forFile: File) {
    }


    fun onExitCode(exitCode: Int, forFile: File) {
    }
}

class FileComparator(sortOptions: Map<SortOption, Boolean?>) : BaseOptionalComparator<FileComparator.SortOption, File>(sortOptions) {

    enum class SortOption : ISortOption {
        NAME, SIZE, LAST_MODIFIED;

        override val optionName: String
            get() = name
    }

    override fun compare(lhs: File, rhs: File, option: SortOption, ascending: Boolean): Int {
        return when (option) {
            SortOption.NAME -> compareStrings(lhs.absolutePath, rhs.absolutePath, ascending, true)
            SortOption.SIZE -> compareLongs(getFileLength(lhs), getFileLength(rhs), ascending)
            SortOption.LAST_MODIFIED -> compareLongs(getLastModified(lhs), getLastModified(rhs), ascending)
        }
    }
}

fun File.openInputStream(): FileInputStream? = try {
    openInputStreamOrThrow()
} catch (e: RuntimeException) {
    logger.e(e)
    null
}

@Throws(RuntimeException::class)
fun File.openInputStreamOrThrow(): FileInputStream = try {
    FileInputStream(this)
} catch (e: FileNotFoundException) {
    throw RuntimeException("File '$this' not found", e)
} catch (e: SecurityException) {
    throw RuntimeException(formatException(e, "create FileInputStream"), e)
}

@JvmOverloads
fun File.openOutputStream(append: Boolean = false): FileOutputStream? = try {
    openOutputStreamOrThrow(append)
} catch (e: RuntimeException) {
    logger.e(e)
    null
}

@Throws(RuntimeException::class)
@JvmOverloads
fun File.openOutputStreamOrThrow(append: Boolean = false): FileOutputStream = try {
    FileOutputStream(this, append)
} catch (e: FileNotFoundException) {
    throw RuntimeException("File '$this' not found", e)
} catch (e: SecurityException) {
    throw RuntimeException(formatException(e, "create FileOutputStream"), e)
}

@JvmOverloads
fun toFile(
    fileName: String?,
    parentPath: String? = null,
    checkSeparators: Boolean = false
): File? {
    if (fileName.isNullOrEmpty() || (checkSeparators && fileName.contains(File.separatorChar))) {
        return null
    }
    return if (parentPath.isNullOrEmpty()) File(fileName) else File(parentPath, fileName)
}

class FileIterationException(
    val type: Type,
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause) {

    enum class Type {
        INTERRUPTED_BY_USER,
        NOT_EXISTS,
        NOT_VALID,
        NOT_CONFIRMED,
        REPLACE_DISABLED,
        NAME_INVALID
    }
}