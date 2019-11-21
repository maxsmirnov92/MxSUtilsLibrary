package net.maxsmr.commonutils.graphic;


import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.maxsmr.commonutils.data.FileHelper;
import net.maxsmr.commonutils.logger.BaseLogger;
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder;
import net.maxsmr.commonutils.shell.CmdThreadInfo;
import net.maxsmr.commonutils.shell.ShellCallback;
import net.maxsmr.commonutils.shell.ThreadsCallback;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import static net.maxsmr.commonutils.data.SymbolConstKt.EMPTY_STRING;
import static net.maxsmr.commonutils.shell.ShellUtilsKt.execProcess;
import static net.maxsmr.commonutils.shell.ShellUtilsKt.execProcessAsync;

public final class ShScreenshotMaker {

    private final static BaseLogger logger = BaseLoggerHolder.getInstance().getLogger(ShScreenshotMaker.class);

    private static ShScreenshotMaker sInstance;

    public static void initInstance() {
        if (sInstance == null) {
            synchronized (ShScreenshotMaker.class) {
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
    public boolean makeScreenshotAsync(String folderName, String fileName, @Nullable ShellCallback sc) {
        logger.d("makeScreenshotAsync(), folderName=" + folderName + ", fileName=" + fileName);

        if (watcher.isRunning()) {
            logger.e(SCREENCAP_PROCESS_NAME + " is running at the moment");
            return false;
        }

        final File destFile = FileHelper.createNewFile(fileName, folderName);

        if (destFile == null) {
            logger.e("can't create file: " + folderName + File.separator + fileName);
            return false;
        }

        return execProcessAsync(
                Arrays.asList("su", "-c", SCREENCAP_PROCESS_NAME, destFile.getName()),
                destFile.getParent(),
                null,
                sc,
                watcher
        );
    }

    /**
     * @return screenshot file if completed successfully
     */
    @Nullable
    public File makeScreenshot(String folderName, String fileName, @Nullable ShellCallback sc) {
        logger.d("makeScreenshot(), folderName=" + folderName + ", fileName=" + fileName);

        if (watcher.isRunning()) {
            logger.e("process " + SCREENCAP_PROCESS_NAME + " is running at the moment");
            return null;
        }

        final File destFile = FileHelper.createNewFile(fileName, folderName);

        if (destFile == null) {
            logger.e("can't create file: " + folderName + File.separator + fileName);
            return null;
        }

        return execProcess(
                Arrays.asList("su", "-c", SCREENCAP_PROCESS_NAME, destFile.getAbsolutePath()),
                EMPTY_STRING,
                null,
                null,
                sc,
                watcher
        ).isSuccessful() ? destFile : null;
    }

    private static class ThreadsWatcher implements ThreadsCallback {

        private final Set<Thread> threads = new LinkedHashSet<>();

        public boolean isRunning() {
            for (Thread thread : threads) {
                if (thread.isAlive()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void onThreadStarted(@NotNull CmdThreadInfo info, @NotNull Thread thread) {
            threads.remove(thread);
        }

        @Override
        public void onThreadFinished(@NotNull CmdThreadInfo info, @NotNull Thread thread) {
            threads.add(thread);
        }
    }


}
