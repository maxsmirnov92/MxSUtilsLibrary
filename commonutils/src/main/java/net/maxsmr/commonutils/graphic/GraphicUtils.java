package net.maxsmr.commonutils.graphic;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.XmlResourceParser;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.YuvImage;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import android.text.TextUtils;
import android.util.TypedValue;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.XmlRes;
import androidx.exifinterface.media.ExifInterface;
import androidx.palette.graphics.Palette;

import net.maxsmr.commonutils.android.gui.GuiUtils;
import net.maxsmr.commonutils.android.media.MetadataRetriever;
import net.maxsmr.commonutils.data.FileHelper;
import net.maxsmr.commonutils.logger.BaseLogger;
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static androidx.exifinterface.media.ExifInterface.TAG_DATETIME;
import static androidx.exifinterface.media.ExifInterface.TAG_EXPOSURE_TIME;
import static androidx.exifinterface.media.ExifInterface.TAG_FLASH;
import static androidx.exifinterface.media.ExifInterface.TAG_FOCAL_LENGTH;
import static androidx.exifinterface.media.ExifInterface.TAG_GPS_ALTITUDE;
import static androidx.exifinterface.media.ExifInterface.TAG_GPS_ALTITUDE_REF;
import static androidx.exifinterface.media.ExifInterface.TAG_GPS_DATESTAMP;
import static androidx.exifinterface.media.ExifInterface.TAG_GPS_LATITUDE;
import static androidx.exifinterface.media.ExifInterface.TAG_GPS_LATITUDE_REF;
import static androidx.exifinterface.media.ExifInterface.TAG_GPS_LONGITUDE;
import static androidx.exifinterface.media.ExifInterface.TAG_GPS_LONGITUDE_REF;
import static androidx.exifinterface.media.ExifInterface.TAG_GPS_PROCESSING_METHOD;
import static androidx.exifinterface.media.ExifInterface.TAG_GPS_TIMESTAMP;
import static androidx.exifinterface.media.ExifInterface.TAG_MAKE;
import static androidx.exifinterface.media.ExifInterface.TAG_MODEL;
import static androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION;
import static androidx.exifinterface.media.ExifInterface.TAG_WHITE_BALANCE;

public final class GraphicUtils {

    private final static BaseLogger logger = BaseLoggerHolder.getInstance().getLogger(GraphicUtils.class);

    private GraphicUtils() {
        throw new AssertionError("no instances.");
    }

    @Nullable
    public static String getFileExtByCompressFormat(Bitmap.CompressFormat compressFormat) {
        if (compressFormat == null) {
            logger.e("compressFormat is null");
            return null;
        }
        switch (compressFormat) {
            case PNG:
                return "png";
            case JPEG:
                return "jpg";
            case WEBP:
                return "webp";
            default:
                return null;
        }
    }

    public static Bitmap writeTextOnBitmap(Bitmap bitmap, String text, int textColor) {
        return writeTextOnBitmap(bitmap, text, textColor, 0, null);
    }

    public static Bitmap writeTextOnBitmap(Bitmap bitmap, String text, @ColorInt int textColor, int fontSize, Point textPos) {

        if (!isBitmapCorrect(bitmap)) {
            logger.e("incorrect bitmap");
            return bitmap;
        }

        if (!bitmap.isMutable()) {
            logger.e("bitmap is immutable, cannot pass to canvas");
            return bitmap;
        }

        if (TextUtils.isEmpty(text)) {
            logger.e("text is null or empty");
            return bitmap;
        }

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(textColor);
        paint.setTypeface(Typeface.create("", Typeface.BOLD));
        paint.setTextAlign(Paint.Align.CENTER);

        Canvas canvas = new Canvas(bitmap);

        paint.setTextSize(fixFontSize(fontSize, text, paint, bitmap));

        final int xPos;
        final int yPos;

        if (!(textPos != null && textPos.x >= 0 && textPos.y >= 0)) {
            xPos = (int) (canvas.getWidth() * 0.05);
            yPos = canvas.getHeight() - (int) (canvas.getHeight() * 0.01);
        } else {
            xPos = textPos.x;
            yPos = textPos.y;
        }

        canvas.drawText(text, xPos, yPos, paint);
        return bitmap;
    }

    public static int fixFontSize(int fontSize, String text, Paint paint, Bitmap bitmap) {

        if (!isBitmapCorrect(bitmap)) {
            logger.e("incorrect bitmap");
            return fontSize;
        }

        if (paint == null) {
            logger.e("paint is null");
            return fontSize;
        }

        if (TextUtils.isEmpty(text)) {
            logger.e("text is empty");
            return fontSize;
        }

        Rect textRect = new Rect();
        paint.getTextBounds(text, 0, text.length(), textRect);

        if (textRect.width() >= (new Canvas(bitmap).getWidth() - 4)) {
            return fontSize <= 0 ? (int) (bitmap.getWidth() * 0.01) / 2 : fontSize / 2;
        } else {
            return fontSize <= 0 ? (int) (bitmap.getWidth() * 0.01) : fontSize;
        }
    }

    public static Bitmap makePreviewFromVideoFile(File videoFile, int gridSize, boolean writeDuration) {
        logger.d("makePreviewFromVideoFile, videoFile=" + videoFile + ", gridSize=" + gridSize);

        if (canDecodeVideo(videoFile)) {
            logger.e("incorrect video file");
            return null;
        }

        if (gridSize <= 0) {
            logger.e("incorrect grid size");
            return null;
        }

        final Map<Long, Bitmap> videoFrames = MetadataRetriever.extractFrames(videoFile, gridSize * gridSize);
        if (videoFrames.isEmpty()) {
            logger.e("videoFrames is empty");
            return null;
        }

        final Bitmap resultImage = combineImagesToOne(videoFrames.values(), gridSize, true);

        if (writeDuration) {
            return writeTextOnBitmap(resultImage, "duration: " + MetadataRetriever.extractMediaDuration(videoFile) + " ms", Color.WHITE);
        }

        return resultImage;
    }

