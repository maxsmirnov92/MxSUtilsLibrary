package net.maxsmr.devicewatchers.storage;


import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import net.maxsmr.commonutils.data.CompareUtils;
import net.maxsmr.commonutils.data.FileHelper;
import net.maxsmr.tasksutils.ScheduledThreadPoolExecutorManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;


public final class StorageStateWatcher {

    private final static Logger logger = LoggerFactory.getLogger(StorageStateWatcher.class);

    public static final Comparator<File> DEFAULT_FILE_COMPARATOR = new FileHelper.FileComparator(Collections.singletonMap(FileHelper.FileComparator.SortOption.LAST_MODIFIED, true));

    public static final int DEFAULT_WATCH_INTERVAL = 20000;

    private final ScheduledThreadPoolExecutorManager executor = new ScheduledThreadPoolExecutorManager(ScheduledThreadPoolExecutorManager.ScheduleMode.FIXED_DELAY, StorageStateWatcher.class.getSimpleName());

    @NonNull
    private final StorageWatchSettings settings;

    @Nullable
    private final IDeleteConfirm confirmer;

    private final Set<WatchListener> watchListeners = new LinkedHashSet<>();

    private boolean isEnabled = true;

    public StorageStateWatcher(@NonNull StorageWatchSettings settings, @Nullable IDeleteConfirm confirmer) {
        logger.debug("StorageStateWatcher(), settings=" + settings + ", confirmer=" + confirmer);
        this.settings = settings;
        this.confirmer = confirmer;
    }

    public void release() {
        stop();
        clearWatchListeners();
    }

    @NonNull
    public StorageWatchSettings getSettings() {
        return settings;
    }


    public void addWatchListener(@NonNull WatchListener l) {
        synchronized (watchListeners) {
            watchListeners.add(l);
        }
    }

    public void removeWatchListener(@NonNull WatchListener l) {
        synchronized (watchListeners) {
            watchListeners.remove(l);
        }
    }

    private void clearWatchListeners() {
        synchronized (watchListeners) {
            watchListeners.clear();
        }
    }



    public boolean isEnabled() {
        synchronized (executor) { // нельзя чтобы треды, запущенные из executor'а вставали на синхронизированные области одновременно, будет deadlock
            return isEnabled;
        }
    }

    public void setEnabled(boolean enabled) {
        synchronized (executor) {
            isEnabled = enabled;
        }
    }

    public boolean isRunning() {
        return executor.isRunning();
    }

    public void stop() {
        logger.debug("stop()");
        if (isRunning()) {
            executor.removeAllRunnableTasks();
        }
    }

    public void restart(long interval) {
        if (interval <= 0) {
            throw new IllegalArgumentException("incorrect interval: " + interval);
        }
        stop();
        executor.addRunnableTask(new StorageStateWatcherRunnable());
        executor.restart(interval);
    }

    public void start(long interval) {
        logger.debug("start(), interval=" + interval);
        if (!isRunning()) {
            restart(interval);
        }
    }

    private class StorageStateWatcherRunnable implements Runnable {

        @NonNull
        private final Map<StorageWatchSettings.DeleteOptionPair, Set<File>> mapping = new HashMap<>();

        @Override
        public void run() {
            doStateWatch(true);
        }

