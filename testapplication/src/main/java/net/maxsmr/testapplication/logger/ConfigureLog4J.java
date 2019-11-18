package net.maxsmr.testapplication.logger;

import net.maxsmr.commonutils.data.FileHelper;

import org.apache.log4j.Level;
import org.jetbrains.annotations.Nullable;

import de.mindpipe.android.logging.log4j.LogConfigurator;

public class ConfigureLog4J {

    public static final long DEFAULT_MIN_FILE_SIZE = FileHelper.SizeUnit.MBYTES.toBytes(1);

    private static ConfigureLog4J mInstance = null;

    public static void initInstance() {
        synchronized (ConfigureLog4J.class) {
            if (mInstance == null) {
                mInstance = new ConfigureLog4J();
            }
        }
    }

    public static void reInitInstance() {
        synchronized (ConfigureLog4J.class) {
            mInstance = null;
            initInstance();
        }
    }

    public static ConfigureLog4J getInstance() {
        initInstance();
        return mInstance;
    }

    private boolean isInitialized = false;

    private ConfigureLog4J() {
    }

    public synchronized boolean isInitialized() {
        return isInitialized;
    }

    /**
     * @param level         minimum logging level
     * @param maxFileSize   log file size in bytes
     * @param maxBackupSize number of log backups
     */
    public synchronized void configure(Level level, boolean useFile, @Nullable String filePath, long maxFileSize, int maxBackupSize) {

        if (isInitialized) {
            throw new IllegalStateException(ConfigureLog4J.class.getSimpleName() + " is already initialized");
        }

        if (level == null)
            throw new NullPointerException("level is null");

        LogConfigurator logConfigurator = new LogConfigurator();

        if (useFile) {

            if (maxFileSize <= 0)
                throw new IllegalArgumentException("incorrect maxFileSize: " + maxFileSize);

            if (maxBackupSize < 0)
                throw new IllegalArgumentException("incorrect maxBackupSize: " + maxBackupSize);

            FileHelper.checkFile(filePath);

            logConfigurator.setUseFileAppender(true);

            logConfigurator.setFileName(filePath);
            logConfigurator.setMaxFileSize(maxFileSize);
            logConfigurator.setMaxBackupSize(maxBackupSize);
            logConfigurator.setFilePattern("%d{dd/MM/yyyy HH:mm:ss,SSS} %5p %c:%L - %m%n");

        } else {
            logConfigurator.setUseFileAppender(false);
        }

        logConfigurator.setRootLevel(level);

        logConfigurator.configure();

        isInitialized = true;
    }

}
