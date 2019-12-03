package net.maxsmr.testapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import net.maxsmr.commonutils.android.media.MetadataRetriever;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.Map;

@RunWith(AndroidJUnit4.class)
public class MetadataRetrieverTest extends LoggerTest {

    private Context context;

    @Before
    @Override
    public void prepare() {
        super.prepare();
        context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getContext();
    }
    
    @Test
    @Override
    public void test() {
        logger.d("registry context: " + context);
        logger.d("app dir: " + context.getFilesDir());
//        Map<Long, Bitmap> frames = MetadataRetriever.extractFrames(context, Uri.parse("file:///android_asset/video/1.mp4"), null, 5);
//        Map<Long, Bitmap> frames = null;
//
//        try {
//            FileDescriptor fd = context.getResources().getAssets().openFd("video/SampleVideo_1280x720_1mb.mp4").getFileDescriptor();
//            frames = MetadataRetriever.extractFrames(fd, 5);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        Map<Long, Bitmap> frames = MetadataRetriever.extractFrames(new File(Environment.getExternalStorageDirectory() + File.separator + "Download", "SampleVideo_1280x720_1mb.mp4"), 5);

        logger.i("frames: " + frames);
    }

}
