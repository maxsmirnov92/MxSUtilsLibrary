import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;

import net.maxsmr.commonutils.android.media.MetadataRetriever;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.Map;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class MetadataRetrieverTest {

    public static final String TAG = MetadataRetrieverTest.class.getSimpleName();

    @Test
    public void testExtractFrames() {
        Context context = InstrumentationRegistry.getContext();
        Log.d(TAG, "registry context: " + context);
        Log.d(TAG, "registry target context: " + InstrumentationRegistry.getTargetContext());
        Log.d(TAG, "app dir: " + context.getFilesDir());
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

        Log.i(TAG, "frames: " + frames);
    }

}
