package net.maxsmr.devicewatchers.storage;


import net.maxsmr.commonutils.FileComparator;
import net.maxsmr.commonutils.FileUtilsKt;
import net.maxsmr.commonutils.GetMode;
import net.maxsmr.commonutils.IDeleteNotifier;
import net.maxsmr.commonutils.IGetNotifier;
import net.maxsmr.commonutils.Predicate;
import net.maxsmr.commonutils.conversion.SizeUnit;
import net.maxsmr.commonutils.logger.BaseLogger;
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder;
import net.maxsmr.tasksutils.ScheduledThreadPoolExecutorManager;
import net.maxsmr.tasksutils.runnable.RunnableInfoRunnable;
import net.maxsmr.tasksutils.taskexecutor.RunnableInfo;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static net.maxsmr.commonutils.CompareUtilsKt.stringsEqual;
import static net.maxsmr.commonutils.FileUtilsKt.DEPTH_UNLIMITED;
import static net.maxsmr.commonutils.FileUtilsKt.deleteFiles;
import static net.maxsmr.commonutils.FileUtilsKt.getFiles;
import static net.maxsmr.commonutils.FileUtilsKt.getPartitionSpace;
import static net.maxsmr.commonutils.FileUtilsKt.isDirExists;
import static net.maxsmr.commonutils.text.TextUtilsKt.isEmpty;
import static net.maxsmr.tasksutils.ScheduledThreadPoolExecutorManager.ScheduleMode.FIXED_DELAY;

public final class StorageStateWatcher {

    private final static BaseLogger logger = BaseLoggerHolder.getInstance().getLogger(StorageStateWatcher.class);

    public static final Comparator<File> DEFAULT_FILE_COMPARATOR = new FileComparator(Collections.singletonMap(FileComparator.SortOption.LAST_MODIFIED, true));

    public static final int DEFAULT_WATCH_INTERVAL = 20000;

    private final ScheduledThreadPoolExecutorManager executor = new ScheduledThreadPoolExecutorManager("StorageStateWatcher");

    @NotNull
    private final StorageWatchSettings settings;

    @Nullable
    private final IDeleteConfirm confirmer;

    private final Set<WatchListener> watchListeners = new LinkedHashSet<>();

    private boolean isEnabled = true;

    public StorageStateWatcher(@NotNull StorageWatchSettings settings, @Nullable IDeleteConfirm confirmer) {
        logger.d("StorageStateWatcher(), settings=" + settings + ", confirmer=" + confirmer);
        this.settings = settings;
        this.confirmer = confirmer;
    }

    public void release() {
        stop();
        clearWatchListeners();
    }

    @NotNull
    public StorageWatchSettings getSettings() {
        return settings;
    }


    public void addWatchListener(@NotNull WatchListener l) {
        synchronized (watchListeners) {
            watchListeners.add(l);
        }
    }

    public void removeWatchListener(@NotNull WatchListener l) {
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
        logger.d("stop()");
        executor.stop();
    }

    public void restart(long interval) {
        logger.d("restart()");
        executor.removeAllRunnableTasks();
        executor.addRunnableTask(new StorageStateWatcherRunnable(), new ScheduledThreadPoolExecutorManager.RunOptions(0, interval, FIXED_DELAY));
        executor.restart(1);
    }

    public void start(long interval) {
        logger.d("start()");
        if (!isRunning()) {
            restart(interval);
        }
    }

    private class StorageStateWatcherRunnable extends RunnableInfoRunnable<RunnableInfo> {

        @NotNull
        private final Map<StorageWatchSettings.DeleteOptionPair, Set<File>> mapping = new HashMap<>();

        StorageStateWatcherRunnable() {
            super(new RunnableInfo(1));
        }

        @Override
        public void run() {
            doStateWatch(true);
        }

