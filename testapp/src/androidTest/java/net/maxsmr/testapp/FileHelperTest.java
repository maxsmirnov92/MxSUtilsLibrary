package net.maxsmr.testapp;

import android.Manifest;
import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.GrantPermissionRule;

import net.maxsmr.commonutils.data.FileHelper;
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

import static org.junit.Assert.assertNotNull;

@RunWith(AndroidJUnit4.class)
public class FileHelperTest extends LoggerTest {

    @Rule
    public GrantPermissionRule readPermissionRule = GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE);
    @Rule
    public GrantPermissionRule writePermissionRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE);

    private Context context;

    private static final String SOURCE_NAME = "source";
    private static final String DEST_NAME = "dest";

    private static final String SEARCH_PREFIX = "qq";

    private File sourceDir;

    private File destinationDir;

    @Before
    public void prepare() {
        super.prepare();
        context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getContext();
        FileHelper.createNewDir("/data/data/" + context.getPackageName() + "/files");
        final File filesDir = context.getFilesDir();
        assertNotNull(filesDir);
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
        Set<File> result = FileHelper.getFilteredExternalFilesDirs(context, false, true, true);
        System.out.println("filteredExternalFilesDirs: " + result);
        Assert.assertFalse(result.isEmpty());
    }

    @Test
    public void testGet() {
        Set<File> result = FileHelper.getFiles(sourceDir, FileHelper.GetMode.ALL, null, new FileHelper.IGetNotifier() {
            @Override
            public boolean onProcessing(@NotNull File current, @NotNull Set<File> collected, int currentLevel) {
                System.out.println("current=" + current + ", currentLevel=" + currentLevel);
                return true;
            }

            @Override
            public boolean onGetFile(@NotNull File file) {
                return true;
            }

            @Override
            public boolean onGetFolder(@NotNull File folder) {
                return true;
            }
        }, FileHelper.DEPTH_UNLIMITED);
        System.out.println("result=" + result);
    }

    @Test
    public void testSearch() {
        Set<File> result = FileHelper.searchByName(SEARCH_PREFIX, sourceDir,
                FileHelper.GetMode.ALL, MatchStringOption.STARTS_WITH_IGNORE_CASE.getFlag(),
                new FileHelper.FileComparator(Collections.singletonMap(FileHelper.FileComparator.SortOption.LAST_MODIFIED, false)),
                new FileHelper.IGetNotifier() {

                    @Override
                    public boolean onProcessing(@NotNull File current, @NotNull Set<File> collected, int currentLevel) {
                        System.out.println("current=" + current + ", currentLevel=" + currentLevel);
                        return true;
                    }

                    @Override
                    public boolean onGetFile(@NotNull File file) {
                        return true;
                    }

                    @Override
                    public boolean onGetFolder(@NotNull File folder) {
                        return true;
                    }
                }, 1);
        System.out.println("result=" + result);
    }

    @Test
    public void testSearchFirst() {
        File result = FileHelper.searchByNameFirst(SEARCH_PREFIX, sourceDir,
                FileHelper.GetMode.ALL, MatchStringOption.STARTS_WITH_IGNORE_CASE.getFlag(),
                null,
                new FileHelper.IGetNotifier() {

                    @Override
                    public boolean onProcessing(@NotNull File current, @NotNull Set<File> collected, int currentLevel) {
                        System.out.println("current=" + current + ", currentLevel=" + currentLevel);
                        return true;
                    }

                    @Override
                    public boolean onGetFile(@NotNull File file) {
                        return false;
                    }

                    @Override
                    public boolean onGetFolder(@NotNull File folder) {
                        return false;
                    }
                }, FileHelper.DEPTH_UNLIMITED);
        System.out.println("result=" + result);
    }

    @Test
    public void testSearchStat() {
        Set<File> result = FileHelper.searchByNameWithStat(SEARCH_PREFIX, Collections.singleton(sourceDir),
                new FileHelper.FileComparator(Collections.singletonMap(FileHelper.FileComparator.SortOption.NAME, true)),

                new FileHelper.IGetNotifier() {

                    @Override
                    public boolean onProcessing(@NotNull File current, @NotNull Set<File> collected, int currentLevel) {
                        System.out.println("current=" + current + ", currentLevel=" + currentLevel);
                        return true;
                    }

                    @Override
                    public boolean onGetFile(@NotNull File file) {
                        return true;
                    }

                    @Override
                    public boolean onGetFolder(@NotNull File folder) {
                        return true;
                    }
                });
        System.out.println("result=" + result);
    }

    @Test
    public void testCopy() {
        FileHelper.copyFilesWithBuffering2(
                sourceDir,
                destinationDir,
                null,
                new FileHelper.ISingleCopyNotifier() {
                    @Override
                    public long notifyInterval() {
                        return TimeUnit.SECONDS.toMillis(1);
                    }

                    @Override
                    public boolean onProcessing(@NotNull File sourceFile, @NotNull File destFile, long bytesCopied, long bytesTotal) {
                        return true;
                    }
                },
                new FileHelper.IMultipleCopyNotifier2() {

                    @Override
                    public boolean onCalculatingSize(File current, Set<File> collected) {
                        return true;
                    }

                    @Override
                    public boolean onProcessing(File currentFile, File destDir, Set<File> copied, long filesProcessed, long filesTotal) {
                        return true;
                    }

                    @Override
                    public boolean confirmCopy(File currentFile, File destDir) {
                        return true;
                    }

                    @Override
                    public File onBeforeCopy(File currentFile, File destDir) {
                        return null;
                    }

                    @Override
                    public boolean onExists(File destFile) {
                        return true;
                    }

                    @Override
                    public void onSucceeded(File currentFile, File resultFile) {

                    }

                    @Override
                    public void onFailed(File currentFile, File destDir) {

                    }

                },
                true,
                FileHelper.DEPTH_UNLIMITED,
                Collections.emptyList());
    }

    //    @Test
    public void deleteTest() {
        Set<File> result = FileHelper.delete(context.getFilesDir(), true, null, null, new FileHelper.IDeleteNotifier() {
            @Override
            public boolean onProcessing(@NotNull File current, @NotNull Set<File> deleted, int currentLevel) {
                return true;
            }

            @Override
            public boolean confirmDeleteFile(File file) {
                return true;
            }

            @Override
            public boolean confirmDeleteFolder(File folder) {
                return true;
            }

            @Override
            public void onDeleteFileFailed(File file) {

            }

            @Override
            public void onDeleteFolderFailed(File folder) {

            }
        }, FileHelper.DEPTH_UNLIMITED);
        System.out.println("result=" + result);
    }

    private void deleteFromDest() {
        FileHelper.delete(destinationDir, true, null, null, new FileHelper.IDeleteNotifier() {
            @Override
            public boolean onProcessing(@NotNull File current, @NotNull Set<File> deleted, int currentLevel) {
                return true;
            }

            @Override
            public boolean confirmDeleteFile(File file) {
                return true;
            }

            @Override
            public boolean confirmDeleteFolder(File folder) {
                return true;
            }

            @Override
            public void onDeleteFileFailed(File file) {

            }

            @Override
            public void onDeleteFolderFailed(File folder) {

            }
        }, FileHelper.DEPTH_UNLIMITED);
    }

    private void createTestFiles() {
        for (int i = 0; i < 5; i++) {
            File innerFile1 = FileHelper.createNewFile(String.valueOf(i + 20), new File(sourceDir.getAbsolutePath(), sourceDir.getName() + i).getAbsolutePath());
            File innerFile2 = FileHelper.createNewFile(String.valueOf(i + 30), innerFile1.getParent() + File.separator + "1");
            FileHelper.createNewFile(String.valueOf(i + 40), innerFile2.getParent() + File.separator + "2");
            File newFile = FileHelper.createNewFile(String.valueOf(i), sourceDir.getAbsolutePath());
            FileHelper.writeBytesToFile(newFile, BigInteger.valueOf(i).toByteArray(), false);
            FileHelper.createNewDir(sourceDir.getAbsolutePath() + File.separator + sourceDir.getName() + i + "_empty");
        }
    }
}
