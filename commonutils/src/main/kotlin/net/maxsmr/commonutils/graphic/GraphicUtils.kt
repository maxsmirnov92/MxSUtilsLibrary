package net.maxsmr.commonutils.graphic

import android.annotation.TargetApi
import android.content.ContentResolver
import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.graphics.Bitmap.CompressFormat
import android.graphics.Bitmap.Config
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.renderscript.*
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import net.maxsmr.commonutils.gui.getFixedSize
import net.maxsmr.commonutils.isPreKitkat
import net.maxsmr.commonutils.*
import net.maxsmr.commonutils.text.isEmpty
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder.formatException
import net.maxsmr.commonutils.media.*
import java.io.*
import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt

private val logger = BaseLoggerHolder.getInstance().getLogger<BaseLogger>("GraphicUtils")

// TODO convert to extensions?

fun isBitmapValid(b: Bitmap?): Boolean =
        b != null && !b.isRecycled && getBitmapByteCount(b) > 0

fun getBitmapByteCount(b: Bitmap?): Int {
    if (b == null || b.isRecycled) return 0
    return if (isPreKitkat()) {
        b.byteCount
    } else {
        b.allocationByteCount
    }
}

/**
 * creating new bitmap using specified source bitmap
 *
 * @param bitmap can be immutable
 * @return newly created bitmap with given config
 */
@JvmOverloads
fun copyBitmap(
        bitmap: Bitmap?,
        @ColorInt
        backgroundColor: Int = Color.BLACK,
        config: Config? = null,
        recycle: Boolean = true
): Bitmap? {
    if (bitmap == null) {
        logger.e("bitmap is null: $bitmap")
        return null
    }
    val convertedBitmap: Bitmap = createBitmapSafe(bitmap.width, bitmap.height, config ?: Config.ARGB_8888) ?: return null
    val canvas = Canvas(convertedBitmap)
    val paint = Paint()
    paint.color = backgroundColor
    if (isBitmapValid(bitmap)) {
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
    }
    return try {
        convertedBitmap
    } finally {
        if (recycle) {
            bitmap.recycle()
        }
    }
}

/**
 * @param bitmap must be mutable
 */
@TargetApi(Build.VERSION_CODES.KITKAT)
@JvmOverloads
fun reconfigureBitmap(
        bitmap: Bitmap?,
        config: Config = Config.ARGB_8888
): Bitmap? {
    if (bitmap == null) {
        logger.e("bitmap is null: $bitmap")
        return null
    }
    if (!bitmap.isMutable) {
        logger.e("bitmap $bitmap is immutable")
        return null
    }
    if (bitmap.config == config) return bitmap
    return try {
        bitmap.reconfigure(bitmap.width, bitmap.height, config)
        bitmap
    } catch (e: Throwable) {
        logger.e(formatException(e, "reconfigure"))
        null
    }
}

@JvmOverloads
fun getBitmapPixelBuffer(
        bitmap: Bitmap?,
        recycleSource: Boolean = true
): Pair<ByteArray, Config>? {
    if (bitmap == null || !isBitmapValid(bitmap)) {
        logger.e("Incorrect bitmap: $bitmap")
        return null
    }
    try {
        val buffer = ByteBuffer.allocate(bitmap.height * bitmap.rowBytes)
        bitmap.copyPixelsToBuffer(buffer)
        return Pair(
                if (buffer.hasArray()) buffer.array() else null,
                bitmap.config
        )
    } catch (e: Throwable) {
        logger.e(formatException(e))
        return null
    } finally {
        if (recycleSource) {
            bitmap.recycle()
        }
    }
}

@JvmOverloads
fun createBitmapByPixelBuffer(
        data: ByteArray?,
        width: Int,
        height: Int,
        config: Config = Config.ARGB_8888
): Bitmap? {
    if (data == null || data.isEmpty()) {
        logger.e("data is null or empty")
        return null
    }
    if (width <= 0 || height <= 0) {
        logger.e("Incorrect image size: $width x $height")
        return null
    }
    val result = createBitmapSafe(width, height, config) ?: return null
    val frameBuffer = ByteBuffer.wrap(data)
    result.copyPixelsFromBuffer(frameBuffer)
    return result
}

@JvmOverloads
fun createBitmapFromFile(
        file: File,
        scale: Int = 1,
        config: Config? = null,
        withSampleSize: Boolean = false
): Bitmap? {
    if (!canDecodeImage(file)) {
        logger.e("Incorrect file: $file")
        return null
    }
    return if (withSampleSize) {
        val options = BitmapFactory.Options()
        decodeBoundsFromFile(file, options)
        applyBitmapSampleOptions(options, scale, config)
        try {
            BitmapFactory.decodeFile(file.absolutePath, options)
        } catch (e: Throwable) {
            logger.e(formatException(e, "decodeFile"))
            null
        }
    } else {
        try {
            BitmapFactory.decodeFile(file.absolutePath)
        } catch (e: Throwable) {
            logger.e(formatException(e, "decodeFile"))
            null
        }
    }
}