        private void doStateWatch(boolean notify) {
            logger.d("doStateWatch(), notify=" + notify);

            final long totalKb = (long) getPartitionSpace(settings.targetPath, null, SizeUnit.KBYTES, true);
            final long freeKb = (long) getPartitionSpace(settings.targetPath, null, SizeUnit.KBYTES, false);
            final long usedKb = totalKb - freeKb;

            logger.i("=== storage total space: " + totalKb + " kB, free: " + freeKb + " kB, used: " + usedKb + " kB ===");

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
                    logger.i("ratio: " + ratio * 100 + "% | threshold: " + settings.value * 100 + "%");
                    differenceRatio = (exceeds = ratio >= settings.value) ? ratio - settings.value : 0;
                    differenceKb = 0;
                    break;

                case SIZE:
                    logger.i("used: " + usedKb + " kB | threshold: " + settings.value + " kB");
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

                logger.i("storage size is less than given limit");

                Collection<Set<File>> filesFoldersList = mapping.values();

                for (Set<File> set : filesFoldersList) {
                    if (set != null && !set.isEmpty()) {
                        set.clear();
                    }
                }

                mapping.clear();

            } else {

                logger.w("storage size exceeds given limit!");

                if (settings.deleteOptionMap != null) {

                    if (mapping.isEmpty()) {
                        for (Map.Entry<String, GetMode> entry : settings.deleteOptionMap.entrySet()) {

                            if (!isEnabled) {
                                logger.e(StorageStateWatcher.class.getSimpleName() + " is disabled");
                                return;
                            }

                            String deletePath = entry.getKey(); // settings.targetPath + File.separator +
                            if (isEmpty(deletePath)) {
                                throw new RuntimeException("deletePath is empty");
                            }
                            if (isDirExists(deletePath)) {
                                final Set<File> filesSet = getFiles(new File(deletePath), entry.getValue(), settings.comparator, DEPTH_UNLIMITED, 0, new IGetNotifier() {
                                    @Override
                                    public boolean shouldProceed(@NotNull File current, @NotNull Set<? extends File> collected, int currentLevel, boolean wasAdded) {
                                        return isEnabled;
                                    }
                                });
                                mapping.put(new StorageWatchSettings.DeleteOptionPair(entry.getValue(), entry.getKey()), filesSet);
                            }
                        }
                    }

                    int deletedCount = 0;

                    for (Map.Entry<String, GetMode> entry : settings.deleteOptionMap.entrySet()) {

                        if (!isEnabled) {
                            logger.e(StorageStateWatcher.class.getSimpleName() + " is disabled");
                            return;
                        }

                        if (entry != null) {

                            final StorageWatchSettings.DeleteOptionPair pair = findPairFromMappingByPath(entry.getKey());

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
                                            deletedCount += FileUtilsKt.deleteFiles(file, true, null, DEPTH_UNLIMITED, 0, new IDeleteNotifier() {

                                                @Override
                                                public boolean shouldProceed(@NotNull File current, @NotNull Set<? extends File> deleted, int currentLevel) {
                                                    return isEnabled;
                                                }

                                                @Override
                                                public boolean confirmDeleteFile(@NotNull File file) {
                                                    return pair.mode == GetMode.FILES || pair.mode == GetMode.ALL;
                                                }

                                                @Override
                                                public boolean confirmDeleteFolder(@NotNull File folder) {
                                                    return pair.mode == GetMode.FOLDERS || pair.mode == GetMode.ALL;
                                                }

                                                @Override
                                                public void onDeleteFileFailed(@NotNull File file) {
                                                    logger.e("onDeleteFileFailed(), file=" + file);
                                                }

                                                @Override
                                                public void onDeleteFolderFailed(@NotNull File folder) {
                                                    logger.e("onDeleteFolderFailed(), folder=" + folder);
                                                }

                                            }).size();
                                        }

                                        if (deletedCount > 0) {
                                            if (!isEnabled) {
                                                logger.e(StorageStateWatcher.class.getSimpleName() + " is disabled");
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
        StorageWatchSettings.DeleteOptionPair findPairFromMappingByPath(final String path) {
            return Predicate.Methods.find(mapping.keySet(), new Predicate<StorageWatchSettings.DeleteOptionPair>() {
                @Override
                public boolean apply(StorageWatchSettings.DeleteOptionPair element) {
                    return element != null && stringsEqual(element.path, path, true);
                }
            });
        }

    }

    public interface IDeleteConfirm {

        boolean allowDeleteFile(@NotNull File file);

        boolean allowDeleteFolder(@NotNull File folder);
    }

    public interface WatchListener {

        void onLimitExceeded(long totalKb, long freeKb, long usedKb, StorageWatchSettings settings, double howMuchExceeded);

        void onLimitNotExceeded(long totalKb, long freeKb, long usedKb, StorageWatchSettings settings);

        void onPartitionReadError(StorageWatchSettings settings);
    }
}