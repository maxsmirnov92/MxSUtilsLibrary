import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import net.maxsmr.commonutils.data.CompareUtils;
import net.maxsmr.commonutils.data.FileHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.math.BigInteger;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class FileHelperTest extends LoggerTest {

    private Context context;

    private static final String SOURCE_NAME = "source";
    private static final String DEST_NAME = "dest";

    private static final String SEARCH_PREFIX = "qq";

    private File sourceDir;

    private File destinationDir;

    @Before
    public void prepare() {
        super.prepare();
        context = InstrumentationRegistry.getTargetContext();
        sourceDir = new File(context.getFilesDir(), SOURCE_NAME);
        destinationDir = new File(context.getFilesDir(), DEST_NAME);
        FileHelper.delete(destinationDir, true, null, null, new FileHelper.IDeleteNotifier() {
            @Override
            public boolean onProcessing(@NonNull File current, @NonNull Set<File> deleted, int currentLevel) {
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
        for (int i = 0; i < 5; i++) {
            File innerFile1 = FileHelper.createNewFile(String.valueOf(i + 20), new File(sourceDir.getAbsolutePath(), sourceDir.getName() + i).getAbsolutePath());
            File innerFile2 = FileHelper.createNewFile(String.valueOf(i + 30), innerFile1.getParent() + File.separator + "1");
            FileHelper.createNewFile(String.valueOf(i + 40), innerFile2.getParent() + File.separator + "2");
            File newFile = FileHelper.createNewFile(String.valueOf(i), sourceDir.getAbsolutePath());
            FileHelper.writeBytesToFile(newFile, BigInteger.valueOf(i).toByteArray(), false);
            FileHelper.createNewDir(sourceDir.getAbsolutePath() + File.separator + sourceDir.getName() + i + "_empty");
        }
    }

    @Override
    @Test
    public void test() {

    }

    @Test
    public void testGet() {
        Set<File> result = FileHelper.getFiles(sourceDir, FileHelper.GetMode.ALL, null, new FileHelper.IGetNotifier() {
            @Override
            public boolean onProcessing(@NonNull File current, @NonNull Set<File> collected, int currentLevel) {
                System.out.println("current=" + current + ", currentLevel=" + currentLevel);
                return true;
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
        System.out.println("result=" + result);
    }

    @Test
    public void testSearch() {
        Set<File> result = FileHelper.searchByName(SEARCH_PREFIX, sourceDir,
                FileHelper.GetMode.ALL, CompareUtils.MatchStringOption.STARTS_WITH_IGNORE_CASE.flag,
                new FileHelper.FileComparator(Collections.singletonMap(FileHelper.FileComparator.SortOption.LAST_MODIFIED, false)),
                new FileHelper.IGetNotifier() {

                    @Override
                    public boolean onProcessing(@NonNull File current, @NonNull Set<File> collected, int currentLevel) {
                        System.out.println("current=" + current + ", currentLevel=" + currentLevel);
                        return true;
                    }

                    @Override
                    public boolean onGetFile(@NonNull File file) {
                        return true;
                    }

                    @Override
                    public boolean onGetFolder(@NonNull File folder) {
                        return true;
                    }
                }, 1);
        System.out.println("result=" + result);
    }

    @Test
    public void testSearchFirst() {
        File result = FileHelper.searchByNameFirst(SEARCH_PREFIX, sourceDir,
                FileHelper.GetMode.ALL, CompareUtils.MatchStringOption.STARTS_WITH_IGNORE_CASE.flag,
                null,
                new FileHelper.IGetNotifier() {

                    @Override
                    public boolean onProcessing(@NonNull File current, @NonNull Set<File> collected, int currentLevel) {
                        System.out.println("current=" + current + ", currentLevel=" + currentLevel);
                        return true;
                    }

                    @Override
                    public boolean onGetFile(@NonNull File file) {
                        return false;
                    }

                    @Override
                    public boolean onGetFolder(@NonNull File folder) {
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
                    public boolean onProcessing(@NonNull File current, @NonNull Set<File> collected, int currentLevel) {
                        System.out.println("current=" + current + ", currentLevel=" + currentLevel);
                        return true;
                    }

                    @Override
                    public boolean onGetFile(@NonNull File file) {
                        return true;
                    }

                    @Override
                    public boolean onGetFolder(@NonNull File folder) {
                        return true;
                    }
                });
        System.out.println("result=" + result);
    }

    @Test
    public void testCopy() {
        FileHelper.copyFilesWithBuffering(sourceDir, destinationDir, null,
                new FileHelper.ISingleCopyNotifier() {
                    @Override
                    public long notifyInterval() {
                        return TimeUnit.SECONDS.toMillis(1);
                    }

                    @Override
                    public boolean onProcessing(@NonNull File sourceFile, @NonNull File destFile, long bytesCopied, long bytesTotal) {
                        return true;
                    }
                },
                new FileHelper.IMultipleCopyNotifier() {
                    @Override
                    public boolean onCalculatingSize(@NonNull File current, @NonNull Set<File> collected, int currentLevel) {
                        return true;
                    }

                    @Override
                    public boolean onProcessing(@NonNull File currentFile, @NonNull File destDir, @NonNull Set<File> copied, long filesTotal, int currentLevel) {
                        return true;
                    }

                    @Override
                    public boolean confirmCopy(@NonNull File currentFile, @NonNull File destDir, int currentLevel) {
                        return true;
                    }

                    @Override
                    public File onBeforeCopy(@NonNull File currentFile, @NonNull File destDir, int currentLevel) {
                        return null;
                    }

                    @Override
                    public boolean onExists(@NonNull File destFile, int currentLevel) {
                        return true;
                    }

                    @Override
                    public void onFailed(@Nullable File currentFile, @NonNull File destFile, int currentLevel) {

                    }

                }, true, FileHelper.DEPTH_UNLIMITED);
    }

    //    @Test
    public void deleteTest() {
        Set<File> result = FileHelper.delete(context.getFilesDir(), true, null, null, new FileHelper.IDeleteNotifier() {
            @Override
            public boolean onProcessing(@NonNull File current, @NonNull Set<File> deleted, int currentLevel) {
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


}