@JvmOverloads
fun createBitmapFromByteArray(
        data: ByteArray?,
        scale: Int = 1,
        config: Config? = null,
        withSampleSize: Boolean = false
): Bitmap? {
    if (data == null || data.isEmpty()) {
        logger.e("data is null or empty")
        return null
    }
    return if (withSampleSize) {
        val options = BitmapFactory.Options()
        decodeBoundsFromByteArray(data, options)
        applyBitmapSampleOptions(options, scale, config)
        try {
            BitmapFactory.decodeByteArray(data, 0, data.size, options)
        } catch (e: Throwable) {
            logger.e(formatException(e, "decodeByteArray"))
            null
        }
    } else {
        try {
            BitmapFactory.decodeByteArray(data, 0, data.size)
        } catch (e: Throwable) {
            logger.e(formatException(e, "decodeByteArray"))
            null
        }
    }
}

fun createBitmapFromUri(
        uri: Uri?,
        contentResolver: ContentResolver,
        scale: Int = 1,
        config: Config? = null,
        withSampleSize: Boolean = false
): Bitmap? {
    if (uri == null) {
        logger.e("uri is null")
        return null
    }
    return createBitmapFromStream(
            uri.openInputStream(contentResolver),
            scale,
            config,
            withSampleSize
    )
}

@JvmOverloads
fun createBitmapFromStream(
        inputStream: InputStream?,
        scale: Int = 1,
        config: Config? = null,
        withSampleSize: Boolean = false,
        closeStream: Boolean = true
): Bitmap? {
    if (inputStream == null) {
        logger.e("inputStream is null")
        return null
    }
    return try {
        if (withSampleSize) {
            val options = BitmapFactory.Options()
            decodeBoundsFromStream(inputStream, options)
            applyBitmapSampleOptions(options, scale, config)
            try {
                BitmapFactory.decodeStream(inputStream, null, options)
            } catch (e: Throwable) {
                logger.e(formatException(e, "decodeStream"))
                null
            }
        } else {
            try {
                BitmapFactory.decodeStream(inputStream)
            } catch (e: Throwable) {
                logger.e(formatException(e, "decodeStream"))
                null
            }
        }
    } finally {
        if (closeStream) {
            try {
                inputStream.close()
            } catch (e: IOException) {
                logger.e(formatException(e, "close"))
            }
        }
    }
}

@JvmOverloads
fun createBitmapFromResource(
        resources: Resources,
        @DrawableRes resId: Int,
        scale: Int = 1,
        config: Config? = null,
        withSampleSize: Boolean = false
): Bitmap? {
    if (resId == 0) {
        logger.e("resId is not specified")
        return null
    }
    return if (withSampleSize) {
        val options = BitmapFactory.Options()
        decodeBoundsFromResource(resId, resources, options)
        applyBitmapSampleOptions(options, scale, config)
        try {
            BitmapFactory.decodeResource(resources, resId, options)
        } catch (e: Throwable) {
            logger.e(formatException(e, "decodeResource"))
            null
        }
    } else {
        try {
            BitmapFactory.decodeResource(resources, resId)
        } catch (e: Throwable) {
            logger.e(formatException(e, "decodeResource"))
            null
        }
    }
}

@JvmOverloads
fun createBitmapFromDrawable(
        drawable: Drawable?,
        width: Int,
        height: Int,
        config: Config = Config.ARGB_8888
): Bitmap? {
    var width = width
    var height = height
    if (drawable == null) {
        logger.e("drawable is null")
        return null
    }
    if (drawable is BitmapDrawable) {
        return copyBitmap(drawable.bitmap, config = config)
    }
    if (drawable !is ColorDrawable) {
        width = drawable.intrinsicWidth
        height = drawable.intrinsicWidth
    }
    if (width <= 0 || height <= 0) {
        logger.e("Incorrect bounds: $width x $height")
        return null
    }
    val bitmap = createBitmapSafe(width, height, config) ?: return null
    return try {
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        bitmap
    } catch (e: Throwable) {
        logger.e(formatException(e))
        null
    }
}

@JvmOverloads
fun createScaledBitmapByWidth(
        bitmap: Bitmap?,
        scaledWidth: Float,
        filter: Boolean = true,
        recycleSource: Boolean = true
): Bitmap? {
    if (bitmap == null || !isBitmapValid(bitmap)) {
        logger.e("Incorrect bitmap: $bitmap")
        return null
    }
    if (scaledWidth <= 0) {
        logger.e("Incorrect scaledWidth: $scaledWidth")
        return null
    }
    val width = bitmap.width
    val height = bitmap.height
    if (width == scaledWidth.toInt()) {
        return bitmap
    }
//        float aspectRatio;
//        aspectRatio = (float) (width / height);
//        int newHeight;
//        newHeight = Math.round((float) newWidth / aspectRatio);
    val scale = scaledWidth / width.toFloat()
    val scaledHeight = ceil(height.toFloat() * scale)
    return try {
        Bitmap.createScaledBitmap(bitmap, scaledWidth.toInt(), scaledHeight.toInt(), filter)
    } catch (e: Throwable) {
        logger.e(formatException(e, "createScaledBitmap"))
        null
    } finally {
        if (recycleSource) {
            bitmap.recycle()
        }
    }
}

