package net.maxsmr.commonutils

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.content.res.AssetManager
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.content.res.Resources
import android.content.res.TypedArray
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.graphics.drawable.StateListDrawable
import android.net.Uri
import android.util.TypedValue
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import androidx.annotation.XmlRes
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.DrawableCompat
import net.maxsmr.commonutils.*
import net.maxsmr.commonutils.ReflectionUtils.invokeMethod
import net.maxsmr.commonutils.gui.DiffOrientationEventListener
import net.maxsmr.commonutils.gui.getCorrectedDisplayRotation
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder.Companion.formatException
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder.Companion.throwRuntimeException
import net.maxsmr.commonutils.stream.IStreamNotifier
import net.maxsmr.commonutils.stream.copyStreamOrThrow
import net.maxsmr.commonutils.stream.readStringOrThrow
import net.maxsmr.commonutils.stream.readStringsOrThrow
import java.io.File
import java.io.InputStream
import java.nio.charset.Charset

const val EMPTY_ID = 0
const val INVALID_ATTRIBUTE = 0
const val INVALID_COLOR = -1

private val logger = BaseLoggerHolder.instance.getLogger<BaseLogger>("ResourceUtils")

/**
 * проверить факт существования ресурса с указанным [resId]
 */
fun Resources.isResourceExists(resId: Int) = getResourceNameOrNull(resId) != null

/**
 * @return имя ресурса с указанным [resId], если таковой существует
 */
fun Resources.getResourceNameOrNull(resId: Int): String? =
    try {
        getResourceName(resId)
    } catch (ignore: Resources.NotFoundException) {
        null
    }

/**
 * проверить факт существования ресурса с указанными именем и типом
 */
fun Context.isResourceIdentifierExists(
    resName: String,
    type: String
) = getResourceIdentifier(resName, type) != 0

/**
 * @return идентификатор ресурса с указанными именем и типом, если таковой существует
 */
fun Context.getResourceIdentifier(
    resName: String,
    type: String
) = resources.getIdentifier(resName, type, packageName)

/**
 * Получить Id ресурса из [TypedArray] или null
 */
fun TypedArray.getResourceIdOrNull(attributeValue: Int): Int? =
    getResourceId(attributeValue, INVALID_ATTRIBUTE).also {
        if (it != INVALID_ATTRIBUTE) {
            return it
        }
        return null
    }

fun Resources.getColoredDrawable(
    @DrawableRes icon: Int,
    @ColorInt color: Int,
    mode: PorterDuff.Mode = PorterDuff.Mode.SRC_IN
): Drawable? {
    ResourcesCompat.getDrawable(this, icon, null)?.let {
        it.setColor(ColorStateList.valueOf(color), mode)
        return it
    }
    return null
}

/**
 * Выставить цветовой фильтр [ColorStateList] для [Drawable]
 */
fun Drawable.setColor(
    color: ColorStateList,
    mode: PorterDuff.Mode = PorterDuff.Mode.SRC_IN
) {
    if (isAtLeastLollipop()) {
        DrawableCompat.setTintList(this, color)
        DrawableCompat.setTintMode(this, mode)
    } else {
        setColorFilter(color.defaultColor, mode)
    }
}

@ColorInt
fun Context.getColorFromAttrs(attrs: IntArray): Int {
    val typedValue = TypedValue()
    val array = theme.obtainStyledAttributes(typedValue.data, attrs)
    try {
        return array.getColor(0, 0)
    } finally {
        array.recycle()
    }
}

fun Context.getDimensionFromAttrs(attrs: IntArray): Int {
    val array = theme.obtainStyledAttributes(attrs)
    try {
        return array.getDimension(0, 0f).toInt()
    } finally {
        array.recycle()
    }
}

/**
 * Получение высоты ActionBar из аттрибутов
 */
fun Context.getActionBarHeight(): Int {
    val attrs = theme.obtainStyledAttributes(intArrayOf(android.R.attr.actionBarSize))
    val actionBarHeight = attrs.getDimension(0, 0f)
    try {
        return actionBarHeight.toInt()
    } finally {
        attrs.recycle()
    }
}

@SuppressLint("InternalInsetResource")
fun Resources.getStatusBarHeight(): Int {
    var result = 0
    val resourceId = getIdentifier("status_bar_height", "dimen", "android")
    if (resourceId > 0) {
        result = getDimensionPixelSize(resourceId)
    }
    return result
}

