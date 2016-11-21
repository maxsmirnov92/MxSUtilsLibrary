package net.maxsmr.commonutils.android.logging;

import net.maxsmr.commonutils.data.FileHelper;

import org.apache.log4j.Level;

import de.mindpipe.android.logging.log4j.LogConfigurator;

public class ConfigureLog4J {

    private static ConfigureLog4J mInstance = null;

    public static void initInstance(long minFileSize, String filePath) {
        if (mInstance == null) {
            synchronized (ConfigureLog4J.class) {
                mInstance = new ConfigureLog4J(minFileSize, filePath);
            }
        }
    }

    public static ConfigureLog4J getInstance() {
        if (mInstance == null) {
            throw new IllegalStateException("initInstance() was not called");
        }
        return mInstance;
    }

    public static final long DEFAULT_MIN_FILE_SIZE = 1024 * 1024;

    private final long minFileSize;
    private final String filePath;

    private ConfigureLog4J(long minFileSize, String filePath) {
        if (minFileSize <= 0) {
            throw new IllegalArgumentException("incorrect minFileSize: " + minFileSize);
        }
        FileHelper.checkFile(filePath);
        this.minFileSize = minFileSize;
        this.filePath = filePath;
    }

    /**
     * @param maxFileSize   log file size in bytes
     * @param maxBackupSize number of log backups
     * @param level         minimum logging level
     */
    public void configure(Level level, boolean useFile, long maxFileSize, int maxBackupSize) {

        if (level == null)
            throw new NullPointerException("level is null");

        if (useFile && maxFileSize < minFileSize)
            throw new IllegalArgumentException("incorrect maxFileSize: " + maxFileSize);

        if (useFile && maxBackupSize < 0)
            throw new IllegalArgumentException("incorrect maxBackupSize: " + maxBackupSize);

        LogConfigurator logConfigurator = new LogConfigurator();

        if (useFile) {
            logConfigurator.setFileName(filePath);
            logConfigurator.setMaxFileSize(maxFileSize);
            logConfigurator.setMaxBackupSize(maxBackupSize);
            logConfigurator.setFilePattern("%d{dd/MM/yyyy HH:mm:ss,SSS} %5p %c:%L - %m%n");
        } else {
            logConfigurator.setUseFileAppender(false);
        }

        logConfigurator.setRootLevel(level);

        logConfigurator.configure();
    }

}