/**
 * Resize bitmap to fit x and y leaving no blank space
 */
@JvmOverloads
fun createResizedBitmapFitXY(
        bitmap: Bitmap?,
        width: Int,
        height: Int,
        config: Config? = null,
        recycleSource: Boolean = true
): Bitmap? {
    if (bitmap == null || !isBitmapValid(bitmap)) {
        logger.e("Incorrect bitmap: $bitmap")
        return null
    }
    val newBitmap = createBitmapSafe(width, height, config ?: bitmap.config) ?: return null
    val originalWidth = bitmap.width.toFloat()
    val originalHeight = bitmap.height.toFloat()

    val scale: Float
    var xTranslation = 0.0f
    var yTranslation = 0.0f
    if (originalWidth > originalHeight) {
        scale = height / originalHeight
        xTranslation = (width - originalWidth * scale) / 2.0f
    } else {
        scale = width / originalWidth
        yTranslation = (height - originalHeight * scale) / 2.0f
    }
    val transformation = Matrix()
    transformation.postTranslate(xTranslation, yTranslation)
    transformation.preScale(scale, scale)
    val paint = Paint()
    paint.isFilterBitmap = true
    return try {
        val canvas = Canvas(newBitmap)
        canvas.drawBitmap(bitmap, transformation, paint)
        newBitmap
    } catch (e: Throwable) {
        logger.e(formatException(e))
        null
    } finally {
        if (recycleSource) {
            bitmap.recycle()
        }
    }
}

@JvmOverloads
fun createResizedBitmapIfNeeded(
        bitmap: Bitmap?,
        parentWidth: Int,
        parentHeight: Int,
        sizeThreshold: Float,
        aspectRatioThreshold: Float,
        parentAspectRatio: Float = parentWidth.toFloat() / parentHeight,
        config: Config? = null,
        recycleSource: Boolean = true
): Bitmap? {

    if (bitmap == null || !isBitmapValid(bitmap)) {
        logger.e("Incorrect bitmap: $bitmap")
        return null
    }

    if (aspectRatioThreshold <= 0) {
        logger.e("aspectRatioThreshold: $aspectRatioThreshold")
        return null
    }

    if (parentAspectRatio <= 0) {
        logger.e("parentAspectRatio: $parentAspectRatio")
        return null
    }

    /**
     * Get size delta in pixels for one dimension of image and screen
     */
    fun getSizeDelta(bitmapSize: Int, screenSize: Int): Int =
            bitmapSize - screenSize


    val bitmapAspectRatio = bitmap.width.toFloat() / bitmap.height
    if (getSizeDelta(bitmap.width, parentWidth) > sizeThreshold
            || getSizeDelta(bitmap.height, parentHeight) > sizeThreshold) {
        // if at least one dimension of wallpaper is bigger then screen's with the set threshold — check aspect ratio and resize it if needed
        val aspectRatioDelta = abs(bitmapAspectRatio - parentAspectRatio)
        if (aspectRatioDelta >= aspectRatioThreshold) {
            return createResizedBitmapFitXY(bitmap, parentWidth, parentHeight, config, recycleSource)
        }
    }
    return bitmap
}

@JvmOverloads
fun scaleDownBitmap(
        bitmap: Bitmap?,
        maxSize: Int,
        filter: Boolean = true,
        recycleSource: Boolean = true
): Bitmap? {
    if (bitmap == null || !isBitmapValid(bitmap)) {
        logger.e("Incorrect bitmap: $bitmap")
        return null
    }
    val fixedSize = getFixedSize(bitmap.width, bitmap.height, maxSize)
    // без отбрасывания дробной части будет более точный коэффициент, а значит и посчитанный height
    return createScaledBitmapByWidth(bitmap, fixedSize.x, filter, recycleSource)
}

fun canDecodeImage(file: File): Boolean {
    val bounds = decodeBoundsFromFile(file)
    return bounds.x > 0 && bounds.y > 0
}

fun canDecodeImage(data: ByteArray?): Boolean {
    val bounds = decodeBoundsFromByteArray(data)
    return bounds.x > 0 && bounds.y > 0
}

fun canDecodeImage(contentResolver: ContentResolver, uri: Uri?): Boolean {
    val bounds = decodeBoundsFromUri(uri, contentResolver)
    return bounds.x > 0 && bounds.y > 0
}

fun canDecodeImage(stream: InputStream?): Boolean {
    val bounds = decodeBoundsFromStream(stream)
    return bounds.x > 0 && bounds.y > 0
}

fun canDecodeImage(@DrawableRes resId: Int?, resources: Resources): Boolean {
    val bounds = decodeBoundsFromResource(resId, resources)
    return bounds.x > 0 && bounds.y > 0
}

fun canDecodeVideo(file: File?): Boolean =
        extractMediaDurationFromFile(file) != null

fun canDecodeVideo(uri: Uri?): Boolean =
        extractMediaDurationFromUri(uri) != null

