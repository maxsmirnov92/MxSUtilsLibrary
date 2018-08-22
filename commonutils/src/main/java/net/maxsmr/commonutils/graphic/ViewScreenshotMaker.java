package net.maxsmr.commonutils.graphic;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.Window;

import net.maxsmr.commonutils.data.FileHelper;
import net.maxsmr.commonutils.logger.BaseLogger;
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder;

import java.io.File;
import java.io.FileOutputStream;

public final class ViewScreenshotMaker {

    private final static BaseLogger logger = BaseLoggerHolder.getInstance().getLogger(ViewScreenshotMaker.class);

    private ViewScreenshotMaker() {
    }

    @Nullable
    public static File makeScreenshot(String folderName, String fileName, @NonNull Bitmap.CompressFormat format, @NonNull Window window) {

        final File destFile = FileHelper.createNewFile(fileName, folderName);

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
