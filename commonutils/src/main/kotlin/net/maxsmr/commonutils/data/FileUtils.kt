package net.maxsmr.commonutils.data

import net.maxsmr.commonutils.data.collection.sort.BaseOptionalComparator
import net.maxsmr.commonutils.data.collection.sort.ISortOption
import net.maxsmr.commonutils.data.conversion.SizeUnit
import net.maxsmr.commonutils.data.conversion.SizeUnit.Companion.convert
import net.maxsmr.commonutils.data.conversion.toLongNotNull
import net.maxsmr.commonutils.data.text.EMPTY_STRING
import net.maxsmr.commonutils.data.text.NEXT_LINE
import net.maxsmr.commonutils.data.text.isEmpty
import net.maxsmr.commonutils.data.text.replaceRangeOrThrow
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder.*
import net.maxsmr.commonutils.shell.DEFAULT_TARGET_CODE
import net.maxsmr.commonutils.shell.ShellCallback
import net.maxsmr.commonutils.shell.ShellWrapper
import java.io.*
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.channels.OverlappingFileLockException
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import java.io.FileWriter as FileWriter1

const val DEPTH_UNLIMITED = -1

private val logger = BaseLoggerHolder.getInstance().getLogger<BaseLogger>("FileUtils")

fun getEnvPathFiles(): Set<File> {
    val result = mutableSetOf<File>()
    val paths = (System.getenv("PATH") ?: EMPTY_STRING).split(":").toTypedArray()
    if (paths.isEmpty()) {
        return emptySet()
    }
    for (path in paths) {
        result.add(File(path))
    }
    return result
}

fun getFileExtension(file: File?): String = getFileExtension(file?.name)

fun getFileExtension(fileName: String?): String {
    if (fileName == null) return EMPTY_STRING
    val index = fileName.lastIndexOf('.')
    return if (index > 0 && index < fileName.length - 1) fileName.substring(index + 1) else EMPTY_STRING
}

fun removeFileExtension(file: File?): String =
        removeFileExtension(file?.name)

/**
 * убрать расширение файла
 *
 * @return новое имя
 */
fun removeFileExtension(fileName: String?): String {
    var result: String = fileName ?: EMPTY_STRING
    if (fileName != null && !isEmpty(fileName)) {
        val startIndex = fileName.lastIndexOf('.')
        if (startIndex >= 0) {
            result = replaceRangeOrThrow(fileName, startIndex, fileName.length, EMPTY_STRING)
        }
    }
    return result
}

fun appendPostfix(file: File?, postfix: String?): File? {
    var result = file
    if (!isEmpty(postfix) && file != null) {
        var name = removeFileExtension(file.name)
        if (name.isNotEmpty()) {
            name += postfix
            val extension = getFileExtension(file.name)
            if (extension.isNotEmpty()) {
                name += ".$extension"
            }
            result = File(file.parent, name)
        }
    }
    return result
}

@JvmOverloads
fun getPartitionSpace(
        fileName: String?,
        parentPath: String? = null,
        unit: SizeUnit = SizeUnit.BYTES,
        totalOrFree: Boolean,
): Double = try {
    getPartitionSpaceOrThrow(fileName, parentPath, unit, totalOrFree)
} catch (e: RuntimeException) {
    0.0
}

fun getPartitionSpace(
        file: File?,
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
        fileName: String?,
        parentPath: String? = null,
        unit: SizeUnit = SizeUnit.BYTES,
        totalOrFree: Boolean
): Double = getPartitionSpaceOrThrow(toFile(fileName, parentPath), unit, totalOrFree)

@Throws(RuntimeException::class)
fun getPartitionSpaceOrThrow(
        file: File?,
        unit: SizeUnit = SizeUnit.BYTES,
        totalOrFree: Boolean,
): Double {
    if (file == null) {
        throw NullPointerException("file is null")
    }
    if (!isDirExistsOrThrow(file) && !isFileExistsOrThrow(file)) {
        throw RuntimeException("file or dir '$file' not exists")
    }
    return try {
        convert(if (totalOrFree) file.totalSpace else file.freeSpace, SizeUnit.BYTES, unit).toDouble()
    } catch (e: SecurityException) {
        throw RuntimeException("totalSpace / freeSpace on file '$file'", e)
    }
}

fun getCanonicalPath(file: File?): String {
    return if (file != null) {
        try {
            file.canonicalPath ?: EMPTY_STRING
        } catch (e: IOException) {
            logException(logger, e, "getCanonicalPath on file '$file'")
            file.absolutePath ?: EMPTY_STRING
        }
    } else EMPTY_STRING
}

fun isFileLocked(f: File?): Boolean {
    val l = lockFileChannel(f, false)
    return try {
        l == null
    } finally {
        releaseLock(l)
    }
}

fun lockFileChannel(f: File?, blocking: Boolean): FileLock? {
    if (f == null || !isFileExists(f)) {
        logger.e("File '$f' not exists")
        return null
    }
    var randomAccFile: RandomAccessFile? = null
    var channel: FileChannel? = null
    try {
        randomAccFile = RandomAccessFile(f, "rw")
        channel = randomAccFile.channel
        try {
            return if (!blocking) channel.tryLock() else channel.lock()
        } catch (e: IOException) {
            logException(logger, e, "tryLock")
        } catch (e: OverlappingFileLockException) {
            logException(logger, e, "tryLock")
        }
    } catch (e: FileNotFoundException) {
        logException(logger, e)
    } finally {
        try {
            channel?.close()
            randomAccFile?.close()
        } catch (e: IOException) {
            logException(logger, e, "close")
        }
    }
    return null
}