        private void doStateWatch(boolean notify) {
            logger.debug("doStateWatch(), notify=" + notify);

            final long totalKb = (long) FileHelper.getPartitionTotalSpaceKb(settings.targetPath, FileHelper.SizeUnit.KBYTES);
            final long freeKb = (long) FileHelper.getPartitionFreeSpaceKb(settings.targetPath, FileHelper.SizeUnit.KBYTES);
            final long usedKb = totalKb - freeKb;

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

                Collection<Set<File>> filesFoldersList = mapping.values();

                for (Set<File> set : filesFoldersList) {
                    if (set != null && !set.isEmpty()) {
                        set.clear();
                    }
                }

                mapping.clear();

            } else {

                logger.warn("storage size exceeds given limit!");

                if (settings.deleteOptionMap != null) {

                    if (mapping.isEmpty()) {
                        for (Map.Entry<String, FileHelper.GetMode> entry : settings.deleteOptionMap.entrySet()) {

                            if (!isEnabled) {
                                logger.error(StorageStateWatcher.class.getSimpleName() + " is disabled");
                                return;
                            }

                            String deletePath = entry.getKey(); // settings.targetPath + File.separator +
                            if (TextUtils.isEmpty(deletePath)) {
                                throw new RuntimeException("deletePath is empty");
                            }
                            if (FileHelper.isDirExists(deletePath)) {
                                final Set<File> filesSet = FileHelper.getFiles(Collections.singleton(new File(deletePath)), entry.getValue(), settings.comparator, new FileHelper.IGetNotifier() {
                                    @Override
                                    public boolean onProcessing(@NonNull File current, @NonNull Set<File> collected, int currentLevel) {
                                        return isEnabled;
                                    }

                                    @Override
                                    public boolean onGetFile(@NonNull File file) {
                                        return true;
                                    }

                                    @Override
                                    public boolean onGetFolder(@NonNull File folder) {
                                        return true;
                                    }
                                }, FileHelper.DEPTH_UNLIMITED);
                                mapping.put(new StorageWatchSettings.DeleteOptionPair(entry.getValue(), entry.getKey()), filesSet);
                            }
                        }
                    }

                    int deletedCount = 0;

                    for (Map.Entry<String, FileHelper.GetMode> entry : settings.deleteOptionMap.entrySet()) {

                        if (!isEnabled) {
                            logger.error(StorageStateWatcher.class.getSimpleName() + " is disabled");
                            return;
                        }

                        if (entry != null) {

                            StorageWatchSettings.DeleteOptionPair pair = findPairFromMappingByPath(entry.getKey());

                            if (pair != null) {

                                final Set<File> filesToDelete = mapping.get(pair);

                                if (filesToDelete != null && !filesToDelete.isEmpty()) {
                                    boolean allowDelete = false;
                                    for (File file : filesToDelete) {
                                        if (confirmer != null) {
                                            switch (pair.mode) {
                                                case FILES:
                                                    if (file.isFile()) {
                                                        allowDelete = confirmer.allowDeleteFile(file);
                                                    }
                                                    break;
                                                case FOLDERS:
                                                    if (file.isDirectory()) {
                                                        allowDelete = confirmer.allowDeleteFolder(file);
                                                    }
                                                    break;
                                                case ALL:
                                                    if (file.isFile()) {
                                                        allowDelete = confirmer.allowDeleteFile(file);
                                                    } else if (file.isDirectory()) {
                                                        allowDelete = confirmer.allowDeleteFolder(file);
                                                    }
                                                    break;
                                                default:
                                                    throw new RuntimeException("unknown mode: " + pair.mode);
                                            }

                                        }

                                        if (allowDelete) {
                                            deletedCount += FileHelper.deleteFromCollection(Collections.singleton(file), pair.mode, true, true, 1, null, null, new FileHelper.IDeleteNotifier() {
                                                @Override
                                                public boolean onProcessing(@NonNull File current, @NonNull Set<File> deleted, int currentLevel) {
                                                    return isEnabled;
                                                }

                                                @Override
                                                public boolean confirmDeleteFile(File file) {
                                                    return true;
                                                }

                                                @Override
                                                public boolean confirmDeleteFolder(File folder) {
                                                    return true;
                                                }
                                            }).size();
                                        }

                                        if (deletedCount > 0) {
                                            if (!isEnabled) {
                                                logger.error(StorageStateWatcher.class.getSimpleName() + " is disabled");
                                                return;
                                            }
                                            doStateWatch(false);
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        @Nullable
        StorageWatchSettings.DeleteOptionPair findPairFromMappingByPath(String path) {
            for (StorageWatchSettings.DeleteOptionPair p : mapping.keySet()) {
                if (p != null && CompareUtils.stringsEqual(p.path, path, true)) {
                    return p;
                }
            }
            return null;
        }

    }

    public interface IDeleteConfirm {

        boolean allowDeleteFile(@NonNull File file);

        boolean allowDeleteFolder(@NonNull File folder);
    }

    public interface WatchListener {

        void onLimitExceeded(long totalKb, long freeKb, long usedKb, StorageWatchSettings settings, double howMuchExceeded);

        void onLimitNotExceeded(long totalKb, long freeKb, long usedKb, StorageWatchSettings settings);

        void onPartitionReadError(StorageWatchSettings settings);
    }


}