@JvmOverloads
fun decodeBoundsFromFile(
        file: File?,
        options: BitmapFactory.Options = BitmapFactory.Options()
): Point {
    if (file == null || !isFileValid(file)) return Point(0, 0)
    options.inJustDecodeBounds = true
    BitmapFactory.decodeFile(file.absolutePath, options)
    return Point(options.outWidth, options.outHeight)
}

@JvmOverloads
fun decodeBoundsFromByteArray(
        data: ByteArray?,
        options: BitmapFactory.Options = BitmapFactory.Options()
): Point {
    if (data == null || data.isEmpty()) return Point(0, 0)
    options.inJustDecodeBounds = true
    BitmapFactory.decodeByteArray(data, 0, data.size, options)
    return Point(options.outWidth, options.outHeight)
}

@JvmOverloads
fun decodeBoundsFromUri(
        uri: Uri?,
        contentResolver: ContentResolver,
        options: BitmapFactory.Options = BitmapFactory.Options()
): Point {
    if (uri == null) return Point(0, 0)
    return decodeBoundsFromStream(uri.openInputStream(contentResolver), options)
}

@JvmOverloads
fun decodeBoundsFromStream(
        stream: InputStream?,
        options: BitmapFactory.Options = BitmapFactory.Options()
): Point {
    if (stream == null) return Point(0, 0)
    options.inJustDecodeBounds = true
    BitmapFactory.decodeStream(stream, null, options)
    return Point(options.outWidth, options.outHeight)
}

@JvmOverloads
fun decodeBoundsFromResource(
        @DrawableRes resId: Int?,
        resources: Resources,
        options: BitmapFactory.Options = BitmapFactory.Options()
): Point {
    if (resId == null) return Point(0, 0)
    options.inJustDecodeBounds = true
    BitmapFactory.decodeResource(resources, resId, options)
    return Point(options.outWidth, options.outHeight)
}

fun getFileExtByCompressFormat(compressFormat: CompressFormat?): String? {
    if (compressFormat == null) {
        logger.e("compressFormat is null")
        return null
    }
    return when (compressFormat) {
        CompressFormat.PNG -> "png"
        CompressFormat.JPEG -> "jpg"
        CompressFormat.WEBP,
        CompressFormat.WEBP_LOSSLESS,
        CompressFormat.WEBP_LOSSY -> "webp"
        else -> null
    }
}

/**
 * @return null if failed, otherwise same file or same file with changed extension
 */
@JvmOverloads
fun compressBitmapToFile(
        file: File?,
        bitmap: Bitmap?,
        format: CompressFormat = CompressFormat.JPEG,
        quality: Int = 100
): File? {
    var file = file
    if (file == null) {
        logger.e("File not specified")
        return null
    }
    if (bitmap == null || !isBitmapValid(bitmap)) {
        logger.e("Bitmap is incorrect: $bitmap")
        return null
    }
    if (quality <= 0) {
        logger.e("Incorrect quality: \$quality")
        return null
    }
    var ext = getFileExtByCompressFormat(format)
    if (isEmpty(ext)) {
        logger.e("Unknown format: $format")
        ext = getFileExtension(file.name)
    }
    file = createFile(removeFileExtension(file.name) + "." + ext, file.parent)
    if (file == null) {
        logger.e("file was not created")
        return null
    }
    if (compressBitmapToStream(file.openOutputStream(false), bitmap, format, quality)) {
        return file
    }
    return null
}

@JvmOverloads
fun compressBitmapToUri(
        uri: Uri,
        contentResolver: ContentResolver,
        bitmap: Bitmap?,
        format: CompressFormat = CompressFormat.JPEG,
        quality: Int = 100
): Boolean {
    val stream = uri.openOutputStream(contentResolver) ?: return false
    return compressBitmapToStream(stream, bitmap, format, quality)
}

@JvmOverloads
fun compressBitmapToStream(
        outputStream: OutputStream?,
        bitmap: Bitmap?,
        format: CompressFormat = CompressFormat.JPEG,
        quality: Int = 100,
        closeStream: Boolean = true
): Boolean {
    if (outputStream == null) {
        logger.e("outputStream is null");
        return false
    }
    if (bitmap == null || !isBitmapValid(bitmap)) {
        logger.e("bitmap is incorrect: $bitmap")
        return false
    }
    try {
        bitmap.compress(format, quality, outputStream)
        outputStream.flush()
        return true
    } catch (e: Throwable) {
        logger.e(formatException(e))
    } finally {
        if (closeStream) {
            try {
                outputStream.close()
            } catch (e: IOException) {
                logger.e(formatException(e, "close"))
            }
        }
    }
    return false
}

