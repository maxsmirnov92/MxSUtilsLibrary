package net.maxsmr.testapp;

import android.Manifest;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.GrantPermissionRule;

import net.maxsmr.commonutils.data.FileComparator;
import net.maxsmr.commonutils.data.FileUtilsKt;
import net.maxsmr.commonutils.data.GetMode;
import net.maxsmr.commonutils.data.IDeleteNotifier;
import net.maxsmr.commonutils.data.IGetNotifier;
import net.maxsmr.commonutils.data.IMultipleCopyNotifier;
import net.maxsmr.commonutils.data.IShellGetNotifier;
import net.maxsmr.commonutils.data.ISingleCopyNotifier;
import net.maxsmr.commonutils.data.MatchStringOption;

import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.math.BigInteger;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static net.maxsmr.commonutils.android.media.MediaUtilsKt.getFilteredExternalFilesDirs;
import static net.maxsmr.commonutils.data.FileUtilsKt.DEPTH_UNLIMITED;
import static net.maxsmr.commonutils.data.FileUtilsKt.checkFilesWithStat;
import static net.maxsmr.commonutils.data.FileUtilsKt.copyFiles;
import static net.maxsmr.commonutils.data.FileUtilsKt.createDir;
import static net.maxsmr.commonutils.data.FileUtilsKt.createFile;
import static net.maxsmr.commonutils.data.FileUtilsKt.createFileOrThrow;
import static net.maxsmr.commonutils.data.FileUtilsKt.deleteFiles;
import static net.maxsmr.commonutils.data.FileUtilsKt.getFiles;
import static net.maxsmr.commonutils.data.FileUtilsKt.searchByName;
import static net.maxsmr.commonutils.data.FileUtilsKt.writeBytesToFile;

@RunWith(AndroidJUnit4.class)
public class FileHelperTest extends LoggerTest {

    @Rule
    public GrantPermissionRule readPermissionRule = GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE);

    @Rule
    public GrantPermissionRule writePermissionRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE);

    private static final String SOURCE_NAME = "source";
    private static final String DEST_NAME = "dest";

    private static final String SEARCH_PREFIX = "qq";

    private File sourceDir;

    private File destinationDir;

    @Before
    public void prepare() {
        super.prepare();
        createDir("/data/data/" + context.getPackageName() + "/files");
        final File filesDir = context.getFilesDir();
        Assert.assertNotNull(filesDir);
        sourceDir = new File(filesDir, SOURCE_NAME);
        destinationDir = new File(filesDir, DEST_NAME);
//        deleteFromDest()
//        createTestFiles()
    }

    @Override
    @Test
    public void test() {

    }

    @Test
    public void testGetExternalFilesDir() {
        Set<File> result = getFilteredExternalFilesDirs(context, false, true, true);
        System.out.println("filteredExternalFilesDirs: " + result);
        Assert.assertFalse(result.isEmpty());
    }

    @Test
    public void testGet() {
        Set<File> result = getFiles(sourceDir, GetMode.ALL, null, 0, DEPTH_UNLIMITED, new IGetNotifier() {
            @Override
            public boolean shouldProceed(@NotNull File current, @NotNull Set<? extends File> collected, int currentLevel, boolean wasAdded) {
                System.out.println("get file=" + current + ", currentLevel=" + currentLevel);
                return true;
            }
        });
        System.out.println("result=" + result);
    }

    @Test
    public void testSearch() {
        search(false);
    }

    @Test
    public void testSearchFirst() {
        search(true);
    }

    @Test
    public void testSearchStat() {
        Set<File> result = checkFilesWithStat(SEARCH_PREFIX, Collections.singleton(sourceDir), true, 0,
                new FileComparator(Collections.singletonMap(FileComparator.SortOption.NAME, true)),

                new IShellGetNotifier() {
                    @Override
                    public boolean shouldProceed(@NotNull File current, @NotNull Set<? extends File> collected, int currentLevel, boolean wasAdded) {
                        System.out.println("stat current=" + current + ", currentLevel=" + currentLevel);
                        return true;
                    }
                });
        System.out.println("result=" + result);
    }

    @Test
    public void testCopy() {
        copyFiles(
                sourceDir,
                destinationDir,
                null,
                true,
                true,
                DEPTH_UNLIMITED,
                new ISingleCopyNotifier() {
                    @Override
                    public long getNotifyInterval() {
                        return TimeUnit.SECONDS.toMillis(1);
                    }
                }, new IMultipleCopyNotifier() {
                    @Override
                    public boolean shouldProceed(@NotNull File currentFile, @NotNull File targetDir, @NotNull Set<? extends File> copied, long filesProcessed, long filesTotal) {
                        System.out.println("copy currentFile=" + currentFile + ", targetDir=" + targetDir + ", copied=" + copied);
                        return true;
                    }
                });
    }

    //    @Test
    public void deleteTest() {
        deleteTest(context.getFilesDir());
    }

    private void search(boolean first) {
        Set<File> result = searchByName(SEARCH_PREFIX, sourceDir,
                MatchStringOption.STARTS_WITH_IGNORE_CASE.getFlag(),
                first,
                GetMode.ALL,
                new FileComparator(Collections.singletonMap(FileComparator.SortOption.LAST_MODIFIED, false)),
                1,
                new IGetNotifier() {

                    @Override
                    public boolean shouldProceed(@NotNull File current, @NotNull Set<? extends File> collected, int currentLevel, boolean wasAdded) {
                        System.out.println("search current=" + current + ", currentLevel=" + currentLevel);
                        return true;
                    }
                });
        System.out.println("result=" + result);
    }

    private void deleteTest(File dir) {
        Set<File> result = FileUtilsKt.deleteFiles(dir, true, null, DEPTH_UNLIMITED, 0, new IDeleteNotifier() {
            @Override
            public boolean shouldProceed(@NotNull File current, @NotNull Set<? extends File> deleted, int currentLevel) {
                System.out.println("delete current=" + current + ", currentLevel=" + currentLevel);
                return true;
            }
        });
        System.out.println("result=" + result);
    }

    private void createTestFiles() {
        for (int i = 0; i < 5; i++) {
            File innerFile1 = createFileOrThrow(String.valueOf(i + 20), new File(sourceDir.getAbsolutePath(), sourceDir.getName() + i).getAbsolutePath());
            File innerFile2 = createFileOrThrow(String.valueOf(i + 30), innerFile1.getParent() + File.separator + "1");
            createFile(String.valueOf(i + 40), innerFile2.getParent() + File.separator + "2");
            File newFile = createFileOrThrow(String.valueOf(i), sourceDir.getAbsolutePath());
            writeBytesToFile(newFile, BigInteger.valueOf(i).toByteArray(), false);
            createDir(sourceDir.getAbsolutePath() + File.separator + sourceDir.getName() + i + "_empty");
        }
    }
}
