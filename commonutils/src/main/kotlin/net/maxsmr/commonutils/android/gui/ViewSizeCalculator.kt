package net.maxsmr.commonutils.android.gui

import android.content.Context
import android.graphics.Point
import android.util.DisplayMetrics
import android.view.View
import android.view.WindowManager
import net.maxsmr.commonutils.data.Pair
import kotlin.math.roundToInt

fun getFixedViewSizeByDisplay(context: Context, targetSize: Point): Point {
    return getFixedViewSizeByDisplay(context, targetSize.x.toFloat() / targetSize.y)
}

fun getFixedViewSizeByDisplay(context: Context, targetScale: Float): Point {
    val display = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
    val metrics = DisplayMetrics()
    display.getMetrics(metrics)
    val screenSize = Point(metrics.widthPixels, metrics.heightPixels)
    return getFixedViewSize(targetScale, screenSize)
}

fun getFixedViewSize(targetSize: Point, view: View): Point {
    return getFixedViewSize(targetSize, Point(view.measuredWidth, view.measuredHeight))
}

fun getFixedViewSize(targetSize: Point, measuredViewSize: Point?): Point {
    return getFixedViewSize(targetSize, measuredViewSize, null)
}

fun getFixedViewSize(targetSize: Point, measuredViewSize: Point?, maxViewSize: Point?): Point {
    return getFixedViewSize(targetSize.x.toFloat() / targetSize.y, measuredViewSize, maxViewSize)
}

fun getFixedViewSize(targetScale: Float, measuredViewSize: Point?): Point {
    return getFixedViewSize(targetScale, measuredViewSize, null)
}

fun getFixedViewSize(targetScale: Float, measuredViewSize: Point?, maxViewSize: Point?): Point {
    var measuredViewSize = measuredViewSize
    require(targetScale >= 0) { "targetScale < 0" }
    val newViewSize = Point()
    if (targetScale == 0f) {
        return newViewSize
    }
    if (measuredViewSize == null) {
        measuredViewSize = Point()
    }
    require(!(measuredViewSize.x < 0 || measuredViewSize.y < 0)) { "incorrect view size: " + measuredViewSize.x + "x" + measuredViewSize.y }
    if (measuredViewSize.x == 0) {
        if (maxViewSize == null || maxViewSize.x <= 0) {
            return newViewSize
        }
        measuredViewSize.x = maxViewSize.x
    }
    if (measuredViewSize.y == 0) {
        if (maxViewSize == null || maxViewSize.y <= 0) {
            return newViewSize
        }
        measuredViewSize.y = maxViewSize.y
    }
    val viewScale = measuredViewSize.x.toFloat() / measuredViewSize.y
    if (viewScale <= targetScale) {
        newViewSize.x = measuredViewSize.x
        newViewSize.y = (newViewSize.x.toFloat() / targetScale).roundToInt()
    } else {
        newViewSize.y = measuredViewSize.y
        newViewSize.x = (newViewSize.y.toFloat() * targetScale).roundToInt()
    }
    return newViewSize
}

fun fixViewSize(targetSize: Point?, view: View?): Point {
    return fixViewSize(targetSize, view, null)
}

fun fixViewSize(targetScale: Float, view: View?): Point {
    return fixViewSize(targetScale, view, null)
}

fun fixViewSize(targetSize: Point?, view: View?, maxViewSize: Point?): Point {
    return fixViewSize(if (targetSize != null) targetSize.x.toFloat() / targetSize.y else 0f, view, maxViewSize)
}

fun fixViewSize(targetScale: Float, view: View?, maxViewSize: Point?): Point {
    var fixedSize = Point()
    fixedSize = if (view != null) {
        getFixedViewSize(targetScale, Point(view.measuredWidth, view.measuredHeight), maxViewSize)
    } else {
        return fixedSize
    }
    if (fixedSize.x > 0 && fixedSize.y > 0) {
        setViewSize(view, Pair(fixedSize.x, fixedSize.y))
    }
    return fixedSize
}

fun getAutoScaledSize(view: View, maxViewSize: Point?, fixedSize: Int): Point {
    var width = view.measuredWidth
    var height = view.measuredHeight
    width = if (width <= 0) maxViewSize?.x ?: 0 else width
    height = if (height <= 0) maxViewSize?.y ?: 0 else height
    return getAutoScaledSize(Point(width, height), fixedSize)
}

fun getAutoScaledSize(size: Point, fixedSize: Int): Point {
    return getAutoScaledSize(size.x.toFloat() / size.y, fixedSize)
}

fun getAutoScaledSize(scale: Float, fixedSize: Int): Point {
    return getScaledSize(scale, fixedSize, scale > 1.0f)
}

fun getScaledSize(view: View, maxViewSize: Point?, fixedSize: Int, isWidth: Boolean): Point {
    var width = view.measuredWidth
    var height = view.measuredHeight
    width = if (width <= 0) maxViewSize?.x ?: 0 else width
    height = if (height <= 0) maxViewSize?.y ?: 0 else height
    return getScaledSize(Point(width, height), fixedSize, isWidth)
}

fun getScaledSize(size: Point, fixedSize: Int, isWidth: Boolean): Point {
    require(!(size.x <= 0 || size.y <= 0)) { "incorrect size: " + size.x + "x" + size.y }
    return getScaledSize(size.x.toFloat() / size.y, fixedSize, isWidth)
}

fun getScaledSize(scale: Float, fixedSize: Int, isWidth: Boolean): Point {
    require(scale > 0) { "incorrect scale: $scale" }
    require(fixedSize > 0) { "incorrect fixedSize: $scale" }
    val result = Point()
    if (isWidth) {
        result.x = fixedSize
        result.y = (fixedSize / scale).toInt()
    } else {
        result.x = (fixedSize * scale).toInt()
        result.y = fixedSize
    }
    return result
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
    val result: Pair<Int, Int>
    result = if (width > height) {
        Pair(dividend, divider)
    } else {
        Pair(divider, dividend)
    }
    return result
}