@JvmOverloads
fun compressBitmapToByteArray(
        bitmap: Bitmap?,
        format: CompressFormat = CompressFormat.JPEG,
        quality: Int = 100
): ByteArray? {
    if (bitmap == null || !isBitmapValid(bitmap)) {
        logger.e("Incorrect bitmap: $bitmap")
        return null
    }
    if (quality <= 0) {
        logger.e("Incorrect quality: $quality")
        return null
    }
    val baos = ByteArrayOutputStream()
    try {
        bitmap.compress(format, quality, baos)
        baos.flush()
        try {
            return baos.toByteArray()
        } catch (e: OutOfMemoryError) {
            logger.e(formatException(e, "toByteArray"))
        }
    } catch (e: IOException) {
        logger.e(formatException(e))
    } finally {
        try {
            baos.close()
        } catch (e: IOException) {
            logger.e(formatException(e, "close"))
        }
    }
    return null
}

fun compressImage(
        imageFile: File?,
        compressedImageFile: File?,
        maxSize: Long,
        maxRetries: Int,
        qualityDecrementStep: Int,
        config: Config,
        format: CompressFormat
): File? {
    var compressedImageFile = compressedImageFile
    if (maxSize < 0) {
        logger.e("Incorrect maxSize: $maxSize")
        return null
    }
    if (maxRetries < 0) {
        logger.e("Incorrect maxRetries: maxRetries$maxRetries")
        return null
    }
    if (qualityDecrementStep <= 0) {
        logger.e("Incorrect qualityDecrementStep: $qualityDecrementStep")
        return null
    }
    var result = imageFile != null && canDecodeImage(imageFile)
    if (result && maxSize > 0) {
        var currentLength = imageFile!!.length()
        result = currentLength in 1..maxSize
        if (!result && currentLength > 0 && maxRetries > 0) {
            if (checkFile(compressedImageFile, false)) {
                val bm = createBitmapFromFile(imageFile, 1, config, false)
                if (bm != null) {
                    deleteFile(compressedImageFile)
                    var currentTry = 0
                    var currentQuality = 100
                    while (currentLength > maxSize && currentTry <= maxRetries && currentQuality > 0) {
                        compressedImageFile = compressBitmapToFile(compressedImageFile, bm, format, currentQuality)
                        currentLength = compressedImageFile?.length() ?: 0
                        currentTry++
                        currentQuality = 100 - currentTry * qualityDecrementStep
                    }
                    result = currentLength in 1..maxSize
                }
            }
        }
    }
    return if (result) {
        if (isFileValid(compressedImageFile)) compressedImageFile else imageFile
    } else {
        imageFile
    }
}

@JvmOverloads
fun fixFontSize(
        bitmap: Bitmap?,
        fontSize: Int,
        text: String,
        paint: Paint?,
        scale: Double = 0.01
): Int {
    if (bitmap == null || !isBitmapValid(bitmap)) {
        logger.e("Incorrect bitmap: $bitmap")
        return 0
    }
    if (paint == null) {
        logger.e("paint is null")
        return 0
    }
    if (isEmpty(text)) {
        logger.e("text is empty")
        return 0
    }
    val textRect = Rect()
    paint.getTextBounds(text, 0, text.length, textRect)
    val bitmapWidth = bitmap.width
    val divider = if (textRect.width() >= bitmapWidth - 4) {
        2
    } else {
        1
    }
    return if (fontSize <= 0) {
        (bitmapWidth * scale).toInt() / divider
    } else {
        fontSize / divider
    }
}

@JvmOverloads
fun cropBitmap(
        bitmap: Bitmap?,
        fromX: Int,
        fromY: Int,
        toX: Int,
        toY: Int,
        config: Config? = null,
        recycleSource: Boolean = true
): Bitmap? {
    if (bitmap == null || !isBitmapValid(bitmap)) {
        logger.e("Incorrect bitmap: $bitmap")
        return null
    }
    if (fromX < 0
            || fromY < 0
            || toX <= 0
            || toY <= 0
            || toX <= fromX
            || toY <= fromY) {
        logger.e("Incorrect coords")
        return null
    }
    val rectWidth = toX - fromX
    val rectHeight = toY - fromY
    val overlayBitmap = createBitmapSafe(rectWidth, rectHeight, config ?: bitmap.config)
            ?: return null
    val p = Paint()
    p.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    return try {
        val canvas = Canvas(overlayBitmap)
        canvas.drawBitmap(bitmap, 0f, 0f, null)
        canvas.drawRect(fromX.toFloat(), fromY.toFloat(), toX.toFloat(), toY.toFloat(), p)
        overlayBitmap
    } catch (e: Throwable) {
        logger.e(formatException(e))
        null
    } finally {
        if (recycleSource) {
            bitmap.recycle()
        }
    }
}

@JvmOverloads
fun rotateBitmap(
        bitmap: Bitmap?,
        angle: Int,
        filter: Boolean = true,
        recycleSource: Boolean = true
): Bitmap? {
    if (bitmap == null || !isBitmapValid(bitmap)) {
        logger.e("Incorrect bitmap: $bitmap")
        return null
    }
    if (angle < 0 || angle > 360) {
        logger.e("Incorrect angle: $angle")
        return null
    }
    val matrix = Matrix()
    matrix.postRotate(angle.toFloat())
    return try {
        createBitmapSafe(bitmap, matrix = matrix, filter = filter)
    } finally {
        if (recycleSource) {
            bitmap.recycle()
        }
    }
}

