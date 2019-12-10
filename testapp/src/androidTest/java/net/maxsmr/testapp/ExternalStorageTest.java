package net.maxsmr.testapp;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import net.maxsmr.commonutils.data.FileHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
public class ExternalStorageTest extends LoggerTest {

    private Context context;

    @Before
    public void prepare() {
        super.prepare();
        context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getContext();
    }

    @Test
    @Override
    public void test() {
        Set<File> dirs;
        dirs = FileHelper.getExternalFilesDirs(context);
        logger.d("external files dirs: " + dirs);
        dirs = FileHelper.getExternalFilesDirs(context, "1");
        logger.d("external files dirs with type: " + dirs);
        dirs = FileHelper.getFilteredExternalFilesDirs(context, true, true, false);
        logger.d("external files dirs filtered: " + dirs);
        dirs = FileHelper.getFilteredExternalFilesDirs(context, "1", true, true, false);
        logger.d("external files dirs filtered with type: " + dirs);
        dirs = new LinkedHashSet<>(Arrays.asList(context.getObbDirs()));
        logger.d("obb dirs: " + dirs);
    }
}
