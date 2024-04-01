package net.maxsmr.commonutils

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Resources
import android.content.res.TypedArray
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.graphics.drawable.StateListDrawable
import android.util.TypedValue
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import androidx.annotation.XmlRes
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.DrawableCompat
import net.maxsmr.commonutils.*
import net.maxsmr.commonutils.ReflectionUtils.invokeMethod
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder.Companion.formatException
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder.Companion.throwRuntimeException
import java.io.File
import java.io.IOException
import java.io.InputStream

const val EMPTY_ID = 0
const val INVALID_ATTRIBUTE = 0
const val INVALID_COLOR = -1

private val logger = BaseLoggerHolder.instance.getLogger<BaseLogger>("ResourceUtils")

/**
 * проверить факт существования ресурса с указанным [resId]
 */
fun isResourceExists(resources: Resources, resId: Int) = getResourceName(resources, resId) != null

/**
 * @return имя ресурса с указанным [resId], если таковой существует
 */
fun getResourceName(resources: Resources, resId: Int): String? =
        try {
            resources.getResourceName(resId)
        } catch (ignore: Resources.NotFoundException) {
            null
        }

/**
 * проверить факт существования ресурса с указанными именем и типом
 */
fun isResourceIdentifierExists(
        context: Context,
        resName: String,
        type: String
) = getResourceIdentifier(context, resName, type) != 0

/**
 * @return идентификатор ресурса с указанными именем и типом, если таковой существует
 */
fun getResourceIdentifier(
        context: Context,
        resName: String,
        type: String
) = context.resources.getIdentifier(resName, type, context.packageName)

/**
 * Получить Id ресурса из [TypedArray] или null
 */
fun getResourceIdOrNull(typedArray: TypedArray, attributeValue: Int): Int? =
        typedArray.getResourceId(attributeValue, INVALID_ATTRIBUTE).also {
            if (it != INVALID_ATTRIBUTE) {
                return it
            }
            return null
        }

fun getColoredDrawable(
        resources: Resources,
        @DrawableRes icon: Int,
        @ColorInt color: Int,
        mode: PorterDuff.Mode = PorterDuff.Mode.SRC_IN
): Drawable? {
    ResourcesCompat.getDrawable(resources, icon, null)?.let {
        setDrawableColor(it, ColorStateList.valueOf(color), mode)
        return it
    }
    return null
}

/**
 * Выставить цветовой фильтр [ColorStateList] для [Drawable]
 */
fun setDrawableColor(
        drawable: Drawable,
        color: ColorStateList,
        mode: PorterDuff.Mode = PorterDuff.Mode.SRC_IN
) {
    if (isAtLeastLollipop()) {
        DrawableCompat.setTintList(drawable, color)
        DrawableCompat.setTintMode(drawable, mode)
    } else {
        drawable.setColorFilter(color.defaultColor, mode)
    }
}

@ColorInt
fun getColorFromAttrs(context: Context, attrs: IntArray): Int {
    val typedValue = TypedValue()
    val array = context.theme.obtainStyledAttributes(typedValue.data, attrs)
    try {
        return array.getColor(0, 0)
    } finally {
        array.recycle()
    }
}

fun getDimensionFromAttrs(context: Context, attrs: IntArray): Int {
    val array = context.theme
            .obtainStyledAttributes(attrs)
    try {
        return array.getDimension(0, 0f).toInt()
    } finally {
        array.recycle()
    }
}

/**
 * Получение высоты ActionBar из аттрибутов
 */
fun getActionBarHeight(context: Context): Int {
    val attrs = context.theme.obtainStyledAttributes(intArrayOf(android.R.attr.actionBarSize))
    val actionBarHeight = attrs.getDimension(0, 0f)
    try {
        return actionBarHeight.toInt()
    } finally {
        attrs.recycle()
    }
}

fun getStatusBarHeight(context: Context): Int {
    var result = 0
    val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
    if (resourceId > 0) {
        result = context.resources.getDimensionPixelSize(resourceId)
    }
    return result
}

fun getNavigationBarHeight(context: Context): Int {
    val resources = context.resources
    val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
    return if (resourceId > 0) {
        resources.getDimensionPixelSize(resourceId)
    } else 0
}


@JvmOverloads
fun readStringsFromAsset(
        context: Context,
        assetName: String,
        count: Int = 0,
        charsetName: String = CHARSET_DEFAULT
): List<String> = try {
    readStringsFromAssetOrThrow(context, assetName, count, charsetName)
} catch (e: RuntimeException) {
    logger.e(e)
    emptyList()
}

/**
 * Читает содержимое assetName в несколько строк
 */
@Throws(RuntimeException::class)
@JvmOverloads
fun readStringsFromAssetOrThrow(
        context: Context,
        assetName: String,
        count: Int = 0,
        charsetName: String = CHARSET_DEFAULT
): List<String> {
    return try {
        openAssetStreamOrThrow(context, assetName).readStringsOrThrow(count, charsetName = charsetName)
    } catch (e: IOException) {
        throw RuntimeException(formatException(e, "readStringsFromInputStream"), e)
    }
}

