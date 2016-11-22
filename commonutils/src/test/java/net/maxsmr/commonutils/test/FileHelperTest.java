package net.maxsmr.commonutils.test;

import android.support.annotation.NonNull;

import net.maxsmr.commonutils.data.CompareUtils;
import net.maxsmr.commonutils.data.FileHelper;

import java.io.File;
import java.util.Collections;
import java.util.Set;

public class FileHelperTest {

    //    @Test
    public void testGet() {
        Set<File> result = FileHelper.getFiles(Collections.singleton(new File(System.getenv("HOME"))), FileHelper.GetMode.FOLDERS, false, null, new FileHelper.IGetNotifier() {
            @Override
            public boolean onProcessing(@NonNull File current, @NonNull Set<File> collected) {
                System.out.println("current=" + current);
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

    //    @Test
    public void testSearch() {
        Set<File> result = FileHelper.searchByName("opencv", Collections.singleton(new File(System.getenv("HOME") + File.separator + "Documents" + File.separator + "AndroidStudioProjects" /*+ File.separator + "OpenCvDetectorExample-android"*/)),
                FileHelper.GetMode.ALL, CompareUtils.MatchStringOption.STARTS_WITH_IGNORE_CASE.flag, true, true,
                new FileHelper.FileComparator(Collections.singletonMap(FileHelper.FileComparator.SortOption.LAST_MODIFIED, false)),
                new FileHelper.ISearchNotifier() {
                    @Override
                    public boolean onProcessing(@NonNull File current, @NonNull Set<File> found) {
                        System.out.println("current=" + current + ", found=" + found);
                        return true;
                    }

                    @Override
                    public boolean onFoundFile(@NonNull File file) {
                        return true;
                    }

                    @Override
                    public boolean onFoundFolder(@NonNull File folder) {
                        return true;
                    }
                });
        System.out.println("result=" + result);
    }

    //    @Test
    public void testSearchFirst() {
        File result = FileHelper.searchByNameFirst("opencv", Collections.singleton(new File(System.getenv("HOME") + File.separator + "Documents" + File.separator + "AndroidStudioProjects")),
                FileHelper.GetMode.ALL, CompareUtils.MatchStringOption.STARTS_WITH_IGNORE_CASE.flag, true, true,
                null,
                new FileHelper.ISearchNotifier() {
                    @Override
                    public boolean onProcessing(@NonNull File current, @NonNull Set<File> found) {
                        System.out.println("current=" + current + ", found=" + found);
                        return true;
                    }

                    @Override
                    public boolean onFoundFile(@NonNull File File) {
                        return true;
                    }

                    @Override
                    public boolean onFoundFolder(@NonNull File folder) {
                        return true;
                    }
                });
        System.out.println("result=" + result);
    }

    //    @Test
    public void testSearchStat() {
        Set<File> result = FileHelper.searchByNameWithStat("opencv", Collections.singleton(new File(System.getenv("HOME") + File.separator + "Documents" + File.separator + "AndroidStudioProjects")),
                new FileHelper.FileComparator(Collections.singletonMap(FileHelper.FileComparator.SortOption.NAME, true)),
                new FileHelper.ISearchNotifier() {
                    @Override
                    public boolean onProcessing(@NonNull File current, @NonNull Set<File> found) {
                        System.out.println("current=" + current + ", found=" + found);
                        return true;
                    }

                    @Override
                    public boolean onFoundFile(@NonNull File File) {
                        return true;
                    }

                    @Override
                    public boolean onFoundFolder(@NonNull File folder) {
                        return true;
                    }
                });
        System.out.println("result=" + result);
    }


}
