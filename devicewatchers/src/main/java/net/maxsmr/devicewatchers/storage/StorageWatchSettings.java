package net.maxsmr.devicewatchers.storage;

import net.maxsmr.commonutils.GetMode;
import net.maxsmr.commonutils.conversion.SizeUnit;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

import static net.maxsmr.commonutils.FileUtilsKt.checkDir;
import static net.maxsmr.commonutils.FileUtilsKt.getPartitionSpace;

public class StorageWatchSettings {

    public enum ThresholdWhat {

        RATIO(0), SIZE(1);

        public final int value;

        ThresholdWhat(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static ThresholdWhat fromValue(int value) {

            for (ThresholdWhat what : ThresholdWhat.values()) {
                if (what.getValue() == value) {
                    return what;
                }
            }

            throw new IllegalArgumentException("Incorrect value " + value + " for enum type " + ThresholdWhat.class.getName());
        }

    }

    @NotNull
    public final ThresholdWhat what;

    /**
     * calculate archive or storage limit size based on storage total size
     */
    public final static int SIZE_AUTO = 0;
    public final static double DEFAULT_PARTITION_MIN_SIZE_KB = 50 * 1024;
    public final static double DEFAULT_PARTITION_THRESHOLD = 0.95;

    public final double value;

    @NotNull
    public final String targetPath;

    @Nullable
    public final Map<String, GetMode> deleteOptionMap;

    @NotNull
    public final Comparator<? super File> comparator;

    /**
     *
     * @param what threshold storage; check by ratio used / total space OR by absolute value in kB
     * @param targetPath path to watch and clean if necessary
     * @param deleteOptionMap map with absolute paths and delete modes for them
     */
    public StorageWatchSettings(@NotNull ThresholdWhat what, double value, @NotNull String targetPath, @Nullable Map<String, GetMode> deleteOptionMap, @NotNull Comparator<? super File> comparator) {

        checkDir(targetPath);
        this.targetPath = targetPath;
        this.comparator = comparator;

        this.deleteOptionMap = deleteOptionMap != null? new LinkedHashMap<>(deleteOptionMap) : null;

        this.what = what;

        switch (what) {
            case RATIO:

                if (value < 0 || value >= 1) {
                    throw new IllegalArgumentException("incorrect value: " + value);
                }

                this.value = value == SIZE_AUTO ? DEFAULT_PARTITION_THRESHOLD : value;
                break;

            case SIZE:

                double totalKb = getPartitionSpace(targetPath, null, SizeUnit.KBYTES, true);

                if ((value < DEFAULT_PARTITION_MIN_SIZE_KB || value >= totalKb) && value != SIZE_AUTO) {
                    throw new IllegalArgumentException("incorrect value: " + value);
                }

                if (value == SIZE_AUTO) {
                    this.value = totalKb * DEFAULT_PARTITION_THRESHOLD;
                } else {
                    this.value = value;
                }

                break;

            default:
                this.value = 0;
                break;
        }
    }

    @Override
    @NotNull
    public String toString() {
        return "StorageWatchSettings{" +
                "what=" + what +
                ", value=" + value +
                ", targetPath='" + targetPath + '\'' +
                ", deleteOptionPairs=" + deleteOptionMap +
                '}';
    }

    static class DeleteOptionPair {

        @NotNull
        public final GetMode mode;

        @NotNull
        public final String path;

        /**
         *
         * @param path relative path above specified target path
         */
        public DeleteOptionPair(@NotNull GetMode mode, @NotNull String path) {
            this.mode = mode;
            this.path = path;
        }

        @Override
        @NotNull
        public String toString() {
            return "DeleteOptionPair{" +
                    "mode=" + mode +
                    ", path='" + path + '\'' +
                    '}';
        }
    }
}