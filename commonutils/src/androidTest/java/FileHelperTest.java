import android.content.Context;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.text.TextUtils;

import net.maxsmr.commonutils.data.CompareUtils;
import net.maxsmr.commonutils.data.FileHelper;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.Collections;
import java.util.Set;

import static android.R.attr.data;
import static android.support.test.InstrumentationRegistry.getTargetContext;

@RunWith(AndroidJUnit4.class)
public class FileHelperTest {

    //    @Test
    public void testGet() {
        Context context = InstrumentationRegistry.getTargetContext();
        Set<File> result = FileHelper.getFiles(Collections.singleton(context.getFilesDir() /*new File(Environment.getExternalStorageDirectory(), "Android/data/ru.gokidgo")*/), FileHelper.GetMode.ALL, null, new FileHelper.IGetNotifier() {
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

//    System.getenv("HOME") + File.separator + "Documents" + File.separator + "AndroidStudioProjects" /*+ File.separator + "OpenCvDetectorExample-android"*/

    //        @Test
    public void testSearch() {
        Context context = InstrumentationRegistry.getTargetContext();
        Set<File> result = FileHelper.searchByName("content", Collections.singleton(context.getFilesDir()),
                FileHelper.GetMode.ALL, CompareUtils.MatchStringOption.STARTS_WITH_IGNORE_CASE.flag, true,
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

    //    @Test
    public void testSearchFirst() {
        File result = FileHelper.searchByNameFirst("opencv", Collections.singleton(new File(System.getenv("HOME") + File.separator + "Documents" + File.separator + "AndroidStudioProjects")),
                FileHelper.GetMode.ALL, CompareUtils.MatchStringOption.STARTS_WITH_IGNORE_CASE.flag, true,
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

    //    @Test
    public void testSearchStat() {
        Set<File> result = FileHelper.searchByNameWithStat("opencv", Collections.singleton(new File(System.getenv("HOME") + File.separator + "Documents" + File.separator + "AndroidStudioProjects")),
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

    //    Collections.singleton(new File(context.getFilesDir(), "ru.altarix.ivory_v2/log/Ivory.log"))
//    @Test
    public void deleteTest() {
        Context context = InstrumentationRegistry.getTargetContext();
        Set<File> result = FileHelper.delete(Collections.singleton(context.getFilesDir()), true, null, null, new FileHelper.IDeleteNotifier() {
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
        }, FileHelper.DEPTH_UNLIMITED);
        System.out.println("result=" + result);
    }

    @Test
    public void sizeTest() {
        Context context = InstrumentationRegistry.getTargetContext();
        String result = FileHelper.getSizeWithValue(context, context.getFilesDir().getParentFile(), FileHelper.DEPTH_UNLIMITED);
        System.out.println("result=" + result);
    }


}