fun releaseLock(lock: FileLock?): Boolean {
    try {
        if (lock != null) {
            lock.release()
            return true
        }
    } catch (e: IOException) {
        logException(logger, e, "release")
    }
    return false
}

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
fun isFileValidOrThrow(file: File?): Boolean {
    if (file != null && isFileExistsOrThrow(file)) {
        try {
            return file.length() > 0
        } catch (e: SecurityException) {
            throwRuntimeException(e, "length on file '$file'")
        }
    }
    return false
}

@JvmOverloads
fun isFileReadOnlyAccessible(file: File?, isFile: Boolean = true) =
        isFileReadAccessible(file, isFile) && !isFileWriteAccessible(file, isFile)

@JvmOverloads
fun isFileReadAccessible(file: File?, isFile: Boolean = true) =
        isFileAccessible(file, isFile, forRead = true, forWrite = false)

@JvmOverloads
fun isFileWriteAccessible(file: File?, isFile: Boolean = true) =
        isFileAccessible(file, isFile, forRead = false, forWrite = true)

@JvmOverloads
fun isFileAccessible(
        file: File?,
        isFile: Boolean = true,
        forRead: Boolean = true,
        forWrite: Boolean = true
): Boolean = try {
    isFileAccessibleOrThrow(file, isFile, forRead, forWrite)
} catch (e: RuntimeException) {
    logger.e(e)
    false
}

