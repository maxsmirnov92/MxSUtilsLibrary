package ru.maxsmr.devicewatchers.storage;


import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import net.maxsmr.commonutils.data.FileHelper;
import net.maxsmr.tasksutils.ScheduledThreadPoolExecutorManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


public class StorageStateWatcher {

    private final static Logger logger = LoggerFactory.getLogger(StorageStateWatcher.class);

    private static StorageStateWatcher sInstance;

    public static void initInstance(@NonNull StorageWatchSettings settings, @Nullable IDeleteConfirm confirmer) {
        if (sInstance == null) {
            synchronized (StorageStateWatcher.class) {
                logger.debug("initInstance()");
                sInstance = new StorageStateWatcher(settings, confirmer);
            }
        }
    }

    public static StorageStateWatcher getInstance() {
        if (sInstance == null) {
            throw new IllegalStateException("initInstance() was not called");
        }
        return sInstance;
    }

    public static void releaseInstance() {
        synchronized (StorageStateWatcher.class) {
            if (sInstance != null) {
                sInstance.release();
                sInstance = null;
            }
        }
    }

    private void release() {
        watchListeners.clear();
        stop();
    }

    @NonNull
    private final StorageWatchSettings settings;

    @Nullable
    private final IDeleteConfirm confirmer;

    /*
     * @param maxArchiveSizeKb maximum possible archive size in kB
     * @param thresholdArchive ratio limit of occupied archive space to given maxArchiveSizeKb value */
    public StorageStateWatcher(@NonNull StorageWatchSettings settings, @Nullable IDeleteConfirm confirmer) {
        logger.debug("StorageStateWatcher(), settings=" + settings + ", confirmer=" + confirmer);
        this.settings = settings;
        this.confirmer = confirmer;
    }

    public interface WatchListener {
        void onLimitExceeded(int totalKb, int freeKb, int usedKb, StorageWatchSettings settings, double howMuchExceeded);

        void onLimitNotExceeded(int totalKb, int freeKb, int usedKb, StorageWatchSettings settings);

        void onPartitionReadError(StorageWatchSettings settings);
    }

    private final LinkedList<WatchListener> watchListeners = new LinkedList<>();

    public void addWatchListener(@NonNull WatchListener l) {
        synchronized (watchListeners) {
            if (!watchListeners.contains(l)) {
                watchListeners.add(l);
            }
        }
    }

    public void removeWatchListener(@NonNull WatchListener l) {
        synchronized (watchListeners) {
            watchListeners.remove(l);
        }
    }

    public static final int DEFAULT_WATCH_INTERVAL = 20000;

    private final ScheduledThreadPoolExecutorManager storageStateWatcher = new ScheduledThreadPoolExecutorManager("DeviceWatcher");

    public boolean isRunning() {
        return storageStateWatcher.isRunning();
    }

    public void stop() {
        logger.debug("stop()");
        if (isRunning()) {
            storageStateWatcher.removeAllRunnableTasks();
            storageStateWatcher.stop(false, 0);
        }
    }

    public void restart(long interval) {
        if (interval <= 0) {
            throw new IllegalArgumentException("incorrect interval: " + interval);
        }
        stop();
        storageStateWatcher.addRunnableTask(new StorageStateWatcherRunnable());
        storageStateWatcher.start(0, interval);
    }

    public void start(long interval) {
        logger.debug("start(), interval=" + interval);
        if (!isRunning()) {
            restart(interval);
        }
    }

    private class StorageStateWatcherRunnable implements Runnable {

        @NonNull
        private final Map<StorageWatchSettings.DeleteOptionPair, List<File>> mapping = new HashMap<>();

        @Override
        public void run() {
            doStateWatch(true);
        }