@SuppressLint("InternalInsetResource")
fun Resources.getNavigationBarHeight(): Int {
    val resourceId = getIdentifier("navigation_bar_height", "dimen", "android")
    return if (resourceId > 0) {
        getDimensionPixelSize(resourceId)
    } else {
        0
    }
}

@JvmOverloads
fun AssetManager.readStringsFromAsset(
    assetName: String,
    count: Int = 0,
    charsetName: String = Charset.defaultCharset().name()
): List<String> = try {
    readStringsFromAssetOrThrow(assetName, count, charsetName)
} catch (e: RuntimeException) {
    logger.e(e)
    emptyList()
}

/**
 * Читает содержимое assetName в несколько строк
 */
@Throws(RuntimeException::class)
@JvmOverloads
fun AssetManager.readStringsFromAssetOrThrow(
    assetName: String,
    count: Int = 0,
    charsetName: String = Charset.defaultCharset().name()
): List<String> {
    return try {
        open(assetName).readStringsOrThrow(count, charsetName = charsetName)
    } catch (e: Exception) {
        throw RuntimeException(formatException(e, "readStrings from InputStream"), e)
    }
}

@JvmOverloads
fun AssetManager.readStringFromAsset(
    assetName: String,
    charsetName: String = Charset.defaultCharset().name()
): String? {
    return try {
        readStringFromAssetOrThrow(assetName, charsetName)
    } catch (e: RuntimeException) {
        logger.e(e)
        null
    }
}

@Throws(RuntimeException::class)
@JvmOverloads
fun AssetManager.readStringFromAssetOrThrow(
    assetName: String,
    charsetName: String = Charset.defaultCharset().name()
): String {
    return try {
        open(assetName).readStringOrThrow(charsetName = charsetName)
    } catch (e: Exception) {
        throw RuntimeException(formatException(e, "readString from InputStream"), e)
    }
}

@JvmOverloads
fun Resources.readStringsFromRes(
    @RawRes resId: Int,
    count: Int = 0,
    charsetName: String = Charset.defaultCharset().name()
): List<String> = try {
    readStringsFromResOrThrow(resId, count, charsetName)
} catch (e: RuntimeException) {
    logger.e(e)
    emptyList()
}

/**
 * Читает содержимое ресурса resId в несколько строк
 */
@Throws(RuntimeException::class)
@JvmOverloads
fun Resources.readStringsFromResOrThrow(
    @RawRes resId: Int,
    count: Int = 0,
    charsetName: String = Charset.defaultCharset().name()
): List<String> {
    return try {
        openRawResource(resId).readStringsOrThrow(count, charsetName = charsetName)
    } catch (e: Exception) {
        throw RuntimeException(formatException(e, "readStrings from InputStream"), e)
    }
}

@JvmOverloads
fun Resources.readStringFromRes(
    @RawRes resId: Int,
    charsetName: String = Charset.defaultCharset().name()
): String? = try {
    readStringFromResOrThrow(resId, charsetName)
} catch (e: RuntimeException) {
    logger.e(e)
    null
}

@Throws(RuntimeException::class)
@JvmOverloads
fun Resources.readStringFromResOrThrow(
    @RawRes resId: Int,
    charsetName: String = Charset.defaultCharset().name()
): String {
    return try {
        openRawResource(resId).readStringOrThrow(charsetName = charsetName)
    } catch (e: Exception) {
        throw RuntimeException(formatException(e, "readString from InputStream"), e)
    }
}

@JvmOverloads
fun AssetManager.copyFromAssets(
    assetName: String,
    targetFile: File,
    rewrite: Boolean = true,
    notifier: IStreamNotifier? = null,
    buffSize: Int = DEFAULT_BUFFER_SIZE
) = try {
    copyFromAssetsOrThrow(assetName, targetFile, rewrite, notifier, buffSize)
    true
} catch (e: RuntimeException) {
    logger.e(e)
    false
}

@Throws(RuntimeException::class)
@JvmOverloads
fun AssetManager.copyFromAssetsOrThrow(
    assetName: String,
    targetFile: File,
    rewrite: Boolean = true,
    notifier: IStreamNotifier? = null,
    buffSize: Int = DEFAULT_BUFFER_SIZE
) {
    createFileOrThrow(targetFile.name, targetFile.parent, rewrite)
    try {
        open(assetName).copyStreamOrThrow(targetFile.openOutputStreamOrThrow(!rewrite), notifier, buffSize)
    } catch (e: Exception) {
        throwRuntimeException(e, "copyStream")
    }
}

