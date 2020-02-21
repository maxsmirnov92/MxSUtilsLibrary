package net.maxsmr.commonutils.android

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Resources
import android.content.res.TypedArray
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.util.TypedValue
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.content.res.ResourcesCompat
import net.maxsmr.commonutils.R
import net.maxsmr.commonutils.android.gui.setColor

const val EMPTY_ID = 0
const val INVALID_ATTRIBUTE = 0
const val INVALID_COLOR = -1

/**
 * проверить факт существования ресурса с указанным [resId]
 */
fun isResourceExists(resources: Resources, resId: Int) = getResourceNameNoThrow(resources, resId) != null

/**
 * @return имя ресурса с указанным [resId], если таковой существует
 */
fun getResourceNameNoThrow(resources: Resources, resId: Int): String? =
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

fun getColorFilteredDrawable(
        resources: Resources,
        @DrawableRes icon: Int,
        @ColorInt color: Int,
        mode: PorterDuff.Mode = PorterDuff.Mode.SRC_IN
): Drawable? {
    ResourcesCompat.getDrawable(resources, icon, null)?.let { drawable ->
        drawable.setColor(ColorStateList.valueOf(color), mode)
        return drawable
    }
    return null
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
    val attrs = context.theme.obtainStyledAttributes(intArrayOf(R.attr.actionBarSize))
    val actionBarHeight = attrs.getDimension(0, 0f)
    try {
        return actionBarHeight.toInt()
    } finally {
        attrs.recycle()
    }
}