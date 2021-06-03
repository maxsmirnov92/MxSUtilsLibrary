package net.maxsmr.testapp.logger

import androidx.annotation.MainThread
import de.mindpipe.android.logging.log4j.LogConfigurator
import net.maxsmr.commonutils.checkFile
import net.maxsmr.commonutils.conversion.SizeUnit
import org.apache.log4j.Level
import java.io.File

val MAX_FILE_SIZE_DEFAULT = SizeUnit.MBYTES.toBytes(1.0)
const val MAX_BACKUP_COUNT_DEFAULT = 1

object ConfigureLog4J {

    private val logConfigurator = LogConfigurator()

    /**
     * @param level         minimum logging level
     * @param maxFileSize   log file size in bytes
     * @param maxBackupSize number of log backups
     */
    @MainThread
    @JvmOverloads
    @JvmStatic
    fun configure(
            level: Level,
            file: File?,
            maxFileSize: Long = MAX_FILE_SIZE_DEFAULT,
            maxBackupSize: Int = MAX_BACKUP_COUNT_DEFAULT
    ) {
        if (file != null) {
            require(maxFileSize > 0) { "Incorrect maxFileSize: $maxFileSize" }
            require(maxBackupSize >= 0) { "Incorrect maxBackupSize: $maxBackupSize" }
            checkFile(file)
            logConfigurator.isUseFileAppender = true
            logConfigurator.fileName = file.absolutePath
            logConfigurator.maxFileSize = maxFileSize
            logConfigurator.maxBackupSize = maxBackupSize
            logConfigurator.filePattern = "%d{dd/MM/yyyy HH:mm:ss,SSS} %5p %c:%L - %m%n"
        } else {
            logConfigurator.isUseFileAppender = false
        }
        logConfigurator.rootLevel = level
        logConfigurator.configure()
    }
}