@JvmOverloads
fun mirrorBitmap(
        bitmap: Bitmap?,
        filter: Boolean = true,
        recycleSource: Boolean = true
): Bitmap? {
    if (bitmap == null || !isBitmapValid(bitmap)) {
        logger.e("Incorrect bitmap: $bitmap")
        return null
    }
    val matrix = Matrix()
    matrix.preScale(-1f, 1f)
    return try {
        createBitmapSafe(bitmap, matrix = matrix, filter = filter)
    } finally {
        if (recycleSource) {
            bitmap.recycle()
        }
    }
}

@JvmOverloads
fun cutBitmap(
        bitmap: Bitmap?,
        range: Rect,
        filter: Boolean = true,
        recycleSource: Boolean = true
): Bitmap? {
    if (bitmap == null || !isBitmapValid(bitmap)) {
        logger.e("Incorrect bitmap: $bitmap")
        return null
    }
    if (range.left < 0
            || range.left >= bitmap.width
            || range.top < 0
            || range.top >= bitmap.height
            || range.width() > bitmap.width - range.left
            || range.height() > bitmap.height - range.top) {
        logger.e("Incorrect bounds: $range")
        return null
    }
    return try {
        createBitmapSafe(
                bitmap,
                range.left,
                range.top,
                range.width(),
                range.height(),
                null,
                filter
        )
    } finally {
        if (recycleSource) {
            bitmap.recycle()
        }
    }
}

fun curveImage(bitmap: Bitmap?, corners: Point): Bitmap? {
    if (bitmap == null || !isBitmapValid(bitmap)) {
        logger.e("Incorrect bitmap: $bitmap")
        return null
    }

    // Bitmap myCoolBitmap = ... ; // <-- Your bitmap you
    // want rounded
    val w = bitmap.width
    val h = bitmap.height
    val c = bitmap.config

    // We have to make sure our rounded corners have an
    // alpha channel in most cases
    val rounder = createBitmapSafe(w,h,c) ?: return null

    // We're going to apply this paint eventually using a
    // porter-duff xfer mode.
    // This will allow us to only overwrite certain pixels.
    // RED is arbitrary. This
    // could be any color that was fully opaque (alpha =
    // 255)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = Color.RED

    try {
        val canvas = Canvas(rounder)
        // We're just reusing xferPaint to paint a normal
        // looking rounded box, the 20.f
        // is the amount we're rounding by.
        canvas.drawRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), corners.x.toFloat(), corners.y.toFloat(), paint)

        // Now we apply the 'magic sauce' to the paint
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        val result = createBitmapSafe(w,h,c) ?: return null
        val resultCanvas = Canvas(result)
        resultCanvas.drawBitmap(bitmap, 0f, 0f, null)
        resultCanvas.drawBitmap(rounder, 0f, 0f, paint)
        return result
    } catch (e: Throwable) {
        logger.e(formatException(e))
        return null
    }
}

@JvmOverloads
fun writeTextOnBitmap(
        bitmap: Bitmap?,
        text: String,
        @ColorInt textColor: Int,
        fontSize: Int = 0,
        textPos: Point? = null
): Bitmap? {
    if (bitmap == null || !isBitmapValid(bitmap)) {
        logger.e("Incorrect bitmap: $bitmap")
        return null
    }
    if (!bitmap.isMutable) {
        logger.e("bitmap $bitmap is immutable")
        return null
    }
    if (isEmpty(text)) {
        logger.e("text is null or empty")
        return null
    }
    val paint = Paint()
    paint.style = Paint.Style.FILL
    paint.color = textColor
    paint.typeface = Typeface.create("", Typeface.BOLD)
    paint.textAlign = Paint.Align.CENTER
    try {
        val canvas = Canvas(bitmap)
        paint.textSize = fixFontSize(bitmap, fontSize, text, paint).toFloat()
        val xPos: Int
        val yPos: Int
        if (!(textPos != null && textPos.x >= 0 && textPos.y >= 0)) {
            xPos = (canvas.width * 0.05).toInt()
            yPos = canvas.height - (canvas.height * 0.01).toInt()
        } else {
            xPos = textPos.x
            yPos = textPos.y
        }
        canvas.drawText(text, xPos.toFloat(), yPos.toFloat(), paint)
        return bitmap
    } catch (e: Throwable) {
        logger.e(formatException(e))
        return null
    }
}

fun createPreviewFromVideoFile(
        videoFile: File,
        gridSize: Int,
        writeDuration: Boolean
): Bitmap? {
    if (!canDecodeVideo(videoFile)) {
        logger.e("Incorrect video file: '$videoFile'")
        return null
    }
    if (gridSize <= 0) {
        logger.e("incorrect grid size: $gridSize")
        return null
    }
    val videoFrames = extractFramesFromFile(videoFile, gridSize * gridSize)
    if (videoFrames.isEmpty()) {
        logger.w("no extracted video frames")
        return null
    }
    val resultImage = combineImagesToOne(videoFrames.values, gridSize, true)
    return if (writeDuration) {
        writeTextOnBitmap(resultImage, "duration: " + extractMediaDurationFromFile(videoFile) + " ms", Color.WHITE)
    } else {
        resultImage
    }
}