@JvmOverloads
fun Resources.copyFromRawRes(
    @RawRes resId: Int,
    targetFile: File,
    rewrite: Boolean = true,
    notifier: IStreamNotifier? = null,
    buffSize: Int = DEFAULT_BUFFER_SIZE
) = try {
    copyFromRawResOrThrow(resId, targetFile, rewrite, notifier, buffSize)
} catch (e: RuntimeException) {
    logger.e(e)
    false
}

/**
 * Copies a raw resource file, given its ID to the given location
 *
 * @param context  context
 * @param mode file permissions (E.g.: "755")
 */
@Throws(RuntimeException::class)
@JvmOverloads
fun Resources.copyFromRawResOrThrow(
    @RawRes resId: Int,
    targetFile: File,
    rewrite: Boolean = true,
    notifier: IStreamNotifier? = null,
    buffSize: Int = DEFAULT_BUFFER_SIZE
) {
    createFileOrThrow(targetFile.name, targetFile.parent, rewrite)
    try {
        openRawResource(resId).copyStreamOrThrow(targetFile.openOutputStreamOrThrow(!rewrite), notifier, buffSize)
    } catch (e: Exception) {
        throwRuntimeException(e, "copyStream")
    }
}

fun AssetManager.openAssetStreamOrNull(assetName: String): InputStream? = try {
    open(assetName)
} catch (e: RuntimeException) {
    logger.e(e)
    null
}

fun Resources.openRawResourceOrNull(@RawRes resId: Int): InputStream? = try {
    openRawResource(resId)
} catch (e: Exception) {
    logger.e(e)
    null
}

/**
 * Converts pixel value to dp value
 */
fun Resources.convertPxToDp(px: Float): Float =
    px / displayMetrics.density

@JvmOverloads
fun Resources.convertAnyToPx(
    value: Float,
    unit: Int = TypedValue.COMPLEX_UNIT_DIP,
): Float {
    // OR simply px = value * density (if DIP)
    return if (value <= 0) {
        0f
    } else {
        TypedValue.applyDimension(unit, value, displayMetrics)
    }
}

fun Resources.getViewsRotationForDisplay(displayRotation: Int): Int {
    var result = 0
    val correctedDisplayRotation = if (displayRotation !in arrayOf(0, 90, 180, 270)) {
        getCorrectedDisplayRotation(displayRotation)
    } else {
        displayRotation
    }
    if (correctedDisplayRotation != DiffOrientationEventListener.ROTATION_NOT_SPECIFIED) {
        val currentOrientation = configuration.orientation
        if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            when (correctedDisplayRotation) {
                0 -> result = 270
                90 -> result = 180
                180 -> result = 90
                270 -> result = 0
            }
        } else {
            when (correctedDisplayRotation) {
                0 -> result = 0
                90 -> result = 270
                180 -> result = 180
                270 -> result = 90
            }
        }
    }
    return result
}

fun Context.getUriFromRawResource(@RawRes rawResId: Int): Uri {
    return Uri.Builder()
        .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
        .authority(packageName)
        .appendPath("$rawResId")
        .build()
}

fun createColorStateListFromRes(context: Context, @XmlRes res: Int): ColorStateList? {
    val parser = try {
        context.resources.getXml(res)
    } catch (e: Resources.NotFoundException) {
        logger.e(formatException(e, "getXml"))
        return null
    }
    return try {
        if (isAtLeastMarshmallow()) {
            ColorStateList.createFromXml(context.resources, parser, null)
        } else {
            @Suppress("DEPRECATION")
            ColorStateList.createFromXml(context.resources, parser)
        }
    } catch (e: Exception) {
        logger.e(formatException(e))
        null
    }
}

fun getDefaultDrawable(d: StateListDrawable?): Drawable? =
    if (d != null) getDrawableForState(d, 0) else null

fun getDrawableForState(stateListDrawable: StateListDrawable, vararg state: Int): Drawable? {
    val clazz = StateListDrawable::class.java
    val parameterTypes = arrayOf(IntArray::class.java)
    val index = invokeMethod<Int>(
        clazz,
        "getStateDrawableIndex",
        parameterTypes,
        stateListDrawable,
        state
    )
    return invokeMethod<Drawable>(
        clazz,
        "getStateDrawable",
        parameterTypes,
        stateListDrawable,
        index
    )
}

fun cloneDrawable(drawable: Drawable?): Drawable? {
    drawable?.let {
        val cloned = drawable.constantState?.newDrawable()
        return cloned?.mutate() // mutate() -> not affecting other instances, for e.g. after setting color filter
            ?: drawable.mutate()
    }
    return null
}
