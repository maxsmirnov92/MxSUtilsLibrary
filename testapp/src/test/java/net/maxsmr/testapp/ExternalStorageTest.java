package net.maxsmr.testapp;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import static net.maxsmr.commonutils.media.MediaUtilsKt.getExternalFilesDirs;
import static net.maxsmr.commonutils.media.MediaUtilsKt.getFilteredExternalFilesDirs;

@RunWith(AndroidJUnit4.class)
public class ExternalStorageTest extends LoggerTest {

    @Test
    @Override
    public void test() {
        Set<File> dirs;
        dirs = getExternalFilesDirs(context);
        logger.d("external files dirs: " + dirs);
        dirs = getExternalFilesDirs(context, "1");
        logger.d("external files dirs with type: " + dirs);
        dirs = getFilteredExternalFilesDirs(context, true, true, false);
        logger.d("external files dirs filtered: " + dirs);
        dirs = getFilteredExternalFilesDirs(context,  true, true, false, "1");
        logger.d("external files dirs filtered with type: " + dirs);
        dirs = new LinkedHashSet<>(Arrays.asList(context.getObbDirs()));
        logger.d("obb dirs: " + dirs);
    }
}