/**
 * @param gridSize number of width or height chunks in result image
 */
@JvmOverloads
fun combineImagesToOne(
        chunkImages: Collection<Bitmap?>?,
        gridSize: Int,
        recycleSource: Boolean = true
): Bitmap? {
    if (chunkImages == null || chunkImages.isEmpty()) {
        logger.e("chunkImages is null or empty")
        return null
    }
    if (gridSize <= 0) {
        logger.e("Incorrect gridSize: $gridSize")
        return null
    }
    val chunkImagesList = chunkImages.toMutableList()
    if (gridSize * gridSize < chunkImagesList.size) {
        logger.w("Grid dimension is less than number of chunks, removing excessive chunks...")
        var i = chunkImagesList.size - 1
        while (i > gridSize * gridSize - 1) {
            val b: Bitmap? = chunkImagesList.removeAt(i)
            if (recycleSource && b != null) {
                b.recycle()
            }
            i = chunkImagesList.size - 1
            i--
        }
    }
    var chunkWidth = 0
    var chunkHeight = 0
    var index = 0
    while (index < chunkImages.size) {
        val chunk = chunkImagesList[index]
        if (chunk != null) {
            if (chunkWidth > 0 && chunkHeight > 0) {
                if (chunk.width != chunkWidth || chunk.height != chunkHeight) {
                    logger.e("Chunk images in list have different dimensions, previous: " + chunkWidth + "x" + chunkHeight
                            + ", current: " + chunk.width + "x" + chunk.height)
                    return null
                }
            } else {
                chunkWidth = chunk.width
                chunkHeight = chunk.height
            }
        } else {
            logger.e("chunk at index $index is null")
            chunkImagesList.removeAt(index)
            index = 0
        }
        index++
    }
    logger.d("Chunk: $chunkWidth x $chunkHeight")
    if (chunkWidth <= 0 || chunkHeight <= 0) {
        logger.e("Incorrect chunk dimensions")
        return null
    }

    // create a bitmap of a size which can hold the complete image after merging
    val resultBitmap: Bitmap
    resultBitmap = createBitmapSafe(chunkWidth * gridSize, chunkHeight * gridSize, Config.ARGB_8888) ?: return null

    // create a canvas for drawing all those small images
    try {
        val canvas = Canvas(resultBitmap)
        var counter = 0
        for (rows in 0 until gridSize) {
            for (cols in 0 until gridSize) {
                if (counter >= chunkImagesList.size) {
                    continue
                }
                val image = chunkImagesList[counter]
                if (image != null) {
                    canvas.drawBitmap(image, (chunkWidth * cols).toFloat(), (chunkHeight * rows).toFloat(), null)
                    if (recycleSource) {
                        image.recycle()
                    }
                }
                counter++
            }
        }
    } catch (e: Throwable) {
        logger.e(formatException(e))
    }
    return resultBitmap
}

fun convertYuvToJpeg(data: ByteArray?, format: Int, width: Int, height: Int): ByteArray? {
    if (data == null || data.isEmpty()) {
        logger.e("data is null or empty")
        return null
    }
    if (format != ImageFormat.NV21 && format != ImageFormat.YUY2) {
        logger.e("Incorrect format: $format")
        return null
    }
    if (width <= 0 || height <= 0) {
        logger.e("Incorrect size: $width x $height")
        return null
    }
    return try {
        val yuvImg = YuvImage(data, format, width, height, null)
        val byteOutStream = ByteArrayOutputStream()
        yuvImg.compressToJpeg(Rect(0, 0, width, height), 100, byteOutStream)
        byteOutStream.toByteArray()
    } catch (e: Throwable) {
        formatException(e)
        null
    }
}

