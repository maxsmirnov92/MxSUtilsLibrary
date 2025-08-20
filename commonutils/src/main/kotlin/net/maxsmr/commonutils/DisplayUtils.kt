package net.maxsmr.commonutils

import android.content.Context
import android.graphics.Insets
import android.util.DisplayMetrics
import android.util.Size
import android.view.Display
import android.view.WindowInsets
import android.view.WindowManager
import android.view.WindowMetrics

fun Context.getDisplaySize(): Size {
    val windowManager = (getSystemService(Context.WINDOW_SERVICE) as WindowManager)
    return if (isAtLeastR()) {
        val windowMetrics: WindowMetrics = windowManager.currentWindowMetrics
        val insets: Insets = windowMetrics.windowInsets
            .getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())
        // insets.top -> statusBar, insets.bottom -> navigationBar
        Size(
            windowMetrics.bounds.width() - insets.left - insets.right,
            windowMetrics.bounds.height() - insets.top - insets.bottom
        )
    } else {
        val outMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(outMetrics)
        Size(outMetrics.widthPixels, outMetrics.heightPixels)
    }
}

fun Context.getDisplaySizeWithDensity(): Pair<Size, Float> {
    val windowManager = (getSystemService(Context.WINDOW_SERVICE) as WindowManager)
    return if (isAtLeastUpsideDownCake()) {
        val windowMetrics: WindowMetrics = windowManager.currentWindowMetrics
        val insets: Insets = windowMetrics.windowInsets
            .getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())
        Size(
            windowMetrics.bounds.width() - insets.left - insets.right,
            windowMetrics.bounds.height() - insets.top - insets.bottom
        ) to windowMetrics.density
    } else {
        val outMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(outMetrics)
        Size(outMetrics.widthPixels, outMetrics.heightPixels) to outMetrics.density
    }
}

fun Context.getDisplayCompat(): Display {
    return if (isAtLeastR()) {
        display
    } else {
        (getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
    }
}