    /**
     * @return null if failed, otherwise same file or same file with changed extension
     */
    public static File compressBitmapToFile(File file, Bitmap data, Bitmap.CompressFormat format, int quality) {

        if (file == null) {
            logger.e("file not specified");
            return null;
        }

        if (!isBitmapCorrect(data)) {
            logger.e("bitmap is incorrect");
            return null;
        }

        if (quality <= 0) {
            logger.e("Incorrect quality: $quality");
            return null;
        }

        String ext = getFileExtByCompressFormat(format);

        if (TextUtils.isEmpty(ext)) {
            logger.e("unknown format: " + format);
            ext = FileHelper.getFileExtension(file.getName());
        }

        file = FileHelper.createNewFile(FileHelper.removeExtension(file.getName()) + "." + ext, file.getParent());

        if (file == null) {
            logger.e("file was not created");
            return null;
        }

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            data.compress(format, quality, fos);
            fos.flush();
            return file;
        } catch (IOException e) {
            logger.e("an IOException occurred", e);
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                logger.e("an IOException occurred during close", e);
            }
        }

        return null;
    }

    @Nullable
    public static byte[] compressBitmapToByteArray(@Nullable Bitmap data, @NotNull Bitmap.CompressFormat format, int quality) {

        if (!isBitmapCorrect(data)) {
            logger.e("bitmap is incorrect");
            return null;
        }

        if (quality <= 0) {
            logger.e("Incorrect quality: $quality");
            return null;
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            data.compress(format, quality, baos);
            baos.flush();
            try {
                return baos.toByteArray();
            } catch (OutOfMemoryError e) {
                logger.e("an OutOfMemoryError occurred during toByteArray()", e);
            }
        } catch (IOException e) {
            logger.e("an IOException occurred", e);
        } finally {
            try {
                baos.close();
            } catch (IOException e) {
                logger.e("an IOException occurred during close", e);
            }
        }

        return null;
    }

    @Nullable
    public static File compressImage(@Nullable File imageFile, @Nullable File compressedImageFile, long maxSize, int maxRetries, int qualityDecrementStep) {
        return compressImage(imageFile, compressedImageFile, maxSize, maxRetries, qualityDecrementStep, Bitmap.Config.ARGB_8888, Bitmap.CompressFormat.JPEG);
    }

    @Nullable
    public static File compressImage(@Nullable File imageFile, @Nullable File compressedImageFile, long maxSize, int maxRetries, int qualityDecrementStep,
                                     @NotNull Bitmap.Config config, @NotNull Bitmap.CompressFormat format) {

        if (maxSize < 0) {
            logger.e("Incorrect maxSize: " + maxSize);
            return null;
        }

        if (maxRetries < 0) {
            logger.e("Incorrect maxRetries: maxRetries" + maxRetries);
            return null;
        }

        if (qualityDecrementStep <= 0) {
            logger.e("Incorrect qualityDecrementStep: " + qualityDecrementStep);
            return null;
        }

        boolean result = imageFile != null && GraphicUtils.canDecodeImage(imageFile);

        if (result && maxSize > 0) {

            long currentLength = imageFile.length();

            result = currentLength > 0 && currentLength <= maxSize;

            if (!result && currentLength > 0 && maxRetries > 0) {

                if (FileHelper.checkFileNoThrow(compressedImageFile)) {

                    final Bitmap bm = createBitmapFromFile(imageFile, 1, config);

                    if (bm != null) {

                        FileHelper.deleteFile(compressedImageFile);

                        int currentTry = 0;
                        int currentQuality = 100;

                        while (currentLength > maxSize && currentTry <= maxRetries && currentQuality > 0) {

                            compressedImageFile = compressBitmapToFile(compressedImageFile, bm, format, currentQuality);
                            currentLength = compressedImageFile != null ? compressedImageFile.length() : 0;

                            currentTry++;
                            currentQuality = 100 - currentTry * qualityDecrementStep;
                        }

                        result = currentLength > 0 && currentLength <= maxSize;
                    }
                }
            }
        }

        if (result) {
            return FileHelper.isFileCorrect(compressedImageFile) ? compressedImageFile : imageFile;
        } else {
            return imageFile;
        }
    }


    /**
     * @param gridSize number of width or height chunks in result image
     */
    public static Bitmap combineImagesToOne(Collection<Bitmap> chunkImages, int gridSize, boolean recycle) {

        if (chunkImages == null || chunkImages.size() == 0) {
            logger.e("chunkImages is null or empty");
            return null;
        }

        if (gridSize <= 0) {
            logger.e("incorrect gridSize: " + gridSize);
            return null;
        }

        List<Bitmap> chunkImagesList = new ArrayList<>(chunkImages);

        if (gridSize * gridSize < chunkImagesList.size()) {
            logger.w("grid dimension is less than number of chunks, removing excessive chunks...");
            for (int i = chunkImagesList.size() - 1; i > gridSize * gridSize - 1; i--) {
                Bitmap b = chunkImagesList.remove(i);
                if (recycle && b != null) {
                    b.recycle();
                }
                i = chunkImagesList.size() - 1;
            }
        }

        int chunkWidth = 0;
        int chunkHeight = 0;

        for (int i = 0; i < chunkImages.size(); i++) {

            Bitmap chunk = chunkImagesList.get(i);

            if (chunk != null) {

                if (chunkWidth > 0 && chunkHeight > 0) {

                    if (chunk.getWidth() != chunkWidth || chunk.getHeight() != chunkHeight) {
                        logger.e("chunk images in list have different dimensions, previous: " + chunkWidth + "x" + chunkHeight
                                + ", current: " + chunk.getWidth() + "x" + chunk.getHeight());
                        return null;
                    }

                } else {
                    chunkWidth = chunk.getWidth();
                    chunkHeight = chunk.getHeight();
                }

            } else {
                logger.e("chunk at index " + i + " is null");
                chunkImagesList.remove(i);
                i = 0;
            }
        }

        logger.d("chunk: " + chunkWidth + " x " + chunkHeight);

        if (chunkWidth <= 0 || chunkHeight <= 0) {
            logger.e("incorrect chunk dimensions");
            return null;
        }

        // create a bitmap of a size which can hold the complete image after merging
        Bitmap resultBitmap;

        try {
            resultBitmap = Bitmap.createBitmap(chunkWidth * gridSize, chunkHeight * gridSize, Bitmap.Config.ARGB_8888);
        } catch (OutOfMemoryError e) {
            logger.e("an OutOfMemoryError error occurred during createBitmap()", e);
            return null;
        }

        // create a canvas for drawing all those small images
        Canvas canvas = new Canvas(resultBitmap);
        int counter = 0;
        for (int rows = 0; rows < gridSize; rows++) {
            for (int cols = 0; cols < gridSize; cols++) {
                if (counter >= chunkImagesList.size()) {
                    continue;
                }
                Bitmap image = chunkImagesList.get(counter);
                if (image != null) {
                    canvas.drawBitmap(image, chunkWidth * cols, chunkHeight * rows, null);
                    if (recycle) {
                        image.recycle();
                    }
                }
                counter++;
            }
        }

        return resultBitmap;
    }

    /**
     * Converts pixel value to dp value
     */
    public static int pxToDp(int px, @NotNull Context context) {
        return (int) ((float) px / context.getResources().getDisplayMetrics().density);
    }

    public static int dpToPx(int dp, @NotNull Context context) {
        // OR simply px = dp * density
        if (dp <= 0) {
            return 0;
        }
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }

    public static Bitmap cropBitmap(Bitmap srcBitmap, int fromX, int fromY, int toX, int toY) {

        if (!isBitmapCorrect(srcBitmap)) {
            logger.e("incorrect bitmap");
            return srcBitmap;
        }

        if (fromX < 0 || fromY < 0 || toX <= 0 || toY <= 0) {
            logger.e("incorrect coords (1)");
            return srcBitmap;
        }

        if (toX <= fromX || toY <= fromY) {
            logger.e("incorrect coords (2)");
            return srcBitmap;
        }

        final int rectWidth = toX - fromX;
        final int rectHeight = toY - fromY;

        Bitmap bmOverlay = Bitmap.createBitmap(rectWidth, rectHeight, Bitmap.Config.RGB_565);

        Paint p = new Paint();
        p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        Canvas c = new Canvas(bmOverlay);
        c.drawBitmap(srcBitmap, 0, 0, null);
        c.drawRect(fromX, fromY, toX, toY, p);

        return bmOverlay;
    }

    private static int calculateInSampleSize(
            BitmapFactory.Options options,
            int reqWidth,
            int reqHeight
    ) {

        if (reqWidth <= 0 || reqHeight <= 0) {
            return 0;
        }

        if (options == null) {
            return 0;
        }

        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            // Calculate ratios of height and width to requested height and width
            final int heightRatio = Math.round((float) height / reqHeight);
            final int widthRatio = Math.round((float) width / reqWidth);

            // Choose the smallest ratio as inSampleSize value, this will guarantee
            // a final image with both dimensions larger than or equal to the
            // requested height and width.
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }
        return inSampleSize;
    }

    private static int calculateInSampleSizeHalf(
            BitmapFactory.Options options,
            int reqWidth,
            int reqHeight) {

        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while (halfHeight / inSampleSize > reqHeight && halfWidth / inSampleSize > reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    @Nullable
    public static Bitmap createBitmapFromUri(@NotNull Context context, Uri uri, int scale) {
        return createBitmapFromUri(context, uri, scale, null);
    }

    @Nullable
    public static Bitmap createBitmapFromUri(@NotNull Context context, Uri uri, int scale, @Nullable Bitmap.Config config) {

        if (!(uri != null && (uri.getScheme() == null || uri.getScheme().equalsIgnoreCase(ContentResolver.SCHEME_FILE)))) {
            logger.e("incorrect resource uri: " + uri);
            return null;
        }

        try {
            return createBitmapFromStream((context.getContentResolver().openInputStream(uri)), scale, config);
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    @Nullable
    public static Bitmap createBitmapFromFile(File file) {
        return createBitmapFromFile(file, 1, null);
    }

    @Nullable
    public static Bitmap createBitmapFromFile(File file, int scale, @Nullable Bitmap.Config config) {

        if (!canDecodeImage(file)) {
            logger.e("incorrect file: " + file);
            return null;
        }

        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), options);

        final int width = scale > 1 ? options.outWidth / scale : options.outWidth;
        final int height = scale > 1 ? options.outHeight / scale : options.outHeight;

        options.inSampleSize = calculateInSampleSizeHalf(options, width, height);
        options.inPurgeable = true;
        options.inInputShareable = true;
        options.inJustDecodeBounds = false;
        options.inPreferredConfig = config == null ? Bitmap.Config.ARGB_8888 : config;

        try {
            return BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        } catch (OutOfMemoryError e) {
            logger.e("an OutOfMemoryError error occurred during decodeFile()", e);
            return null;
        }
    }

    @Nullable
    public static Bitmap createBitmapFromByteArray(byte[] data) {
        return createBitmapFromByteArray(data, 1, null);
    }

    @Nullable
    public static Bitmap createBitmapFromByteArray(byte[] data, int scale, @Nullable Bitmap.Config config) {

        if (data == null || data.length == 0) {
            logger.e("data is null or empty");
            return null;
        }

        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, options);

        final int width = scale > 1 ? options.outWidth / scale : options.outWidth;
        final int height = scale > 1 ? options.outHeight / scale : options.outHeight;

        options.inSampleSize = calculateInSampleSizeHalf(options, width, height);
        options.inPurgeable = true;
        options.inInputShareable = true;
        options.inJustDecodeBounds = false;
        options.inPreferredConfig = config == null ? Bitmap.Config.ARGB_8888 : config;

        try {
            return BitmapFactory.decodeByteArray(data, 0, data.length, options);
        } catch (OutOfMemoryError e) {
            logger.e("an OutOfMemoryError error occurred during decodeByteArray()", e);
            return null;
        }
    }

    @Nullable
    public static Bitmap createBitmapFromStream(InputStream is) {
        return createBitmapFromStream(is, 1, null);
    }

    @Nullable
    public static Bitmap createBitmapFromStream(InputStream is, int scale, @Nullable Bitmap.Config config) {

        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(is, null, options);

        final int width = scale > 1 ? options.outWidth / scale : options.outWidth;
        final int height = scale > 1 ? options.outHeight / scale : options.outHeight;

        options.inSampleSize = calculateInSampleSizeHalf(options, width, height);
        options.inPurgeable = true;
        options.inInputShareable = true;
        options.inJustDecodeBounds = false;
        options.inPreferredConfig = config == null ? Bitmap.Config.ARGB_8888 : config;

        try {
            return BitmapFactory.decodeStream(is, null, options);
        } catch (OutOfMemoryError e) {
            logger.e("an OutOfMemoryError error occurred during decodeStream()", e);
            return null;
        }
    }

    @Nullable
    public static Bitmap createBitmapFromResource(@NotNull Context context, @DrawableRes int resId) {
        return createBitmapFromResource(context, resId, 1, null);
    }

    @Nullable
    public static Bitmap createBitmapFromResource(@NotNull Context context, @DrawableRes int resId, int scale, @Nullable Bitmap.Config config) {

        if (resId == 0) {
            logger.e("resId is not specified");
            return null;
        }

        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(context.getResources(), resId, options);

        final int width = scale > 1 ? options.outWidth / scale : options.outWidth;
        final int height = scale > 1 ? options.outHeight / scale : options.outHeight;

        options.inSampleSize = calculateInSampleSizeHalf(options, width, height);
        options.inPurgeable = true;
        options.inInputShareable = true;
        options.inJustDecodeBounds = false;
        options.inPreferredConfig = config == null ? Bitmap.Config.ARGB_8888 : config;

        try {
            return BitmapFactory.decodeResource(context.getResources(), resId, options);
        } catch (OutOfMemoryError e) {
            logger.e("an OutOfMemoryError error occurred during decodeResource()", e);
            return null;
        }
    }


    public static Bitmap createScaledBitmap(Bitmap bm, int newWidth) {

        if (bm == null) {
            return null;
        }

        if (newWidth <= 0) {
            return null;
        }

        int width = bm.getWidth();
        int height = bm.getHeight();

//        float aspectRatio;
//        aspectRatio = (float) (width / height);
//        int newHeight;
//        newHeight = Math.round((float) newWidth / aspectRatio);

        float scale = (float) newWidth / (float) width;
        int newHeight = (int) ((float) height * scale);

        return Bitmap.createScaledBitmap(bm, newWidth, newHeight, false);
    }

    public static boolean canDecodeImage(byte[] data) {
        if (data == null || data.length == 0) {
            return false;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length);
        return options.outWidth > -1 && options.outHeight > -1;
    }

    public static boolean canDecodeImage(File file) {
        if (!FileHelper.isFileCorrect(file)) {
            return false;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        return options.outWidth > -1 && options.outHeight > -1;
    }

    public static boolean canDecodeVideo(File file) {
        return FileHelper.isFileCorrect(file) && MetadataRetriever.extractMediaDuration(file) > 0;
    }

    /**
     * Определяет поворот картинки
     */
    public static int getOrientation(@NotNull Context context, Uri photoUri) {
        /* it's on the external media. */
        Cursor cursor = context.getContentResolver().query(photoUri,
                new String[]{MediaStore.Images.ImageColumns.ORIENTATION}, null, null, null);

        int orientation;
        try {
            if (cursor == null || cursor.getCount() != 1) {
                orientation = -1;
            } else {
                cursor.moveToFirst();
                orientation = cursor.getInt(0);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return orientation;
    }


    /**
     * Определяем угол для поворота http://sylvana.net/jpegcrop/exif_orientation.html
     */
    public static int getRotationAngleByExifOrientation(int orientation) {
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                return 90;
            case ExifInterface.ORIENTATION_ROTATE_180:
                return 180;
            case ExifInterface.ORIENTATION_ROTATE_270:
                return 270;
            default:
                return 0;
        }
    }

    public static int getExifOrientationByRotationAngle(int degrees) {
        int orientation = GuiUtils.getCorrectedDisplayRotation(degrees);
        switch (orientation) {
            case 90:
                orientation = ExifInterface.ORIENTATION_ROTATE_90;
                break;
            case 180:
                orientation = ExifInterface.ORIENTATION_ROTATE_180;
                break;
            case 270:
                orientation = ExifInterface.ORIENTATION_ROTATE_270;
                break;
            default:
                orientation = ExifInterface.ORIENTATION_NORMAL;
        }
        return orientation;
    }

    public static Bitmap getCorrectlyOrientedImage(@NotNull Context context, Uri uri, Bitmap sourceBitmap) {
        if (isBitmapCorrect(sourceBitmap)) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                final String path = FileHelper.getPath(context, uri);
                if (!TextUtils.isEmpty(path)) {
                    ExifInterface exif = null;
                    try {
                        exif = new ExifInterface(path);
                    } catch (Exception e) {
                        logger.e("an Exception occurred during opening " + path, e);
                    }
                    if (exif != null) {
                        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
                        int rotateAngle = getRotationAngleByExifOrientation(orientation);
                        return rotateBitmap(sourceBitmap, rotateAngle);
                    }
                } else {
                    logger.e("cannot extract path from uri: " + uri);
                }
            } else {
                return sourceBitmap;
            }
        }
        return null;
    }

    public static Bitmap getCorrectlyOrientedImage(@NotNull Context context, Uri photoUri, final int maxImageDimension) throws IOException {
        InputStream is = context.getContentResolver().openInputStream(photoUri);
        BitmapFactory.Options dbo = new BitmapFactory.Options();
        dbo.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(is, null, dbo);

        if (is != null)
            is.close();

        int rotatedWidth, rotatedHeight;
        int orientation = getOrientation(context, photoUri);

        if (orientation == 90 || orientation == 270) {
            rotatedWidth = dbo.outHeight;
            rotatedHeight = dbo.outWidth;
        } else {
            rotatedWidth = dbo.outWidth;
            rotatedHeight = dbo.outHeight;
        }

        Bitmap srcBitmap;
        is = context.getContentResolver().openInputStream(photoUri);
        if (rotatedWidth > maxImageDimension || rotatedHeight > maxImageDimension) {
            float widthRatio = ((float) rotatedWidth) / ((float) maxImageDimension);
            float heightRatio = ((float) rotatedHeight) / ((float) maxImageDimension);
            float maxRatio = Math.max(widthRatio, heightRatio);

            // Create the bitmap from file
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = (int) maxRatio;
            srcBitmap = BitmapFactory.decodeStream(is, null, options);
        } else {
            srcBitmap = BitmapFactory.decodeStream(is);
        }
        if (is != null) {
            is.close();
        }

        /*
         * if the orientation is not 0 (or -1, which means we don't know), we
         * have to do a rotation.
         */
        if (orientation > 0 && Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return rotateBitmap(srcBitmap, orientation);
        }

        return srcBitmap;
    }

    public static int getRotationAngleFromExif(File imageFile) {
        if (GraphicUtils.canDecodeImage(imageFile)) {
            ExifInterface exif;
            try {
                exif = new ExifInterface(imageFile.getAbsolutePath());
                int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
                return getRotationAngleByExifOrientation(orientation);
            } catch (Exception e) {
                logger.e(e);
            }
        }
        return -1;
    }

    public static boolean writeRotationAngleToExif(File imageFile, int degrees) {

        if (!GraphicUtils.canDecodeImage(imageFile)) {
            logger.e("incorrect picture file: " + imageFile);
            return false;
        }

        if (degrees < 0) {
            logger.w("incorrect angle: " + degrees);
            return false;
        }

        try {
            ExifInterface exif = new ExifInterface(imageFile.getAbsolutePath());
            exif.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(getExifOrientationByRotationAngle(degrees)));
            exif.saveAttributes();
            return true;
        } catch (IOException e) {
            logger.e("an IOException occurred", e);
            return false;
        }
    }

    /**
     * Копирование exif-информации из одного файла в другой
     *
     * @param sourcePath исходный файл с exif-информацией
     * @param targetPath файл-получатель
     */
    public boolean copyDefaultExifInfo(String sourcePath, String targetPath) {
        final String[] attributes = new String[]{
                TAG_DATETIME,
                TAG_EXPOSURE_TIME,
                TAG_FLASH,
                TAG_FOCAL_LENGTH,
                TAG_GPS_ALTITUDE,
                TAG_GPS_ALTITUDE_REF,
                TAG_GPS_DATESTAMP,
                TAG_GPS_LATITUDE,
                TAG_GPS_LATITUDE_REF,
                TAG_GPS_LONGITUDE,
                TAG_GPS_LONGITUDE_REF,
                TAG_GPS_PROCESSING_METHOD,
                TAG_GPS_TIMESTAMP,
                TAG_MAKE,
                TAG_MODEL,
                TAG_ORIENTATION,
                TAG_WHITE_BALANCE
        };
        return copyExifInfo(sourcePath, targetPath, Arrays.asList(attributes));
    }

    public boolean copyExifInfo(
            String sourcePath,
            String targetPath,
            Collection<String> attributes
    ) {
        final ExifInterface oldExif;
        final ExifInterface newExif;
        try {
            oldExif = new ExifInterface(sourcePath);
        } catch (IOException e) {
            logger.e("an Exception occurred during opening " + sourcePath, e);
            return false;
        }
        try {
            newExif = new ExifInterface(targetPath);
        } catch (IOException e) {
            logger.e("an Exception occurred during opening " + sourcePath, e);
            return false;
        }
        if (attributes != null) {
            for (String attr : attributes) {
                if (!TextUtils.isEmpty(attr)) {
                    final String value = oldExif.getAttribute(attr);
                    if (value != null) {
                        newExif.setAttribute(attr, value);
                    }
                }
            }
        }

        try {
            newExif.saveAttributes();
        } catch (IOException e) {
            logger.e("an Exception occurred during saving attribute", e);
            return false;
        }

        return true;
    }

    @Nullable
    public static Bitmap rotateBitmap(Bitmap sourceBitmap, int angle) {

        if (!isBitmapCorrect(sourceBitmap)) {
            logger.e("incorrect bitmap: " + sourceBitmap);
            return null;
        }

        if (angle < 0 || angle > 360) {
            logger.e("incorrect angle: " + angle);
            return null;
        }

        Matrix matrix = new Matrix();
        matrix.postRotate(angle);

        try {
            return Bitmap.createBitmap(sourceBitmap, 0, 0, sourceBitmap.getWidth(),
                    sourceBitmap.getHeight(), matrix, true);
        } catch (OutOfMemoryError e) {
            logger.e("an OutOfMemoryError occurred during createBitmap()", e);
            return null;
        }
    }

    @Nullable
    public static Bitmap mirrorBitmap(Bitmap sourceBitmap) {

        if (!isBitmapCorrect(sourceBitmap)) {
            logger.e("incorrect bitmap: " + sourceBitmap);
            return null;
        }

        Matrix matrix = new Matrix();
        matrix.preScale(-1, 1);
        try {
            return Bitmap.createBitmap(sourceBitmap, 0, 0, sourceBitmap.getWidth(),
                    sourceBitmap.getHeight(), matrix, true);
        } catch (OutOfMemoryError e) {
            logger.e("an OutOfMemoryError occurred during createBitmap()", e);
            return null;
        }
    }

    @Nullable
    public static Bitmap cutBitmap(Bitmap sourceBitmap, @NotNull Rect range) {

        if (!isBitmapCorrect(sourceBitmap)) {
            logger.e("incorrect bitmap: " + sourceBitmap);
            return null;
        }

        if (range.left < 0 || range.left >= sourceBitmap.getWidth()
                || range.top < 0 || range.top >= sourceBitmap.getHeight()
                || range.width() > sourceBitmap.getWidth() - range.left
                || range.height() > sourceBitmap.getHeight() - range.top) {
            logger.e("incorrect bounds: " + range);
            return null;
        }

        try {
            return Bitmap.createBitmap(sourceBitmap, range.left, range.top, range.width(), range.height());
        } catch (OutOfMemoryError e) {
            logger.e("an OutOfMemoryError occurred during createBitmap()", e);
            return null;
        }
    }


    public static boolean isBitmapCorrect(Bitmap b) {
        return (b != null && !b.isRecycled() && getBitmapByteCount(b) > 0);
    }

    @SuppressLint("NewApi")
    public static int getBitmapByteCount(Bitmap b) {
        if (b == null)
            return 0;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            return b.getByteCount();
        } else {
            return b.getAllocationByteCount();
        }
    }

    @Nullable
    public static byte[] getBitmapData(Bitmap b) {
        if (!isBitmapCorrect(b)) {
            logger.e("incorrect bitmap");
            return null;
        }
        ByteBuffer buffer = ByteBuffer.allocate(b.getHeight() * b.getRowBytes());
        b.copyPixelsToBuffer(buffer);
        return buffer.hasArray() ? buffer.array() : null;
    }

    @Nullable
    public static Bitmap setBitmapData(byte[] data, int width, int height, Bitmap.Config config) {

        if (data == null || data.length == 0) {
            logger.e("data is null or empty");
            return null;
        }

        if (width <= 0 || height <= 0) {
            logger.e("incorrect image size: " + width + "x" + height);
            return null;
        }

        if (config == null) {
            logger.e("config is null");
            return null;
        }

        Bitmap b = Bitmap.createBitmap(width, height, config);
        ByteBuffer frameBuffer = ByteBuffer.wrap(data);
        b.copyPixelsFromBuffer(frameBuffer);

        return b;
    }

    /**
     * creating new bitmap using specified source bitmap
     *
     * @param b can be immutable
     * @return newly created bitmap with given config
     */
    @Nullable
    public static Bitmap copyBitmap(Bitmap b, Bitmap.Config c) {

        if (!isBitmapCorrect(b)) {
            logger.e("incorrect bitmap");
            return b;
        }

        if (c == null) {
            logger.e("config is null");
            return b;
        }

        Bitmap convertedBitmap = null;

        try {
            convertedBitmap = Bitmap.createBitmap(b.getWidth(), b.getHeight(), c);
        } catch (OutOfMemoryError e) {
            logger.e("an OutOfMemoryError error occurred during createBitmap()", e);
            return b;
        }

        Canvas canvas = new Canvas(convertedBitmap);
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        canvas.drawBitmap(b, 0, 0, paint);
        return convertedBitmap;
    }

    /**
     * @param b must be mutable
     */
    @Nullable
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static Bitmap reconfigureBitmap(Bitmap b, Bitmap.Config c) {

        if (b == null || c == null)
            return null;

        if (!b.isMutable()) {
            logger.e("given bitmap is immutable!");
            return null;
        }

        if (b.getConfig() == c)
            return b;

        b.reconfigure(b.getWidth(), b.getHeight(), c);
        return b;
    }

    @Nullable
    public static Bitmap createBitmapFromDrawable(Drawable drawable, int width, int height, @NotNull Bitmap.Config config) {

        if (drawable == null) {
            return null;
        }

        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        if (!(drawable instanceof ColorDrawable)) {
            width = drawable.getIntrinsicWidth();
            height = drawable.getIntrinsicWidth();
        }

        if (width <= 0 || height <= 0) {
            logger.e("incorrect bounds: " + width + "x" + height);
            return null;
        }

        try {
            Bitmap bitmap;

            bitmap = Bitmap.createBitmap(width, height, config);

            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
            return bitmap;

        } catch (Exception e) {
            logger.e("an Exception occurred", e);
            return null;
        }
    }

    @Nullable
    public static Bitmap curveImage(Bitmap bitmap, @NotNull Point corners) {

        if (!GraphicUtils.isBitmapCorrect(bitmap)) {
            return null;
        }

        // Bitmap myCoolBitmap = ... ; // <-- Your bitmap you
        // want rounded
        int w = bitmap.getWidth(), h = bitmap.getHeight();
        Bitmap.Config c = bitmap.getConfig();

        // We have to make sure our rounded corners have an
        // alpha channel in most cases
        Bitmap rounder = Bitmap.createBitmap(w, h, c);
        Canvas canvas = new Canvas(rounder);

        // We're going to apply this paint eventually using a
        // porter-duff xfer mode.
        // This will allow us to only overwrite certain pixels.
        // RED is arbitrary. This
        // could be any color that was fully opaque (alpha =
        // 255)
        Paint xferPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        xferPaint.setColor(Color.RED);

        // We're just reusing xferPaint to paint a normal
        // looking rounded box, the 20.f
        // is the amount we're rounding by.
        canvas.drawRoundRect(new RectF(0, 0, w, h), corners.x, corners.y, xferPaint);

        // Now we apply the 'magic sauce' to the paint
        xferPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));

        Bitmap result = Bitmap.createBitmap(w, h, c);
        Canvas resultCanvas = new Canvas(result);
        resultCanvas.drawBitmap(bitmap, 0, 0, null);
        resultCanvas.drawBitmap(rounder, 0, 0, xferPaint);

        return result;
    }

    public static byte[] convertYuvToJpeg(byte[] data, int format, int width, int height) {
        logger.d("convertYuvToJpeg(), data (length)=" + (data != null ? data.length : 0) + ", format=" + format + ", width=" + width
                + ", height=" + height);

        if (data == null || data.length == 0) {
            logger.e("data is null or emty");
            return null;
        }

        if (!(format == ImageFormat.NV21 || format == ImageFormat.YUY2)) {
            logger.e("incorrect image format: " + format);
            return null;
        }

        if (width <= 0 || height <= 0) {
            logger.e("incorrect resolution: " + width + "x" + height);
            return null;
        }

        YuvImage yuvImg = new YuvImage(data, format, width, height, null);
        ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();
        yuvImg.compressToJpeg(new Rect(0, 0, width, height), 100, byteOutStream);
        return byteOutStream.toByteArray();
    }

    public static byte[] convertRgbToYuv420SP(int[] aRGB, int width, int height) {
        // logger.d("convertRgbToYuv420SP(), width=" + width + ", height=" + height);

        if (aRGB == null || aRGB.length == 0) {
            logger.e("data is null or empty");
            return null;
        }

        if (width <= 0 || height <= 0) {
            logger.e("incorrect image size");
            return null;
        }

        final int frameSize = width * height;
        final int chromaSize = frameSize / 4;

        int yIndex = 0;
        int uIndex = frameSize;
        int vIndex = frameSize + chromaSize;
        byte[] yuv = new byte[width * height * 3 / 2];

        int a, R, G, B, Y, U, V;
        int index = 0;
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {

                // a = (aRGB[index] & 0xff000000) >> 24; //not using it right now
                R = (aRGB[index] & 0xff0000) >> 16;
                G = (aRGB[index] & 0xff00) >> 8;
                B = (aRGB[index] & 0xff) >> 0;

                Y = ((66 * R + 129 * G + 25 * B + 128) >> 8) + 16;
                U = ((-38 * R - 74 * G + 112 * B + 128) >> 8) + 128;
                V = ((112 * R - 94 * G - 18 * B + 128) >> 8) + 128;

                yuv[yIndex++] = (byte) ((Y < 0) ? 0 : ((Y > 255) ? 255 : Y));

                if (j % 2 == 0 && index % 2 == 0) {
                    yuv[uIndex++] = (byte) ((U < 0) ? 0 : ((U > 255) ? 255 : U));
                    yuv[vIndex++] = (byte) ((V < 0) ? 0 : ((V > 255) ? 255 : V));
                }

                index++;
            }
        }
        return yuv;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public static Bitmap renderScriptNVToRGBA(@NotNull Context context, int width, int height, byte[] nv, @Nullable Bitmap.Config bitmapConfig) {

        if (width <= 0 || height <= 0) {
            logger.e("incorrect size: " + width + "x" + height);
            return null;
        }

        if (nv == null || nv.length == 0) {
            logger.e("incorrect source");
            return null;
        }

        RenderScript rs = null;
        ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic = null;
        Allocation in = null;
        Allocation out = null;

        try {
            rs = RenderScript.create(context);
            yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));

            Type.Builder yuvType = new Type.Builder(rs, Element.U8(rs)).setX(nv.length);
            in = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT);

            final Element element;

            if (bitmapConfig == null) {
                bitmapConfig = Bitmap.Config.ARGB_8888;
            }

            switch (bitmapConfig) {
                case ALPHA_8:
                    element = Element.A_8(rs);
                    break;
                case ARGB_4444:
                    element = Element.RGBA_4444(rs);
                    break;
                case RGB_565:
                    element = Element.RGB_565(rs);
                    break;
                default:
                    element = Element.RGBA_8888(rs);
            }

            Type.Builder rgbaType = new Type.Builder(rs, element).setX(width).setY(height);
            out = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_SCRIPT);

            in.copyFrom(nv);

            yuvToRgbIntrinsic.setInput(in);
            yuvToRgbIntrinsic.forEach(out);

            Bitmap resultBitmap = null;
            try {
                resultBitmap = Bitmap.createBitmap(width, height, bitmapConfig);
            } catch (OutOfMemoryError e) {
                logger.e("an OutOfMemoryError occurred during createBitmap()", e);
            }

            if (resultBitmap != null) {
                try {
                    out.copyTo(resultBitmap);
                    return resultBitmap;
                } catch (Throwable e) {
                    logger.e("an Exception occurred during copyTo()", e);
                    if (!resultBitmap.isRecycled()) {
                        resultBitmap.recycle();
                    }
                }
            }

        } catch (RuntimeException e) {
            logger.e("a RuntimeException occurred during rendering", e);

        } finally {
            if (out != null) {
                out.destroy();
            }
            if (in != null) {
                in.destroy();
            }
            if (yuvToRgbIntrinsic != null) {
                yuvToRgbIntrinsic.destroy();
            }
            if (rs != null) {
                rs.destroy();
            }
        }
        return null;
    }


    public static int generateRandomColor(int color) {
        Random random = new Random();
        int red = random.nextInt(256);
        int green = random.nextInt(256);
        int blue = random.nextInt(256);
        return Color.rgb((red + (Color.red(color))) / 2, (green + (Color.green(color))) / 2, (blue + (Color.blue(color))) / 2);
    }

    @Nullable
    public static ColorStateList createColorStateListFromRes(@NotNull Context context, @XmlRes int res) {
        XmlResourceParser parser = context.getResources().getXml(res);
        try {
            return ColorStateList.createFromXml(context.getResources(), parser);
        } catch (XmlPullParserException | IOException e) {
            logger.e("an Exception occurred", e);
            return null;
        }
    }

    @ColorInt
    public static int getDefaultColor(ColorStateList c) {
        return c != null ? c.getDefaultColor() : 0;
    }

    @Nullable
    public static Drawable getDefaultDrawable(StateListDrawable d) {
        return d != null ? getDrawableForState(d, 0) : null;
    }

    @SuppressWarnings("PrimitiveArrayArgumentToVariableArgMethod")
    @Nullable
    public static Drawable getDrawableForState(@NotNull StateListDrawable stateListDrawable, int... state) {
        try {
            Method getStateDrawableIndex = StateListDrawable.class.getMethod("getStateDrawableIndex", int[].class);
            Method getStateDrawable = StateListDrawable.class.getMethod("getStateDrawable", int.class);
            getStateDrawableIndex.setAccessible(true);
            getStateDrawable.setAccessible(true);
            int index = (int) getStateDrawableIndex.invoke(stateListDrawable, state);
            return (Drawable) getStateDrawable.invoke(stateListDrawable, index);
        } catch (Exception e) {
            logger.e("an Exception occurred", e);
        }
        return null;
    }

    @Nullable
    public static Drawable cloneDrawable(@Nullable Drawable d) {
        Drawable cloned = d;
        if (d != null) {
            cloned = d.getConstantState() != null ? (d.getConstantState().newDrawable()) : null;
            if (cloned != null) {
                cloned = cloned.mutate(); // mutate() -> not affecting other instances, for e.g. after setting color filter
            } else {
                cloned = d.mutate();
            }

        }
        return cloned;
    }

    @NotNull
    private static PaletteColors makePaletteColors(Palette palette, @ColorInt final int defaultColor, final Swatch sw) {
        final PaletteColors colors = new PaletteColors();
        if (palette != null && sw != null) {
            switch (sw) {
                case MUTED:
                    colors.background = palette.getMutedColor(defaultColor);
                    colors.title = palette.getMutedSwatch() != null ? palette.getMutedSwatch().getTitleTextColor() : defaultColor;
                    colors.body = palette.getMutedSwatch() != null ? palette.getMutedSwatch().getBodyTextColor() : defaultColor;
                    break;
                case LIGHT_MUTED:
                    colors.background = palette.getLightMutedColor(defaultColor);
                    colors.title = palette.getLightMutedSwatch() != null ? palette.getLightMutedSwatch().getTitleTextColor() : defaultColor;
                    colors.body = palette.getLightMutedSwatch() != null ? palette.getLightMutedSwatch().getBodyTextColor() : defaultColor;
                    break;
                case DARK_MUTED:
                    colors.background = palette.getDarkMutedColor(defaultColor);
                    colors.title = palette.getDarkMutedSwatch() != null ? palette.getDarkMutedSwatch().getTitleTextColor() : defaultColor;
                    colors.body = palette.getDarkMutedSwatch() != null ? palette.getDarkMutedSwatch().getBodyTextColor() : defaultColor;
                    break;
                case VIBRANT:
                    colors.background = palette.getVibrantColor(defaultColor);
                    colors.title = palette.getVibrantSwatch() != null ? palette.getVibrantSwatch().getTitleTextColor() : defaultColor;
                    colors.body = palette.getVibrantSwatch() != null ? palette.getVibrantSwatch().getBodyTextColor() : defaultColor;
                    break;
                case LIGHT_VIBRANT:
                    colors.background = palette.getLightVibrantColor(defaultColor);
                    colors.title = palette.getLightVibrantSwatch() != null ? palette.getLightVibrantSwatch().getTitleTextColor() : defaultColor;
                    colors.body = palette.getLightVibrantSwatch() != null ? palette.getLightVibrantSwatch().getBodyTextColor() : defaultColor;
                    break;
                case DARK_VIBRANT:
                    colors.background = palette.getDarkVibrantColor(defaultColor);
                    colors.title = palette.getDarkVibrantSwatch() != null ? palette.getDarkVibrantSwatch().getTitleTextColor() : defaultColor;
                    colors.body = palette.getDarkVibrantSwatch() != null ? palette.getDarkVibrantSwatch().getBodyTextColor() : defaultColor;
                    break;
                default:
                    break;
            }
        }
        return colors;
    }


    @Nullable
    public static PaletteColors generateColorByBitmap(Bitmap bm, @ColorInt final int defaultColor, final Swatch sw) {
        if (isBitmapCorrect(bm) && sw != null) {
            return makePaletteColors(new Palette.Builder(bm).generate(), defaultColor, sw);
        }
        return null;
    }

    public static void generateColorByBitmapAsync(Bitmap bm, @ColorInt final int defaultColor, final Swatch sw, final OnPaletteColorsGeneratedListener listener) {
        if (isBitmapCorrect(bm) && sw != null && listener != null) {
            new Palette.Builder(bm).generate(new Palette.PaletteAsyncListener() {
                @Override
                public void onGenerated(Palette palette) {
                    listener.onPaletteColorsGenerated(makePaletteColors(palette, defaultColor, sw));
                }
            });
        }
    }

    /**
     * Resize bitmap to fit x and y leaving no blank space
     */
    public static Bitmap resizeBitmapFitXY(@NotNull Bitmap bitmap, int width, int height) {
        Bitmap background = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        float originalWidth = bitmap.getWidth(), originalHeight = bitmap.getHeight();
        Canvas canvas = new Canvas(background);
        float scale, xTranslation = 0.0f, yTranslation = 0.0f;
        if (originalWidth > originalHeight) {
            scale = height / originalHeight;
            xTranslation = (width - originalWidth * scale) / 2.0f;
        } else {
            scale = width / originalWidth;
            yTranslation = (height - originalHeight * scale) / 2.0f;
        }
        Matrix transformation = new Matrix();
        transformation.postTranslate(xTranslation, yTranslation);
        transformation.preScale(scale, scale);
        Paint paint = new Paint();
        paint.setFilterBitmap(true);
        canvas.drawBitmap(bitmap, transformation, paint);
        return background;
    }

    /**
     * Get size delta in pixels for one dimension of image and screen
     */
    private static int getSizeDelta(int bitmapSize, int screenSize) {
        return bitmapSize - screenSize;
    }

    /**
     * Resize bitmap if needed
     */
    public static Bitmap resizeBitmap(Bitmap bitmap, int screenWidth, int screenHeight, float screenAspectRatio, float wallpaperSizeThreshold, float wallpaperAspectRatioThreshold) {
        logger.d("Resizing bitmap");
        logger.d("Screen size: " + screenWidth + "x" + screenHeight + ", wallpaper size: " + bitmap.getWidth() + "x" + bitmap.getHeight());

        float wallpaperAspectRatio = bitmap.getWidth() / bitmap.getHeight();

        logger.d("Wallpaper aspect ratio: " + wallpaperAspectRatio +
                ", screen aspect ratio: " + screenAspectRatio);

        if (getSizeDelta(bitmap.getWidth(), screenWidth) > wallpaperSizeThreshold || getSizeDelta(bitmap.getHeight(), screenHeight) > wallpaperSizeThreshold) {
            // if at least one dimension of wallpaper is bigger then screen's with the set threshold — check aspect ratio and resize it if needed
            float aspectRatioDelta = Math.abs(wallpaperAspectRatio - screenAspectRatio);

            if (aspectRatioDelta >= wallpaperAspectRatioThreshold) {
                logger.d("Wallpaper aspect ratio differs from screen: " + aspectRatioDelta);
                return resizeBitmapFitXY(bitmap, screenWidth, screenHeight);
            }
        }

        return bitmap;
    }

    public enum Swatch {
        VIBRANT,
        LIGHT_VIBRANT,
        DARK_VIBRANT,
        MUTED,
        LIGHT_MUTED,
        DARK_MUTED
    }

    public static class PaletteColors {

        @ColorInt
        public int background = Color.WHITE;
        @ColorInt
        public int title = Color.BLACK;
        @ColorInt
        public int body = Color.BLACK;

        public PaletteColors() {
        }

        public PaletteColors(@ColorInt int background, @ColorInt int title, @ColorInt int body) {
            this.background = background;
            this.title = title;
            this.body = body;
        }
    }

    public interface OnPaletteColorsGeneratedListener {

        void onPaletteColorsGenerated(@NotNull PaletteColors colors);
    }
}