@Throws(RuntimeException::class)
@JvmOverloads
fun isFileAccessibleOrThrow(
        file: File?,
        isFile: Boolean = true,
        forRead: Boolean = true,
        forWrite: Boolean = true
): Boolean {
    if (file == null) {
        throw NullPointerException("file is null")
    }
    if (!(if (isFile) isFileExistsOrThrow(file) else isDirExistsOrThrow(file))) {
        throw RuntimeException("file or dir '$file' not exists")
    }
    return try {
        var isAccessible = true
        if (forRead) {
            isAccessible = isAccessible and file.canRead()
        }
        if (forWrite) {
            isAccessible = isAccessible and file.canWrite()
        }
        isAccessible
    } catch (e: SecurityException) {
        throw RuntimeException(formatException(e, "canRead / canWrite on file '$file'"))
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
private fun isFileOrDirExistsOrThrow(dir: File?, isFile: Boolean): Boolean =
        try {
            dir != null && (if (isFile) dir.isFile else dir.isDirectory) && dir.exists()
        } catch (e: SecurityException) {
            throw RuntimeException(formatException(e), e)
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
                    throw RuntimeException(formatException(e, "listFiles on dir $dir"))
                }
        return files == null || files.isEmpty()
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
    require(checkDir(dir, createIfNotExists)) { "Incorrect file: $dir" }
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
        throw IllegalArgumentException("Incorrect path: " + parent + File.separator + fileName)
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
        fileName: String?,
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
        fileName: String?,
        parentPath: String? = null,
        recreate: Boolean = true
): File {
    if (!parentPath.isNullOrEmpty()) {
        createDirOrThrow(parentPath)
    }
    val newFile = toFile(fileName, parentPath)
            ?: throw RuntimeException("Incorrect fileName or parentPath")
    if (isFileExistsOrThrow(newFile)) {
        if (recreate) {
            deleteFileOrThrow(newFile)
        }
    }
    if (!isFileExistsOrThrow(newFile)) {
        try {
            if (!newFile.createNewFile()) {
                throw RuntimeException("Cannot create new file: $newFile")
            }
            if (!isFileExistsOrThrow(newFile)) {
                throw RuntimeException("File still not exists: $newFile")
            }
        } catch (e: Exception) {
            throwRuntimeException(e, "createNewFile on $newFile")
        }
    }
    return newFile
}

@JvmOverloads
fun createDir(
        dirName: String?,
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
        dirName: String?,
        parentPath: String? = null,
        throwIfExists: Boolean = false
): File {
    val dir = toFile(dirName, parentPath)
            ?: throw RuntimeException("Incorrect dirName or parentPath")
    if (isDirExistsOrThrow(dir)) {
        if (throwIfExists) {
            throw RuntimeException("Cannot create dir $dir: already exists")
        }
        return dir
    }
    val created = try {
        dir.mkdirs()
    } catch (e: SecurityException) {
        throw RuntimeException(formatException(e, "mkdirs on $dir"), e)
    }
    if (!created || !isDirExistsOrThrow(dir)) {
        throw RuntimeException("Cannot create new dir $dir")
    }
    return dir
}

@JvmOverloads
fun deleteEmptyDir(dir: File?, throwIfNotExists: Boolean = false) = try {
    deleteEmptyDirOrThrow(dir, throwIfNotExists)
    true
} catch (e: RuntimeException) {
    logger.e(e)
    false
}

@Throws(RuntimeException::class)
@JvmOverloads
fun deleteEmptyDirOrThrow(dir: File?, throwIfNotExists: Boolean = false) {
    if (dir == null) {
        throw NullPointerException("dir is null")
    }
    if (!isDirExists(dir)) {
        if (throwIfNotExists) {
            throw RuntimeException("Cannot delete dir $dir: already not exists")
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
        throwRuntimeException(e, "delete dir $dir")
    }
}

@JvmOverloads
fun deleteFile(file: File?, throwIfNotExists: Boolean = false) = try {
    deleteFileOrThrow(file, throwIfNotExists)
    true
} catch (e: RuntimeException) {
    logger.e(e)
    false
}

@Throws(RuntimeException::class)
@JvmOverloads
fun deleteFileOrThrow(file: File?, throwIfNotExists: Boolean = false) {
    if (file == null) {
        throw NullPointerException("file is null")
    }
    if (!isFileExistsOrThrow(file)) {
        if (throwIfNotExists) {
            throw RuntimeException("Cannot delete file $file: already not exists")
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

fun resetFile(file: File?) = try {
    resetFileOrThrow(file)
    true
} catch (e: RuntimeException) {
    logger.e(e)
    false
}

@Throws(RuntimeException::class)
fun resetFileOrThrow(file: File?) {
    if (file == null) {
        throw NullPointerException("file is null")
    }
    if (!isFileExistsOrThrow(file)) {
        throw RuntimeException("file '$file' not exists")
    }
    if (file.length() == 0L) {
        return
    }
    var ra: RandomAccessFile? = null
    try {
        ra = RandomAccessFile(file, "rw")
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

fun isBinaryFile(f: File?): Boolean = try {
    isBinaryFileOrThrow(f)
} catch (e: RuntimeException) {
    logger.e(e)
    false
}

@Throws(RuntimeException::class)
fun isBinaryFileOrThrow(f: File?): Boolean {
    val data = readBytesFromFileOrThrow(f)
    var ascii = 0
    var other = 0
    for (b in data) {
        if (b < 0x09) return true
        if (b.toInt() == 0x09 || b.toInt() == 0x0A || b.toInt() == 0x0C || b.toInt() == 0x0D) ascii++ else if (b in 0x20..0x7E) ascii++ else other++
    }
    return other != 0 && 100 * other / (ascii + other) > 95
}

fun readBytesFromFile(file: File?): ByteArray? = try {
    readBytesFromFileOrThrow(file)
} catch (e: RuntimeException) {
    logger.e(e)
    null
}

@Throws(RuntimeException::class)
fun readBytesFromFileOrThrow(file: File?): ByteArray {
    if (file == null) {
        throw NullPointerException("file is null")
    }
    if (!isFileValidOrThrow(file)) {
        throw RuntimeException("Invalid file: '$file'")
    }
//    if (!file.canRead()) {
//        throw RuntimeException("Cannot read from file: '$file'")
//    }
    return readBytesFromInputStreamOrThrow(file.toFisOrThrow())
}

@JvmOverloads
fun readStringsFromFile(
        file: File?,
        count: Int = 0,
        charsetName: String = CHARSET_DEFAULT
): List<String> = try {
    readStringsFromFileOrThrow(file, count, charsetName)
} catch (e: RuntimeException) {
    logger.e(e)
    emptyList()
}

@Throws(RuntimeException::class)
@JvmOverloads
fun readStringsFromFileOrThrow(
        file: File?,
        count: Int = 0,
        charsetName: String = CHARSET_DEFAULT
): List<String> {
    if (file == null || !isFileValidOrThrow(file)) {
        throw RuntimeException("Incorrect file: '$file'")
    }
    if (!file.canRead()) {
        throw RuntimeException("Cannot read from file: '$file'")
    }
    return readStringsFromInputStreamOrThrow(file.toFisOrThrow(), count, charsetName = charsetName)
}

@JvmOverloads
fun writeBytesToFile(
        file: File?,
        data: ByteArray?,
        append: Boolean = false
) = try {
    writeBytesToFileOrThrow(file, data, append)
    true
} catch (e: RuntimeException) {
    logger.e(e)
    false
}

@Throws(RuntimeException::class)
@JvmOverloads
fun writeBytesToFileOrThrow(
        file: File?,
        data: ByteArray?,
        append: Boolean = false
) {
    if (file == null) {
        throw NullPointerException("file is null")
    }
    if (data == null) {
        throw NullPointerException("data is null")
    }
    if (!isFileExistsOrThrow(file)) {
        createFileOrThrow(file.name, file.absolutePath, !append)
    }
//    if (!file.canWrite()) {
//        throw RuntimeException("Cannot write to file: '$file'")
//    }
    writeBytesToOutputStreamOrThrow(file.toFosOrThrow(append), data)
}

@JvmOverloads
fun writeStringsToFile(
        file: File?,
        data: Collection<String>?,
        append: Boolean = false
) = try {
    writeStringsToFileOrThrow(file, data, append)
    true
} catch (e: RuntimeException) {
    logger.e(e)
    false
}

@Throws(RuntimeException::class)
@JvmOverloads
fun writeStringsToFileOrThrow(
        file: File?,
        data: Collection<String>?,
        append: Boolean = false
) {
    if (file == null) {
        throw NullPointerException("file is null")
    }
    if (!isFileExistsOrThrow(file)) {
        createFileOrThrow(file.name, file.absolutePath, !append)
    }
//    if (!file.canWrite()) {
//        throw RuntimeException("Cannot write to file: '$file'")
//    }
    val writer: FileWriter1 = try {
        FileWriter1(file)
    } catch (e: IOException) {
        throw RuntimeException(formatException(e, "create FileWriter"))
    }
    val bw = BufferedWriter(writer)
    try {
        for (line in (data ?: emptyList())) {
            bw.append(line)
            bw.append(NEXT_LINE)
            bw.flush()
        }
    } catch (e: IOException) {
        throwRuntimeException(e)
    } finally {
        try {
            bw.close()
        } catch (e: IOException) {
            logException(logger, e, "close")
        }
    }
}

@JvmOverloads
fun writeFromStreamToFile(
        inputStream: InputStream?,
        targetFile: File?,
        append: Boolean = false,
        notifier: IStreamNotifier? = null
): File? = writeFromStreamToFile(inputStream, targetFile?.name, targetFile?.parent, append, notifier)

@Throws(RuntimeException::class)
@JvmOverloads
fun writeFromStreamToFileOrThrow(
        inputStream: InputStream?,
        targetFile: File?,
        append: Boolean = false,
        notifier: IStreamNotifier? = null
): File = writeFromStreamToFileOrThrow(inputStream, targetFile?.name, targetFile?.parent, append, notifier)

@JvmOverloads
fun writeFromStreamToFile(
        inputStream: InputStream?,
        targetFileName: String?,
        parentPath: String?,
        append: Boolean = false,
        notifier: IStreamNotifier? = null,
        buffSize: Int = DEFAULT_BUFFER_SIZE
): File? = try {
    writeFromStreamToFileOrThrow(inputStream, targetFileName, parentPath, append, notifier, buffSize)
} catch (e: RuntimeException) {
    logger.e(e)
    null
}

@Throws(RuntimeException::class)
@JvmOverloads
fun writeFromStreamToFileOrThrow(
        inputStream: InputStream?,
        targetFileName: String?,
        parentPath: String?,
        append: Boolean = false,
        notifier: IStreamNotifier? = null,
        buffSize: Int = DEFAULT_BUFFER_SIZE
): File {
    if (inputStream == null) {
        throw NullPointerException("inputStream is null")
    }
    val file: File = createFileOrThrow(targetFileName, parentPath, !append)
//    if (!file.canWrite()) {
//        throw RuntimeException("Cannot write to file '$file'")
//    }
    copyStreamOrThrow(inputStream, file.toFosOrThrow(), notifier, buffSize)
    return file
}

@JvmOverloads
fun moveFile(
        sourceFile: File?,
        targetFile: File?,
        deleteIfExists: Boolean,
        deleteEmptyDirs: Boolean,
        preserveFileDate: Boolean = true,
        copyWithBuffering: Boolean = true,
        notifier: ISingleCopyNotifier? = null
): File? {
    if (sourceFile == null) {
        throw NullPointerException("sourceFile is null")
    }
    if (sourceFile == targetFile) {
        logger.w("Cannot rename: files match ('$sourceFile')")
        return targetFile
    }
    var targetFile = renameFile(sourceFile, targetFile?.parent, targetFile?.name, deleteIfExists, deleteEmptyDirs)
    if (targetFile == null) {
        targetFile = if (copyWithBuffering) {
            copyFileWithBuffering(sourceFile, targetFile?.name, targetFile?.parent, deleteIfExists, preserveFileDate, notifier)
        } else {
            try {
                copyFileOrThrow(sourceFile, targetFile?.name, targetFile?.parent, deleteIfExists, preserveFileDate)
            } catch (e: RuntimeException) {
                notifier?.onExceptionOccurred(e)
                null
            }
        }
        deleteFile(sourceFile)
    }
    return targetFile
}

fun renameFile(
        sourceFile: File?,
        destinationDir: String?,
        newFileName: String?,
        deleteIfExists: Boolean,
        deleteEmptyDirs: Boolean
): File? = try {
    renameFileOrThrow(sourceFile, destinationDir, newFileName, deleteIfExists, deleteEmptyDirs)
} catch (e: RuntimeException) {
    logger.e(e)
    null
}

@Throws(RuntimeException::class)
fun renameFileOrThrow(
        sourceFile: File?,
        destinationDir: String?,
        newFileName: String?,
        deleteIfExists: Boolean,
        deleteEmptyDirs: Boolean
): File {
    if (sourceFile == null) {
        throw NullPointerException("sourceFile is null")
    }
    if (!isFileExistsOrThrow(sourceFile)) {
        throw RuntimeException("Source file not exists: $sourceFile")
    }
    if (newFileName.isNullOrEmpty()) {
        throw RuntimeException("File name for new file is not specified")
    }
    val newFile: File
    val newDir = createDir(destinationDir)
    if (newDir != null) {
        newFile = File(newDir, newFileName)
        if (newFile != sourceFile) {
            if (isFileExistsOrThrow(newFile)) {
                logger.d("Target file $newFile already exists")
                if (deleteIfExists) {
                    if (!deleteFile(newFile)) {
                        throw RuntimeException("Delete file $newFile failed")
                    }
                } else {
                    throw RuntimeException("File $newFile exists, not deleting")
                }
            }
            if (sourceFile.renameTo(newFile)) {
                val sourceParentDir = sourceFile.parentFile
                if (deleteEmptyDirs) {
                    deleteEmptyDirOrThrow(sourceParentDir)
                }
            } else {
                throw RuntimeException("Rename $sourceFile to $newFile failed")
            }
        } else {
            logger.w("New file $newFile is same as source file")
        }
    } else {
        throw RuntimeException("Create new dir: '$destinationDir' failed")
    }
    return newFile
}


@JvmOverloads
fun copyFileWithBuffering(
        sourceFile: File?,
        targetFile: File?,
        rewrite: Boolean = true,
        preserveFileDate: Boolean = true,
        notifier: ISingleCopyNotifier? = null
): File? = copyFileWithBuffering(sourceFile, targetFile?.name, targetFile?.parent, rewrite, preserveFileDate, notifier)

/**
 * @return target file
 */
@JvmOverloads
fun copyFileWithBuffering(
        sourceFile: File?,
        targetName: String?,
        targetDir: String?,
        rewrite: Boolean = true,
        preserveFileDate: Boolean = true,
        notifier: ISingleCopyNotifier? = null
): File? {
    if (sourceFile == null || !isFileExistsOrThrow(sourceFile)) {
        notifier?.onExceptionOccurred(RuntimeException("Source file not exists: $sourceFile"))
        return null
    }
    val targetName = if (targetName.isNullOrEmpty()) sourceFile.name else targetName
    val targetFile = if (targetDir != null && !isEmpty(targetName)) File(targetDir, targetName) else null
    if (targetFile == null || targetFile == sourceFile) {
        notifier?.onExceptionOccurred(RuntimeException("Incorrect destination file: '$targetDir' (source file: $sourceFile)"))
        return null
    }
    val fis = try {
        sourceFile.toFisOrThrow()
    } catch (e: RuntimeException) {
        notifier?.onExceptionOccurred(e)
        return null
    }

    val totalBytesCount = sourceFile.length()

    val resultFile = try {

        writeFromStreamToFileOrThrow(fis, targetFile.name, targetFile.parent, !rewrite, if (notifier != null) object : IStreamNotifier {

            override val notifyInterval: Long
                get() = notifier.notifyInterval

            override fun onProcessing(inputStream: InputStream, outputStream: OutputStream, bytesWrite: Long, bytesLeft: Long): Boolean =
                    notifier.shouldProceed(sourceFile, targetFile, bytesWrite, totalBytesCount)

        } else null)

    } catch (e: RuntimeException) {
        notifier?.onExceptionOccurred(e)
        null
    }

    if (preserveFileDate) {
        setLastModified(resultFile, sourceFile.lastModified())
    }
    return null
}

@JvmOverloads
fun copyFile(
        sourceFile: File?,
        targetName: String?,
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
        sourceFile: File?,
        targetName: String?,
        targetDir: String?,
        rewrite: Boolean = true,
        preserveFileDate: Boolean = true
): File {
    if (sourceFile == null || !isFileValidOrThrow(sourceFile)) {
        throw RuntimeException("Source file is not valid: $sourceFile")
    }
    val targetName = if (targetName.isNullOrEmpty()) sourceFile.name else targetName
    val targetFile = createFileOrThrow(targetName, targetDir, rewrite)
    writeBytesToFileOrThrow(targetFile, readBytesFromFile(sourceFile), !rewrite)
    if (preserveFileDate) {
        setLastModified(targetFile, sourceFile.lastModified())
    }
    return targetFile
}

fun setLastModified(
        file: File?,
        timestamp: Long
) = try {
    setLastModifiedOrThrow(file, timestamp)
    true
} catch (e: RuntimeException) {
    logger.e(e)
    false
}

@Throws(RuntimeException::class)
fun setLastModifiedOrThrow(
        file: File?,
        timestamp: Long
) {
    if (file == null) {
        throw RuntimeException("file is null")
    }
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
        notifier: IGetSizeNotifier? = null,
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
        fromFile: File?,
        depth: Int = DEPTH_UNLIMITED,
        currentLevel: Int = 0,
        notifier: IGetSizeNotifier? = null,
): Long {
    var size: Long = 0
    if (fromFile != null) {
        if (fromFile.isDirectory && (notifier == null || notifier.onGetFolder(fromFile, size, currentLevel))) {
            val files = fromFile.listFiles()
            if (files != null && files.isNotEmpty()) {
                for (file in files) {
                    if (depth == DEPTH_UNLIMITED || depth > currentLevel) {
                        size += getSize(file, depth, currentLevel + 1)
                    }
                }
            }
        } else if (fromFile.isFile && (notifier == null || notifier.onGetFile(fromFile, size, currentLevel))) {
            size = fromFile.length()
        }
    }
    return size
}

@JvmOverloads
fun getFiles(
        fromFiles: Collection<File>?,
        mode: GetMode = GetMode.ALL,
        comparator: Comparator<in File>?,
        depth: Int = DEPTH_UNLIMITED,
        notifier: IGetListNotifier?,
): Set<File> {
    val result = mutableSetOf<File>()
    if (fromFiles != null) {
        for (fromFile in fromFiles) {
            result.addAll(getFiles(fromFile, mode, comparator, depth, notifier = notifier))
        }
    }
    return result
}

/**
 * @param fromFile file or directory
 * @return collected set of files or directories from specified directories without source files
 */
@JvmOverloads
fun getFiles(
        fromFile: File?,
        mode: GetMode = GetMode.ALL,
        comparator: Comparator<in File>?,
        depth: Int = DEPTH_UNLIMITED,
        currentLevel: Int = 0,
        notifier: IGetListNotifier? = null
): Set<File> {
    val result = mutableSetOf<File>()
    if (depth != DEPTH_UNLIMITED && currentLevel > depth - 1) {
        logger.w("Collect depth was reached: $depth")
        return result
    }
    if (fromFile == null) {
        notifier?.onExceptionOccurred(NullPointerException("fromFile is null"))
        return result
    }
    if (fromFile.exists()) {
        logger.w("file '$fromFile' not exists")
        return result
    }
    if (!fromFile.isFile && !fromFile.isDirectory) {
        logger.w("Invalid file or folder: '$fromFile'")
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
                })) {
            result.add(fromFile)
            wasAdded = true
        }
    }
    if (notifier == null || notifier.shouldProceed(fromFile, Collections.unmodifiableSet(result), currentLevel, wasAdded)) {
        if (fromFile.isDirectory) {
            val files = fromFile.listFiles()?.toList() ?: emptyList()
            for (f in files) {
//                    if (f.isDirectory) {
//                        if (depth == DEPTH_UNLIMITED || depth > currentLevel) {
//                            result.addAll(getFiles(f, mode, comparator, depth, currentLevel + 1, notifier))
//                        }
//                    } else if (f.isFile) {
//                        result.addAll(getFiles(f, mode, comparator, depth, currentLevel, notifier))
//                    } else {
//                        logger.w("Invalid file or folder: $f")
//                    }
                result.addAll(getFiles(f, mode, comparator, depth, if (f.isDirectory) currentLevel + 1 else currentLevel, notifier))
            }
        }
    } else {
        logger.w("Collecting files from $fromFile was interrupted")
    }
    comparator?.let {
        return result.sortedWith(it).toSet()
    }
    return result
}

@JvmOverloads
fun searchByName(
        name: String,
        searchFile: File?,
        searchFlags: Int = MatchStringOption.AUTO.flag,
        searchFirst: Boolean = false,
        mode: GetMode = GetMode.ALL,
        comparator: Comparator<in File>? = null,
        depth: Int = DEPTH_UNLIMITED,
        notifier: IGetListNotifier? = null,
): Set<File> = searchByName(name, if (searchFile != null) listOf(searchFile) else null, searchFlags, searchFirst, mode, comparator, depth, notifier)

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
        notifier: IGetListNotifier? = null,
): Set<File> = getFiles(searchFiles, mode, comparator, depth, notifier = object : IGetListNotifier {

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
fun copyFilesWithBuffering(
        sourceFile: File,
        targetDir: File?,
        comparator: Comparator<in File>? = null,
        preserveFileDate: Boolean = true,
        tryToMoveFirst: Boolean = true,
        copyWithBuffering: Boolean = true,
        depth: Int = DEPTH_UNLIMITED,
        exclusionList: Collection<File>? = null,
        singleCopyNotifier: ISingleCopyNotifier? = null,
        multipleCopyNotifier: IMultipleCopyNotifier? = null,
): Set<File>? {
    val result = mutableSetOf<File>()
    if (targetDir == null) {
        multipleCopyNotifier?.onExceptionOccurred(NullPointerException("targetDir is null"))
        return result
    }
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
    val files: Set<File> = getFiles(sourceFile, GetMode.FILES, comparator, depth, notifier = if (multipleCopyNotifier != null) object : IGetListNotifier {

        override fun onGetFile(file: File, collected: Set<File>, currentLevel: Int): Boolean {
            return multipleCopyNotifier.onCollecting(file, collected, currentLevel)
        }

        override fun onGetFolder(folder: File, collected: Set<File>, currentLevel: Int): Boolean {
            return multipleCopyNotifier.onCollecting(folder, collected, currentLevel)
        }

    } else null)

    val totalFilesCount = files.size.toLong()
    var filesProcessed = 0
    for (f in files) {
        if (!isFileExistsOrThrow(f)) {
            logger.w("File '$f' not exists, skipping...")
            continue
        }
        if (exclusionList != null && exclusionList.contains(f)) {
            logger.w("File '$f' is on exclusion list, skipping...")
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
            if (!multipleCopyNotifier.shouldProceed(f, currentDestDir, Collections.unmodifiableSet(result), filesProcessed.toLong(), totalFilesCount)) {
                logger.w("Copying from '$sourceFile' to '$targetDir' was interrupted")
                break
            }
        }
        var targetFile: File? = null
        var confirmCopy = true
        if (multipleCopyNotifier != null) {
            confirmCopy = multipleCopyNotifier.confirmCopy(f, currentDestDir)
        }
        if (confirmCopy) {
            if (multipleCopyNotifier != null) {
                targetFile = multipleCopyNotifier.onBeforeCopy(f, currentDestDir)
            }
            val isSameFile = targetFile == f
            if (targetFile == null || isSameFile) {
                if (isSameFile) {
                    logger.w("Target file cannot be equals to source file ('$targetFile')!")
                }
                targetFile = File(currentDestDir, f.name)
            }
            var replaceOptions = IMultipleCopyNotifier.ReplaceOptions()
            if (multipleCopyNotifier != null && isFileExistsOrThrow(targetFile)) {
                replaceOptions = multipleCopyNotifier.confirmReplace(targetFile)
            }
            val shouldReplace = replaceOptions.enableReplace
                    || !replaceOptions.enableReplace && replaceOptions.enableAppend
            if (!shouldReplace) {
                logger.w("Replace disabled for file $targetFile, skipping...")
                continue
            }
            val resultFile =
                    if (tryToMoveFirst) {
                        moveFile(f, targetFile, !replaceOptions.enableAppend, false, preserveFileDate, copyWithBuffering, singleCopyNotifier)
                    } else {
                        if (copyWithBuffering) {
                            copyFileWithBuffering(f, targetFile.name, targetFile.parent, !replaceOptions.enableAppend,
                                    preserveFileDate, singleCopyNotifier)
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
        }
        filesProcessed++
    }
    comparator?.let {
        return result.sortedWith(it).toSet()
    }
    return result
}

/**
 * @param fromFile file or directory
 */
@Deprecated("use copyFilesWithBuffering")
@JvmOverloads
fun copyFilesWithBufferingLegacy(
        fromFile: File?,
        targetDir: File?,
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

    if (targetDir == null) {
        multipleCopyNotifier?.onExceptionOccurred(NullPointerException("targetDir is null"))
        return result
    }
    if (fromFile != null && fromFile.exists()) {
        isValid = true
        if (currentLevel == 0) {
            totalFilesCount = getFiles(fromFile, GetMode.FILES, comparator, depth, notifier = if (multipleCopyNotifier != null) object : IGetListNotifier {

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
            if (!multipleCopyNotifier.onProcessing(fromFile, targetDir, Collections.unmodifiableSet(copied), totalFilesCount.toLong(), currentLevel)) {
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
                            result.addAll(copyFilesWithBufferingLegacy(f,  /*new File(targetDir + File.separator + fromFile.getName(), f.getName())*/targetDir, comparator,
                                    preserveFileDate, depth, currentLevel + 1, totalFilesCount, copied, exclusionList, singleNotifier, multipleCopyNotifier))
                        }
                    } else {
                        result.addAll(copyFilesWithBufferingLegacy(f,  /*new File(targetDir, fromFile.getName()) */targetDir, comparator,
                                preserveFileDate, depth, currentLevel, totalFilesCount, copied, exclusionList, singleNotifier, multipleCopyNotifier))
                    }
                }
            }
            if (files == null || files.isEmpty()) {
                val emptyDir = if (currentLevel == 0) targetDir.toString() + File.separator + fromFile.name else targetDir.absolutePath
                if (!isDirExistsOrThrow(emptyDir)) {
                    createDir(emptyDir)
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
                    resultFile = copyFileWithBuffering(fromFile, targetFile.name, targetFile.parent, rewrite,
                            preserveFileDate, singleNotifier)
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
fun delete(
        fromFiles: Collection<File>?,
        deleteEmptyDirs: Boolean = true,
        exclusionList: Collection<File>? = null,
        comparator: Comparator<in File>?,
        depth: Int = DEPTH_UNLIMITED,
        notifier: IDeleteNotifier? = null
): Set<File> {
    val result: MutableSet<File> = LinkedHashSet()
    if (fromFiles != null) {
        for (file in fromFiles) {
            result.addAll(delete(file, deleteEmptyDirs, exclusionList, comparator, depth, notifier = notifier))
        }
    }
    return result
}

/**
 * @param depth кол-во уровней вложенености (номер последней итерации == depth - 1)
 */
@JvmOverloads
fun delete(
        fromFile: File?,
        deleteEmptyDirs: Boolean = true,
        exclusionList: Collection<File>? = null,
        comparator: Comparator<in File>?,
        depth: Int = DEPTH_UNLIMITED,
        currentLevel: Int = 0,
        notifier: IDeleteNotifier? = null
): Set<File> {

    val result = mutableSetOf<File>()

    if (depth != DEPTH_UNLIMITED && currentLevel > depth - 1) {
        logger.w("Delete depth was reached: $depth")
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

    if (fromFile == null) {
        notifier?.onExceptionOccurred(NullPointerException("fromFile is null"))
        return result
    }

    if (notifier != null && !notifier.shouldProceed(fromFile, Collections.unmodifiableSet(result), currentLevel)) {
        logger.w("Deleting from '$fromFile' was interrupted")
        return result
    }

    if (!fromFile.exists()) {
        logger.w("File '$fromFile' not exists")
        return result
    }

    if (exclusionList != null && exclusionList.contains(fromFile)) {
        logger.w("File '$fromFile' is on exclusion list, skipping...")
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
            result.addAll(delete(f, deleteEmptyDirs, exclusionList, comparator, depth, if (f.isDirectory) currentLevel + 1 else currentLevel, notifier))
        }
        deleteEmptyDirChecked(fromFile)
    } else if (fromFile.isFile) {
        if (notifier == null || notifier.confirmDeleteFile(fromFile)) {
            if (deleteFile(fromFile)) {
                result.add(fromFile)
            } else {
                notifier?.onDeleteFileFailed(fromFile)
            }
        }
    } else {
        logger.w("Invalid file or folder: '$fromFile'")
    }

    return result
}

@JvmOverloads
fun compressFilesToZip(
        srcFiles: Collection<File>?,
        destZipName: String?,
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
        srcFiles: Collection<File>?,
        destZipName: String?,
        destZipParent: String?,
        recreate: Boolean = true,
        buffSize: Int = DEFAULT_BUFFER_SIZE,
        notifier: IStreamNotifier? = null
): File {
    val zipFile = createFileOrThrow(destZipName, destZipParent, recreate)

    val os = zipFile.toFosOrThrow()
    val zos = try {
        ZipOutputStream(BufferedOutputStream(os))
    } catch (e: Exception) {
        throw RuntimeException(formatException(e, "create OutputStream"))
    }

    try {
        var zippedFiles = 0
        for (srcFile in (srcFiles ?: emptyList())) {
            if (!isFileValidOrThrow(srcFile)) {
                logger.w("Invalid file to zip: '$srcFile', skipping...")
                continue
            }
            val entry = ZipEntry(srcFile.name)
            zos.putNextEntry(entry)
            copyStreamOrThrow(srcFile.toFisOrThrow(), zos, notifier, buffSize, closeInput = true, closeOutput = false)
            zos.closeEntry()
            zippedFiles++
        }
        if (zippedFiles > 0) {
            return File(destZipName as String)
        } else {
            throw RuntimeException("Nothing zipped")
        }
    } catch (e: Exception) {
        if (e !is RuntimeException) {
            throw RuntimeException(formatException(e))
        } else {
            throw e
        }
    } finally {
        try {
            zos.close()
            os.close()
        } catch (e: IOException) {
            logException(logger, e, "close")
        }
    }
}

@Throws(RuntimeException::class)
@JvmOverloads
fun unzipFileOrThrow(
        zipFile: File,
        destPath: File?,
        saveDirHierarchy: Boolean = true,
        recreate: Boolean = true,
        notifier: IStreamNotifier? = null,
        buffSize: Int = DEFAULT_BUFFER_SIZE,
) {
    if (!isFileValidOrThrow(zipFile)) {
        throw IllegalArgumentException("Invalid zip file: '$zipFile'")
    }
    if (destPath == null) {
        throw NullPointerException("destPath is null")
    }
    val zip: ZipFile = try {
        ZipFile(zipFile)
    } catch (e: IOException) {
        throw RuntimeException(formatException(e, "create ZipFile"))
    }
    try {
        for (e in zip.entries()) {
            val isDirectory = e.isDirectory
            if (isDirectory && !saveDirHierarchy) {
                continue
            }
            val parts = e.name.split(File.separator).toTypedArray()
            val entryName = if (!saveDirHierarchy && parts.isNotEmpty()) parts[parts.size - 1] else e.name
            if (isDirectory) {
                createDirOrThrow(entryName, destPath.absolutePath)
            } else {
                createFileOrThrow(entryName, destPath.absolutePath, recreate)
                copyStreamOrThrow(zip.getInputStream(e), File(destPath, entryName).toFosOrThrow(!recreate), notifier, buffSize)
            }
        }
    } catch (e: Exception) {
        if (e !is RuntimeException) {
            throwRuntimeException(e)
        } else {
            throw e
        }
    } finally {
        try {
            zip.close()
        } catch (e: IOException) {
            logException(logger, e, "close")
        }
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
        notifier: IShellGetListNotifier? = null
): Set<File> {
    val result = mutableSetOf<File>()
    for (file in paths?.toList() ?: emptyList()) {
        var path = file.absolutePath
        if (!path.endsWith(File.separator)) {
            path += File.separator
        }
        val targetPath = path + name
        ShellWrapper(false).executeCommand(listOf("stat", targetPath), useSU, DEFAULT_TARGET_CODE, execTimeout, TimeUnit.MILLISECONDS, object : ShellCallback {

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
        notifier: IShellGetListNotifier? = null
): Map<File, Long> {
    val collectedMap = mutableMapOf<File, Long>()
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
    @JvmDefault
    fun onExceptionOccurred(e: RuntimeException) {
        logException(logger, e)
    }
}

interface IGetSizeNotifier : IFsNotifier {
    @JvmDefault
    fun onGetFile(file: File, currentSize: Long, currentLevel: Int): Boolean = true

    @JvmDefault
    fun onGetFolder(folder: File, currentSize: Long, currentLevel: Int): Boolean = true
}

interface IGetListNotifier : IFsNotifier {
    /**
     * @return false if client code doesn't want to append this file to result
     */
    @JvmDefault
    fun onGetFile(file: File, collected: Set<File>, currentLevel: Int): Boolean = true

    /**
     * @return 1st false - if client code doesn't want to append this folder to result, 2nd false - if not intended to go deeper
     */
    @JvmDefault
    fun onGetFolder(folder: File, collected: Set<File>, currentLevel: Int): Boolean = true

    /**
     * @return false to interrupt collecting
     */
    @JvmDefault
    fun shouldProceed(current: File, collected: Set<File>, currentLevel: Int, wasAdded: Boolean): Boolean = true
}

interface ISingleCopyNotifier : IFsNotifier {
    val notifyInterval: Long get() = 0L

    @JvmDefault
    fun shouldProceed(sourceFile: File, targetFile: File, bytesCopied: Long, bytesTotal: Long): Boolean = true
}

interface IMultipleCopyNotifier : IFsNotifier {
    /**
     * @return false if process should be interrupted
     */
    @JvmDefault
    fun onCollecting(current: File, collected: Set<File>, currentLevel: Int): Boolean = true

    /**
     * @return false if process should be interrupted
     */
    @JvmDefault
    fun shouldProceed(currentFile: File, targetDir: File, copied: Set<File>, filesProcessed: Long, filesTotal: Long): Boolean = true

    /**
     * true if copying confirmed by client code, false to cancel
     */
    @JvmDefault
    fun confirmCopy(currentFile: File, targetDir: File): Boolean = true

    /**
     * @return target file to copy in or null for default
     */
    @JvmDefault
    fun onBeforeCopy(currentFile: File, targetDir: File): File? = null

    /**
     * @return true if specified destination file is should be replaced (it currently exists)
     */
    @JvmDefault
    fun confirmReplace(targetFile: File): ReplaceOptions = ReplaceOptions()

    @JvmDefault
    fun onSucceeded(currentFile: File, resultFile: File) {
    }

    @JvmDefault
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
    @JvmDefault
    fun shouldProceed(current: File, deleted: Set<File>, currentLevel: Int): Boolean = true

    /**
     * @return false if client code doesn't want to delete this file
     */
    @JvmDefault
    fun confirmDeleteFile(file: File): Boolean = true

    /**
     * @return false if client code doesn't want to delete this folder
     */
    @JvmDefault
    fun confirmDeleteFolder(folder: File): Boolean = true

    @JvmDefault
    fun onDeleteFileFailed(file: File) {
    }

    @JvmDefault
    fun onDeleteFolderFailed(folder: File) {
    }
}

interface IShellGetListNotifier : IGetListNotifier {
    @JvmDefault
    fun onStartFailed(t: Throwable?, forFile: File) {
    }

    @JvmDefault
    fun onExitCode(exitCode: Int, forFile: File) {
    }
}

class FileComparator(sortOptions: Map<SortOption, Boolean?>) : BaseOptionalComparator<FileComparator.SortOption, File>(sortOptions) {

    enum class SortOption : ISortOption {
        NAME, SIZE, LAST_MODIFIED;

        override val optionName: String
            get() = name
    }

    override fun compare(lhs: File, rhs: File, option: SortOption, ascending: Boolean?): Int {
        var ascending = ascending
        ascending = ascending != null && ascending
        return when (option) {
            SortOption.NAME -> compareStrings(lhs.absolutePath, rhs.absolutePath, ascending, true)
            SortOption.SIZE -> compareLongs(lhs.length(), rhs.length(), ascending)
            SortOption.LAST_MODIFIED -> compareLongs(lhs.lastModified(), rhs.lastModified(), ascending)
        }
    }
}

fun File?.toFis(): FileInputStream? = try {
    toFisOrThrow()
} catch (e: RuntimeException) {
    logger.e(e)
    null
}

@Throws(RuntimeException::class)
fun File?.toFisOrThrow(): FileInputStream = try {
    if (this == null) {
        throw NullPointerException("file is null")
    }
    FileInputStream(this)
} catch (e: FileNotFoundException) {
    throw RuntimeException("File $this not found", e)
} catch (e: SecurityException) {
    throw RuntimeException(formatException(e, "create FileInputStream"))
}

fun File?.toFos(): FileOutputStream? = try {
    toFosOrThrow()
} catch (e: RuntimeException) {
    logger.e(e)
    null
}

@Throws(RuntimeException::class)
@JvmOverloads
fun File?.toFosOrThrow(append: Boolean = false): FileOutputStream = try {
    if (this == null) {
        throw NullPointerException("file is null")
    }
    FileOutputStream(this, append)
} catch (e: FileNotFoundException) {
    throw RuntimeException("File $this not found", e)
} catch (e: SecurityException) {
    throw RuntimeException(formatException(e, "create FileOutputStream"))
}

private fun toFile(
        fileName: String?,
        parentPath: String? = null,
        checkSeparators: Boolean = false
): File? {
    if (fileName.isNullOrEmpty() || (checkSeparators && fileName.contains(File.separatorChar))) {
        return null
    }
    return if (parentPath == null) File(fileName) else File(parentPath, fileName)
}