fun convertRgbToYuv420SP(aRGB: IntArray?, width: Int, height: Int): ByteArray? {
    if (aRGB == null || aRGB.isEmpty()) {
        logger.e("data is null or empty")
        return null
    }
    if (width <= 0 || height <= 0) {
        logger.e("Incorrect size: $width x $height")
        return null
    }
    val frameSize = width * height
    val chromaSize = frameSize / 4
    var yIndex = 0
    var uIndex = frameSize
    var vIndex = frameSize + chromaSize
    val yuv = ByteArray(width * height * 3 / 2)
    var R: Int
    var G: Int
    var B: Int
    var Y: Int
    var U: Int
    var V: Int
    var index = 0
    for (j in 0 until height) {
        for (i in 0 until width) {

            // a = (aRGB[index] & 0xff000000) >> 24; //not using it right now
            R = aRGB[index] and 0xff0000 shr 16
            G = aRGB[index] and 0xff00 shr 8
            B = aRGB[index] and 0xff shr 0
            Y = (66 * R + 129 * G + 25 * B + 128 shr 8) + 16
            U = (-38 * R - 74 * G + 112 * B + 128 shr 8) + 128
            V = (112 * R - 94 * G - 18 * B + 128 shr 8) + 128
            yuv[yIndex++] = (if (Y < 0) 0 else if (Y > 255) 255 else Y).toByte()
            if (j % 2 == 0 && index % 2 == 0) {
                yuv[uIndex++] = (if (U < 0) 0 else if (U > 255) 255 else U).toByte()
                yuv[vIndex++] = (if (V < 0) 0 else if (V > 255) 255 else V).toByte()
            }
            index++
        }
    }
    return yuv
}

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
fun renderScriptNVToRGBA(
        context: Context,
        width: Int,
        height: Int,
        nv: ByteArray?,
        config: Config = Config.ARGB_8888
): Bitmap? {
    if (width <= 0 || height <= 0) {
        logger.e("Incorrect size: $width x $height")
        return null
    }
    if (nv == null || nv.isEmpty()) {
        logger.e("Incorrect source: $nv")
        return null
    }
    var rs: RenderScript? = null
    var yuvToRgbIntrinsic: ScriptIntrinsicYuvToRGB? = null
    var `in`: Allocation? = null
    var out: Allocation? = null
    try {
        rs = RenderScript.create(context)
        yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs))
        val yuvType = Type.Builder(rs, Element.U8(rs)).setX(nv.size)
        `in` = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT)
        val element = when (config) {
            Config.ALPHA_8 -> Element.A_8(rs)
            Config.ARGB_4444 -> Element.RGBA_4444(rs)
            Config.RGB_565 -> Element.RGB_565(rs)
            else -> Element.RGBA_8888(rs)
        }
        val rgbaType = Type.Builder(rs, element).setX(width).setY(height)
        out = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_SCRIPT)
        `in`.copyFrom(nv)
        yuvToRgbIntrinsic.setInput(`in`)
        yuvToRgbIntrinsic.forEach(out)
        val resultBitmap: Bitmap = createBitmapSafe(width, height, config) ?: return null
        try {
            out.copyTo(resultBitmap)
            return resultBitmap
        } catch (e: Throwable) {
            logger.e(formatException(e, "copyTo"))
        }
    } catch (e: Exception) {
        logger.e(formatException(e))
    } finally {
        try {
            out?.destroy()
            `in`?.destroy()
            yuvToRgbIntrinsic?.destroy()
            rs?.destroy()
        } catch (e: Exception) {
            logger.e(formatException(e, "destroy"))
        }
    }
    return null
}

fun createBitmapSafe(
        width: Int,
        height: Int,
        config: Config
): Bitmap? {
    return try {
        Bitmap.createBitmap(width, height, config)
    } catch (e: Throwable) {
        logger.e(formatException(e, "createBitmap"))
        null
    }
}

fun createBitmapSafe(
        bitmap: Bitmap,
        x: Int = 0,
        y: Int = 0,
        width: Int = bitmap.width,
        height: Int = bitmap.height,
        matrix: Matrix? = null,
        filter: Boolean = true
): Bitmap? {
    return try {
        Bitmap.createBitmap(bitmap, x, y, width, height, matrix, filter)
    } catch (e: Throwable) {
        logger.e(formatException(e, "createBitmap"))
        null
    }
}

private fun calculateInSampleSize(
        options: BitmapFactory.Options?,
        reqWidth: Int,
        reqHeight: Int
): Int {
    if (reqWidth <= 0 || reqHeight <= 0) {
        return 0
    }
    if (options == null) {
        return 0
    }

    // Raw height and width of image
    val height = options.outHeight
    val width = options.outWidth
    var inSampleSize = 1
    if (height > reqHeight || width > reqWidth) {

        // Calculate ratios of height and width to requested height and width
        val heightRatio = (height.toFloat() / reqHeight).roundToInt()
        val widthRatio = (width.toFloat() / reqWidth).roundToInt()

        // Choose the smallest ratio as inSampleSize value, this will guarantee
        // a final image with both dimensions larger than or equal to the
        // requested height and width.
        inSampleSize = if (heightRatio < widthRatio) heightRatio else widthRatio
    }
    return inSampleSize
}

private fun calculateInSampleSizeHalf(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int): Int {
    val height = options.outHeight
    val width = options.outWidth
    var inSampleSize = 1
    if (height > reqHeight || width > reqWidth) {
        val halfHeight = height / 2
        val halfWidth = width / 2
        while (halfWidth / inSampleSize > reqWidth
                && halfHeight / inSampleSize > reqHeight) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}


private fun applyBitmapSampleOptions(
        options: BitmapFactory.Options,
        scale: Int,
        config: Config?,
        sampleSizeHalf: Boolean = true
) {
    val width = if (scale > 1) options.outWidth / scale else options.outWidth
    val height = if (scale > 1) options.outHeight / scale else options.outHeight
    options.inSampleSize = if (sampleSizeHalf) {
        calculateInSampleSizeHalf(options, width, height)
    } else {
        calculateInSampleSize(options, width, height)
    }
    options.inPurgeable = true
    options.inInputShareable = true
    options.inJustDecodeBounds = false
    options.inPreferredConfig = config ?: Config.ARGB_8888
}