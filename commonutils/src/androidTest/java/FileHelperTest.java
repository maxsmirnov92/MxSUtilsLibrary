import android.content.Context;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import net.maxsmr.commonutils.data.CompareUtils;
import net.maxsmr.commonutils.data.FileHelper;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.Collections;
import java.util.Set;

import static android.support.test.InstrumentationRegistry.getTargetContext;

@RunWith(AndroidJUnit4.class)
public class FileHelperTest {

    @Test
    public void testGet() {
        Context context = InstrumentationRegistry.getTargetContext();
        Set<File> result = FileHelper.getFiles(Collections.singleton(context.getFilesDir()), FileHelper.GetMode.FOLDERS, null, new FileHelper.IGetNotifier() {
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

    //    @Test
    public void testSearch() {
        Set<File> result = FileHelper.searchByName("opencv", Collections.singleton(new File(System.getenv("HOME") + File.separator + "Documents" + File.separator + "AndroidStudioProjects" /*+ File.separator + "OpenCvDetectorExample-android"*/)),
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
                }, FileHelper.DEPTH_UNLIMITED);
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


}