@JvmOverloads
fun readStringsFromRes(
        context: Context,
        @RawRes resId: Int,
        count: Int = 0,
        charsetName: String = CHARSET_DEFAULT
): List<String> = try {
    readStringsFromResOrThrow(context, resId, count, charsetName)
} catch (e: RuntimeException) {
    logger.e(e)
    emptyList()
}

/**
 * Читает содержимое ресурса resId в несколько строк
 */
@Throws(RuntimeException::class)
@JvmOverloads
fun readStringsFromResOrThrow(
        context: Context,
        @RawRes resId: Int,
        count: Int = 0,
        charsetName: String = CHARSET_DEFAULT
): List<String> {
    return try {
        openRawResourceOrThrow(context, resId).readStringsOrThrow(count, charsetName = charsetName)
    } catch (e: IOException) {
        throw RuntimeException(formatException(e, "readStringsFromInputStream"), e)
    }
}

@JvmOverloads
fun copyFromAssets(
        context: Context,
        assetName: String,
        targetFile: File?,
        rewrite: Boolean = true,
        notifier: IStreamNotifier? = null,
        buffSize: Int = DEFAULT_BUFFER_SIZE
) = try {
    copyFromAssetsOrThrow(context, assetName, targetFile, rewrite, notifier, buffSize)
    true
} catch (e: RuntimeException) {
    logger.e(e)
    false
}

@Throws(RuntimeException::class)
@JvmOverloads
fun copyFromAssetsOrThrow(
        context: Context,
        assetName: String,
        targetFile: File?,
        rewrite: Boolean = true,
        notifier: IStreamNotifier? = null,
        buffSize: Int = DEFAULT_BUFFER_SIZE
) {
    if (targetFile == null) {
        throw NullPointerException("targetFile is null")
    }
    createFileOrThrow(targetFile.name, targetFile.parent, rewrite)
    try {
        openAssetStreamOrThrow(context, assetName).copyStreamOrThrow(targetFile.openOutputStreamOrThrow(!rewrite), notifier, buffSize)
    } catch (e: IOException) {
        throwRuntimeException(e, "copyStream")
    }
}

@JvmOverloads
fun copyFromRawRes(
        context: Context,
        @RawRes resId: Int,
        targetFile: File?,
        rewrite: Boolean = true,
        notifier: IStreamNotifier? = null,
        buffSize: Int = DEFAULT_BUFFER_SIZE
) = try {
    copyFromRawResOrThrow(context, resId, targetFile, rewrite, notifier, buffSize)
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
fun copyFromRawResOrThrow(
        context: Context,
        @RawRes resId: Int,
        targetFile: File?,
        rewrite: Boolean = true,
        notifier: IStreamNotifier? = null,
        buffSize: Int = DEFAULT_BUFFER_SIZE
) {
    if (targetFile == null) {
        throw NullPointerException("targetFile is null")
    }
    createFileOrThrow(targetFile.name, targetFile.parent, rewrite)
    try {
        openRawResourceOrThrow(context, resId).copyStreamOrThrow(targetFile.openOutputStreamOrThrow(!rewrite), notifier, buffSize)
    } catch (e: IOException) {
        throwRuntimeException(e, "copyStream")
    }
}

fun openAssetStream(context: Context, assetName: String): InputStream? = try {
    openAssetStreamOrThrow(context, assetName)
} catch (e: RuntimeException) {
    logger.e(e)
    null
}

@Throws(RuntimeException::class)
fun openAssetStreamOrThrow(context: Context, assetName: String): InputStream = try {
    context.assets.open(assetName)
} catch (e: IOException) {
    throw RuntimeException(formatException(e, "open"), e)
}

fun openRawResource(context: Context, @RawRes resId: Int): InputStream? = try {
    openRawResourceOrThrow(context, resId)
} catch (e: RuntimeException) {
    logger.e(e)
    null
}

@Throws(RuntimeException::class)
fun openRawResourceOrThrow(context: Context, @RawRes resId: Int): InputStream = try {
    context.resources.openRawResource(resId)
} catch (e: IOException) {
    throw RuntimeException(formatException(e, "openRawResource"), e)
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

@ColorInt
fun getDefaultColor(c: ColorStateList?): Int =
        c?.defaultColor ?: 0


fun getDefaultDrawable(d: StateListDrawable?): Drawable? =
        if (d != null) getDrawableForState(d, 0) else null

fun getDrawableForState(stateListDrawable: StateListDrawable, vararg state: Int): Drawable? {
    val clazz = StateListDrawable::class.java
    val parameterTypes = arrayOf(IntArray::class.java)
    val index = invokeMethod<Int>(clazz,
            "getStateDrawableIndex",
            parameterTypes,
            stateListDrawable,
            state)
    return invokeMethod<Drawable>(clazz,
            "getStateDrawable",
            parameterTypes,
            stateListDrawable,
            index)
}

fun cloneDrawable(drawable: Drawable?): Drawable? {
    drawable?.let {
        val cloned = drawable.constantState?.newDrawable()
        return cloned?.mutate() // mutate() -> not affecting other instances, for e.g. after setting color filter
                ?: drawable.mutate()
    }
    return null
}
