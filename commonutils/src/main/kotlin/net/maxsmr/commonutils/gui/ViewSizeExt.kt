package net.maxsmr.commonutils.gui

import android.util.Size
import android.view.View
import net.maxsmr.commonutils.asActivityOrThrow
import net.maxsmr.commonutils.getDisplaySize
import kotlin.math.roundToInt

fun View.scaleByContent(contentSize: Size?, displaySizeAsMax: Boolean): Size {
    return scaleByContent(
        contentSize,
        if (displaySizeAsMax) {
            asActivityOrThrow().getDisplaySize()
        } else {
            null
        }
    )
}

fun View.scaleByContent(contentScale: Float, displaySizeAsMax: Boolean): Size {
    return scaleByContent(
        contentScale,
        if (displaySizeAsMax) {
            asActivityOrThrow().getDisplaySize()
        } else {
            null
        }
    )
}

@JvmOverloads
fun View.scaleByContent(contentSize: Size?, maxViewSize: Size? = null): Size {
    return scaleByContent(if (contentSize != null) contentSize.width.toFloat() / contentSize.height else 0f, maxViewSize)
}

@JvmOverloads
fun View.scaleByContent(contentScale: Float, maxViewSize: Size? = null): Size {
    val result = getScaledSize(contentScale, Size(measuredWidth, measuredHeight), maxViewSize)
    return if (result.width > 0 && result.height > 0) {
        setSize(result)
        result
    } else {
        Size(0, 0)
    }
}

@JvmOverloads
fun View.getScaledSize(contentSize: Size, displaySizeAsMax: Boolean = true): Size {
    return getScaledSize(
        contentSize,
        Size(measuredWidth, measuredHeight),
        if (displaySizeAsMax) {
            asActivityOrThrow().getDisplaySize()
        } else {
            null
        }
    )
}

@JvmOverloads
fun getScaledSize(
    contentSize: Size,
    measuredViewSize: Size?,
    maxViewSize: Size? = null
): Size {
    return getScaledSize(
        contentSize.width.toFloat() / contentSize.height,
        measuredViewSize,
        maxViewSize
    )
}

@JvmOverloads
fun getScaledSize(
    contentScale: Float,
    measuredViewSize: Size?,
    maxViewSize: Size? = null
): Size {
    require(contentScale >= 0) { "contentScale < 0" }

    if (contentScale == 0f) {
        return Size(0, 0)
    }

    var measuredWidth = measuredViewSize?.width ?: 0
    var measuredHeight = measuredViewSize?.height ?: 0

    require(!(measuredWidth < 0 || measuredHeight < 0)) {
        "Incorrect view size: " + measuredWidth + "x" + measuredHeight
    }

    if (measuredWidth == 0) {
        if (maxViewSize == null || maxViewSize.width <= 0) {
            return Size(0, 0)
        }
        measuredWidth = maxViewSize.width
    }
    if (measuredHeight == 0) {
        if (maxViewSize == null || maxViewSize.height <= 0) {
            return Size(0, 0)
        }
        measuredHeight = maxViewSize.height
    }

    val viewScale = measuredWidth.toFloat() / measuredHeight
    return if (viewScale <= contentScale) {
        Size(measuredWidth, (measuredWidth.toFloat() / contentScale).roundToInt())
    } else {
        Size((measuredHeight * contentScale).roundToInt(), measuredHeight)
    }
}

fun getAspectRatioFor(width: Int, height: Int): Pair<Int, Int> {
    val aspectRatio = width.toDouble() / height.toDouble()
    var dividend = if (width > height) width else height
    var divider = if (width > height) height else width
    var scale = 2
    while (scale <= 9) {
        val scaledDividend = dividend.toDouble() / scale.toDouble()
        val scaledDivider = divider.toDouble() / scale.toDouble()
        val diff1 = scaledDividend.toInt() - scaledDividend
        val diff2 = scaledDivider.toInt() - scaledDivider
        if (diff1 == 0.0 && diff2 == 0.0) {
            dividend = scaledDividend.toInt()
            divider = scaledDivider.toInt()
            scale = 2
        } else {
            scale++
        }
    }
    return if (width > height) {
        Pair(dividend, divider)
    } else {
        Pair(divider, dividend)
    }
}
