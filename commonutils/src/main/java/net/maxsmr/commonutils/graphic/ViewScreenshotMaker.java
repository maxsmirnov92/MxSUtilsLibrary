package net.maxsmr.commonutils.graphic;

import android.graphics.Bitmap;
import android.view.View;
import android.view.Window;

import net.maxsmr.commonutils.logger.BaseLogger;
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileOutputStream;

import static net.maxsmr.commonutils.data.FileUtilsKt.createFile;

public final class ViewScreenshotMaker {

    private final static BaseLogger logger = BaseLoggerHolder.getInstance().getLogger(ViewScreenshotMaker.class);

    private ViewScreenshotMaker() {
    }

    @Nullable
    public static File makeScreenshot(String folderName, String fileName, @NotNull Bitmap.CompressFormat format, @NotNull Window window) {

        final File destFile = createFile(fileName, folderName);

        if (destFile == null) {
            logger.e("can't create file: " + folderName + File.separator + fileName);
            return null;
        }

        try {
            // create bitmap screen capture
            View v1 = window.getDecorView().getRootView();
            v1.setDrawingCacheEnabled(true);
            Bitmap bitmap = Bitmap.createBitmap(v1.getDrawingCache());
            v1.setDrawingCacheEnabled(false);

            FileOutputStream outputStream = new FileOutputStream(destFile);
            int quality = 100;
            bitmap.compress(format, quality, outputStream);
            outputStream.flush();
            outputStream.close();

            return destFile;

        } catch (Throwable e) {
            logger.e("an Exception occurred", e);
        }

        return null;
    }
}