        private void doStateWatch(boolean notify) {
            logger.debug("doStateWatch(), notify=" + notify);

            final int totalKb = FileHelper.getPartitionTotalSpaceKb(settings.targetPath);
            final int freeKb = FileHelper.getPartitionFreeSpaceKb(settings.targetPath);
            final int usedKb = totalKb - freeKb;

            logger.info("=== storage total space: " + totalKb + " kB, free: " + freeKb + " kB, used: " + usedKb + " kB ===");

            if (totalKb == 0) {
                if (notify) {
                    synchronized (watchListeners) {
                        for (WatchListener l : watchListeners) {
                            l.onPartitionReadError(settings);
                        }
                    }
                }
                return;
            }

            final boolean exceeds;
            final double differenceRatio;
            final double differenceKb;

            switch (settings.what) {

                case RATIO:
                    final double ratio = (double) usedKb / (double) totalKb;
                    logger.info("ratio: " + ratio * 100 + "% | threshold: " + settings.value * 100 + "%");
                    differenceRatio = (exceeds = ratio >= settings.value) ? ratio - settings.value : 0;
                    differenceKb = 0;
                    break;

                case SIZE:
                    logger.info("used: " + usedKb + " kB | threshold: " + settings.value + " kB");
                    differenceRatio = 0;
                    differenceKb = (exceeds = usedKb >= settings.value) ? usedKb - settings.value : 0;
                    break;

                default:
                    exceeds = false;
                    differenceRatio = 0;
                    differenceKb = 0;
                    break;
            }

            if (notify) {
                synchronized (watchListeners) {
                    for (WatchListener l : watchListeners) {
                        if (exceeds) {
                            l.onLimitExceeded(totalKb, freeKb, usedKb, settings, settings.what == StorageWatchSettings.ThresholdWhat.RATIO ? differenceRatio : differenceKb);
                        } else {
                            l.onLimitNotExceeded(totalKb, freeKb, usedKb, settings);
                        }
                    }
                }
            }

            if (!exceeds) {

                logger.info("storage size is less than given limit");

                Collection<List<File>> filesFoldersList = mapping.values();

                for (List<File> list : filesFoldersList) {
                    if (list != null && !list.isEmpty()) {
                        list.clear();
                    }
                }

                mapping.clear();

            } else {

                logger.warn("storage size exceeds given limit!");

                if (settings.deleteOptionPairs != null) {

                    if (mapping.isEmpty()) {
                        for (StorageWatchSettings.DeleteOptionPair pair : settings.deleteOptionPairs) {
                            String deletePath = pair != null ? settings.targetPath + File.separator + pair.path : null;
                            if (FileHelper.isDirExists(deletePath)) {
                                List<File> filesList;
                                switch (pair.mode) {
                                    case FILES:
                                        filesList = FileHelper.getFiles(new File(deletePath), true, null);
                                        break;
                                    case FOLDERS:
                                        filesList = FileHelper.getFolders(new File(deletePath), false, null);
                                        break;
                                    default:
                                        filesList = null;
                                        break;
                                }
                                FileHelper.sortFilesByLastModified(filesList, true, true);
                                mapping.put(pair, filesList);
                            }
                        }
                    }

                    int deletedCount = 0;

                    for (StorageWatchSettings.DeleteOptionPair pair : settings.deleteOptionPairs) {
                        if (pair != null) {
                            final List<File> filesToDelete = mapping.get(pair);
                            if (filesToDelete != null && !filesToDelete.isEmpty()) {
                                boolean allowDelete = true;
                                switch (pair.mode) {
                                    case FILES:
                                        for (File file : filesToDelete) {
                                            if (confirmer != null) {
                                                allowDelete = confirmer.allowDeleteFile(file);
                                            }
                                            deletedCount += allowDelete ? FileHelper.deleteFilesInList(Collections.singletonList(file), true, 1) : 0;
                                            if (deletedCount > 0) {
                                                doStateWatch(false);
                                                break;
                                            }
                                        }
                                        break;

                                    case FOLDERS:
                                        for (File folder : filesToDelete) {
                                            if (confirmer != null) {
                                                allowDelete = confirmer.allowDeleteFolder(folder);
                                            }
                                            deletedCount += allowDelete ? FileHelper.deleteFoldersInList(Collections.singletonList(folder), true, 0) : 0;
                                            if (deletedCount > 0) {
                                                doStateWatch(false);
                                                break;
                                            }
                                        }
                                        break;
                                }
                            }
                        }
                    }

                }
            }
        }
    }

    public interface IDeleteConfirm {
        boolean allowDeleteFile(@NonNull File file);

        boolean allowDeleteFolder(@NonNull File folder);
    }


}
