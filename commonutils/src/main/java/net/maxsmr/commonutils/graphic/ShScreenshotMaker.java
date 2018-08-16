package net.maxsmr.commonutils.graphic;


import android.support.annotation.Nullable;

import net.maxsmr.commonutils.data.FileHelper;
import net.maxsmr.commonutils.shell.ShellUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ShScreenshotMaker {

    private final static Logger logger = LoggerFactory.getLogger(ShScreenshotMaker.class);

    private static ShScreenshotMaker sInstance;

    public static void initInstance() {
        if (sInstance == null) {
            synchronized (ShScreenshotMaker.class) {
                logger.debug("initInstance()");
                sInstance = new ShScreenshotMaker();
            }
        }
    }

    public static ShScreenshotMaker getInstance() {
        initInstance();
        return sInstance;
    }

    private static final String SCREENCAP_PROCESS_NAME = "screencap";

    private final ThreadsWatcher watcher = new ThreadsWatcher();

    /**
     * @return true if started successfully, false - otherwise
     */
    public boolean makeScreenshotAsync(String folderName, String fileName, @Nullable ShellUtils.ShellCallback sc) {
        logger.debug("makeScreenshotAsync(), folderName=" + folderName + ", fileName=" + fileName);

        if (watcher.isRunning()) {
            logger.error(SCREENCAP_PROCESS_NAME + " is running at the moment");
            return false;
        }

        final File destFile = FileHelper.createNewFile(fileName, folderName);

        if (destFile == null) {
            logger.error("can't create file: " + folderName + File.separator + fileName);
            return false;
        }

        return ShellUtils.execProcessAsync(new ArrayList<>(Arrays.asList(new String[]{"su", "-c", SCREENCAP_PROCESS_NAME, destFile.getName()})), destFile.getParent(), sc, watcher);
    }

    /**
     * @return screenshot file if completed successfully
     */
    @Nullable
    public File makeScreenshot(String folderName, String fileName, @Nullable ShellUtils.ShellCallback sc) {
        logger.debug("makeScreenshot(), folderName=" + folderName + ", fileName=" + fileName);

        if (watcher.isRunning()) {
            logger.error("process " + SCREENCAP_PROCESS_NAME + " is running at the moment");
            return null;
        }

        final File destFile = FileHelper.createNewFile(fileName, folderName);

        if (destFile == null) {
            logger.error("can't create file: " + folderName + File.separator + fileName);
            return null;
        }

        return ShellUtils.execProcess(new ArrayList<>(Arrays.asList(new String[]{"su", "-c", SCREENCAP_PROCESS_NAME, destFile.getAbsolutePath()})), null, sc, watcher).isSuccessful() ? destFile : null;
    }

    private static class ThreadsWatcher implements ShellUtils.ThreadsCallback {

        private final List<Thread> threads = new ArrayList<>();

        public boolean isRunning() {
            for (Thread thread : threads) {
                if (thread.isAlive() && !thread.isInterrupted()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void onThreadsStarted(List<Thread> threads) {
            this.threads.clear();
            this.threads.addAll(threads);
        }
    }


}
