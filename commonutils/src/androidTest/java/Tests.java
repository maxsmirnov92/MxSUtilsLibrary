import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;

import net.maxsmr.commonutils.android.hardware.DeviceUtils;
import net.maxsmr.commonutils.android.media.MetadataRetriever;
import net.maxsmr.commonutils.data.FileHelper;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class Tests {

    public static final String TAG = Tests.class.getSimpleName();

    @Test
    public void testExtractFrames() {
        Context context = InstrumentationRegistry.getContext();
        Log.d(TAG, "registry context: " + context);
        Log.d(TAG, "registry target context: " + InstrumentationRegistry.getTargetContext());
        Log.d(TAG, "app dir: " + context.getFilesDir());
        boolean b1 = DeviceUtils.checkPermission(context, "android.permission.READ_EXTERNAL_STORAGE");
        boolean b2 = DeviceUtils.checkPermission(context, "android.permission.WRITE_EXTERNAL_STORAGE");
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

//    @Test
    public void testGetFiles() {
        FileHelper.getFiles(Collections.singleton(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)), FileHelper.GetMode.ALL, true, null, new FileHelper.IGetNotifier() {
            @Override
            public boolean onProcessing(@NonNull File current, @NonNull Set<File> collected) {
                Log.d(TAG, "current: " + current);
                return true;
            }

            @Override
            public boolean onGet(@NonNull File file) {
                return true;
            }
        });
    }
}
